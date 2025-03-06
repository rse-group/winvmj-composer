package webshop.order.core;

import webshop.catalog.core.*;
import webshop.customer.core.*;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.ManyToOne;

@Entity(name="order_comp")
@Table(name="order_comp")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class OrderComponent implements Order{
	@Id
	public UUID orderId; 
	public String status;
	public Date date;
	public int amount;
	@ManyToOne(targetEntity=webshop.catalog.core.CatalogComponent.class)
	public Catalog catalog;
	public int quantity;
	public String city;
	public String street;
	public String state;
	public String country;
	public int zipcode;
	@ManyToOne(targetEntity=webshop.customer.core.CustomerComponent.class)
	public Customer customer;
	protected String objectName = OrderImpl.class.getName();

	public OrderComponent() {

	} 
	public UUID getOrderId(){ return this.orderId; }
	public void setOrderId(UUID orderId){ this.orderId = orderId; }
	
	public String getStatus(){ return this.status; }
	public void setStatus(String status){ this.status = status; }

	public Date getDate(){ return this.date; }
	public void setDate(Date date){ this.date = date; }
	
	public int getAmount(){ return this.amount; }
	public void setAmount(int amount){ this.amount = amount; }

	public Catalog getCatalog(){ return this.catalog; }
	public void setCatalog(Catalog catalog){ this.catalog = catalog; }
	
	public int getQuantity(){ return this.quantity; }
	public void setQuantity(int quantity){ this.quantity = quantity; }
	
	public String getCity(){ return this.city; }
	public void setCity(String city){ this.city = city; }
	
	public String getStreet(){ return this.street; }
	public void setStreet(String street){ this.street = street; }
	
	public String getState(){ return this.state; }
	public void setState(String state){ this.state = state; }
	
	public String getCountry(){ return this.country; }
	public void setCountry(String country){ this.country = country; }
	
	public int getZipcode(){ return this.zipcode; }
	public void setZipcode(int zipcode){ this.zipcode = zipcode; }

	public Customer getCustomer() { return this.customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
	@Override
    public String toString() {
        return "{" +
            " status='" + getStatus() + "'" +
            " orderId='" + getOrderId() + "'" +
            " date='" + getDate() + "'" +
            " amount='" + getAmount() + "'" +
            " catalog='" + getCatalog() + "'" +
            " city='" + getCity() + "'" +
            " street='" + getStreet() + "'" +
            " state='" + getState() + "'" +
            " country='" + getCountry() + "'" +
            " zipcode='" + getZipcode() + "'" +
            " customer='" + getCustomer() + "'" +
            "}";
    }
	
    public HashMap<String, Object> toHashMap() {
    	System.out.println("orderId:" + getOrderId());
        HashMap<String, Object> orderMap = new HashMap<String,Object>();
		orderMap.put("orderId",getOrderId());
		orderMap.put("status",getStatus());
		orderMap.put("date",getDate());
		orderMap.put("amount",getAmount());
		orderMap.put("quantity",getQuantity());
		orderMap.put("city",getCity());
		orderMap.put("street",getStreet());
		orderMap.put("state",getState());
		orderMap.put("country",getCountry());
		orderMap.put("zipcode",getZipcode());
		orderMap.put("customer",getCustomer());
		orderMap.put("name", getCatalog().getName());  
		orderMap.put("price", getCatalog().getPrice()); 
		orderMap.put("pictureURL", getCatalog().getPictureUrl()); 

        return orderMap;
    }
}
