import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import vmj.hibernate.integrator.RepositoryUtil;
import vmj.messaging.Property;
import vmj.messaging.StateTransferMessage;
import vmj.messaging.MessageConsumeException;
import vmj.messaging.rabbitmq.MessageConsumer;


class MessageConsumerImpl implements MessageConsumer {

    private Map<String, RepositoryUtil> repositoryMap = new HashMap<>();
    
    public MessageConsumerImpl() {}

    @Override
    public void consume(StateTransferMessage message) throws MessageConsumeException {
        try {
            // 🔹 Debug output
            System.out.println("Deserialized Message:");
            System.out.println("ID: " + message.getId());
            System.out.println("Type: " + message.getType());
            System.out.println("Action: " + message.getAction());
            System.out.println("Properties:");
            for (Property property : message.getProperties()) {
                System.out.println(" - " + property.getVarName() + " (" + property.getType() + "): " + property.getValue());
            }

            switch (message.getAction()) {
                case "create" -> createObjectHandler(message);
                case "update" -> updateObjectHandler(message);
                case "delete" -> deleteObjectHandler(message);
                default -> System.out.println("Unsupported action: " + message.getAction());
            }
        } catch (Exception e) {
            throw new MessageConsumeException("Failed to consume message", e);
        }
    }

    private void createObjectHandler(StateTransferMessage message) throws Exception {
        String domainInterface = message.getType();

        String fqn = "";
        List<Object> arguments = new ArrayList<>();
        for (Property property : message.getProperties()) {
            if (property.getVarName().equals("fqn")){
                fqn = property.getValue().toString();
                continue;
            }
            arguments.add(getTypedValue(property));
        }

    }

    private void updateObjectHandler(StateTransferMessage message) throws Exception {
        Object domainObject;
        String domainInterface = message.getType();
        String moduleName = message.getModuleName();
        Object id = message.getId();
        String idStr = id.toString();
        if (message.getTableName().isEmpty()){
            try {
                UUID uuid = UUID.fromString(idStr);
                domainObject = repositoryMap.get(domainInterface).getObject(uuid);
            } catch (IllegalArgumentException e) {
                int intId = ((Number) id).intValue();
                domainObject = repositoryMap.get(domainInterface).getObject(intId);
            }
            
            // Jika diakses bukan dari tableName maka model berasal dari core module
            int lastDotIndex = moduleName.lastIndexOf('.');
            if (lastDotIndex != -1) {
                moduleName = moduleName.substring(0, lastDotIndex) + ".core";
            }
        } else {
            String columnName =  domainInterface.substring(0, 1).toLowerCase() + domainInterface.substring(1) + "Id";
            try {
                UUID uuid = UUID.fromString(idStr);
                domainObject = repositoryMap.get(domainInterface).getListObject(message.getTableName(),columnName,uuid).get(0);
            } catch (IllegalArgumentException e) {
                int intId = ((Number) id).intValue();
                domainObject = repositoryMap.get(domainInterface).getListObject(message.getTableName(),columnName,intId).get(0);
            }
        }

        Map<String, Object> attributes = new HashMap<>();
        for (Property property : message.getProperties()) {
            String attributeName = property.getVarName();
            Object attributeValue = getTypedValue(property);
            attributes.put(attributeName,attributeValue);
        }
        String domainClassImpl = moduleName + "." + domainInterface + "Impl";

        setAttributes(domainObject, domainClassImpl, attributes);
        repositoryMap.get(domainInterface).updateObject(domainObject);
    }

    private void deleteObjectHandler(StateTransferMessage message) {
        Object id = message.getId();
        String idStr = id.toString();
        try {
            UUID uuid = UUID.fromString(idStr);
            repositoryMap.get(message.getType()).deleteObject(uuid);
        } catch (IllegalArgumentException e) {
            int intId = ((Number) id).intValue();
            repositoryMap.get(message.getType()).deleteObject(intId);
        }
    }

    private Object getTypedValue(Property property) throws ParseException {
        String varName = property.getVarName();
        String type = property.getType();
        Object value = property.getValue();
        if (value == null) {
            return null;
        }
        String valueStr = value.toString();

        if (repositoryMap.containsKey(type)){
        	try {
                UUID uuid = UUID.fromString(valueStr);
                value = repositoryMap.get(type).getObject(uuid);
            } catch (IllegalArgumentException e) {
                int intId = ((Number) value).intValue();
                value = repositoryMap.get(type).getObject(intId);
            }
        }
        else if (type.equals("UUID")){
            value = UUID.fromString(valueStr);
        } else if (type.equals("Date")) {
        	value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(valueStr);
        } else if (type.equals("boolean") || type.equals("Boolean")) {
            value = Boolean.parseBoolean(valueStr);
        } else if (type.equals("float") || type.equals("Float")) {
            value = Float.parseFloat(valueStr);
        } else if (type.equals("int") || type.equals("Integer")) {
            value = ((Number) value).intValue();
        } else if (type.equals("double") || type.equals("Double")) {
            value = Double.parseDouble(valueStr);
        }


        return value;
    }

    private void setAttributes(Object obj, String domainClassImpl, Map<String, Object> attributes) throws Exception	 {
        Class<?> clazz = obj.getClass();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        	Field field = getFieldFromHierarchy(clazz, entry.getKey());
            field.setAccessible(true);

            Object value = entry.getValue();

            field.set(obj, value);
        }
    }
    
    private Field getFieldFromHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass(); // traverse to parent class
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }
}
