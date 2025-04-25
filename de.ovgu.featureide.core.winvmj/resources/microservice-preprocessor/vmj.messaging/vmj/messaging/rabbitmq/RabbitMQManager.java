package vmj.messaging.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import vmj.messaging.StateTransferMessage;
import vmj.messaging.MessageConsumeException;

public class RabbitMQManager {
    private static RabbitMQManager instance;
    private final Connection publisherConnection;
    private final Connection consumerConnection;

    private final ThreadLocal<Channel> publisherChannelThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Channel> consumerChannelThreadLocal = new ThreadLocal<>();

    private final String BASE_EXCHANGE = System.getenv("BASE_EXCHAGE") != null ? System.getenv("BASE_EXCHAGE") : "base_exchange";
    private final String APP_ID = System.getenv("APP_ID");

    private List<MessageConsumer> consumers = new ArrayList<>();

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

    public void bindQueue(String queueName, String routingKey) {
        try {
            Channel channel = getConsumerChannel();

            channel.addShutdownListener(cause -> {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); 
                        System.out.println("Attempting to re-bind queue: " + queueName);
                        bindQueue(queueName, routingKey); // re-bind queue
                    } catch (Exception ex) {
                        System.err.println("Failed to rebind queue '" + queueName + "': " + ex.getMessage());
                    }
                }).start();
            });

            boolean durable = true;
            boolean exclusive = false;
            boolean autoDelete = false;
            channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
            channel.queueBind(queueName, BASE_EXCHANGE, routingKey);

            consumeMessage(queueName); // call tanpa listener tambahan
            System.out.println("Queue bound & consuming: " + queueName);

        } catch (IOException e) {
            System.err.printf("Failed to bind or consume queue '%s': %s%n", queueName, e.getMessage());
        }
    }


    public void publishMessage(String routingKey, StateTransferMessage message) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                .create();
        String messageJson = gson.toJson(message);

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .appId(APP_ID)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            getPublisherChannel().basicPublish(BASE_EXCHANGE, routingKey, props, messageJson.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + routingKey + "':'" + messageJson + "'");
        } catch (IOException e){
            System.out.println("Failed to publish catalog message");
        }
    }

    public void addMessageConsumer(MessageConsumer consumer) {consumers.add(consumer);}
    public void removeMessageConsumer(MessageConsumer consumer) {consumers.remove(consumer);}

    public void consumeMessage(String queueName) throws IOException {
        Channel channel = getConsumerChannel();
        if (channel == null) {
            System.out.println("Channel is null");
            return;
        }
        
        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (APP_ID.equals(delivery.getProperties().getAppId())) {
                    System.out.println("Skipping own message...");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    return;
                }

                String messageJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received JSON: " + messageJson);

                // 🔹 Transform JSON back into vmj.messaging.rabbitmq.StateTransferMessage
                Gson gson = new Gson();
                StateTransferMessage message = gson.fromJson(messageJson, StateTransferMessage.class);

                try {
                    for (MessageConsumer consumer : consumers) {
                        consumer.consume(message);
                    }

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (MessageConsumeException e) {
                    System.err.println("Failed to consume message: " + e.getMessage());
                }

            };
            
            boolean autoAck = false;	
            channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            System.out.println("Error while consuming message");
        }
    }

}
