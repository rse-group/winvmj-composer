package webshop.customer.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name="customer_comp")
@Table(name="customer_comp", uniqueConstraints = @UniqueConstraint(columnNames = { "email" }))
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class CustomerComponent implements Customer{
	@Id
	public UUID customerId; 
	public String email;
	public String name;
	protected String objectName = CustomerImpl.class.getName();

	public CustomerComponent() {

	} 

	public UUID getCustomerId(){ return this.customerId; }
	public void setCustomerId(UUID customerId) {this.customerId=customerId; }
	
	public String getEmail(){ return this.email; }
	public void setEmail(String email) {this.email=email; }
	
	public String getName(){ return this.name; }
	public void setName(String name) {this.name=name; }
	
 

	@Override
    public String toString() {
        return "{" +
            " customerId='" + getCustomerId() + "'" +
            " email='" + getEmail() + "'" +
            " name='" + getName() + "'" +
            "}";
    }
	
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> customerMap = new HashMap<String,Object>();
		customerMap.put("id",getCustomerId());
		customerMap.put("email",getEmail());
		customerMap.put("email",getName());
        return customerMap;
    }
}
