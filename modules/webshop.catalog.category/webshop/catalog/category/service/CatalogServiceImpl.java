package webshop.catalog.category;

import webshop.catalog.core.CatalogResourceDecorator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import webshop.catalog.core.*;

public class CatalogServiceImpl extends CatalogServiceDecorator {

    public CatalogServiceImpl (CatalogServiceComponent record) {
        super(record);
    }

    public HashMap<String, Object> getCatalogByCategory(String category){
        List<Catalog> filteredCatalogs = catalogRepository.getListObject("catalog_impl", "category", category);
    	filteredCatalogs.removeIf(Catalog::getIsDeleted); // Remove all elements with isDeleted = true
        
        HashMap<String, Object> result = new HashMap<>();
        result.put("catalogs", filteredCatalogs);
        return result;
    }
    
    public List<Map<String, String>> getAllCatalogCategory() {
        List<Catalog> catalogList = catalogRepository.getAllObject("catalog_impl");
        catalogList.removeIf(Catalog::getIsDeleted);

        List<Map<String, String>> data = new ArrayList<>();
        Set<String> uniqueCategory = new HashSet<>();

        for (Catalog catalog : catalogList) {
            String category = catalog.getCategory();
            if (category != null && !category.isEmpty() && uniqueCategory.add(category) ) { 
                Map<String, String> innerMap = new HashMap<>();
                innerMap.put("name", category);
                data.add(innerMap);
            }
        }

        return data;
    }
  
}
