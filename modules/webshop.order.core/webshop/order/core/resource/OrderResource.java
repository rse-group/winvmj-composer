package webshop.order.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface OrderResource {
    HashMap<String, Object> validateQuantity(VMJExchange vmjExchange);
	HashMap<String, Object> previewOrder(VMJExchange vmjExchange);
    HashMap<String, Object> saveOrder(VMJExchange vmjExchange);
    HashMap<String, Object> getOrder(VMJExchange vmjExchange);
    List<HashMap<String, Object>> getOrderHistory(VMJExchange vmjExchange);
}
