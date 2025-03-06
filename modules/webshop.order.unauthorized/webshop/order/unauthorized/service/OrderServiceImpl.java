package webshop.order.unauthorized;

import webshop.order.core.OrderServiceDecorator;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.auth.annotations.Restricted;
import vmj.routing.route.exceptions.NotFoundException;

import webshop.order.core.*;
import webshop.order.OrderFactory;

public class OrderServiceImpl extends OrderServiceDecorator {

    public OrderServiceImpl (OrderServiceComponent record) {
    	 super(record);
    }

    public Order saveOrder(HashMap<String, Object> body){
		String email = (String) body.get("email");
		Order order = record.saveOrder(body, null); // null because without authentication user
		UUID orderId = UUID.randomUUID();
		Order unauthorizedOrder = OrderFactory.createOrder("webshop.order.unauthorized.OrderImpl", orderId, order, email);
		orderRepository.saveObject(unauthorizedOrder);
		unauthorizedOrder = orderRepository.getObject(orderId);
		return unauthorizedOrder;
	}
}
