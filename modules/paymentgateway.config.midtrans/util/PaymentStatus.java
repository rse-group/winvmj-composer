package paymentgateway.config.midtrans;


public enum PaymentStatus {
    FAIL("failed"),
    FAILED("FAILED"),

    CANCEL("cancel"),
	CANCELLED("CANCELLED"),

	SETTLEMENT("settlement"),
	SUCCESSFUL("SUCCESSFUL"),
	
	CAPTURE("capture");
    

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getStatus() {
        return value;
    }
}
