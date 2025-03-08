package webshop.order.core;
import java.util.*;

public abstract class OrderServiceDecorator extends OrderServiceComponent{
	protected OrderServiceComponent record;

	public OrderServiceDecorator(OrderServiceComponent record) {
		this.record = record;
	}

	public HashMap<String, Object> validateQuantity(HashMap<String, Object> body){
		return record.validateQuantity(body);
	}

	public HashMap<String, Object> previewOrder(UUID catalogId, int quantity, int amount) {
		return record.previewOrder(catalogId,quantity,amount);
	}

	public Order saveOrder(HashMap<String, Object> body, String email){
		return record.saveOrder(body, email);
	}

	public Order getOrder(UUID orderId){
		return record.getOrder(orderId);
	}

	public List<HashMap<String, Object>>transformOrderListToHashMap(List<Order> orderList){
		return record.transformOrderListToHashMap(orderList);
	}

	public List<Order> getOrderHistory(String email){
		return record.getOrderHistory(email);
	}

	public void updateAndPublishOrder(Order order){record.updateAndPublishOrder(order);}

}
