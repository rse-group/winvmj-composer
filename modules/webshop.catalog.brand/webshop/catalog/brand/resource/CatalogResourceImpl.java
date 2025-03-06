package webshop.catalog.brand;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import vmj.routing.route.exceptions.*;
import vmj.auth.annotations.Restricted;

import webshop.catalog.core.*;
import webshop.catalog.brand.CatalogImpl;
import webshop.catalog.CatalogFactory;

public class CatalogResourceImpl extends CatalogResourceDecorator {

    private CatalogService catalogService;
    public CatalogResourceImpl(CatalogResourceComponent recordController, CatalogServiceComponent recordService) {
      super(recordController);
      this.catalogService = new CatalogServiceImpl(recordService);
    }
    
    @Restricted(permissionName = "CreateCatalog")
    @Route(url="call/brand/save")
    public HashMap<String,Object> saveCatalog(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
        String email =  vmjExchange.getAuthPayload().getEmail();
		Catalog catalog = catalogService.saveCatalog((HashMap<String, Object>) vmjExchange.getPayload(), email);
		return catalog.toHashMap();
	}

    @Restricted(permissionName = "UpdateCatalog")
    @Route(url="call/brand/update")
    public HashMap<String, Object> updateCatalog(VMJExchange vmjExchange){
    	HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
        if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		Catalog updatedCatalog = catalogService.updateCatalog(body);
		return 	updatedCatalog.toHashMap();
	}

    @Route(url="call/brand/detail")
    public HashMap<String, Object> getCatalog(VMJExchange vmjExchange){
		String catalogIdStr = vmjExchange.getGETParam("catalogId"); 
		UUID catalogId = UUID.fromString(catalogIdStr);
        Catalog catalogBrand = catalogService.getCatalog(catalogId);
        return ((CatalogImpl) catalogBrand).toHashMap();
	}

    @Route(url="call/brand/list")
    public List<HashMap<String,Object>> getAllCatalog(VMJExchange vmjExchange){
		List <Catalog> catalogList = catalogService.getAllCatalog();
		return catalogService.transformCatalogListToHashMap(catalogList);
	}

	@Restricted(permissionName = "DeleteCatalog")
    @Route(url="call/brand/delete")
    public List<HashMap<String,Object>> deleteCatalog(VMJExchange vmjExchange) {
        if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
            return null;
        }

        HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		String catalogIdStr = (String) body.get("catalogId");
    	UUID catalogId = UUID.fromString(catalogIdStr);
		Catalog catalog = catalogRepository.getObject(catalogId);
		List <Catalog> catalogList = catalogService.deleteCatalog(catalogId);
		return catalogService.transformCatalogListToHashMap(catalogList);
    }


    @Route(url="call/brand/brands")
    public List<Map<String, String>> getBrands(VMJExchange vmjExchange) {
        return ((CatalogServiceImpl) catalogService).getBrands();
    }
    
    @Route(url="call/brand/filter")
    public HashMap<String, Object> getCatalogByBrand(VMJExchange vmjExchange){
    	String brand = vmjExchange.getGETParam("brand");
        HashMap<String, Object> filteredCatalogs = ((CatalogServiceImpl) catalogService).getCatalogByBrand(brand);
        return filteredCatalogs;
    }
    
    @Route(url="call/brandcategory/filter")
    public HashMap<String, Object> getCatalogByBrandAndCategory(VMJExchange vmjExchange){
    	String brand = vmjExchange.getGETParam("brand");
        String category = vmjExchange.getGETParam("category");
        HashMap<String, Object> filteredCatalogs = ((CatalogServiceImpl) catalogService).getCatalogByBrandAndCategory(brand, category);
        return filteredCatalogs;
    }

}
