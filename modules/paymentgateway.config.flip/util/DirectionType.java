package paymentgateway.config.flip;

public enum DirectionType {
    DOMESTIC_TRANSFER("domestic_transfer"),
    DOMESTIC_SPECIAL_TRANSFER("domestic_special_transfer"),
    FOREIGN_INBOUND_SPECIAL_TRANSFER("foreign_inbound_special_transfer");

    private final String value;

    DirectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}