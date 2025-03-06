package paymentgateway.payment.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
//add other required packages

import paymentgateway.payment.core.Payment;
public abstract class PaymentResourceComponent implements PaymentResource{

    public PaymentResourceComponent(){}
    
    public abstract int callback(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> getPayment(VMJExchange vmjExchange);
    public abstract List<HashMap<String, Object>> getAllPayment(VMJExchange vmjExchange);
    public abstract List<HashMap<String, Object>> deletePayment(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> updatePayment(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> payment(VMJExchange vmjExchange);
}
