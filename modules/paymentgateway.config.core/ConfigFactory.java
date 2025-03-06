package paymentgateway.config;

import paymentgateway.config.core.Config;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;
import paymentgateway.config.core.PropertiesReader;

public class ConfigFactory{
    private static final Logger LOGGER = Logger.getLogger(ConfigFactory.class.getName());

    public ConfigFactory()
    {

    }

    // public static Config createConfig(String fullyQualifiedName, Object ... base)
    // {
    //     Config record = null;
    //     try {
    //         Class<?> clz = Class.forName(fullyQualifiedName);
    //         Constructor<?> constructor = clz.getDeclaredConstructors()[0];
    //         record = (Config) constructor.newInstance(base);
    //     } 
    //     catch (IllegalArgumentException e)
    //     {
    //         e.printStackTrace();
    //         LOGGER.severe("Failed to create instance of Configuration.");
    //         LOGGER.severe("Given FQN: " + fullyQualifiedName);
    //         LOGGER.severe("Failed to run: Check your constructor argument");
    //         System.exit(20);
    //     }
    //     catch (ClassCastException e)
    //     {
    //         e.printStackTrace();
    //         LOGGER.severe("Failed to create instance of Configuration.");
    //         LOGGER.severe("Given FQN: " + fullyQualifiedName);
    //         LOGGER.severe("Failed to cast the object");
    //         System.exit(30);
    //     }
    //     catch (ClassNotFoundException e)
    //     {
    //         e.printStackTrace();
    //         LOGGER.severe("Failed to create instance of Configuration.");
    //         LOGGER.severe("Given FQN: " + fullyQualifiedName);
    //         LOGGER.severe("Decorator can't be applied to the object");
    //         System.exit(40);
    //     }
    //     catch (Exception e)
    //     {
    //         e.printStackTrace();
    //         LOGGER.severe("Failed to create instance of Payment.");
    //         LOGGER.severe("Given FQN: " + fullyQualifiedName);
    //         System.exit(50);
    //     }
    //     return record;
    // }

    public static Config createConfig(String vendorName, Object ... base)
    {
        String fullyQualifiedName = "paymentgateway.config." + vendorName.toLowerCase() + "." + vendorName + "Configuration";
        Config record = null;
        try {
            Class<?> clz = Class.forName(fullyQualifiedName);
            Constructor<?> constructor = clz.getDeclaredConstructors()[0];
            record = (Config) constructor.newInstance(base);
        } 
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Failed to run: Check your constructor argument");
            System.exit(20);
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Failed to cast the object");
            System.exit(30);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Decorator can't be applied to the object");
            System.exit(40);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Payment.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            System.exit(50);
        }
        return record;
    }

    public static Config createConfig(String fullyQualifiedName)
    {   
        Config record = null;
        try {
            Class<?> clz = Class.forName(fullyQualifiedName);
            Constructor<?> constructor = clz.getDeclaredConstructors()[0];
            record = (Config) constructor.newInstance();
        } 
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Failed to run: Check your constructor argument");
            System.exit(20);
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Failed to cast the object");
            System.exit(30);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Configuration.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            LOGGER.severe("Decorator can't be applied to the object");
            System.exit(40);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.severe("Failed to create instance of Payment.");
            LOGGER.severe("Given FQN: " + fullyQualifiedName);
            System.exit(50);
        }
        return record;
    }

}
