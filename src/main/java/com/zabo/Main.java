package com.zabo;

import com.zabo.dao.ElasticSearchDAOFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.elasticsearch.client.Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhaoboliu on 2/15/16.
 */
public class Main {
    public static void main(String[] args) {
        System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        final Logger logger = LoggerFactory.getLogger(Main.class.getName());

        try(InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties(System.getProperties());
            prop.load(input);
            System.setProperties(prop);
        } catch (IOException ex) {
            logger.error(ex);
            return;
        }

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("com.zabo.verticles.RestAPIVerticle");
        logger.info("Main is started");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                final CountDownLatch latch = new CountDownLatch(1);
                vertx.close(ar -> {
                    if (ar.succeeded()) {
                        logger.info("Application is stopped!");
                    } else {
                        logger.error("Failure in stopping Vert.x", ar.cause());
                    }
                    latch.countDown();
                });
                try {
                    if (!latch.await(2, TimeUnit.MINUTES)) {
                        logger.error("Timed out waiting to undeploy all");
                    }
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
//
//        VertxOptions options = new VertxOptions();
//        options.setClustered(true);
//        Vertx.clusteredVertx(options, res -> {
//            if (res.succeeded()) {
//                Vertx vertx = res.result();
//                EventBus eventBus = vertx.eventBus();
//
//                vertx.deployVerticle("com.zabo.verticles.RestAPIVerticle");
//            } else {
//                System.out.println("Failed: " + res.cause());
//            }
//        });
    }
}