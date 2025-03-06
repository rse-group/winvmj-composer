package webshop.paymentorder.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface PaymentOrderResource {
    List<HashMap<String,Object>> getUnpaidOrderHistory(VMJExchange vmjExchange);
}
