package webshop.paymentorder.ewallet;
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
	PaymentService ewalletPaymentService;
    public PaymentOrderServiceImpl (PaymentOrderServiceComponent record) {
        super(record);
		this.ewalletPaymentService = 
				PaymentServiceFactory.createPaymentService(
					"paymentgateway.payment.ewallet.PaymentServiceImpl", 
						PaymentServiceFactory.createPaymentService(
							"paymentgateway.payment.core.PaymentServiceImpl"));
    }
	public HashMap<String, Object> createPaymentOrder(HashMap<String, Object> body, String email) {
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
		String description = (String) body.get("description");
		String ewalletType = (String) body.get("ewalletType");
		String name = (String) body.get("name");
		String phone = (String) body.get("phone");
		String vendorName = (String) body.get("paymentGateway");

		
		requestData.put("amount", amountStr);
		requestData.put("title", description);
		requestData.put("email", email);
		requestData.put("ewallet_type", ewalletType);
		requestData.put("name", name);
		requestData.put("payment_method", "ewallet");
		requestData.put("phone", phone);
		requestData.put("vendor_name", vendorName);

		try {
        	Payment result = ewalletPaymentService.createPayment(requestData);
			Map<String, Object> dataMap = result.toHashMap();
			paymentStatus = "Success";
			int paymentIdInt = (int) dataMap.get("id"); 
			paymentId = String.valueOf(paymentIdInt);
			String paymentMethod = "EWallet";
			record.createPaymentOrder(paymentId, paymentStatus, paymentMethod, order);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
			throw new FieldValidationException("An unexpected error occurred during payment processing. Please check your input and try again.");
        } 
		return order.toHashMap();
    }

}
