package webshop.order.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
import webshop.catalog.core.*;

public abstract class OrderResourceComponent implements OrderResource{
	protected RepositoryUtil<Order> orderRepository;

    public OrderResourceComponent(){
        this.orderRepository = new RepositoryUtil<Order>(webshop.order.core.OrderComponent.class); 
    }	
    public abstract HashMap<String, Object> validateQuantity(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> previewOrder(VMJExchange vmjExchange);
	public abstract HashMap<String, Object> saveOrder(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> getOrder(VMJExchange vmjExchange);
    public abstract List<HashMap<String,Object>> getOrderHistory(VMJExchange vmjExchange);
}
