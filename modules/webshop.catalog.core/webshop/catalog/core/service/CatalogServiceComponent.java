package webshop.catalog.core;
import java.util.*;

import vmj.hibernate.integrator.RepositoryUtil;

public abstract class CatalogServiceComponent implements CatalogService{
	protected RepositoryUtil<Catalog> catalogRepository;

    public CatalogServiceComponent(){
        this.catalogRepository = new RepositoryUtil<Catalog>(webshop.catalog.core.CatalogComponent.class);
    }	

    public abstract Catalog saveCatalog(HashMap<String, Object> body, String email); 
	public abstract Catalog updateCatalog(HashMap<String, Object> body);
    public abstract Catalog getCatalog(UUID catalogId);
    public abstract List<Catalog> getAllCatalog();
    public abstract List<Catalog> getCatalogByName(String name);
    public abstract List<HashMap<String, Object>> transformCatalogListToHashMap(List<Catalog> CatalogList);
    public abstract List<Catalog> deleteCatalog(UUID catalogId);

}
