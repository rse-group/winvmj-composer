package webshop.paymentorder.core;

import java.lang.Math;
import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import webshop.order.core.Order;

@Entity(name="paymentorder_impl")
@Table(name="paymentorder_impl")
public class PaymentOrderImpl extends PaymentOrderComponent {

	public PaymentOrderImpl(UUID paymentOrderId, String paymentId, String paymentStatus, String paymentMethod, Order order) {
		this.paymentOrderId = paymentOrderId;
		this.paymentId = paymentId;
		this.paymentStatus = paymentStatus;
		this.paymentMethod = paymentMethod;
		this.order = order;
		this.objectName = PaymentOrderImpl.class.getName();
	}

	public PaymentOrderImpl(String paymentId, String paymentStatus, String paymentMethod, Order order) {
		this.paymentOrderId =  UUID.randomUUID();
		this.paymentId = paymentId;
		this.paymentStatus = paymentStatus;
		this.paymentMethod = paymentMethod;
		this.order = order;
		this.objectName = PaymentOrderImpl.class.getName();
	}
	public PaymentOrderImpl() {
		this.paymentOrderId =  UUID.randomUUID();
		this.paymentId = "";
		this.paymentStatus = "";
		this.paymentMethod = "";
		this.order = null;
		this.objectName = PaymentOrderImpl.class.getName();
	}
}
