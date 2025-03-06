package paymentgateway.payment.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
//add other required packages

import paymentgateway.payment.core.Payment;
public abstract class PaymentServiceComponent implements PaymentService{
	protected RepositoryUtil<Payment> PaymentRepository;

    public PaymentServiceComponent(){
        this.PaymentRepository = new RepositoryUtil<Payment>(paymentgateway.payment.core.PaymentComponent.class);
    }
    
    public abstract Payment createPayment(Map<String, Object> requestBody, int id);
    public abstract Payment createPayment(Map<String, Object> requestBody);
    public abstract HashMap<String, Object> getPayment(Map<String, Object> requestBody);
    public abstract List<HashMap<String, Object>> getAllPayment(Map<String, Object> requestBody);
    public abstract List<HashMap<String, Object>> deletePayment(Map<String, Object> requestBody);
    public abstract HashMap<String, Object> updatePayment(Map<String, Object> requestBody);
    public abstract List<HashMap<String, Object>> transformListToHashMap(List<Payment> List);
    public abstract Map<String, Object> sendTransaction(Map<String, Object> requestBody);
    public abstract Map<String, Object> checkPaymentStatus(Map<String, Object> requestBody);
    public abstract HashMap<String, Object> getPaymentById(int id);
}
