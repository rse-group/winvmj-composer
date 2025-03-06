package webshop.seller.core;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import java.util.*;

public interface Seller {
	public UUID getSellerId();
	public void setSellerId(UUID sellerId);
	public String getEmail();
	public void setEmail(String email);
	public String getName();
	public void setName(String name);
	HashMap<String, Object> toHashMap();
}
