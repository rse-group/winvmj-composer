import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ApiGateway {

    private static final Map<String, String> routeMap = new HashMap<>();

    private static void initRoutingMap(){
    	routeMap.put("/auth/user/save", "ServiceAuth_URL");
    	routeMap.put("/auth/register", "ServiceAuth_URL");
    	routeMap.put("/auth/forgot-password-token", "ServiceAuth_URL");
    	routeMap.put("/auth/forgot-password", "ServiceAuth_URL");
    	routeMap.put("/auth/login/pwd", "ServiceAuth_URL");
    	routeMap.put("/call/role/save", "ServiceAuth_URL");
    	routeMap.put("/call/role/detail", "ServiceAuth_URL");
    	routeMap.put("/call/role/list", "ServiceAuth_URL");
    	routeMap.put("/call/role/update", "ServiceAuth_URL");
    	routeMap.put("/call/role/delete", "ServiceAuth_URL");
    	routeMap.put("/call/user/save", "ServiceAuth_URL");
    	routeMap.put("/call/user/detail", "ServiceAuth_URL");
    	routeMap.put("/call/user/list", "ServiceAuth_URL");
    	routeMap.put("/call/user/update", "ServiceAuth_URL");
    	routeMap.put("/call/user/delete", "ServiceAuth_URL");
    	routeMap.put("/call/user/changerole", "ServiceAuth_URL");
    }


    public static void main(String[] args) throws IOException {
    	initRoutingMap();

        String hostAddress = System.getenv("AMANAH_HOST_BE") != null ? System.getenv("AMANAH_HOST_BE") : "localhost";

        String portStr = System.getenv("AMANAH_PORT_BE") != null ? System.getenv("AMANAH_PORT_BE") : "7776";
        int port = Integer.parseInt(portStr);

        HttpServer server = HttpServer.create(new InetSocketAddress(hostAddress, port), 0);
        server.createContext("/", new ProxyHandler());
        server.setExecutor(null);
        System.out.println("API Gateway running on " + hostAddress + ":" + port);
        server.start();
    }

    static class ProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            String fullPath = path + (query != null ? "?" + query : "");

            String method = exchange.getRequestMethod();
            String serviceEnvVar = routeMap.get(path);

            if (serviceEnvVar == null) {
                String msg = "404 Not Found: The requested URL was not found on this server " + path;
                exchange.sendResponseHeaders(404, msg.getBytes().length);
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
                return;
            }

            String serviceUrl = System.getenv(serviceEnvVar);
            if (serviceUrl == null) {
                String msg = "503 Service Unavailable: Env var " + serviceEnvVar + " is not set";
                exchange.sendResponseHeaders(503, msg.getBytes().length);
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
                return;
            }

            URL url = new URL(serviceUrl + fullPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            exchange.getRequestHeaders().forEach((k, v) -> conn.setRequestProperty(k, String.join(",", v)));

            if (exchange.getRequestBody().available() > 0) {
                conn.setDoOutput(true);
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody);
                }
                try (OutputStream os = conn.getOutputStream();
                     InputStream is = exchange.getRequestBody()) {
                    is.transferTo(os);
                }
            }

            int responseCode = conn.getResponseCode();
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    for (String value : values) {
                        exchange.getResponseHeaders().add(key, value);
                    }
                }
            });

            InputStream backendResponse = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] responseBytes = backendResponse.readAllBytes();

            exchange.sendResponseHeaders(responseCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

    }
}
