package webshop.order.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.OneToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.CascadeType;
import webshop.catalog.core.Catalog;
import webshop.customer.core.Customer;

@MappedSuperclass
public abstract class OrderDecorator extends OrderComponent {

    @OneToOne(cascade = CascadeType.ALL)
    protected OrderComponent record;

    public OrderDecorator(OrderComponent record, UUID orderId) {
    	super();
        this.record = record;
        this.orderId = orderId;
    }
    
    public OrderDecorator(OrderComponent record) {
        super();
        this.record = new OrderImpl();
        this.orderId = UUID.randomUUID();
    }

    public OrderDecorator() {
        super();
        this.record = new OrderImpl();
        this.orderId = UUID.randomUUID();
    }

    public OrderComponent getRecord() { return this.record; }
    public void setRecord(OrderComponent record) { this.record = record; }

    public UUID getId() { return this.record.getOrderId(); }
    public void setId(UUID orderId) { this.record.setOrderId(orderId); }

    public String getStatus() { return this.record.getStatus(); }
    public void setStatus(String status) { this.record.setStatus(status); }

    public Date getDate() { return this.record.getDate(); }
    public void setDate(Date date) { this.record.setDate(date); }

    public int getAmount() { return this.record.getAmount(); }
    public void setAmount(int amount) { this.record.setAmount(amount); }

    public Catalog getCatalog() { return this.record.getCatalog(); }
    public void setCatalog(Catalog catalog) { this.record.setCatalog(catalog); }

    public int getQuantity() { return this.record.getQuantity(); }
    public void setQuantity(int quantity) { this.record.setQuantity(quantity); }

    public String getCity() { return this.record.getCity(); }
    public void setCity(String city) { this.record.setCity(city); }

    public String getStreet() { return this.record.getStreet(); }
    public void setStreet(String street) { this.record.setStreet(street); }

    public String getState() { return this.record.getState(); }
    public void setState(String state) { this.record.setState(state); }

    public String getCountry() { return this.record.getCountry(); }
    public void setCountry(String country) { this.record.setCountry(country); }

    public int getZipcode() { return this.record.getZipcode(); }
    public void setZipcode(int zipcode) { this.record.setZipcode(zipcode); }

    public Customer getCustomer() { return this.record.getCustomer(); }
    public void setCustomer(Customer customer) { this.record.setCustomer(customer); }
    
    public HashMap<String, Object> toHashMap() { return this.record.toHashMap(); }
}
