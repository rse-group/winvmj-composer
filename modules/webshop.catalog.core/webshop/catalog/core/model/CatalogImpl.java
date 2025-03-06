package webshop.catalog.core;
import webshop.seller.core.Seller;
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

@Entity(name="catalog_impl")
@Table(name="catalog_impl")
public class CatalogImpl extends CatalogComponent {

	public CatalogImpl(UUID catalogId, String name, String description, int price, String pictureURL, int availableStock, String category, Seller seller) {
		this.catalogId = catalogId;
		this.name = name;
		this.description = description;
		this.price = price;
		this.pictureURL = pictureURL;
		this.availableStock = availableStock;
		this.category = category;
		this.seller = seller;
	}

	public CatalogImpl(String name, String description, int price, String pictureURL, int availableStock, String category, Seller seller) {
		this.catalogId = UUID.randomUUID();
		this.name = name;
		this.description = description;
		this.price = price;
		this.pictureURL = pictureURL;
		this.availableStock = availableStock;
		this.category = category;
		this.seller = seller;
	}

	public CatalogImpl() {
		this.catalogId = UUID.randomUUID();
		this.name = "";
		this.description = "";
		this.price = 0;
		this.pictureURL = "";
		this.availableStock = 0;
		this.category = "";
		this.seller = null;
	}

}