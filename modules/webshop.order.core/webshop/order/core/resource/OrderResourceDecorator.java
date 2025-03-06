package webshop.order.core;
import java.util.*;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

public abstract class OrderResourceDecorator extends OrderResourceComponent{
	protected OrderResourceComponent record;

    public OrderResourceDecorator(OrderResourceComponent record) {
        this.record = record;
    }

	public HashMap<String, Object> validateQuantity(VMJExchange vmjExchange){
		return record.validateQuantity(vmjExchange);
	}
    
    public HashMap<String, Object> previewOrder(VMJExchange vmjExchange){
		return record.previewOrder(vmjExchange);
	}

    public HashMap<String, Object> saveOrder(VMJExchange vmjExchange){
		return record.saveOrder(vmjExchange);
	}

    public HashMap<String, Object> getOrder(VMJExchange vmjExchange){
		return record.getOrder(vmjExchange);
	}
    
    public List<HashMap<String,Object>> getOrderHistory(VMJExchange vmjExchange){
		return record.getOrderHistory(vmjExchange);
	}

}
