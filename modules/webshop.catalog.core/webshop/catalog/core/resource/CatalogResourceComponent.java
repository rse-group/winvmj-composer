package webshop.catalog.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;
import vmj.routing.route.VMJExchange;
//add other required packages

public abstract class CatalogResourceComponent implements CatalogResource{
	protected RepositoryUtil<Catalog> catalogRepository;

    public CatalogResourceComponent(){
        this.catalogRepository = new RepositoryUtil<Catalog>(webshop.catalog.core.CatalogComponent.class);
    }	

    public abstract HashMap<String,Object> saveCatalog(VMJExchange vmjExchange); 
	public abstract HashMap<String, Object> updateCatalog(VMJExchange vmjExchange);
    public abstract HashMap<String, Object> getCatalog(VMJExchange vmjExchange);
    public abstract List<HashMap<String,Object>> getAllCatalog(VMJExchange vmjExchange);
    public abstract List<HashMap<String,Object>> getCatalogByName(VMJExchange vmjExchange);
    public abstract List<HashMap<String,Object>> deleteCatalog(VMJExchange vmjExchange);

}
