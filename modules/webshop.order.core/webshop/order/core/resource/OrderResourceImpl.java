package webshop.order.core;
import java.util.*;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.auth.core.*;
import vmj.auth.annotations.Restricted;
import vmj.routing.route.exceptions.NotFoundException;
import webshop.order.OrderFactory;
import vmj.auth.annotations.Restricted;

public class OrderResourceImpl extends OrderResourceComponent{
	private OrderService orderService;

	public OrderResourceImpl(OrderService orderService) {
		this.orderService = orderService;
	}

	@Route(url="call/order/validate")
	public HashMap<String, Object> validateQuantity(VMJExchange vmjExchange){
		HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		HashMap<String, Object> result = orderService.validateQuantity(body);
		return result;
	}

	@Route(url="call/order/preview")
    public HashMap<String, Object> previewOrder(VMJExchange vmjExchange){
		String catalogIdStr = vmjExchange.getGETParam("catalogId"); 
		UUID catalogId = UUID.fromString(catalogIdStr);
		String quantityStr = vmjExchange.getGETParam("quantity"); 
		int quantity = Integer.parseInt(quantityStr);
		String amountStr = vmjExchange.getGETParam("amount"); 
		int amount = Integer.parseInt(amountStr);
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		HashMap<String, Object> result = orderService.previewOrder(catalogId, quantity, amount);
		return result;
	}

    
    @Restricted(permissionName = "CreateOrder")
    @Route(url="call/order/save")
    public HashMap<String, Object> saveOrder(VMJExchange vmjExchange){
		HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		String email =  vmjExchange.getAuthPayload().getEmail();
		Order order = orderService.saveOrder(body, email);
		return order.toHashMap();
	}

    @Route(url="call/order/detail")
    public HashMap<String, Object> getOrder(VMJExchange vmjExchange){
		String orderIdStr = vmjExchange.getGETParam("orderId"); 
		UUID orderId = UUID.fromString(orderIdStr);
		Order order = orderService.getOrder(orderId);
		return order.toHashMap();
	}
    
    @Restricted(permissionName = "HistoryOrder")
    @Route(url="call/order/list")
    public List<HashMap<String,Object>> getOrderHistory(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		String email =  vmjExchange.getAuthPayload().getEmail();
		List<Order> orderList = orderService.getOrderHistory(email);
		return orderService.transformOrderListToHashMap(orderList);
	}

}
