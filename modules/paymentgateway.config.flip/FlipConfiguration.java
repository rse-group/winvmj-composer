package paymentgateway.config.flip;

import paymentgateway.config.core.ConfigDecorator;
import paymentgateway.config.core.ConfigComponent;
import paymentgateway.config.core.PropertiesReader;
import paymentgateway.config.core.RequestBodyValidator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;

import vmj.routing.route.VMJExchange;
import vmj.routing.route.exceptions.BadRequestException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FlipConfiguration extends ConfigDecorator{
    private String CONFIG_FILE = "flip.properties";

    public FlipConfiguration(ConfigComponent record) {
        super(record);
    }

    @Override
    public String getVendorName(){
        return "Flip";
    }
    
    @Override
    public Map<String, Object> getCallbackPaymentRequestBody(VMJExchange vmjExchange){
        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> payload = vmjExchange.getPayload();
		String status = "PENDING";
		String flipToken = PropertiesReader.getProp(CONFIG_FILE, "token");
        String id = "";
		
		String data = (String) payload.get("data");
		String token = (String) payload.get("token");
		
		Gson gson = new Gson();
    	Map<String, Object> decodedData = gson.fromJson(data, Map.class);
    	status = (String) decodedData.get("status");
    	id = String.valueOf(((Double) decodedData.get("bill_link_id")).intValue());
        if (token.equals(flipToken)) {
            requestMap.put("id",id);
            requestMap.put("status", status);
        }     
        return requestMap;
    }
    
    @Override
    public Map<String, Object> getCallbackDisbursementRequestBody(Map<String, Object> requestBody){
        Map<String, Object> requestMap = new HashMap<>();
		String status = "PENDING";
		String flipToken = PropertiesReader.getProp(CONFIG_FILE, "token");
        String id = "";
		
		String data = (String) requestBody.get("data");
		String token = (String) requestBody.get("token");
		
		Gson gson = new Gson();
    	Map<String, Object> decodedData = gson.fromJson(data, Map.class);
    	status = (String) decodedData.get("status");
    	id = String.valueOf(((Double) decodedData.get("id")).intValue());
        if (token.equals(flipToken)) {
            requestMap.put("id",id);
            requestMap.put("status", status);
        }

        return requestMap;
    }

    @Override
    public Map<String, Object> getDisbursementRequestBody(Map<String, Object> requestBody) {
        String vendor_name = RequestBodyValidator.stringRequestBodyValidator(requestBody, "vendor_name");
        String bank_code = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            new String[]{ "bank_code", "beneficiary_bank_name" }
        );
        String account_number = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            new String[]{ "account_number", "beneficiary_account_number" }
        );
        double amount = RequestBodyValidator.doubleRequestBodyValidator(requestBody, "amount");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("vendor_name", vendor_name);
        requestMap.put("bank_code", bank_code);
        requestMap.put("account_number", account_number);
        requestMap.put("amount", amount);

        return requestMap;
    }

    @Override
    public Map<String, Object> getDomesticDisbursementRequestBody(Map<String, Object> requestBody) {
        if (!requestBody.containsKey("direction")) {
            throw new BadRequestException("direction tidak ditemukan pada payload.");
        }

        String direction = (String) requestBody.get("direction");
        try {
            DirectionType.valueOf(direction);
        } catch (Exception e) {
            throw new BadRequestException(
                String.format("Direction dengan tipe %s tidak tersedia.", direction)
            );
        }

        Map<String, Object> requestMap = getDisbursementRequestBody(requestBody);
        requestMap.put("direction", direction);

        return requestMap;
    }

    @Override
    public Map<String, Object> getInternationalDisbursementRequestBody(Map<String, Object> requestBody) {
        Integer senderCountry = RequestBodyValidator.intRequestBodyValidator(requestBody, "sender_country");
        String senderName = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "sender_name"
        );
        String senderAddress = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "sender_address"
        );
        String senderJob = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "sender_job"
        );

        Map<String, Object> requestMap = getDisbursementRequestBody(requestBody);
        requestMap.put("sender_country", senderCountry);
        requestMap.put("sender_name", senderName);
        requestMap.put("sender_address", senderAddress);
        requestMap.put("sender_job", senderJob);

        return requestMap;
    }
    
    @Override
    public String getPaymentDetailEndpoint(String configUrl,String id){
        configUrl = configUrl.replace("[id]", id);
        return configUrl;
    }

    @Override
    public Map<String, Object> getPaymentStatusResponse(String rawResponse, String id){
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        if (rawResponseMap.containsKey("errors")) {
            List<Map<String, Object>> errors = (List<Map<String, Object>>) rawResponseMap.get("errors");
            if (!errors.isEmpty()) {
                Map<String, Object> firstError = errors.get(0);
                String errorMessage = (String) firstError.get("message");
                response.put("error", errorMessage);
                return response;
            }
        }
        ArrayList<Object> dataList = (ArrayList<Object>) rawResponseMap.get("data");
        if (!dataList.isEmpty()) {
            Map<String, Object> dataObject = (Map<String, Object>) dataList.get(0);
            String status = (String) dataObject.get("status");
            response.put("status", status);
            response.put("id", id);
        } else {
            response.put("id", id);
            response.put("status", PaymentStatus.PENDING.getStatus());
        }
        return response;
    }

    @Override
    public String getProductEnv(String serviceName){
        return record.getProductEnv(CONFIG_FILE, serviceName);
    }
        
    @Override
    public String getRequestString(Map<String, Object> requestMap){
        return EncodeResponse.getParamsUrlEncoded(requestMap);
    }

    @Override
    public Map<String, Object> getDisbursementResponse(String rawResponse){
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        
        int id = ((Double) rawResponseMap.get("id")).intValue();
        int userId = ((Double) rawResponseMap.get("user_id")).intValue();
        String status = (String) rawResponseMap.get("status");
        response.put("status", status);
        response.put("user_id", userId);
        response.put("id", id);
        return response;
    }

    @Override
    public Map<String, Object> getAgentDisbursementResponse(String rawResponse){
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        int agentId = ((Double) rawResponseMap.get("agent_id")).intValue();
        String direction = (String) rawResponseMap.get("direction");
        int id = ((Double) rawResponseMap.get("id")).intValue();
        String status = (String) rawResponseMap.get("status");
        response.put("status", status);
        response.put("id", id);
        response.put("agent_id", agentId);
        response.put("user_id", agentId);
        response.put("direction", direction);
        return response;
    }

    @Override
    public Map<String, Object> getSpecialDisbursementResponse(String rawResponse){
        Map<String, Object> response = getDisbursementResponse(rawResponse);
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        Map<String, Object> senderMap = (Map<String, Object>) rawResponseMap.get("sender");
        String senderName = (String) senderMap.get("sender_name");
        String senderAddress = (String) senderMap.get("sender_address");
        int senderCountry = ((Double) senderMap.get("sender_country")).intValue();;
        String senderJob = (String) senderMap.get("sender_job");

        String direction = (String) rawResponseMap.get("direction");
        response.put("direction", direction);
        response.put("name", senderName);
        response.put("address", senderAddress);
        response.put("country", senderCountry);
        response.put("job", senderJob);
        return response;
    }

    @Override
    public Map<String, Object> getInternationalDisbursementResponse(String rawResponse){
        Map<String, Object> response = getDisbursementResponse(rawResponse);
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        double exchangeRate = (double) rawResponseMap.get("exchange_rate");
        double fee = (double) rawResponseMap.get("fee");
        double amount = (double) rawResponseMap.get("amount");
        String sourceCountry = (String) rawResponseMap.get("source_country");
        String destinationCountry = (String) rawResponseMap.get("destination_country");
        String beneficiaryCurrencyCode = (String) rawResponseMap.get("beneficiary_currency_code");

        response.put("exchange_rate", exchangeRate);
        response.put("fee", fee);
        response.put("amount", amount);
        response.put("source_country", sourceCountry);
        response.put("destination_country", destinationCountry);
        response.put("beneficiary_currency_code", beneficiaryCurrencyCode);
        return response;
    }

    @Override
    public Map<String, Object> getVirtualAccountRequestBody(Map<String, Object> requestBody){
        int id = generateId();
        Map<String, Object> requestMap = new HashMap<>();
        String title = (String) requestBody.get("title");
        String amountStr = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "amount"
        );
        int amount = Integer.parseInt(amountStr);
        String senderName = (String) requestBody.get("name");
        String senderEmail = (String) requestBody.get("email");
        String senderBank = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "bank"
        );

        requestMap.put("id",id);
        requestMap.put("title", title);
        requestMap.put("type", PaymentType.SINGLE.getValue());
        requestMap.put("amount",amount);
        requestMap.put("sender_name",senderName);
        requestMap.put("sender_email",senderEmail);
        requestMap.put("sender_bank",senderBank);
        requestMap.put("sender_bank_type",SenderBankType.VIRTUALACCOUNT.getValue());
        requestMap.put("step", PaymentFlow.THIRD.getValue());

        return requestMap;
    }

    @Override
    public Map<String, Object> getVirtualAccountResponse(String rawResponse, int id) {
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        Map<String, Object> billPayment = (Map<String, Object>) rawResponseMap.get("bill_payment");
        Map<String, Object> receiverBankAccount = (Map<String, Object>) billPayment.get("receiver_bank_account");
        String vaNumber = (String) receiverBankAccount.get("account_number");
        int billId = (int) ((Double) rawResponseMap.get("link_id")).doubleValue();
        response.put("va_number", vaNumber);
        response.put("id", billId);
        
        return response;
    }


    @Override
    public Map<String, Object> getEWalletRequestBody(Map<String, Object> requestBody){
        int id = generateId();
        Map<String, Object> requestMap = new HashMap<>();
        String title = (String) requestBody.get("title");

        String amountStr = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "amount"
        );
        int amount = Integer.parseInt(amountStr);

        String senderName = (String) requestBody.get("name");
        String senderEmail = (String) requestBody.get("email");
        String senderBank = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "ewallet_type"
        );
        String senderPhoneNumber = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "phone"
        );

        requestMap.put("id",id);
        requestMap.put("title", title);
        requestMap.put("type", PaymentType.SINGLE.getValue());
        requestMap.put("amount",amount);
        requestMap.put("sender_name",senderName);
        requestMap.put("sender_email",senderEmail);
        requestMap.put("sender_bank",senderBank);
        requestMap.put("is_phone_number_required",PhoneNumberRequired.TRUE.getValue());
        requestMap.put("sender_phone_number", senderPhoneNumber);
        requestMap.put("sender_bank_type",SenderBankType.EWALLET.getValue());
        requestMap.put("step", PaymentFlow.THIRD.getValue());

        return requestMap;
    }

    @Override
    public Map<String, Object> getEWalletResponse(String rawResponse, int id){
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        String url = (String) rawResponseMap.get("payment_url");

        Map<String, Object> billPayment = (Map<String, Object>) rawResponseMap.get("bill_payment");

        String paymentType = (String) billPayment.get("sender_bank");
        String phoneNumber = (String) rawResponseMap.get("user_phone");
        int billId = (int) ((Double) rawResponseMap.get("link_id")).doubleValue();
        response.put("phone_number",phoneNumber);
        response.put("url", url);
        response.put("payment_type",paymentType);
        response.put("id", billId);
        return response;
    }


    @Override
    public Map<String, Object> getPaymentLinkRequestBody(Map<String, Object> requestBody){
        int id = generateId();
        Map<String, Object> requestMap = new HashMap<>();
        String title = (String) requestBody.get("title");
        String amountStr = RequestBodyValidator.stringRequestBodyValidator(
            requestBody,
            "amount"
        );
        int amount = Integer.parseInt(amountStr);
        String senderEmail = (String) requestBody.get("email");
        String senderName = (String) requestBody.get("sender_name");
        
        requestMap.put("id",id);
        requestMap.put("title", title);
        requestMap.put("type", PaymentType.SINGLE.getValue());
        requestMap.put("sender_name", senderName);
        requestMap.put("sender_email",senderEmail);
        requestMap.put("amount",amount);
        requestMap.put("step", PaymentFlow.SECOND.getValue() );

        return requestMap;
    }

    @Override
    public Map<String, Object> getPaymentLinkResponse(String rawResponse, int id){
        Map<String, Object> response = new HashMap<>();
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> rawResponseMap = gson.fromJson(rawResponse, mapType);
        String url = (String) rawResponseMap.get("link_url");
        int billId = (int) ((Double) rawResponseMap.get("link_id")).doubleValue();
        response.put("url", url);
        response.put("id", billId);
        return response;
    }

    @Override
    public HashMap<String, String> getHeaderParams() {
        HashMap<String, String> flipHeaderParams = new HashMap<>();
        String contentType = PropertiesReader.getProp(CONFIG_FILE, "content_type");
        String authorization = PropertiesReader.getProp(CONFIG_FILE, "authorization");
        String cookie = PropertiesReader.getProp(CONFIG_FILE, "cookie");
        flipHeaderParams.put("Content-Type",contentType);
        flipHeaderParams.put("idempotency-key", UUID.randomUUID().toString());
        flipHeaderParams.put("X-TIMESTAMP","");
        flipHeaderParams.put("Authorization",authorization);
        flipHeaderParams.put("Cookie",cookie);
        return flipHeaderParams;
    }
}