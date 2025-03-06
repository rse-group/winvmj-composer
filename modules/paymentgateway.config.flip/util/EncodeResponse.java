package paymentgateway.config.flip;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EncodeResponse {
    
    public static String getParamsUrlEncoded(Map<String, Object> vmjExchange) {
		ArrayList<String> paramList = new ArrayList<>();
		for (Map.Entry<String, Object> entry : vmjExchange.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			if (val instanceof String) {
				paramList.add(key + "=" + URLEncoder.encode(val.toString(), StandardCharsets.UTF_8));
			} else if (val instanceof Integer) {
				paramList.add(key + "=" + URLEncoder.encode(val.toString(), StandardCharsets.UTF_8));
			} else if (val instanceof Double) {
				int temp = ((Double) val).intValue();
				paramList.add(key + "=" + URLEncoder.encode(Integer.toString(temp), StandardCharsets.UTF_8));
			}

		}
		String encodedURL = String.join("&",paramList);
		return encodedURL;
	}
    
}
