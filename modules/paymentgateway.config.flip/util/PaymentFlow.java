package paymentgateway.config.flip;

public enum PaymentFlow {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    private final int value;

    PaymentFlow(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
