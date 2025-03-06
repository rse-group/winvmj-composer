package webshop.order.unauthorized;

import webshop.order.core.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

@Entity(name="order_unauthorized")
@Table(name="order_unauthorized")
public class OrderImpl extends OrderDecorator {

	public String email;
	public OrderImpl(UUID orderId, OrderComponent record, String email) {
		super(record, orderId);
		this.email = email;
	}
	public OrderImpl(OrderComponent record) {
		super(record);
		this.email = null;
	}
	public OrderImpl() {
		super();
		this.email = null;
	}
	
    public String getEmail(){ return this.email; }
	public void setEmail(String email){ this.email = email; }
	
}
