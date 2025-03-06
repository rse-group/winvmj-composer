package paymentgateway.config.core;

import java.util.*;
import java.net.http.HttpRequest;
import vmj.routing.route.VMJExchange;

public abstract class ConfigDecorator extends ConfigComponent{

    protected ConfigComponent record;

    public ConfigDecorator(ConfigComponent record){
        this.record = record;
    }

    public ConfigDecorator(){
    }

    public String getVendorName(){
        return record.getVendorName();
    }

    public String getProductEnv(String fileName, String serviceName){
        return record.getProductEnv(fileName, serviceName);
    }
    
    public Map<String, Object> getCallbackPaymentRequestBody(VMJExchange vmjExchange){
    	return record.getCallbackPaymentRequestBody(vmjExchange);
    }
    
    public Map<String, Object> getCallbackDisbursementRequestBody(Map<String, Object> requestBody){
    	return record.getCallbackDisbursementRequestBody(requestBody);
    }

    public String getRequestString(Map<String, Object> requestMap){
        return record.getRequestString(requestMap);
    }

    public String getPaymentDetailEndpoint(String configUrl,String id){
        return record.getPaymentDetailEndpoint(configUrl,id);
    }

    public Map<String, Object> getPaymentStatusResponse(String rawResponse, String id){
        return record.getPaymentStatusResponse(rawResponse,id);
    }

    public String getProductEnv(String serviceName){
        return record.getProductEnv(serviceName);
    }

    public HttpRequest.Builder getBuilder(HttpRequest.Builder builder, HashMap<String, String> headerParams){
        return record.getBuilder(builder,headerParams);
    }

    public HashMap<String, String> getHeaderParams(){
        return record.getHeaderParams();
    }

    public String constructUrlParam(String serviceName, Map<String, Object> requestBody){
        return record.constructUrlParam(serviceName, requestBody);
    }

    public Map<String, Object> processRequestMap(VMJExchange vmjExchange, String serviceName){
        return record.processRequestMap(vmjExchange, serviceName);
    }

    public List<Map<String,Object>> toListMap(VMJExchange vmjExchange, String name){
        return record.toListMap(vmjExchange, name);
    }

    public int generateId(){
        return record.generateId();    
    }

    public Map<String, Object> getDisbursementRequestBody(Map<String, Object> requestBody){
        return record.getDisbursementRequestBody(requestBody);
    }

    public Map<String, Object> getDomesticDisbursementRequestBody(Map<String, Object> requestBody){
        return record.getDomesticDisbursementRequestBody(requestBody);
    }

    public Map<String, Object> getInternationalDisbursementRequestBody(Map<String, Object> requestBody){
        return record.getInternationalDisbursementRequestBody(requestBody);
    }

    public Map<String, Object> getPaymentLinkRequestBody(Map<String, Object> requestBody){
        return record.getPaymentLinkRequestBody(requestBody);
    }

    public Map<String, Object> getRetailOutletRequestBody(Map<String, Object> requestBody){
        return record.getRetailOutletRequestBody(requestBody);
    }

    public Map<String, Object> getVirtualAccountRequestBody(Map<String, Object> requestBody){
        return record.getVirtualAccountRequestBody(requestBody);
    }

    public Map<String, Object> getEWalletRequestBody(Map<String, Object> requestBody){
        return record.getEWalletRequestBody(requestBody);
    }

    public Map<String, Object> getDebitCardRequestBody(Map<String, Object> requestBody){
        return record.getDebitCardRequestBody(requestBody);
    }

    public Map<String, Object> getCreditCardRequestBody(Map<String, Object> requestBody){
        return record.getCreditCardRequestBody(requestBody);
    }

    public Map<String, Object> getInvoiceRequestBody(Map<String, Object> requestBody){
        return record.getInvoiceRequestBody(requestBody);
    }

    public Map<String, Object> getPaymentRoutingRequestBody(Map<String, Object> requestBody){
        return record.getPaymentRoutingRequestBody(requestBody);
    }

    public Map<String, Object> getPaymentLinkResponse(String rawResponse, int id){
        return record.getPaymentLinkResponse(rawResponse, id);
    }

    public Map<String, Object> getDebitCardResponse(String rawResponse, int id){
        return record.getDebitCardResponse(rawResponse, id);
    }

    public Map<String, Object> getCreditCardResponse(String rawResponse, int id){
        return record.getCreditCardResponse(rawResponse, id);
    }

    public Map<String, Object> getInvoiceResponse(String rawResponse, int id){
        return record.getInvoiceResponse(rawResponse, id);
    }

    public Map<String, Object> getEWalletResponse(String rawResponse, int id){
        return record.getEWalletResponse(rawResponse, id);
    }

    public Map<String, Object> getPaymentRoutingResponse(String rawResponse, int id){
        return record.getPaymentRoutingResponse(rawResponse, id);
    }

    public Map<String, Object> getRetailOutletResponse(String rawResponse, int id){
        return record.getRetailOutletResponse(rawResponse, id);
    }

    public Map<String, Object> getVirtualAccountResponse(String rawResponse, int id){
        return record.getVirtualAccountResponse(rawResponse, id);
    }

    public Map<String, Object> getDisbursementResponse(String rawResponse){
        return record.getDisbursementResponse(rawResponse);
    }

    public Map<String, Object> getSpecialDisbursementResponse(String rawResponse){
        return record.getSpecialDisbursementResponse(rawResponse);
    }

    public Map<String, Object> getInternationalDisbursementResponse(String rawResponse){
        return record.getInternationalDisbursementResponse(rawResponse);
    }

    public Map<String, Object> getAgentDisbursementResponse(String rawResponse){
        return record.getAgentDisbursementResponse(rawResponse);
    }
    
}