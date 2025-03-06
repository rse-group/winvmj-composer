package webshop.catalog.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface CatalogResource {
	HashMap<String,Object> saveCatalog(VMJExchange vmjExchange);
    HashMap<String, Object> updateCatalog(VMJExchange vmjExchange);
    HashMap<String, Object> getCatalog(VMJExchange vmjExchange);
    List<HashMap<String,Object>> getAllCatalog(VMJExchange vmjExchange);
    List<HashMap<String,Object>> getCatalogByName(VMJExchange vmjExchange);
    List<HashMap<String,Object>> deleteCatalog(VMJExchange vmjExchange);
}
