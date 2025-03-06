package webshop.customer.core;
import java.util.*;
import vmj.routing.route.exceptions.*;

import webshop.customer.CustomerFactory;

public class CustomerServiceImpl extends CustomerServiceComponent{

	public Customer getCustomerByEmail(String email){
    	Customer customer = null;
		try {
			customer = customerRepository.getListObject("customer_comp", "email", email).get(0);
		} catch (Exception e) {
			throw new NotFoundException("Customer with email " + email + " not exist.");
		}
		return customer;
	}

}
