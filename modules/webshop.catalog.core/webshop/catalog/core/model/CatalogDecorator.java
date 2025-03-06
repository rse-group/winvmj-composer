package webshop.catalog.core;
import webshop.seller.core.Seller;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.OneToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.CascadeType;

@MappedSuperclass
public abstract class CatalogDecorator extends CatalogComponent{
	@OneToOne(cascade = CascadeType.ALL)
	protected CatalogComponent record;
		
	public CatalogDecorator (CatalogComponent record) {
        this.record = record;
		this.catalogId = UUID.randomUUID();
	}

	public CatalogDecorator (UUID catalogId, CatalogComponent record) {
		this.catalogId =  catalogId;
		this.record = record;
	}
	
	public CatalogDecorator() {
        super();
        this.record = new CatalogImpl();
        this.catalogId = UUID.randomUUID();
    }

	public CatalogComponent getRecord() {
        return this.record;
    }

    public void setRecord(CatalogComponent record) {
        this.record = record;
    }
	
	public UUID getId(){ return this.record.catalogId; }
	public void setId(UUID catalogId){ this.record.catalogId = catalogId; }
	
	public String getName(){ return this.record.name; }
	public void setName(String name){ this.record.name = name; }
	
	public String getDescription(){ return this.record.description; }
	public void setDescription(String description){ this.record.description = description; }
	
	public int getPrice(){ return this.record.price; }
	public void setPrice(int price) {this.record.price = price; }
	
	public String getPictureUrl(){ return this.record.pictureURL; }
	public void setPictureUrl(String pictureURL){this.record.pictureURL = pictureURL; }
	
	public int getAvailableStock(){ return this.record.availableStock; }
	public void setAvailableStock(int availableStock){this.record.availableStock = availableStock; }
	
	public String getCategory() {return this.record.category; }
	public void setCategory(String category) {this.record.category = category; }
	
	public boolean getIsDeleted() {return this.isDeleted; }
	public void setIsDeleted(boolean isDeleted) {this.isDeleted = isDeleted; }
	
	public Seller getSeller() { return this.record.getSeller(); }
    public void setSeller(Seller seller) { this.record.setSeller(seller); }
    

	public HashMap<String, Object> toHashMap() {
        return this.record.toHashMap();
    }

}