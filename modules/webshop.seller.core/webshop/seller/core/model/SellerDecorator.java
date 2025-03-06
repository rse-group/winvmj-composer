package webshop.seller.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.OneToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.CascadeType;
//add other required packages

@MappedSuperclass
public abstract class SellerDecorator extends SellerComponent{
	protected SellerComponent record;
		
	public SellerDecorator (SellerComponent record) {
		this.record = record;
	}

	public SellerDecorator (UUID sellerId, SellerComponent record) {
		this.sellerId =  sellerId;
		this.record = record;
	}
	
	public SellerDecorator(){
		super();
		this.sellerId =  UUID.randomUUID();
	}

    public UUID getSellerId() {
        return this.record.getSellerId();
    }

    public void setSellerId(UUID sellerId) {
        this.record.setSellerId(sellerId);
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
