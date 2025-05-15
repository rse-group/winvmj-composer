package vmj.messaging;

public class Property {

    private String varName;
    private String type;
    private Object value;

    public Property() {}

    public Property(String varName, String type, Object value) {
        this.varName = varName;
        this.type = type;
        this.value = value;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Property{" +
                "varName='" + varName + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                '}';
    }
}
