package paymentgateway.payment.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface PaymentResource {
	int callback(VMJExchange vmjExchange);
	HashMap<String, Object> getPayment(VMJExchange vmjExchange);
    List<HashMap<String, Object>> getAllPayment(VMJExchange vmjExchange);
    List<HashMap<String, Object>> deletePayment(VMJExchange vmjExchange);
    HashMap<String, Object> updatePayment(VMJExchange vmjExchange);
    HashMap<String, Object> payment(VMJExchange vmjExchange);
}
