package paymentgateway.config.oy;


public enum PaymentStatus {
    FAIL("failed"),
    FAILED("FAILED"),
    
    CLOSED("closed"),
	CANCELLED("CANCELLED"),

    COMPLETE("complete"),
	SUCCESSFUL("SUCCESSFUL");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getStatus() {
        return value;
    }
}


//created 	String 	Status that will be returned when the payment link is first created and user has not selected a payment method
//waiting_payment 	String 	Status that indicates that the user has selected a payment method
//expired 	String 	The payment link has expired
//charge_in_progress 	String 	Payment currently in processed
//failed 	String 	OTP for card payment method has been succesfully entered but payment is rejected or the selected payment channel has expired (but the link is still not yet expired)
//complete 	String 	Transaction has been succesfully completed
//closed 	String 	Payment link has been deleted