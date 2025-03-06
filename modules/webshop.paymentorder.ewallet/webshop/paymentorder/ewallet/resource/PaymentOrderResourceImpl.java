package webshop.paymentorder.ewallet;
import java.util.*;
import webshop.paymentorder.core.*;
import webshop.order.core.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.auth.core.*;
import vmj.auth.annotations.Restricted;

public class PaymentOrderResourceImpl extends PaymentOrderResourceDecorator {
    private PaymentOrderService paymentOrderService;
	
    public PaymentOrderResourceImpl(PaymentOrderResourceComponent recordController, PaymentOrderServiceComponent recordService) {
      	super(recordController);
      	this.paymentOrderService = new PaymentOrderServiceImpl(recordService);
	}
	@Restricted(permissionName = "CreateOrder")
	@Route(url="call/paymentorder/ewallet")
    public HashMap<String, Object> createPaymentOrder(VMJExchange vmjExchange){
    	HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		String email =  vmjExchange.getAuthPayload().getEmail();
    	HashMap<String, Object> result = ((PaymentOrderServiceImpl) paymentOrderService).createPaymentOrder(body, email);
        return result;
    }

}
