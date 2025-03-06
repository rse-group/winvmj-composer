package webshop.paymentorder.core;
import java.io.IOException;
import java.util.*;

import com.rabbitmq.client.DeliverCallback;
import webshop.order.core.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import webshop.order.core.*;
import webshop.order.core.OrderServiceImpl;
import webshop.paymentorder.PaymentOrderFactory;
import vmj.auth.annotations.Restricted;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class PaymentOrderServiceImpl extends PaymentOrderServiceComponent{
	String BASE_EXCHANGE = "webshop";
	String appId;
	Channel channel;
	String orderQueue;
	String paymentOrderQueue;

	OrderService orderService;

//	public PaymentOrderServiceImpl() {} // Hanya bisa mendefinisikasn 1 constructor karena di factory hanya mengambil 1 constructor saja

	public PaymentOrderServiceImpl (Channel channel, String appId, OrderService orderService) {
		this.channel = channel;
		this.appId = appId;

		this.orderQueue = appId + ".order";
		this.orderService = orderService;

		this.paymentOrderQueue = appId + ".paymentOrder";

		BindQueue();
	}

    public PaymentOrder createPaymentOrder(String paymentId, String paymentStatus, String paymentMethod, Order order){
		UUID paymentOrderId = UUID.randomUUID();
		PaymentOrder paymentOrder = PaymentOrderFactory.createPaymentOrder("webshop.paymentorder.core.PaymentOrderImpl", paymentOrderId, paymentId, paymentStatus, paymentMethod, order);
		paymentOrderRepository.saveObject(paymentOrder);

		order.setStatus("Paid");
		orderRepository.updateObject(order);

		publishOrderMessage(order,"update");
		publishPaymentOrderMessage(paymentOrder,"create");

		return paymentOrder;
	}

	public List<Order> getUnpaidOrderHistory(String email){
    	List<Order> orderHistory = orderService.getOrderHistory(email);
		orderHistory.removeIf(order -> !"Not Paid".equals(order.getStatus()));
		return orderHistory;
	}

	public void createPaymentOrderFromMessage(JsonObject body){
		if (!body.has("paymentOrderId")) {
			System.out.println("Field 'paymentOrderId' not found");
			return;
		}
		if (!body.has("paymentId")) {
			System.out.println("Field 'paymentId' not found");
			return;
		}
		if (!body.has("paymentStatus")) {
			System.out.println("Field 'paymentStatus' not found");
			return;
		}
		if (!body.has("paymentMethod")) {
			System.out.println("Field 'paymentMethod' not found");
			return;
		}
		if (!body.has("orderId")) {
			System.out.println("Field 'orderId' not found");
		}

		UUID paymentOrderId = UUID.fromString(body.get("paymentOrderId").getAsString());
		String paymentId = body.get("paymentId").getAsString();
		String paymentStatus = body.get("paymentStatus").getAsString();
		String paymentMethod = body.get("paymentMethod").getAsString();

		UUID orderId = UUID.fromString(body.get("orderId").getAsString());
		Order order = orderService.getOrder(orderId);

		PaymentOrder paymentOrder = PaymentOrderFactory.createPaymentOrder("webshop.paymentorder.core.PaymentOrderImpl", paymentOrderId, paymentId, paymentStatus, paymentMethod, order);
		paymentOrderRepository.saveObject(paymentOrder);
	}

	public void BindQueue(){
		try {
			boolean durable = true;
			boolean exclusive = false;
			boolean autoDelete = false;
			Map<String, Object> arguments = null;

			channel.queueDeclare(paymentOrderQueue, durable, exclusive, autoDelete, arguments);
			String bindingKey = "paymentOrder";
			channel.queueBind(paymentOrderQueue, BASE_EXCHANGE, bindingKey);

			consumePaymentOrderMessage();
		} catch (Exception e) {
			System.out.println("Failed to create " + paymentOrderQueue + " queeue " +e);
		}
	}

	public void consumePaymentOrderMessage(){
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

				if (action.equals("create")) {
					createPaymentOrderFromMessage(jsonObject);
				}

			};

			channel.basicConsume(paymentOrderQueue, true, deliverCallback, consumerTag -> { });
		} catch (IOException e) {
			System.out.println("Error while consuming catalog message");
		}
	}

	private void publishPaymentOrderMessage(PaymentOrder paymentOrder, String action) {
		if (paymentOrder instanceof PaymentOrderDecorator) {
			paymentOrder = ((PaymentOrderDecorator) paymentOrder).getRecord();
		}

		Gson gson = new Gson();
		JsonObject jsonObject = new JsonObject();

		if (action.equals("create")) {
			jsonObject = gson.toJsonTree(paymentOrder).getAsJsonObject();

			jsonObject.add("paymentId", new JsonPrimitive(paymentOrder.getPaymentId()));
			jsonObject.add("paymentStatus", new JsonPrimitive(paymentOrder.getPaymentOrderStatus()));
			jsonObject.add("paymentMethod", new JsonPrimitive(paymentOrder.getPaymentMethod	()));

			String orderId = paymentOrder.getOrder().getOrderId().toString();
			jsonObject.add("orderId", new JsonPrimitive(orderId));
		}

		jsonObject.add("action", new JsonPrimitive(action));

		try {
			String routingKey = "paymentOrder";
			String message = gson.toJson(jsonObject);

			BasicProperties props = new BasicProperties.Builder()
					.appId(appId)
					.contentType("application/json")
					.deliveryMode(2)
					.build();

			channel.basicPublish(BASE_EXCHANGE, routingKey, props, message.getBytes("UTF-8"));
			System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
		} catch (IOException e){
			System.out.println("Failed to publish paymentOrder message");
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
