package webshop.seller.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;

public abstract class SellerServiceComponent implements SellerService{
	protected RepositoryUtil<Seller> sellerRepository;

    public SellerServiceComponent(){
        this.sellerRepository = new RepositoryUtil<Seller>(webshop.seller.core.SellerComponent.class);
    }	
    public abstract Seller getSellerByEmail(String email);


}
