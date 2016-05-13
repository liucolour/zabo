package com.zabo.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class.getName());

    public static Integer getPropertyInt(String key, int default_val) {
        String val = System.getProperty(key);
        try {
            return  Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.error("Invalid key for integer ", e);
            return default_val;
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

    public static boolean ifStringEmpty(String input){
        if(input == null || input.equals(""))
            return true;
        return false;
    }
}
