package webshop.seller.core;

import java.util.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name="seller_comp")
@Table(name="seller_comp", uniqueConstraints = @UniqueConstraint(columnNames = { "email" }))
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class SellerComponent implements Seller{
	@Id
	public UUID sellerId; 
	public String email;
	public String name;
	protected String objectName = SellerImpl.class.getName();

	public SellerComponent() {

	} 

	public UUID getSellerId(){ return this.sellerId; }
	public void setSellerId(UUID sellerId) {this.sellerId=sellerId; }
	
	public String getEmail(){ return this.email; }
	public void setEmail(String email) {this.email=email; }
	
	public String getName(){ return this.name; }
	public void setName(String name) {this.name=name; }
	
 

	@Override
    public String toString() {
        return "{" +
            " id='" + getSellerId() + "'" +
            " email='" + getEmail() + "'" +
            " name='" + getName() + "'" +
            "}";
    }
	
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> sellerMap = new HashMap<String,Object>();
		sellerMap.put("id",getSellerId());
		sellerMap.put("email",getEmail());
		sellerMap.put("email",getName());
        return sellerMap;
    }
}
