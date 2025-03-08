package webshop.catalog.core;
import java.util.*;

public abstract class CatalogServiceDecorator extends CatalogServiceComponent{
	protected CatalogServiceComponent record;

    public CatalogServiceDecorator(CatalogServiceComponent record) {
        this.record = record;
    }

    public Catalog saveCatalog(HashMap<String, Object> body, String email){
		return record.saveCatalog(body, email);
	}

    public Catalog updateCatalog(HashMap<String, Object> body){
		return record.updateCatalog(body);
	}

    public Catalog getCatalog(UUID catalogId){
		return record.getCatalog(catalogId);
	}

    public List<Catalog> getAllCatalog(){
		return record.getAllCatalog();
	}

	public List<Catalog> getCatalogByName(String name){
		return record.getCatalogByName(name);
	}

    public List<HashMap<String, Object>> transformCatalogListToHashMap(List<Catalog> catalogList){
		return record.transformCatalogListToHashMap(catalogList);
	}

    public List<Catalog> deleteCatalog(UUID catalogId){
		return record.deleteCatalog(catalogId);
	}

	public void updateAndPublishCatalog(Catalog catalog) {record.updateAndPublishCatalog(catalog);}

}
