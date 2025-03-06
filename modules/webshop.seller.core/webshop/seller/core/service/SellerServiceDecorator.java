package webshop.seller.core;
import java.util.*;

public abstract class SellerServiceDecorator extends SellerServiceComponent{
	protected SellerServiceComponent record;

    public SellerServiceDecorator(SellerServiceComponent record) {
        this.record = record;
    }

	public Seller getSellerByEmail(String email){
		return record.getSellerByEmail(email);
	}

}
