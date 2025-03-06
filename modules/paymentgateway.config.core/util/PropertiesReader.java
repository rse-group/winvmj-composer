package paymentgateway.config.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import paymentgateway.config.Constants;

public class PropertiesReader {
    // private static String fileName = Constants.DEFAULT_PRODUCT_FILE;

    private static Properties loadProperties(String fileName) {
        //nambah argumen disini
        FileInputStream input = null;
        Properties prop = new Properties();

        try {
            input = new FileInputStream(fileName);
            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        return prop;
    }

    public static String getProp(String fileName, String propName) {
        Properties prop = loadProperties(fileName);
        return prop.getProperty(propName);
    }

    // public static String getVendorName() {
    //     return getProp("vendor_name");
    // }

}