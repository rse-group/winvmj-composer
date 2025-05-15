package vmj.messaging;

import java.util.List;

public class StateTransferMessage {

    private Object id; // UUID or int
    private String type;
    private String action;
    private String tableName;
    private String moduleName;
    private List<Property> properties;

    public StateTransferMessage() {}

    public StateTransferMessage(Object id, String type, String action, String tableName, String moduleName, List<Property> properties) {
        this.id = id;
        this.type = type;
        this.action = action;
        this.tableName = tableName;
        this.moduleName = moduleName;
        this.properties = properties;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }
}

