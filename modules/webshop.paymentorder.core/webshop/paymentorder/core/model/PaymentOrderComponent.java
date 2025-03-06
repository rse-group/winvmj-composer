package webshop.paymentorder.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.ManyToOne;
import webshop.order.core.Order;

@Entity(name="paymentorder_comp")
@Table(name="paymentorder_comp")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PaymentOrderComponent implements PaymentOrder{
	@Id
	public UUID paymentOrderId; 
	public String paymentId;
	public String paymentStatus;
	public String paymentMethod;
	@ManyToOne(targetEntity=webshop.order.core.OrderComponent.class)
	public Order order;
	protected String objectName = PaymentOrderComponent.class.getName();

	public PaymentOrderComponent() {

	} 

	public UUID getPaymentOrderId(){return this.paymentOrderId;}
	public String getPaymentId(){return this.paymentId;}
	public String getPaymentOrderStatus(){return this.paymentStatus;}
	public String getPaymentMethod(){return this.paymentMethod;}
	public Order getOrder(){return this.order;}

	@Override
    public String toString() {
        return "{" +
            " paymentOrderId='" + getPaymentOrderId() + "'" +
            " paymentStatus='" + getPaymentOrderStatus() + "'" +
            " paymentMethod='" + getPaymentMethod() + "'" +
            " order='" + getOrder() + "'" +
            "}";
    }
	
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> paymentMap = new HashMap<String,Object>();
		paymentMap.put("paymentOrderId",getPaymentOrderId());
		paymentMap.put("paymentId",getPaymentId());
		paymentMap.put("paymentStatus",getPaymentOrderStatus());
		paymentMap.put("paymentMethod",getPaymentMethod());
		paymentMap.put("order",getOrder());

        return paymentMap;
    }
}
