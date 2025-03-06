package webshop.paymentorder.core;
import java.util.*;
import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
//add other required packages

public abstract class PaymentOrderResourceComponent implements PaymentOrderResource{
	
    public PaymentOrderResourceComponent(){
    }	
    public abstract List<HashMap<String,Object>> getUnpaidOrderHistory(VMJExchange vmjExchange);

}
