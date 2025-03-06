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
import paymentgateway.payment.core.PaymentServiceDecorator;
import paymentgateway.payment.core.PaymentServiceComponent;
import paymentgateway.config.core.Config;
import paymentgateway.config.ConfigFactory;

public class PaymentServiceImpl extends PaymentServiceDecorator {

	public PaymentServiceImpl (PaymentServiceComponent record) {
        super(record);
    }

	public Payment createPayment(Map<String, Object> requestBody) {
		Map<String, Object> response = sendTransaction(requestBody);
		String idToken = (String) requestBody.get("token_id");

		String statusCreditPayment = (String) response.get("status");
		int id = (int) response.get("id");

		Payment transaction = record.createPayment(requestBody, id);
		Payment creditCardTransaction = PaymentFactory.createPayment(
				"paymentgateway.payment.creditcard.PaymentImpl", transaction, idToken, statusCreditPayment);
		PaymentRepository.saveObject(creditCardTransaction);
		return creditCardTransaction;
	}

	
	public Map<String, Object> sendTransaction(Map<String, Object> requestBody) {
	    String vendorName = (String) requestBody.get("vendor_name");
	    
	    Config config = ConfigFactory.createConfig(vendorName, ConfigFactory.createConfig("paymentgateway.config.core.ConfigImpl"));
	    Gson gson = new Gson();
	    
	    // Step 1: Get credit card token
	    String tokenUrl = config.constructUrlParam("CreditCardToken", requestBody);
	    
		HashMap<String, String> headerParams = config.getHeaderParams();
		HttpRequest tokenRequest = (config.getBuilder(HttpRequest.newBuilder(), headerParams))
			.uri(URI.create(tokenUrl))
			.GET()
			.build();
	    
	    String tokenId = null;
	    try {
	        HttpResponse<String> tokenResponse = HttpClient.newHttpClient().send(tokenRequest, HttpResponse.BodyHandlers.ofString());
	        Map<String, Object> tokenResponseMap = gson.fromJson(tokenResponse.body(), Map.class);
	        tokenId = (String) tokenResponseMap.get("token_id");
	        System.out.println(tokenId);
	    } catch (Exception e) {
	        System.out.println("Failed to get token: " + e.getMessage());
	        return Map.of("error", "Token retrieval failed");
	    }
	    
	    // Step 2: Send transaction request
	    Map<String, Object> requestMap = config.getCreditCardRequestBody(requestBody);
	    int id = ((Integer) requestMap.get("id")).intValue();
	    requestMap.remove("id");
	    requestMap.put("credit_card", Map.of("token_id", tokenId, "authentication", false));
	    requestMap.put("payment_type", "credit_card");

	    String requestString = gson.toJson(requestMap);
	    String configUrl = config.getProductEnv("CreditCard");
	    System.out.println(configUrl);

	    HttpRequest transactionRequest = (config.getBuilder(HttpRequest.newBuilder(), headerParams))
	        .uri(URI.create(configUrl))
	        .POST(HttpRequest.BodyPublishers.ofString(requestString))
	        .build();

	    Map<String, Object> responseMap = new HashMap<>();
	    try {
	        HttpResponse<String> response = HttpClient.newHttpClient().send(transactionRequest, HttpResponse.BodyHandlers.ofString());
	        String rawResponse = response.body();
	        System.out.println("Transaction Response: " + rawResponse);
	        responseMap = config.getCreditCardResponse(rawResponse, id);
	    } catch (Exception e) {
	        System.out.println("Transaction failed: " + e.getMessage());
	    }

	    return responseMap;
	}

}
