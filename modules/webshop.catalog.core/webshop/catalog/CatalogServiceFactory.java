package webshop.catalog;

import webshop.catalog.core.CatalogService;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;

public class CatalogServiceFactory {
  private static final Logger LOGGER = Logger.getLogger(CatalogServiceFactory.class.getName());

  public CatalogServiceFactory() {

  }

  public static CatalogService createCatalogService(String fullyQualifiedName, Object... base) {
    CatalogService record = null;
    if (true) {
      try {
        Class<?> clz = Class.forName(fullyQualifiedName);
        Constructor<?> constructor = clz.getDeclaredConstructors()[0];
        record = (CatalogService) constructor.newInstance(base);
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Failed to create instance of CatalogService.");
        LOGGER.severe("Given FQN: " + fullyQualifiedName);
        LOGGER.severe("Failed to run: Check your constructor argument");
        System.exit(20);
      } catch (ClassCastException e) {
        LOGGER.severe("Failed to create instance of CatalogService.");
        LOGGER.severe("Given FQN: " + fullyQualifiedName);
        LOGGER.severe("Failed to cast the object");
        System.exit(30);
      } catch (ClassNotFoundException e) {
        LOGGER.severe("Failed to create instance of CatalogService.");
        LOGGER.severe("Given FQN: " + fullyQualifiedName);
        LOGGER.severe("Decorator can't be applied to the object");
        System.exit(40);
      } catch (Exception e) {
        LOGGER.severe("Failed to create instance of CatalogService.");
        LOGGER.severe("Given FQN: " + fullyQualifiedName);
        System.exit(50);
      }
    } else {
      System.out.println("Config Fail");
      System.exit(10);
    }
    return record;
  }
}
