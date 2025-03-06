package paymentgateway.config.flip;


public enum PaymentStatus {
	SUCCESSFUL("SUCCESSFUL"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    PENDING("PENDING");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getStatus() {
        return value;
    }
}
