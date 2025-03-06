package webshop.order.core;

import java.util.*;
import java.lang.Math;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import webshop.catalog.core.Catalog;
import webshop.customer.core.Customer;

@Entity(name="order_impl")
@Table(name="order_impl")
public class OrderImpl extends OrderComponent {
	public OrderImpl(
			String status, UUID orderId, Date date, int amount, Catalog catalog, int quantity, 
			String city, String street, String state, String country, int zipcode, Customer customer) {
		this.status = status;
		this.orderId = orderId;
		this.date = date;
		this.amount = amount;
		this.catalog = catalog;
		this.quantity = quantity;
		this.city = city;
		this.street = street;
		this.state = state;
		this.country = country;
		this.zipcode = zipcode;
		this.customer = customer;
	}

	public OrderImpl(
			String status, UUID orderId, Date date, int amount, 
			Catalog catalog, int quantity, String city, Customer customer) {
		this.orderId =  orderId;
		this.status = status;
		this.date = date;
		this.amount = amount;
		this.catalog = catalog;
		this.quantity = quantity;
		this.city = city;
		this.street = null;
		this.state = null;
		this.country = null;
		this.zipcode = -1;
		this.customer = customer;
	}

	public OrderImpl(){
		this.orderId =  UUID.randomUUID();
		this.catalog = null;
		this.status = "Not Paid";
		this.date = new Date();
		this.amount = 0;
		this.quantity = 0;
		this.city = "";
		this.street = "";
		this.state = "";
		this.country = "";
		this.zipcode = 0;
		this.customer = null;
	}

}
