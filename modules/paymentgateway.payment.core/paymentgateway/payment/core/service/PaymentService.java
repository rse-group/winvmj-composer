package paymentgateway.payment.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface PaymentService {
	Payment createPayment(Map<String, Object> requestBody, int id);	
	Payment createPayment(Map<String, Object> requestBody);
    HashMap<String, Object> getPayment(Map<String, Object> requestBody);
    List<HashMap<String, Object>> getAllPayment(Map<String, Object> requestBody);
    List<HashMap<String, Object>> deletePayment(Map<String, Object> requestBody);
    HashMap<String, Object> updatePayment(Map<String, Object> requestBody);
    List<HashMap<String, Object>> transformListToHashMap(List<Payment> List);
    Map<String, Object> sendTransaction(Map<String, Object> requestBody);
    Map<String, Object> checkPaymentStatus(Map<String, Object> requestBody);
}
