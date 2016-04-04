package com.zabo;

import com.zabo.dao.ElasticSearchDAOFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import org.elasticsearch.client.Client;

/**
 * Created by zhaoboliu on 2/15/16.
 */
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("com.zabo.verticles.RestAPIVerticle");
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
