package webshop.seller.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface SellerService {
	Seller getSellerByEmail(String email);
}
