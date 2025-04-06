package vmj.messaging.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import vmj.hibernate.integrator.RepositoryUtil;
import vmj.messaging.Property;
import vmj.messaging.StateTransferMessage;

public class RabbitMQManager {
    private static RabbitMQManager instance;
    private final Connection publisherConnection;
    private final Connection consumerConnection;

    private final ThreadLocal<Channel> publisherChannelThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Channel> consumerChannelThreadLocal = new ThreadLocal<>();

    private final String BASE_EXCHANGE = System.getenv("base_exchange") != null ? System.getenv("base_exchange") : "base_exchange";
    private final String appId = System.getenv("app_id");

    private Map<String, RepositoryUtil> repositoryMap = new HashMap<>();

    private RabbitMQManager() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "localhost");
            factory.setUsername(System.getenv("RABBITMQ_USER") != null ? System.getenv("RABBITMQ_USER") : "guest");
            factory.setPassword(System.getenv("RABBITMQ_PASS") != null ? System.getenv("RABBITMQ_PASS") : "guest");

            this.publisherConnection = factory.newConnection();
            this.consumerConnection = factory.newConnection();

            getConsumerChannel().exchangeDeclare(BASE_EXCHANGE, "direct");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                RabbitMQManager.getInstance().closeConnections();
            }));

        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to connect with RabbitMQ server", e);
        }
    }

    public static RabbitMQManager getInstance() {
        if (instance == null) {
            synchronized (RabbitMQManager.class) {
                if (instance == null) {
                    instance = new RabbitMQManager();
                }
            }
        }
        return instance;
    }

    public Channel getPublisherChannel() throws IOException {
        Channel channel = publisherChannelThreadLocal.get();
        if (channel == null || !channel.isOpen()) {
            channel = publisherConnection.createChannel();
            publisherChannelThreadLocal.set(channel);
        }
        return channel;
    }

    public Channel getConsumerChannel() throws IOException {
        Channel channel = consumerChannelThreadLocal.get();
        if (channel == null || !channel.isOpen()) {
            channel = consumerConnection.createChannel();
            consumerChannelThreadLocal.set(channel);
        }
        return channel;
    }

    public void closeConnections() {
        try {
            if (publisherConnection != null) publisherConnection.close();
            if (consumerConnection != null) consumerConnection.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close RabbitMQ connection", e);
        } finally {
            publisherChannelThreadLocal.remove();
            consumerChannelThreadLocal.remove();
        }
    }

    public void bindQueue(String queueName, String routingKey)  {
        try {
            Channel channel = getConsumerChannel();

            boolean durable = true;
            boolean exclusive = false;
            boolean autoDelete = false;
            Map<String, Object> arguments = null;
            channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments);
            channel.queueBind(queueName, BASE_EXCHANGE, routingKey);
            consumeMessage(queueName);
        } catch (IOException e) {
            System.out.printf("Failed to create %s queue %s%n", queueName, e);
        }
    }

    public void publishMessage(String routingKey, StateTransferMessage message) {
        Gson gson = new Gson();
        String messageJson = gson.toJson(message);

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .appId(appId)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            getPublisherChannel().basicPublish(BASE_EXCHANGE, routingKey, props, messageJson.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + routingKey + "':'" + messageJson + "'");
        } catch (IOException e){
            System.out.println("Failed to publish catalog message");
        }
    }

    public void consumeMessage(String queueName) throws IOException {
        Channel channel = getConsumerChannel();
        if (channel == null) {
            System.out.println("Channel is null");
            return;
        }

        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (appId.equals(delivery.getProperties().getAppId())) {
                    System.out.println("Skipping own message...");
                    return;
                }

                String messageJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received JSON: " + messageJson);

                // 🔹 Transform JSON back into vmj.messaging.rabbitmq.StateTransferMessage
                Gson gson = new Gson();
                StateTransferMessage message = gson.fromJson(messageJson, StateTransferMessage.class);

                // 🔹 Debug output
                System.out.println("Deserialized Message:");
                System.out.println("ID: " + message.id());
                System.out.println("Type: " + message.type());
                System.out.println("Action: " + message.action());
                System.out.println("Properties:");
                for (Property property : message.properties()) {
                    System.out.println(" - " + property.varName() + " (" + property.type() + "): " + property.value());
                }

                switch (message.action()) {
                    case "create" -> createObjectHandler(message);
                    case "update" -> updateObjectHandler(message);
                    case "delete" -> deleteObjectHandler(message);
                    default -> System.out.println("Unsupported action: " + message.action());
                }


            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            System.out.println("Error while consuming message");
        }
    }

    private void createObjectHandler(StateTransferMessage message) {
        String domainInterface = message.type();

        String fqn = "";
        List<Object> arguments = new ArrayList<>();
        for (Property property : message.properties()) {
            if (property.varName().equals("fqn")){
                fqn = property.value().toString();
                continue;
            }
            arguments.add(parsingObject(property));
        }

    }

    private void updateObjectHandler(StateTransferMessage message) {
        Object domainObject;
        String domainInterface = message.type();
        if (message.tableName().isEmpty()){
            Object id = message.id();
            if (id instanceof UUID uuid) {
                domainObject = repositoryMap.get(domainInterface).getObject(uuid);
            } else  { // int
                int intId = (Integer) id;
                domainObject = repositoryMap.get(domainInterface).getObject(intId);
            }
        } else {
            String columnName =  domainInterface.substring(0, 1).toLowerCase() + domainInterface.substring(1) + "Id";
            Object id = message.id();
            if (id instanceof UUID uuid) {
                domainObject = repositoryMap.get(domainInterface).getListObject(message.tableName(),columnName,uuid).get(0);
            } else  { // int
                int intId = (Integer) id;
                domainObject = repositoryMap.get(domainInterface).getListObject(message.tableName(),columnName,intId).get(0);
            }
        }

        Map<String, Object> attributes = new HashMap<>();
        for (Property property : message.properties()) {
            String attributeName = property.varName();
            Object attributeValue = parsingObject(property);
            attributes.put(attributeName,attributeValue);
        }

        setAttributes(domainObject, attributes);
        repositoryMap.get(domainInterface).updateObject(domainObject);
    }

    private void deleteObjectHandler(StateTransferMessage message) {
        Object id = message.id();
        if (id instanceof UUID uuid) {
            repositoryMap.get(message.type()).deleteObject(uuid);
        } else  { // int
            int intId = (Integer) id;
            repositoryMap.get(message.type()).deleteObject(intId);
        }
    }

    private Object parsingObject(Property property){
        String varName = property.varName();
        String type = property.type();
        Object value = property.value();

        if (type.equals("Object")){
            if (!varName.toLowerCase().contains("id")){
                Object id = value;
                if (id instanceof UUID uuid) {
                    value = repositoryMap.get(type).getObject(uuid);
                } else  { // int
                    int intId = (Integer) id;
                    value = repositoryMap.get(type).getObject(intId);
                }
            }
        }
        else if (type.equals("UUID")){
            value = UUID.fromString(varName);
        } else if (type.equals("Date")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
                value = dateFormat.parse(varName);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        return value;
    }

    private void setAttributes(Object obj, Map<String, Object> attributes) {
        Class<?> clazz = obj.getClass();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            try {
                Field field = clazz.getDeclaredField(entry.getKey());
                field.setAccessible(true);

                Object value = entry.getValue();

                field.set(obj, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

}
