package webshop.order.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface OrderService {
    HashMap<String, Object> validateQuantity(HashMap<String, Object> body);
	HashMap<String, Object> previewOrder(UUID catalogId, int quantity, int amount);
    Order saveOrder(HashMap<String, Object> body, String email);
    Order getOrder(UUID orderId);
    List<HashMap<String, Object>> transformOrderListToHashMap(List<Order> OrderList);
    List<Order> getOrderHistory(String email);
    void updateAndPublishOrder(Order order);
}
