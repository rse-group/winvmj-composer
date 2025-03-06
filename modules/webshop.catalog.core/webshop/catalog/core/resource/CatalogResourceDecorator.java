package webshop.catalog.core;
import java.util.*;

import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;

public abstract class CatalogResourceDecorator extends CatalogResourceComponent{
	protected CatalogResourceComponent record;

    public CatalogResourceDecorator(CatalogResourceComponent record) {
        this.record = record;
    }

    public HashMap<String,Object> saveCatalog(VMJExchange vmjExchange){
		return record.saveCatalog(vmjExchange);
	}

    public HashMap<String, Object> updateCatalog(VMJExchange vmjExchange){
		return record.updateCatalog(vmjExchange);
	}

    public HashMap<String, Object> getCatalog(VMJExchange vmjExchange){
		return record.getCatalog(vmjExchange);
	}

    public List<HashMap<String,Object>> getAllCatalog(VMJExchange vmjExchange){
		return record.getAllCatalog(vmjExchange);
	}

	public List<HashMap<String,Object>> getCatalogByName(VMJExchange vmjExchange){
		return record.getCatalogByName(vmjExchange);
	}

    public List<HashMap<String,Object>> deleteCatalog(VMJExchange vmjExchange){
		return record.deleteCatalog(vmjExchange);
	}

}
