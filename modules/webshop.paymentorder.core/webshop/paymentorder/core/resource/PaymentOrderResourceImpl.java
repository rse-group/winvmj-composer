package webshop.paymentorder.core;
import java.util.*;
import webshop.order.core.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import webshop.paymentorder.PaymentOrderFactory;
import vmj.auth.annotations.Restricted;

public class PaymentOrderResourceImpl extends PaymentOrderResourceComponent{
    private PaymentOrderService paymentOrderService;
    private OrderService orderService;

	public PaymentOrderResourceImpl(PaymentOrderService paymentOrderService, OrderService orderService) {
		this.paymentOrderService = paymentOrderService;
		this.orderService = orderService;
	}
	
    @Restricted(permissionName = "HistoryOrder")
    @Route(url="call/paymentorder/list")
    public List<HashMap<String,Object>> getUnpaidOrderHistory(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		String email =  vmjExchange.getAuthPayload().getEmail();
		List<Order> orderList = paymentOrderService.getUnpaidOrderHistory(email);
		return orderService.transformOrderListToHashMap(orderList);
	}
}
