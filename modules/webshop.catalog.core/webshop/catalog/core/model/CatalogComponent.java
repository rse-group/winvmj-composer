package webshop.catalog.core;
import webshop.seller.core.*;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.ManyToOne;

@Entity
@Table(name="catalog_comp")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class CatalogComponent implements Catalog{
	@Id
	public UUID catalogId; 
	public String name;
	public String description;
	public int price;
	public String pictureURL;
	public int availableStock;
	public String category;
	protected String objectName = CatalogImpl.class.getName();
	public boolean isDeleted = false;
	@ManyToOne(targetEntity=webshop.seller.core.SellerComponent.class)
	public Seller seller;

	public CatalogComponent() {

	} 

	public UUID getCatalogId(){ return this.catalogId; }
	public void setCatalogId(UUID catalogId){ this.catalogId = catalogId; }
	
	public String getName(){ return this.name; }
	public void setName(String name){ this.name = name; }
	
	public String getDescription(){ return this.description; }
	public void setDescription(String description){ this.description = description; }
	
	public int getPrice(){ return this.price; }
	public void setPrice(int price) {this.price = price; }
	
	public String getPictureUrl(){ return this.pictureURL; }
	public void setPictureUrl(String pictureURL){this.pictureURL = pictureURL; }
	
	public int getAvailableStock(){ return this.availableStock; }
	public void setAvailableStock(int availableStock){this.availableStock = availableStock; }
	
	public String getCategory() {return this.category; }
	public void setCategory(String category) {this.category = category; }
	
	public boolean getIsDeleted() {return this.isDeleted; }
	public void setIsDeleted(boolean isDeleted) {this.isDeleted = isDeleted; }

	public Seller getSeller() {return this.seller; }
	public void setSeller(Seller seller) {this.seller = seller; }
	
	@Override
    public String toString() {
        return "{" +
            " catalogId='" + getCatalogId() + "'" +
            " name='" + getName() + "'" +
            " description='" + getDescription() + "'" +
            " price='" + getPrice() + "'" +
            " pictureURL='" + getPictureUrl() + "'" +
            " availableStock='" + getAvailableStock() + "'" +
            " category='" + getCategory() + "'" +
            "}";
    }
	
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> catalogMap = new HashMap<String,Object>();
		catalogMap.put("catalogId",getCatalogId());
		catalogMap.put("name",getName());
		catalogMap.put("description",getDescription());
		catalogMap.put("price",getPrice());
		catalogMap.put("pictureURL",getPictureUrl());
		catalogMap.put("availableStock",getAvailableStock());
		catalogMap.put("category",getCategory());

        return catalogMap;
    }
}
