package paymentgateway.payment.core;
import java.util.*;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

public abstract class PaymentServiceDecorator extends PaymentServiceComponent{
	protected PaymentServiceComponent record;

    public PaymentServiceDecorator(PaymentServiceComponent record) {
        this.record = record;
    }

    public Payment createPayment(Map<String, Object> requestBody, int id){
		return record.createPayment(requestBody, id);
	}
    
    public Payment createPayment(Map<String, Object> requestBody){
        return record.createPayment(requestBody);
    }

    public HashMap<String, Object> getPayment(Map<String, Object> requestBody){
        return record.getPayment(requestBody);
    }

    public List<HashMap<String, Object>> getAllPayment(Map<String, Object> requestBody){
        return record.getAllPayment(requestBody);
    }

    public List<HashMap<String, Object>> deletePayment(Map<String, Object> requestBody){
        return record.deletePayment(requestBody);
    }

    public HashMap<String, Object> updatePayment(Map<String, Object> requestBody){
        return record.updatePayment(requestBody);
    }

    public List<HashMap<String, Object>> transformListToHashMap(List<Payment> List){
        return record.transformListToHashMap(List);
    }
    
    public Map<String, Object> sendTransaction(Map<String, Object> requestBody){
        return record.sendTransaction(requestBody);
    }
     
    public Map<String, Object> checkPaymentStatus(Map<String, Object> requestBody){
        return record.checkPaymentStatus(requestBody);
    }

    public HashMap<String, Object> getPaymentById(int id){
        return record.getPaymentById(id);
    }
}
