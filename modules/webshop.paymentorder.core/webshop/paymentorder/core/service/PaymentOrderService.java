package webshop.paymentorder.core;
import java.util.*;
import webshop.order.core.*;
import vmj.routing.route.VMJExchange;

public interface PaymentOrderService {
    PaymentOrder createPaymentOrder(String paymentId, String paymentStatus, String paymentMethod, Order order);
    List<Order> getUnpaidOrderHistory(String email);
}
