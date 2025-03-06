package webshop.order.core;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import java.util.*;
import webshop.catalog.core.Catalog;
import webshop.customer.core.Customer;

public interface Order {

    public UUID getOrderId();
    public void setOrderId(UUID orderId);

    public String getStatus();
    public void setStatus(String status);

    public Date getDate();
    public void setDate(Date date);

    public int getAmount();
    public void setAmount(int amount);

    public Catalog getCatalog();
    public void setCatalog(Catalog catalog);

    public int getQuantity();
    public void setQuantity(int quantity);

    public String getCity();
    public void setCity(String city);

    public String getStreet();
    public void setStreet(String street);

    public String getState();
    public void setState(String state);

    public String getCountry();
    public void setCountry(String country);

    public int getZipcode();
    public void setZipcode(int zipcode);

    public Customer getCustomer();
    public void setCustomer(Customer customer);

    public HashMap<String, Object> toHashMap();
}
