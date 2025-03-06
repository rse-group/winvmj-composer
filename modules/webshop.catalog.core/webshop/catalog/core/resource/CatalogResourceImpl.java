package webshop.catalog.core;
import java.util.*;

import vmj.routing.route.exceptions.*;
import vmj.routing.route.Route;
import vmj.routing.route.VMJExchange;
import webshop.catalog.CatalogFactory;
import webshop.catalog.core.CatalogService;
import vmj.auth.annotations.Restricted;

public class CatalogResourceImpl extends CatalogResourceComponent{
    private CatalogService catalogService;

	public CatalogResourceImpl(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@Restricted(permissionName = "CreateCatalog")
    @Route(url="call/catalog/save")
    public HashMap<String,Object> saveCatalog(VMJExchange vmjExchange){
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		String email =  vmjExchange.getAuthPayload().getEmail();
		Catalog catalog = catalogService.saveCatalog((HashMap<String, Object>) vmjExchange.getPayload(), email);
		return catalog.toHashMap();
	}

    @Restricted(permissionName = "UpdateCatalog")
    @Route(url="call/catalog/update")
    public HashMap<String, Object> updateCatalog(VMJExchange vmjExchange){
		HashMap<String, Object> body = (HashMap<String, Object>) vmjExchange.getPayload();
		if (vmjExchange.getHttpMethod().equals("OPTIONS")) {
			return null;
		}
		Catalog catalog = catalogService.updateCatalog(body);
		return catalog.toHashMap();
	}

    @Route(url="call/catalog/detail")
    public HashMap<String, Object> getCatalog(VMJExchange vmjExchange){
		String catalogIdStr = vmjExchange.getGETParam("catalogId"); 
		UUID catalogId = UUID.fromString(catalogIdStr);
		Catalog catalog = catalogService.getCatalog(catalogId);
		return catalog.toHashMap();
	}

    @Route(url="call/catalog/list")
    public List<HashMap<String,Object>> getAllCatalog(VMJExchange vmjExchange){
		List <Catalog> catalogList = catalogService.getAllCatalog();
		return catalogService.transformCatalogListToHashMap(catalogList);
	}
	@Route(url="call/catalog/filter")
    public List<HashMap<String,Object>> getCatalogByName(VMJExchange vmjExchange){
		String name = vmjExchange.getGETParam("name"); 
		List <Catalog> catalogList = catalogService.getCatalogByName(name);
		return catalogService.transformCatalogListToHashMap(catalogList);
	}

	@Restricted(permissionName = "DeleteCatalog")
    @Route(url="call/catalog/delete")
    public List<HashMap<String,Object>> deleteCatalog(VMJExchange vmjExchange){
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

}
