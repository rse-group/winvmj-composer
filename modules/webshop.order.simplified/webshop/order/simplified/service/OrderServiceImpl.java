package webshop.order.simplified;
import java.util.*;
import java.time.Month;

import vmj.routing.route.exceptions.NotFoundException;
import vmj.routing.route.exceptions.FieldValidationException;
import webshop.order.OrderFactory;
import webshop.order.core.*;
import webshop.catalog.core.*;
import webshop.customer.core.*;

public class OrderServiceImpl extends OrderServiceDecorator{

    CustomerService customerService = new CustomerServiceImpl();

	public OrderServiceImpl (OrderServiceComponent record) {
		super(record);
	}

    public Order saveOrder(HashMap<String, Object> body, String email){
		
        if (email == null) {
	        throw new NotFoundException("Email "+ email +"not exist in repository.");
	    } 
		Customer customer = customerService.getCustomerByEmail(email);
		Date date = new Date();
		String status = "Not Paid";
		UUID orderId = UUID.randomUUID();
		if (!body.containsKey("catalogId")) {
			throw new NotFoundException("Field 'catalogId' not found in the request body.");
		}
		String catalogIdStr = (String) body.get("catalogId");
		UUID catalogId = UUID.fromString(catalogIdStr);
		Catalog catalog = catalogRepository.getObject(catalogId);

		if (!body.containsKey("quantity")) {
			throw new NotFoundException("Field 'quantity' not found in the request body.");
		}
		String quantityStr = (String) body.get("quantity");
		int quantity = Integer.parseInt(quantityStr);

		int price = catalog.getPrice();
		int amount = price * quantity;
		if (!body.containsKey("city")) {
			throw new NotFoundException("Field 'city' not found in the request body.");
		}
		String city = (String) body.get("city");
		
		Order order = OrderFactory.createOrder("webshop.order.core.OrderImpl", 
					status, orderId, date, amount, catalog, quantity, city, customer);
		orderRepository.saveObject(order);
		int availableStock = catalog.getAvailableStock();
		catalog.setAvailableStock(availableStock - quantity);
		return order;
	}

    public Order getOrder(UUID orderId){
		Order order = orderRepository.getObject(orderId);
		return order;
	}

    public List<HashMap<String,Object>> transformOrderListToHashMap(List<Order> orderList){
		List<HashMap<String,Object>> resultList = new ArrayList<HashMap<String,Object>>();
        for(int i = 0; i < orderList.size(); i++) {
            resultList.add(orderList.get(i).toHashMap());
        }

        return resultList;
	}
	
}
