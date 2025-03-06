package paymentgateway.payment.creditcard;

import com.google.gson.Gson;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.routing.route.exceptions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import paymentgateway.payment.PaymentFactory;
import paymentgateway.payment.core.Payment;
import paymentgateway.payment.core.PaymentResourceDecorator;
import paymentgateway.payment.core.PaymentResourceComponent;
import paymentgateway.payment.core.PaymentServiceComponent;
import paymentgateway.config.core.Config;
import paymentgateway.config.ConfigFactory;

public class PaymentResourceImpl extends PaymentResourceDecorator {
	// implement this to work with authorization module
	protected String apiKey;
	protected String apiEndpoint;
	private PaymentServiceImpl paymentServiceImpl;

	public PaymentResourceImpl(PaymentResourceComponent record, PaymentServiceComponent recordService) {
		super(record);
		paymentServiceImpl = new PaymentServiceImpl(recordService);
	}
	
	@Route(url = "call/creditcard")
	public HashMap<String, Object> payment(VMJExchange vmjExchange) {
		if (vmjExchange.getHttpMethod().equals("POST")){
			Map<String, Object> requestBody = vmjExchange.getPayload(); 
			Payment result = paymentServiceImpl.createPayment(requestBody);
			return result.toHashMap();
		}
		throw new NotFoundException("Route tidak ditemukan");
	}
}
