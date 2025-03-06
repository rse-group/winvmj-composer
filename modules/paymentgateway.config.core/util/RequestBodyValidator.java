package paymentgateway.config.core;

import java.util.Map;

import vmj.routing.route.exceptions.BadRequestException;

public class RequestBodyValidator {
    public static String stringRequestBodyValidator(Map<String, Object> requestBody, String key) {
        if (requestBody.containsKey(key)) {
            return (String) requestBody.get(key);
        }
        
        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                key
            )
        );
    }

    public static String stringRequestBodyValidator(Map<String, Object> requestBody, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            if (requestBody.containsKey(keys[i])) {
                return (String) requestBody.get(keys[i]);
            }
        }

        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                String.join(", ", keys)
            )
        );
    }

    public static int intRequestBodyValidator(Map<String, Object> requestBody, String key) {
        if (requestBody.containsKey(key)) {
            return Integer.parseInt((String) requestBody.get(key));
        }

        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                key
            )
        );
    }

    public static int intRequestBodyValidator(Map<String, Object> requestBody, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            if (requestBody.containsKey(keys[i])) {
                return Integer.parseInt((String) requestBody.get(keys[i]));
            }
        }

        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                String.join(", ", keys)
            )
        );
    }

    public static double doubleRequestBodyValidator(Map<String, Object> requestBody, String key) {
        if (requestBody.containsKey(key)) {
            return Double.parseDouble((String) requestBody.get(key));
        }

        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                key
            )
        );
    }

    public static double doubleRequestBodyValidator(Map<String, Object> requestBody, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            if (requestBody.containsKey(keys[i])) {
                return Double.parseDouble((String) requestBody.get(keys[i]));
            }
        }

        throw new BadRequestException(
            String.format(
                "%s tidak ditemukan pada payload.",
                String.join(", ", keys)
            )
        );
    }
}
