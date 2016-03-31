package com.zabo;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * Created by zhaoboliu on 2/15/16.
 */
public class Main {
    public static void main(String[] args) {
        // Create an HTTP server which simply returns "Hello World!" to each request.
//        Vertx.vertx()
//                .createHttpServer()
//                .requestHandler(req -> {
//                    req.response().end("Hello World!");
//                })
//                .listen(8080);


        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/ABC").handler(routingContext -> {

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("ABC");
            //response.write(n).end();

        });
        router.route("/EFG").blockingHandler(routingContext -> {

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("EFG");
            //response.write(n).end();

        }, false);


//        router.route().blockingHandler(routingContext -> {
//
//            //HttpServerResponse response = routingContext.response();
//            //response.putHeader("content-type", "application/json");
//            //response.write("ABC").end();
//            //response.write(n).end();
//            System.out.print("blocking thread");
//
//        });
        server.requestHandler(router::accept).listen(8080);
    }
}
