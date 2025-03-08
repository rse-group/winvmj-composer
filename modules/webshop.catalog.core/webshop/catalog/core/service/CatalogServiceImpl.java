package webshop.catalog.core;
import java.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.lang.RuntimeException;

import com.rabbitmq.client.DeliverCallback;
import vmj.routing.route.exceptions.*;
import webshop.catalog.CatalogFactory;
import webshop.seller.core.*;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CatalogServiceImpl extends CatalogServiceComponent{
    private CatalogFactory catalogFactory = new CatalogFactory();
    SellerService sellerService = new SellerServiceImpl();

    String BASE_EXCHANGE = "webshop";
    String appId;
    Channel channel;
    String catalogQueue;

//    public CatalogServiceImpl() {} // Hanya bisa mendefinisikasn 1 constructor karena di factory hanya mengambil 1 constructor saja

    public CatalogServiceImpl(Channel channel, String appId){
        this.channel = channel;
        this.appId = appId;
        this.catalogQueue = appId + ".catalog";

        BindQueue();
    }

    public Catalog saveCatalog(HashMap<String, Object> body, String email){
        if (!body.containsKey("name")) {
            throw new FieldValidationException("Field 'name' not found in the request body.");
        }
        String name = (String) body.get("name");
        List<Catalog> savedCatalogs = getCatalogByName(name);
        if (!savedCatalogs.isEmpty()) {
            throw new BadRequestException("Catalog with name '" + name + "' already exists in repository.");
        }

        if (!body.containsKey("description")) {
            throw new FieldValidationException("Field 'description' not found in the request body.");
        }
        String description = (String) body.get("description");
        if (!body.containsKey("price")) {
            throw new FieldValidationException("Field 'price' not found in the request body.");
        }
        String priceStr = (String) body.get("price");
        int price = Integer.parseInt(priceStr);

        if (!body.containsKey("availableStock")) {
            throw new FieldValidationException("Field 'availableStock' not found in the request body.");
        }
        String availableStockStr = (String) body.get("availableStock");
        int availableStock = Integer.parseInt(availableStockStr);

        if (!body.containsKey("category")) {
            throw new FieldValidationException("Field 'category' not found in the request body.");
        }
        String category = (String) body.get("category");

        if (!body.containsKey("pictureURL")) {
            throw new FieldValidationException("Field 'pictureURL' not found in the request body.");
        }
        Map<String, byte[]> uploadedFile = (HashMap<String, byte[]>) body.get("pictureURL");

        String pictureURL = "data:" + (new String(uploadedFile.get("type"))).split(" ")[1].replaceAll("\\s+", "")
                + ";base64," + Base64.getEncoder().encodeToString(uploadedFile.get("content"));
        int fileSize = uploadedFile.get("content").length;
        if (fileSize > 4000000)
            throw new FileSizeException(4.0, ((double) fileSize) / 1000000, "megabyte");
        try {
            String type = URLConnection
                    .guessContentTypeFromStream(new ByteArrayInputStream(uploadedFile.get("content")));
            if (type == null || !type.startsWith("image"))
                throw new FileTypeException("image");
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
        UUID catalogId = UUID.randomUUID();
        Seller seller = null;
        if (email != null) {
            seller = sellerService.getSellerByEmail(email);
        }
        Catalog catalog = catalogFactory.createCatalog("webshop.catalog.core.CatalogImpl", catalogId, name, description, price, pictureURL, availableStock, category, seller);
        catalogRepository.saveObject(catalog);

        publishCatalogMessage(catalog, email, "create");
        return catalogRepository.getObject(catalogId);
    }

    public Catalog updateCatalog(HashMap<String, Object> body){
        if (!body.containsKey("catalogId")) {
            throw new NotFoundException("Field 'catalogId' not found in the request body.");
        }
        String catalogIdStr = (String) body.get("catalogId");
        UUID catalogId = UUID.fromString(catalogIdStr);

        Catalog catalog = catalogRepository.getObject(catalogId);
        if (catalog == null) {
            throw new NotFoundException("Catalog with catalogId " + catalogId +" not found");
        }
        if (body.containsKey("category")) {
            String category = (String) body.get("category");
            catalog.setCategory(category);
        }
        if (body.containsKey("name")) {
            String name =  (String) body.get("name");
            catalog.setName(name);
        }
        if (body.containsKey("description")) {
            String description = (String) body.get("description");
            catalog.setDescription(description);
        }

        int price = -1 ;
        if (body.containsKey("price")) {
            try {
                price = Integer.parseInt((String) body.get("price"));
            } catch (NumberFormatException e) {
                throw new FieldValidationException("Price must be an integer");
            }
        }
        if (price != -1) {
            catalog.setPrice(price);
        }

        int availableStock = -1 ;
        if (body.containsKey("availableStock")) {
            try {
                availableStock = Integer.parseInt((String) body.get("availableStock"));
            } catch (NumberFormatException e) {
                throw new FieldValidationException("Available stock must be an integer");
            }
        }
        if (availableStock != -1) {
            catalog.setAvailableStock(availableStock);
        }

        if (body.containsKey("pictureURL")) {
            Object rawUploadedFile = body.get("pictureURL");

            if (rawUploadedFile instanceof HashMap) {
                Map<String, byte[]> uploadedFile = (HashMap<String, byte[]>) rawUploadedFile;

                if (uploadedFile != null) {
                    String pictureURL = "data:" + new String(uploadedFile.get("type")).split(" ")[1].replaceAll("\\s+", "")
                            + ";base64," + Base64.getEncoder().encodeToString(uploadedFile.get("content"));

                    // Validasi ukuran file
                    int fileSize = uploadedFile.get("content").length;
                    if (fileSize > 4000000) {
                        throw new FileSizeException(4.0, ((double) fileSize) / 1000000, "megabyte");
                    }

                    try {
                        String type = URLConnection
                                .guessContentTypeFromStream(new ByteArrayInputStream(uploadedFile.get("content")));
                        if (type == null || !type.startsWith("image")) {
                            throw new FileTypeException("image");
                        }
                    } catch (IOException e) {
                        throw new FileNotFoundException();
                    }

                    catalog.setPictureUrl(pictureURL);
                }
            } else {
                throw new IllegalArgumentException("Invalid type for 'pictureURL': Expected HashMap<String, byte[]>.");
            }
        }

        updateAndPublishCatalog(catalog);

        catalog = catalogRepository.getObject(catalogId);
        return catalog;
    }

    public Catalog getCatalog(UUID catalogId){
        Catalog catalog = catalogRepository.getObject(catalogId);
        if (catalog.getIsDeleted()) {
            catalog = null;
        }
        return catalog;
    }

    public List<Catalog> getAllCatalog(){
        List<Catalog> catalogList = catalogRepository.getListObject(
                "catalog_impl", "isDeleted", false);
        return catalogList;
    }

    public List <Catalog> getCatalogByName(String name){
        List<Catalog> filteredCatalogs = catalogRepository.getListObject("catalog_impl", "name", name);
        filteredCatalogs.removeIf(Catalog::getIsDeleted);  // Remove all elements with isDeleted = true
        return filteredCatalogs;
    }

    public List<Catalog> deleteCatalog(UUID catalogId){
        // Soft delete
        Catalog catalog = catalogRepository.getObject(catalogId);
        catalog.setIsDeleted(true);
        catalogRepository.updateObject(catalog);
        publishCatalogMessage(catalog,"delete");
        return getAllCatalog();
    }

    public List<HashMap<String,Object>> transformCatalogListToHashMap(List<Catalog> catalogList){
        List<HashMap<String,Object>> resultList = new ArrayList<HashMap<String,Object>>();
        for(int i = 0; i < catalogList.size(); i++) {
            resultList.add(catalogList.get(i).toHashMap());
        }

        return resultList;
    }

    public void updateAndPublishCatalog(Catalog catalog){
        catalogRepository.updateObject(catalog);
        publishCatalogMessage(catalog, "update");
    }

    public void saveCatalogFromMessage(JsonObject body){
        if (!body.has("catalogId")) {
            System.out.println("Field 'catalogId' not found");
        }
        UUID catalogId = UUID.fromString(body.get("catalogId").getAsString());
        if (!body.has("name")) {
            System.out.println("Field 'name' not found in the request body.");
            return;
        }
        String name = body.get("name").getAsString();
        List<Catalog> savedCatalogs = getCatalogByName(name);
        if (!savedCatalogs.isEmpty()) {
            System.out.println("Catalog with name '" + name + "' already exists in repository.");
            return;
        }

        if (!body.has("description")) {
            System.out.println("Field 'description' not found in the request body.");
            return;
        }
        String description = body.get("description").getAsString();
        if (!body.has("price")) {
            System.out.println("Field 'price' not found in the request body.");
            return;
        }
        int price = body.get("price").getAsInt();

        if (!body.has("availableStock")) {
            System.out.println("Field 'availableStock' not found in the request body.");
            return;
        }

        int availableStock = body.get("availableStock").getAsInt();

        if (!body.has("category")) {
            System.out.println("Field 'category' not found in the request body.");
            return;
        }
        String category = body.get("category").getAsString();

        if (!body.has("pictureURL")) {
            System.out.println("Field 'pictureURL' not found in the request body.");
            return;
        }
        String pictureURL = body.get("pictureURL").getAsString();

        String email = body.get("email").getAsString();

        Seller seller = null;
        if (body.has("email")) {
            seller = sellerService.getSellerByEmail(email);
        }
        Catalog catalog = catalogFactory.createCatalog("webshop.catalog.core.CatalogImpl", catalogId, name, description, price, pictureURL, availableStock, category, seller);
        catalogRepository.saveObject(catalog);
    }

    public void updateCatalogFromMessage(JsonObject body){
        if (!body.has("catalogId")) {
            System.out.println("Field 'catalogId' not found in the request body.");
            return;
        }
        String catalogIdStr = body.get("catalogId").getAsString();
        UUID catalogId = UUID.fromString(catalogIdStr);

        Catalog catalog = catalogRepository.getObject(catalogId);
        if (catalog == null) {
            System.out.println("Catalog with catalogId " + catalogId +" not found");
            return;
        }
        if (body.has("category")) {
            String category = body.get("category").getAsString();
            catalog.setCategory(category);
        }
        if (body.has("name")) {
            String name = body.get("name").getAsString();
            catalog.setName(name);
        }
        if (body.has("description")) {
            String description = body.get("description").getAsString();
            catalog.setDescription(description);
        }

        int price = -1 ;
        if (body.has("price")) {
            try {
                price = body.get("price").getAsInt();
            } catch (NumberFormatException e) {
                System.out.println("Price must be an integer");
                return;
            }
        }
        if (price != -1) {
            catalog.setPrice(price);
        }

        int availableStock = -1 ;
        if (body.has("availableStock")) {
            try {
                availableStock = body.get("availableStock").getAsInt();
            } catch (NumberFormatException e) {
                System.out.println("Available stock must be an integer");
                return;
            }
        }
        if (availableStock != -1) {
            catalog.setAvailableStock(availableStock);
        }

        if (body.has("pictureURL")) {
            String pictureURL = body.get("pictureURL").getAsString();
            catalog.setPictureUrl(pictureURL);
        }

        catalogRepository.updateObject(catalog);
    }

    public void deleteCatalogFromMessage(JsonObject body){
        if (!body.has("catalogId")) {
            System.out.println("Field 'catalogId' not found in the request body.");
            return;
        }
        String catalogIdStr = body.get("catalogId").getAsString();
        UUID catalogId = UUID.fromString(catalogIdStr);

        Catalog catalog = catalogRepository.getObject(catalogId);
        catalog.setIsDeleted(true);
        catalogRepository.updateObject(catalog);
    }

    public void BindQueue(){
        try {
            boolean durable = true;
            boolean exclusive = false;
            boolean autoDelete = false;
            Map<String, Object> arguments = null;

            channel.queueDeclare(catalogQueue, durable, exclusive, autoDelete, arguments);
            String bindingKey = "catalog";
            channel.queueBind(catalogQueue, BASE_EXCHANGE, bindingKey);

            consumeCatalogMessage();
        } catch (Exception e) {
            System.out.println("Failed to create " + catalogQueue + " queeue " +e);
        }
    }

    public void consumeCatalogMessage(){
        if (channel == null) {
            System.out.println("Channel is null");
            return;
        }

        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (appId.equals(delivery.getProperties().getAppId())) {
                    System.out.println("Skipping own message...");
                    return;
                }

                String message = new String(delivery.getBody(), "UTF-8");

                System.out.println(" [x] Received: " + message);

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(message, JsonObject.class);

                String action = jsonObject.get("action").getAsString();
                if (action.equals("delete")){
                    deleteCatalogFromMessage(jsonObject);
                    return;
                }

//                HashMap<String, Object> catalogBody = gson.fromJson(jsonObject, HashMap.class);

                if (action.equals("update")) {
                    updateCatalogFromMessage(jsonObject);
                } else { // create
                    saveCatalogFromMessage(jsonObject);
                }

            };

            channel.basicConsume(catalogQueue, true, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            System.out.println("Error while consuming catalog message");
        }
    }

    private void publishCatalogMessage(Catalog catalog, String action) {
        publishCatalogMessage(catalog, null, action);
    }

    private void publishCatalogMessage(Catalog catalog, String email, String action) {
        if (catalog instanceof CatalogDecorator) {
            catalog = ((CatalogDecorator) catalog).getRecord();
        }

        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(catalog).getAsJsonObject();

        jsonObject.remove("objectName");
        jsonObject.remove("isDeleted");
        jsonObject.remove("seller");

        if (action.equals("create")) {
            jsonObject.add("email", new JsonPrimitive(email));
        }

        if (action.equals("delete")) {
            jsonObject = new JsonObject();
            String catalogId = catalog.getCatalogId().toString();
            jsonObject.add("catalogId", new JsonPrimitive(catalogId));
        }

        jsonObject.add("action", new JsonPrimitive(action));

        try {
            String routingKey = "catalog";
            String message = gson.toJson(jsonObject);

            BasicProperties props = new BasicProperties.Builder()
                    .appId(appId)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            channel.basicPublish(BASE_EXCHANGE, routingKey, props, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        } catch (IOException e){
            System.out.println("Failed to publish catalog message");
        }
    }

}
