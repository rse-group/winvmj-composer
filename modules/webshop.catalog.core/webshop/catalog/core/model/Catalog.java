package webshop.catalog.core;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import java.util.*;
import webshop.seller.core.*;

public interface Catalog {
	public UUID getCatalogId();
	public void setCatalogId(UUID catalogId);
	public String getName();
	public void setName(String name);
	public String getDescription();
	public void setDescription(String description);
	public int getPrice();
	public void setPrice(int price);
	public String getPictureUrl();
	public void setPictureUrl(String pictureURL);
	public int getAvailableStock();
	public void setAvailableStock(int availableStock);
	public String getCategory();
	public void setCategory(String category);
	public boolean getIsDeleted();
	public void setIsDeleted(boolean isDeleted);
	public Seller getSeller();
    public void setSeller(Seller seller);
	HashMap<String, Object> toHashMap();
}
