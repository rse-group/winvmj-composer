package webshop.seller.core;
import java.util.*;
import vmj.routing.route.exceptions.*;

import webshop.seller.SellerFactory;

public class SellerServiceImpl extends SellerServiceComponent{

	public Seller getSellerByEmail(String email){
    	Seller seller = null;
		try {
			seller = sellerRepository.getListObject("seller_comp", "email", email).get(0);
		} catch (Exception e) {
			throw new NotFoundException("Seller with email "+email+" not exist in repository.");
		}
		return seller;
	}

}
