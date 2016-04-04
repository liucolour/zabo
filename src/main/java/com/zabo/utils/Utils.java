package com.zabo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public class Utils {
    public static String getProperty(String key){
        Properties prop = new Properties();
        InputStream input = null;

        try {
            String filename = "config.properties";
            input = Utils.class.getClassLoader().getResourceAsStream(filename);
            if(input == null){
                System.out.println("Sorry, unable to find " + filename);
                return null;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            return prop.getProperty(key);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Integer getPropertyInt(String key) {
        String val = getProperty(key);
        try {
            return  Integer.parseInt(val);
        } catch (NumberFormatException e) {
            System.out.println("Invalid key for integer");
            return null;
        }
    }

    public static String generateRandom(int length) {
        Random random = new Random();
        char[] digits = new char[length];
        digits[0] = (char) (random.nextInt(9) + '1');
        for (int i = 1; i < length; i++) {
            digits[i] = (char) (random.nextInt(10) + '0');
        }
        return new String(digits);
    }
}
