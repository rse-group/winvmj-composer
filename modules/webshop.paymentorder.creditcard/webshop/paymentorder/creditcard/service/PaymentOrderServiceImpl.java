package webshop.paymentorder.creditcard;
import java.util.*;
import java.time.Month;
import vmj.routing.route.exceptions.NotFoundException;
import vmj.routing.route.exceptions.FieldValidationException;

import webshop.paymentorder.*;
import webshop.paymentorder.core.*;
import webshop.order.core.*;
import paymentgateway.payment.core.PaymentServiceComponent;
import paymentgateway.payment.core.PaymentService;
import paymentgateway.payment.PaymentServiceFactory;
import paymentgateway.payment.core.Payment;


public class PaymentOrderServiceImpl extends PaymentOrderServiceDecorator {
	PaymentService creditcardPaymentService;
    public PaymentOrderServiceImpl(PaymentOrderServiceComponent record) {
        super(record);
		this.creditcardPaymentService = 
				PaymentServiceFactory.createPaymentService(
					"paymentgateway.payment.creditcard.PaymentServiceImpl", 
						PaymentServiceFactory.createPaymentService(
							"paymentgateway.payment.core.PaymentServiceImpl"));
    }
	public HashMap<String, Object> createPaymentOrder(HashMap<String, Object> body) {
    	String hostAddress = EnvUtilization.getEnvVariableHostAddress("AMANAH_HOST_BE");
        int portNum = EnvUtilization.getEnvVariablePortNumber("AMANAH_PORT_BE");
        String paymentStatus = "PENDING";
		String paymentId = "";
        
		Map<String, Object> requestData = new HashMap<>();
		if (!body.containsKey("orderId")) {
			throw new NotFoundException("Field 'orderId' not found in the request body.");
		}
		String orderIdStr = (String) body.get("orderId");
		UUID orderId = UUID.fromString(orderIdStr);
		Order order = orderRepository.getObject(orderId);
		int amount = order.getAmount();
		String amountStr = Integer.toString(amount);
		String cardNumber = (String) body.get("cardNumber");
		String cardExpMonth = (String) body.get("cardExpMonth");
		int cardExpMonthInt = Month.valueOf(cardExpMonth.toUpperCase()).getValue();
		String cardExpYear = (String) body.get("cardExpYear");
		String cardCvv = (String) body.get("cardCvv");
		String vendorName = (String) body.get("paymentGateway");

		requestData.put("amount", amountStr);
		requestData.put("card_number", cardNumber);
		requestData.put("card_exp_month", String.valueOf(cardExpMonthInt));
		requestData.put("card_exp_year", cardExpYear);
		requestData.put("card_cvv", cardCvv);
		requestData.put("vendor_name", vendorName);
		try {
        	Payment result = creditcardPaymentService.createPayment(requestData);
			Map<String, Object> dataMap = result.toHashMap();
			System.out.println(dataMap);
            paymentStatus = (String) dataMap.get("statusCreditPayment");
			paymentStatus = paymentStatus.equals("BERHASIL") ? "Success" : "Failed";
			int paymentIdInt = (int) dataMap.get("id");
			paymentId = String.valueOf(paymentIdInt);
			String paymentMethod = "Credit Card";
			record.createPaymentOrder(paymentId, paymentStatus, paymentMethod, order);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
			throw new FieldValidationException("An unexpected error occurred during payment processing. Please check your input and try again.");
        } 
	
		return order.toHashMap();
    }

}
