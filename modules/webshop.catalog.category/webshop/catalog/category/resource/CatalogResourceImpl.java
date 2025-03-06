package webshop.catalog.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import webshop.catalog.core.*;

public class CatalogResourceImpl extends CatalogResourceDecorator {
    private CatalogService catalogService;
    public CatalogResourceImpl(CatalogResourceComponent recordController, CatalogServiceComponent recordService) {
      super(recordController);
      this.catalogService = new CatalogServiceImpl(recordService);
    }

    @Route(url="call/category/filter")
    public HashMap<String, Object> getCatalogByCategory(VMJExchange vmjExchange){
        String category = vmjExchange.getGETParam("category"); 
    	HashMap<String, Object> result = ((CatalogServiceImpl) catalogService).getCatalogByCategory(category);
        return result;
    }
    
    @Route(url="call/catalog/category")
    public List<Map<String, String>> getAllCatalogCategory(VMJExchange vmjExchange) {
        List<Map<String, String>> categoryList = ((CatalogServiceImpl) catalogService).getAllCatalogCategory();
        return categoryList;
    }
}
