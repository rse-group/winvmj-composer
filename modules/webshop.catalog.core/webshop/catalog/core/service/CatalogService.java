package webshop.catalog.core;
import java.util.*;

import vmj.routing.route.VMJExchange;

public interface CatalogService {
	Catalog saveCatalog(HashMap<String, Object> body, String email);
    Catalog updateCatalog(HashMap<String, Object> body);
    Catalog getCatalog(UUID catalogId);
    List<Catalog> getAllCatalog();
    List<Catalog> getCatalogByName(String name);
    List<Catalog> deleteCatalog(UUID catalogId);
    List<HashMap<String, Object>> transformCatalogListToHashMap(List<Catalog> catalogList);

    void updateAndPublishCatalog(Catalog catalog);
}
