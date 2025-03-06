package webshop.customer.core;

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


@Entity(name="customer_impl")
@Table(name="customer_impl")
public class CustomerImpl extends CustomerComponent {

	public CustomerImpl(UUID customerId, String email, String name) {
		this.customerId = customerId;
		this.email = email;
		this.name = name;
	}

	public CustomerImpl(String email, String name) {
		this.customerId =  UUID.randomUUID();
		this.email = email;
		this.name = name;
		
	}

	public CustomerImpl() {
		this.customerId =  UUID.randomUUID();
		this.email = "";
		this.name = "";
	}


}
