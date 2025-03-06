package paymentgateway.config.core;

import java.lang.reflect.*;

import java.math.BigInteger;
import java.util.*;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import vmj.routing.route.VMJExchange;

import paymentgateway.config.core.PropertiesReader;

public class ConfigImpl extends ConfigComponent {

    protected ConfigComponent record;

//    public ConfigImpl(){
//
//    }

    public String getVendorName(){
        return "";
    }
    
    public String getRequestString(Map<String, Object> requestMap){
        Gson gson = new Gson();
        return gson.toJson(requestMap);
    }

    public String getProductEnv(String fileName, String serviceName){
        String url = "";
        String baseUrl = (String) PropertiesReader.getProp(fileName, "base_url");
        String apiEndpoint = (String) PropertiesReader.getProp(fileName, serviceName);

        url = baseUrl + apiEndpoint;

        return url;
    }
    
    public Map<String, Object> getCallbackPaymentRequestBody(VMJExchange vmjExchange){
    	throw new UnsupportedOperationException();
    }
    
    public Map<String, Object> getCallbackDisbursementRequestBody(Map<String, Object> requestBody){
    	throw new UnsupportedOperationException();
    }

    public String getProductEnv(String serviceName){
        throw new UnsupportedOperationException();
    }
    
    public String constructUrlParam(String serviceName, Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public String getPaymentDetailEndpoint(String configUrl,String id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getPaymentStatusResponse(String rawResponse, String id){
        throw new UnsupportedOperationException();
    }

    public HttpRequest.Builder getBuilder(HttpRequest.Builder builder, HashMap<String, String> headerParams){
        for (Map.Entry<String, String> e : headerParams.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
        return builder;
    }

    public HashMap<String, String> getHeaderParams(){
        HashMap<String, String> headerParams = new HashMap<>();
        return headerParams;
    }

    public int generateId(){
        String generateUUIDNo = String.format("%010d",new BigInteger(UUID.randomUUID().toString().replace("-",""),16));
        return Integer.parseInt(generateUUIDNo.substring(0,5));
    }

    public List<Map<String,Object>> toListMap(VMJExchange vmjExchange, String name){
        Gson gson = new Gson();
        Type resultType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> result = gson.fromJson(gson.toJson(vmjExchange.getRequestBodyForm(name)), resultType);
        return result;
    }

    public Map<String, Object>  processRequestMap(VMJExchange vmjExchange, String serviceName){
        return vmjExchange.getPayload();
    }

    public Map<String, Object> getDisbursementRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getDomesticDisbursementRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getInternationalDisbursementRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }
    
    public Map<String, Object> getPaymentLinkRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getRetailOutletRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getVirtualAccountRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getEWalletRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getDebitCardRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getCreditCardRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getInvoiceRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getPaymentRoutingRequestBody(Map<String, Object> requestBody){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getPaymentLinkResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getDebitCardResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getCreditCardResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getInvoiceResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getEWalletResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getPaymentRoutingResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getRetailOutletResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getVirtualAccountResponse(String rawResponse, int id){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getDisbursementResponse(String rawResponse){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getSpecialDisbursementResponse(String rawResponse){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getInternationalDisbursementResponse(String rawResponse){
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getAgentDisbursementResponse(String rawResponse){
        throw new UnsupportedOperationException();
    }
}