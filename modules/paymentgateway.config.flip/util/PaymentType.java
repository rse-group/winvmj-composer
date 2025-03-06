package paymentgateway.config.flip;

public enum PaymentType {
    SINGLE("SINGLE"),
    MULTIPLE("MULTIPLE");

    private final String value;

    PaymentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
