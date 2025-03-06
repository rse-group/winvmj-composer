package webshop.catalog.brand;

import java.util.*;
import java.util.stream.Collectors;
import vmj.routing.route.exceptions.*;
import webshop.catalog.core.*;
import webshop.catalog.brand.CatalogImpl;
import webshop.catalog.CatalogFactory;
import webshop.catalog.core.Catalog;
import webshop.catalog.core.CatalogDecorator;

public class CatalogServiceImpl extends CatalogServiceDecorator {

    public CatalogServiceImpl(CatalogServiceComponent record) {
        super(record);
    }

    public Catalog saveCatalog(HashMap<String, Object> body, String email){
		String brand = (String) body.get("brand");
		Catalog catalog = record.saveCatalog(body, email);
		Catalog catalogBrand = CatalogFactory.createCatalog("webshop.catalog.brand.CatalogImpl", catalog, brand);
		catalogRepository.saveObject(catalogBrand);
		UUID catalogBrandId = catalogBrand.getCatalogId();
		return catalogRepository.getObject(catalogBrandId);
	}

    public Catalog updateCatalog(HashMap<String, Object> body){
    	String brand = null;
    	String catalogIdStr = (String) body.get("catalogId");
    	UUID catalogId = UUID.fromString(catalogIdStr);
        Catalog catalogBrand = catalogRepository.getListObject("catalog_brand", "catalogId", catalogId).get(0);
        if (body.containsKey("brand")) {
    		brand = (String) body.get("brand");
    		((CatalogImpl) catalogBrand).setBrand(brand);
            catalogRepository.updateObject(catalogBrand);      
        }

        Catalog catalog = record.updateCatalog(body);
        
        Catalog catalogBrandUpdated = catalogRepository.getObject(catalogId);
        return catalogBrandUpdated;
	}

    public Catalog getCatalog(UUID catalogId){
    	Catalog catalogBrand = catalogRepository.getListObject("catalog_brand", "catalogId", catalogId).get(0);
		return catalogBrand;
	}

    public List<Catalog> getAllCatalog(){
    	List<Catalog> listCatalog = catalogRepository.getAllObject("catalog_brand");
    	listCatalog.removeIf(Catalog::getIsDeleted); 
		return listCatalog;
	}

    public List<Catalog> deleteCatalog(UUID catalogBrandId) {
        Catalog catalog = catalogRepository.getObject(catalogBrandId);
        catalog.setIsDeleted(true);
        catalogRepository.updateObject(catalog);

        Catalog catalogComponent = ((CatalogDecorator) catalog).getRecord();
        record.deleteCatalog(catalogComponent.getCatalogId());

        return getAllCatalog();
    }
    
    public List<Map<String, String>> getBrands() {
        List<Catalog> catalogList = catalogRepository.getAllObject("catalog_brand");
        catalogList.removeIf(Catalog::getIsDeleted);

        Set<String> uniqueBrands = new HashSet<>();
        List<Map<String, String>> data = new ArrayList<>();

        for (Catalog catalog : catalogList) {
            CatalogImpl catalogWithBrand = (CatalogImpl) catalog;
            String brand = catalogWithBrand.getBrand();
            if (brand != null && !brand.isEmpty() && uniqueBrands.add(brand)) { 
                Map<String, String> innerMap = new HashMap<>();
                innerMap.put("name", brand);
                data.add(innerMap);
            }
        }

        return data;
    }

    
    public HashMap<String, Object> getCatalogByBrand(String brand){
        List<Catalog> filteredCatalogs = catalogRepository.getListObject("catalog_brand", "brand", brand);
        filteredCatalogs.removeIf(Catalog::getIsDeleted);
        HashMap<String, Object> result = new HashMap<>();
        result.put("catalogs", filteredCatalogs);
        return result;
    }
    
    public HashMap<String, Object> getCatalogByBrandAndCategory(String brand, String category){
        List<Catalog> filteredCatalogsByBrand = catalogRepository.getListObject("catalog_brand", "brand", brand);
        List<Catalog> filteredCatalogsByBrandAndCategory = new ArrayList<>();
        for (Catalog catalog : filteredCatalogsByBrand) {
            if (catalog.getCategory().equals(category)) {
                filteredCatalogsByBrandAndCategory.add(catalog);
            }
        }
        filteredCatalogsByBrandAndCategory.removeIf(Catalog::getIsDeleted); // Remove all elements with isDeleted = true
        HashMap<String, Object> result = new HashMap<>();
        result.put("catalogs", filteredCatalogsByBrandAndCategory);
        return result;
    }

}
