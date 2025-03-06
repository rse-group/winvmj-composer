package webshop.seller.core;

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


@Entity(name="seller_impl")
@Table(name="seller_impl")
public class SellerImpl extends SellerComponent {

	public SellerImpl(UUID sellerId, String email, String name) {
		this.sellerId = sellerId;
		this.email = email;
		this.name = name;
	}

	public SellerImpl(String email, String name) {
		this.sellerId =  UUID.randomUUID();
		this.email = email;
		this.name = name;
		
	}

	public SellerImpl() {
		this.sellerId =  UUID.randomUUID();
		this.email = "";
		this.name = "";
	}


}
