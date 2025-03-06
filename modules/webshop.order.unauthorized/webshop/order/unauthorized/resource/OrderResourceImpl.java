package webshop.order.unauthorized;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.auth.annotations.Restricted;
import vmj.routing.route.exceptions.NotFoundException;

import webshop.order.core.*;
import webshop.order.OrderFactory;

public class OrderResourceImpl extends OrderResourceDecorator {
	private OrderService orderService;
    public OrderResourceImpl(OrderResourceComponent recordController, OrderServiceComponent recordService) {
      super(recordController);
      this.orderService = new OrderServiceImpl(recordService);
    }

    @Route(url="call/unauthorized/save")
    public HashMap<String, Object> saveOrder(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		Order order = ((OrderServiceImpl) orderService).saveOrder(body);
		return order.toHashMap();
	}

}
