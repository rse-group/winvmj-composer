package webshop.customer.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface CustomerService {
	Customer getCustomerByEmail(String email);
}
