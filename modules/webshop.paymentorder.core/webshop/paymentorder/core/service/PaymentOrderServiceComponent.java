package webshop.paymentorder.core;
import java.util.*;
import webshop.order.core.*;
import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
//add other required packages

public abstract class PaymentOrderServiceComponent implements PaymentOrderService{
	protected RepositoryUtil<PaymentOrder> paymentOrderRepository;
    protected RepositoryUtil<Order> orderRepository;

    public PaymentOrderServiceComponent(){
        this.paymentOrderRepository = new RepositoryUtil<PaymentOrder>(webshop.paymentorder.core.PaymentOrderComponent.class);
        this.orderRepository = new RepositoryUtil<Order>(webshop.order.core.OrderComponent.class);
    }	

    public abstract PaymentOrder createPaymentOrder(String paymentId, String paymentStatus, String paymentMethod, Order order);
    public abstract List<Order> getUnpaidOrderHistory(String email);
}
