package webshop.order.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
import webshop.catalog.core.*;

public abstract class OrderServiceComponent implements OrderService{
    protected RepositoryUtil<Order> orderRepository;
    protected RepositoryUtil<Catalog> catalogRepository;

    public OrderServiceComponent(){
        this.orderRepository = new RepositoryUtil<Order>(webshop.order.core.OrderComponent.class);
        this.catalogRepository = new RepositoryUtil<Catalog>(webshop.catalog.core.CatalogComponent.class);
    }
    public abstract HashMap<String, Object> validateQuantity(HashMap<String, Object> body);
    public abstract HashMap<String, Object> previewOrder(UUID catalogId, int quantity, int amount);
    public abstract Order saveOrder(HashMap<String, Object> body,String email);
    public abstract Order getOrder(UUID orderId);
    public abstract List<HashMap<String, Object>> transformOrderListToHashMap(List<Order> OrderList);
    public abstract List<Order> getOrderHistory(String email);
    public abstract void updateAndPublishOrder(Order order);
}
