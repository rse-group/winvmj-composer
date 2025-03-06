package paymentgateway.config.flip;

public enum PhoneNumberRequired{
    TRUE(1),
    FALSE(0);

    private final int value;

    PhoneNumberRequired(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
