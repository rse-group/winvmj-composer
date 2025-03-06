package webshop.catalog.brand;

import webshop.catalog.core.CatalogDecorator;

import java.util.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import vmj.routing.route.VMJExchange;

import webshop.catalog.core.Catalog;
import webshop.catalog.core.CatalogComponent;

@Entity(name="catalog_brand")
@Table(name="catalog_brand")
public class CatalogImpl extends CatalogDecorator {
	public String brand;

    public CatalogImpl(){
        super();
    }
    
    public CatalogImpl(String brand) {
    	super();
    	this.brand = brand;
    }
    
    public CatalogImpl(CatalogComponent record, String brand) {
        super(record);
    	this.brand = brand;
    }

    public CatalogImpl(UUID catalogId, CatalogComponent record, String brand) {
        super(catalogId, record);
        this.brand = brand;
      }

    public String getBrand() {
        return this.brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public String toString() {
        return "{" +
            " catalogId ='" + catalogId + "'" +
            " brand='" + getBrand() + "'" +
            "}";
    }
 
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> catalogMap = new HashMap<String,Object>();
        catalogMap.put("catalogId", catalogId);
        catalogMap.put("name", getName());
        catalogMap.put("description", getDescription());
        catalogMap.put("brand", brand);
		catalogMap.put("price",getPrice());
		catalogMap.put("pictureURL",getPictureUrl());
		catalogMap.put("availableStock",getAvailableStock());
		catalogMap.put("category",getCategory());
        
        return catalogMap;
    }
}

