package webshop.order.simplified;

import java.util.*;
import webshop.order.core.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.auth.core.*;
import vmj.auth.annotations.Restricted;
public class OrderResourceImpl extends OrderResourceDecorator {

	private OrderService orderService;
    public OrderResourceImpl(OrderResourceComponent recordController, OrderServiceComponent recordService) {
      super(recordController);
      this.orderService = new OrderServiceImpl(recordService);
    }

    @Restricted(permissionName = "CreateOrder")
    @Route(url="call/simplified/save")
    public HashMap<String, Object> saveOrder(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();

		Map<String, Object> payload = vmjExchange.getPayload();
		String email =  vmjExchange.getAuthPayload().getEmail();
		Order order = ((OrderServiceImpl) orderService).saveOrder(body,email);
		return order.toHashMap();
	}

}
