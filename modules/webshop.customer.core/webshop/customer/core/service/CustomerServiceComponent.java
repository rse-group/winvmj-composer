package webshop.customer.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;

public abstract class CustomerServiceComponent implements CustomerService{
	protected RepositoryUtil<Customer> customerRepository;

    public CustomerServiceComponent(){
        this.customerRepository = new RepositoryUtil<Customer>(webshop.customer.core.CustomerComponent.class);
    }	
    public abstract Customer getCustomerByEmail(String email);


}
