package webshop.customer.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.OneToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.CascadeType;
//add other required packages

@MappedSuperclass
public abstract class CustomerDecorator extends CustomerComponent{
	protected CustomerComponent record;
		
	public CustomerDecorator (CustomerComponent record) {
		this.record = record;
	}

	public CustomerDecorator (UUID customerId, CustomerComponent record) {
		this.customerId =  customerId;
		this.record = record;
	}
	
	public CustomerDecorator(){
		super();
		this.customerId =  UUID.randomUUID();
	}

    public UUID getCustomerId() {
        return this.record.getCustomerId();
    }

    public void setCustomerId(UUID id) {
        this.record.setCustomerId(id);
    }

    public String getName() {
        return this.record.getName();
    }

    public void setName(String name) {
        this.record.setName(name);
    }

    public String getEmail() {
        return this.record.getEmail();
    }

    public void setEmail(String email) {
        this.record.setEmail(email);
    }
	public HashMap<String, Object> toHashMap() {
        return this.record.toHashMap();
    }

}
