package webshop.paymentorder.core;
import java.util.*;
import webshop.order.core.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

public abstract class PaymentOrderServiceDecorator extends PaymentOrderServiceComponent{
	protected PaymentOrderServiceComponent record;

    public PaymentOrderServiceDecorator(PaymentOrderServiceComponent record) {
        this.record = record;
    }
    public PaymentOrder createPaymentOrder(String paymentId, String paymentStatus, String paymentMethod, Order order){
		return record.createPaymentOrder(paymentId, paymentStatus,paymentMethod, order);
	}

    public List<Order> getUnpaidOrderHistory(String email){
        return record.getUnpaidOrderHistory(email);
    }

}
