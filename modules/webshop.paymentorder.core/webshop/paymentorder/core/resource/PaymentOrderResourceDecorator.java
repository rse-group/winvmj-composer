package webshop.paymentorder.core;
import java.util.*;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

public abstract class PaymentOrderResourceDecorator extends PaymentOrderResourceComponent{
	protected PaymentOrderResourceComponent record;

    public PaymentOrderResourceDecorator(PaymentOrderResourceComponent record) {
        this.record = record;
    }

    public List<HashMap<String,Object>> getUnpaidOrderHistory(VMJExchange vmjExchange){
		return record.getUnpaidOrderHistory(vmjExchange);
	}

}
