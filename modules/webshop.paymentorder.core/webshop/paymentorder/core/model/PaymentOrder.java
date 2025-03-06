package webshop.paymentorder.core;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import java.util.*;
import webshop.order.core.Order;
public interface PaymentOrder {
	public UUID getPaymentOrderId();
	public String getPaymentId();
	public String getPaymentOrderStatus();
	public String getPaymentMethod();
	public Order getOrder();
	HashMap<String, Object> toHashMap();
}
