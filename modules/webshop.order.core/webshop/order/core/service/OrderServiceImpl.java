package webshop.order.core;
import java.io.IOException;
import java.util.*;

import com.rabbitmq.client.DeliverCallback;
import vmj.routing.route.exceptions.NotFoundException;
import vmj.routing.route.exceptions.FieldValidationException;
import webshop.catalog.core.Catalog;
import webshop.order.OrderFactory;
import webshop.catalog.core.*;
import webshop.customer.core.*;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class OrderServiceImpl extends OrderServiceComponent{
	CustomerService customerService = new CustomerServiceImpl();

	String BASE_EXCHANGE = "webshop";
	String appId;
	Channel channel;
	String orderQueue;

	CatalogService catalogService;

//	public OrderServiceImpl () {} // Hanya bisa mendefinisikasn 1 constructor karena di factory hanya mengambil 1 constructor saja

	public OrderServiceImpl (Channel channel, String appId, CatalogService catalogService) {
		this.channel = channel;
		this.appId = appId;

		this.catalogService = catalogService;
		this.orderQueue = appId + ".order";
		BindQueue();
	}

	public HashMap<String, Object> validateQuantity(HashMap<String, Object> body){
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
		if (catalog == null) {
			throw new NotFoundException("Catalog with id " + catalogId +" not exist.");
		}
		int availableStock = catalog.getAvailableStock();
		if (quantity < 1){
			throw new FieldValidationException("Quantity must be more than 0");
		}
		if (availableStock < quantity) {
			throw new FieldValidationException("The quantity you have selected exceeds the available stock. Please adjust your order accordingly.");
		}
		int price = catalog.getPrice();
		int amount = quantity * price;
		HashMap<String, Object> responseMap = new HashMap<String,Object>();
		responseMap.put("catalogId", catalogId);
		responseMap.put("quantity",quantity);
		responseMap.put("amount",amount);
		return responseMap;
	}

	public HashMap<String, Object> previewOrder(UUID catalogId, int quantity, int amount){
		Catalog catalog = catalogRepository.getObject(catalogId);
		if (catalog == null) {
			throw new NotFoundException("Catalog with id " + catalogId +" not exist.");
		}
		int price = catalog.getPrice();
		String pictureURL = catalog.getPictureUrl();
		String name = catalog.getName();

		HashMap<String, Object> responseMap = new HashMap<String,Object>();
		responseMap.put("catalogId", catalogId);
		responseMap.put("name",name);
		responseMap.put("price",price);
		responseMap.put("amount",amount);
		responseMap.put("quantity",quantity);
		responseMap.put("pictureURL",pictureURL);

		return responseMap;
	}

	public Order saveOrder(HashMap<String, Object> body, String email){
		if (!body.containsKey("catalogId")) {
			throw new NotFoundException("Field 'catalogId' not found in the request body.");
		}
		String catalogIdStr = (String) body.get("catalogId");
		UUID catalogId = UUID.fromString(catalogIdStr);
		Catalog catalog = catalogRepository.getObject(catalogId);
		System.out.println("CatalogId: " + catalogId);
		System.out.println("Catalog: " + catalog);
		System.out.println("Catalog Price: " + catalog.getPrice());
		Date date = new Date();
		UUID orderId = UUID.randomUUID();
		int price = catalog.getPrice();

		if (!body.containsKey("quantity")) {
			throw new NotFoundException("Field 'quantity' not found in the request body.");
		}
		String quantityStr = (String) body.get("quantity");
		int quantity = Integer.parseInt(quantityStr);
		int availableStock = catalog.getAvailableStock();
		if (quantity > availableStock) {
			throw new FieldValidationException("The quantity you have chosen is more than what we currently have in stock.");
		}
		int amount = price*quantity;
		body.put("amount", String.valueOf(amount));

		if (!body.containsKey("city")) {
			throw new NotFoundException("Field 'city' not found in the request body.");
		}
		String city = (String) body.get("city");

		if (!body.containsKey("street")) {
			throw new NotFoundException("Field 'street' not found in the request body.");
		}
		String street = (String) body.get("street");

		if (!body.containsKey("state")) {
			throw new NotFoundException("Field 'state' not found in the request body.");
		}
		String state = (String) body.get("state");

		if (!body.containsKey("country")) {
			throw new NotFoundException("Field 'country' not found in the request body.");
		}
		String country = (String) body.get("country");

		if (!body.containsKey("zipcode")) {
			throw new NotFoundException("Field 'zipcode' not found in the request body.");
		}
		int zipcode = Integer.parseInt((String) body.get("zipcode"));


		Customer customer = null;
		if (email != null) {
			customer = customerService.getCustomerByEmail(email);
		}
		String status = "Not Paid";
		Order order = OrderFactory.createOrder("webshop.order.core.OrderImpl",
				status, orderId, date, amount, catalog, quantity, city, street,
				state, country, zipcode, customer);
		orderRepository.saveObject(order);
		catalog.setAvailableStock(availableStock - quantity);
		catalogService.updateAndPublishCatalog(catalog);

		publishOrderMessage(order,email,catalogIdStr, "create");
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

	public List<Order> getOrderHistory(String email){
		Customer customer = customerService.getCustomerByEmail(email);
		UUID customerId = customer.getCustomerId();
		List<Order> orderHistory = orderRepository.getListObject("order_comp", "customer_customerid", customerId);
		return orderHistory;
	}

	public void updateAndPublishOrder(Order order){
		orderRepository.updateObject(order);

		publishOrderMessage(order,"update");
	}

	public void saveOrderFromMessage(JsonObject body){
		if (!body.has("orderId")) {
			System.out.println("Field 'orderId' not found");
			return;
		}
		UUID orderId = UUID.fromString(body.get("orderId").getAsString());

		if (!body.has("catalogId")) {
			System.out.println("Field 'catalogId' not found in the request body.");
			return;
		}
		String catalogIdStr = body.get("catalogId").getAsString();
		UUID catalogId = UUID.fromString(catalogIdStr);
		Catalog catalog = catalogRepository.getObject(catalogId);
		Date date = new Date();
		int price = catalog.getPrice();

		if (!body.has("quantity")) {
			System.out.println("Field 'quantity' not found in the request body.");
			return;
		}
		int quantity = body.get("quantity").getAsInt();
		int availableStock = catalog.getAvailableStock();
		if (quantity > availableStock) {
			System.out.println("The quantity you have chosen is more than what we currently have in stock.");
			return;
		}
		int amount = price*quantity;

		if (!body.has("city")) {
			System.out.println("Field 'city' not found in the request body.");
			return;
		}
		String city = body.get("city").getAsString();

		if (!body.has("street")) {
			System.out.println("Field 'street' not found in the request body.");
			return;
		}
		String street = body.get("street").getAsString();

		if (!body.has("state")) {
			System.out.println("Field 'state' not found in the request body.");
			return;
		}
		String state = body.get("state").getAsString();

		if (!body.has("country")) {
			System.out.println("Field 'country' not found in the request body.");
			return;
		}
		String country = body.get("country").getAsString();

		if (!body.has("zipcode")) {
			System.out.println("Field 'zipcode' not found in the request body.");
			return;
		}
		int zipcode = body.get("zipcode").getAsInt();

		String email = body.get("email").getAsString();

		Customer customer = null;
		if (email != null) {
			customer = customerService.getCustomerByEmail(email);
		}
		String status = "Not Paid";
		Order order = OrderFactory.createOrder("webshop.order.core.OrderImpl",
				status, orderId, date, amount, catalog, quantity, city, street,
				state, country, zipcode, customer);
		orderRepository.saveObject(order);
		catalog.setAvailableStock(availableStock - quantity);
		catalogRepository.updateObject(catalog);
	}

	public void updateOrderStatusFromMessage(JsonObject body){
		if (!body.has("orderId")) {
			System.out.println("Field 'orderId' not found in the request body.");
			return;
		}
		String orderIdStr =  body.get("orderId").getAsString();
		UUID orderId = UUID.fromString(orderIdStr);

		Order order = orderRepository.getObject(orderId);

		if (!body.has("status")) {
			System.out.println("Field 'status' not found in the request body.");
			return;
		}
		String status = body.get("status").getAsString();

		order.setStatus(status);
		orderRepository.updateObject(order);
	}

	public void BindQueue(){
		try {
			boolean durable = true;
			boolean exclusive = false;
			boolean autoDelete = false;
			Map<String, Object> arguments = null;

			channel.queueDeclare(orderQueue, durable, exclusive, autoDelete, arguments);
			String bindingKey = "order";
			channel.queueBind(orderQueue, BASE_EXCHANGE, bindingKey);

			consumeOrderMessage();
		} catch (Exception e) {
			System.out.println("Failed to create " + orderQueue + " queeue " +e);
		}
	}

	public void consumeOrderMessage(){
		if (channel == null) {
			System.out.println("Channel is null");
			return;
		}

		try {
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				if (appId.equals(delivery.getProperties().getAppId())) {
					System.out.println("Skipping own message...");
					return;
				}

				String message = new String(delivery.getBody(), "UTF-8");

				System.out.println(" [x] Received: " + message);

				Gson gson = new Gson();
				JsonObject jsonObject = gson.fromJson(message, JsonObject.class);

				String action = jsonObject.get("action").getAsString();

				if (action.equals("update")) {
					updateOrderStatusFromMessage(jsonObject);
				} else { // create
					saveOrderFromMessage(jsonObject);
				}

			};

			channel.basicConsume(orderQueue, true, deliverCallback, consumerTag -> { });
		} catch (IOException e) {
			System.out.println("Error while consuming catalog message");
		}
	}

	private void publishOrderMessage(Order order, String action) {
		publishOrderMessage(order, null, null, action);
	}

	private void publishOrderMessage(Order order, String email, String catalogIdStr, String action) {
		if (order instanceof OrderDecorator) {
			order = ((OrderDecorator) order).getRecord();
		}

		Gson gson = new Gson();
		JsonObject jsonObject = null;

		if (action.equals("create")) {
			jsonObject = gson.toJsonTree(order).getAsJsonObject();

			jsonObject.remove("customer");
			jsonObject.remove("catalog");

			jsonObject.add("email", new JsonPrimitive(email));
			jsonObject.add("catalogId", new JsonPrimitive(catalogIdStr));
		} else if (action.equals("update")) {
			jsonObject = new JsonObject();
			String orderId = order.getOrderId().toString();
			String status = order.getStatus();

			jsonObject.add("orderId", new JsonPrimitive(orderId));
			jsonObject.add("status", new JsonPrimitive(status));
		}

		jsonObject.add("action", new JsonPrimitive(action));

		try {
			String routingKey = "order";
			String message = gson.toJson(jsonObject);

			BasicProperties props = new BasicProperties.Builder()
					.appId(appId)
					.contentType("application/json")
					.deliveryMode(2)
					.build();

			channel.basicPublish(BASE_EXCHANGE, routingKey, props, message.getBytes("UTF-8"));
			System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
		} catch (IOException e){
			System.out.println("Failed to publish order message");
		}
	}

}
