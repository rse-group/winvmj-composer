package paymentgateway.payment.ewallet;

import com.google.gson.Gson;

import java.lang.reflect.*;

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
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import paymentgateway.payment.PaymentFactory;
import paymentgateway.payment.core.Payment;
import paymentgateway.payment.core.PaymentServiceDecorator;
import paymentgateway.payment.core.PaymentImpl;
import paymentgateway.payment.core.PaymentServiceComponent;
import paymentgateway.config.core.Config;
import paymentgateway.config.ConfigFactory;

public class PaymentServiceImpl extends PaymentServiceDecorator {
	// implement this with author

	public PaymentServiceImpl (PaymentServiceComponent record) {
        super(record);
    }

	
	public Payment createPayment(Map<String, Object> requestBody) {
		Map<String, Object> response = sendTransaction(requestBody);

		String url = (String) response.get("url");
		String type = (String) response.get("payment_type");
		int id = (int) response.get("id");

		String phoneNumber = (String) requestBody.get("phone");
		System.out.println(id);
		Payment transaction = record.createPayment(requestBody, id);
		Payment ewalletTransaction =
			PaymentFactory.createPayment(
					"paymentgateway.payment.ewallet.EWalletImpl",
					transaction,
					phoneNumber,
					type,
					url
			);
		PaymentRepository.saveObject(ewalletTransaction);
		return ewalletTransaction;
	}

	public Map<String, Object> sendTransaction(Map<String, Object> requestBody) {
		String vendorName = (String) requestBody.get("vendor_name");

		Config config = ConfigFactory.createConfig(vendorName, ConfigFactory.createConfig("paymentgateway.config.core.ConfigImpl"));
		
		Gson gson = new Gson();
		Map<String, Object> requestMap = config.getEWalletRequestBody(requestBody);
		int id = ((Integer) requestMap.get("id")).intValue();
		requestMap.remove("id");
		String configUrl = config.getProductEnv("EWallet");
		HashMap<String, String> headerParams = config.getHeaderParams();
		System.out.println("configUrl: " + configUrl);
		String requestString = config.getRequestString(requestMap);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = (config.getBuilder(HttpRequest.newBuilder(),headerParams))
				.uri(URI.create(configUrl))
				.POST(HttpRequest.BodyPublishers.ofString(requestString))
				.build();

		Map<String, Object> responseMap = new HashMap<>();
		
		try {
			System.out.println(request.toString());
			HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String rawResponse = response.body().toString();
			System.out.println("rawResponse " + rawResponse);
			responseMap = config.getEWalletResponse(rawResponse, id);
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return responseMap;
	}
}

