package webshop.paymentorder.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.OneToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.CascadeType;


@MappedSuperclass
public abstract class PaymentOrderDecorator extends PaymentOrderComponent{
	protected PaymentOrderComponent record;
		
	public PaymentOrderDecorator (PaymentOrderComponent record) {
		this.record = record;
	}

	public PaymentOrderDecorator (UUID paymentOrderId, PaymentOrderComponent record) {
		this.paymentOrderId = paymentOrderId;
		this.record = record;
	}
	
	public PaymentOrderDecorator(){
		super();
		this.paymentOrderId =  UUID.randomUUID();
	}

	public PaymentOrderComponent getRecord() { return this.record; }

	public HashMap<String, Object> toHashMap() {
        return this.record.toHashMap();
    }

}
