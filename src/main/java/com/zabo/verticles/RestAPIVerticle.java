package com.zabo.verticles;

import com.zabo.post.PostService;
import com.zabo.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Created by zhaoboliu on 4/3/16.
 */
public class RestAPIVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(RestAPIVerticle.class.getName());

    @Override
    public void start(Future<Void> fut) {
        Router router = Router.router(vertx);

        //Use "webroot" as default directory to hold static files
        router.route().handler(StaticHandler.create());

        router.route("/api/posts/*").consumes("application/json").handler(BodyHandler.create());
        router.route("/api/upload/*").handler(BodyHandler.create().setUploadsDirectory(System.getProperty("image.dir")));

        router.post("/api/posts/:category").handler(PostService::addOne);
        router.get("/api/posts/:category/:id").handler(PostService::getOne);
        router.put("/api/posts/:category/:id").handler(PostService::updateOne);
        router.delete("/api/posts/:category/:id").handler(PostService::deleteOne);
        router.post("/api/posts/:category/query/:type").handler(PostService::query);

        router.get("/api/upload/ui").handler(PostService::getUploadUI);
        router.post("/api/upload/form").handler(PostService::uploadForm);

        //doesn't seem to need this as html tag <img src=> can transfer image directly
//        router.get("/image/:id").handler(cxt -> {
//            String id = cxt.request().getParam("id");
//            //TODO: check file exist
//            cxt.response().sendFile("image/" + id);
//        });

        Integer port = Utils.getPropertyInt("http.port");
        if(port == null)
            return;
        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(
                port,
                result -> {
                    if (result.succeeded()) {
                        logger.info("Verticle {} is started successfully at port {}",
                                RestAPIVerticle.class.getSimpleName(),
                                port);
                        fut.complete();
                    } else {
                        logger.error("Verticle {} failed to start at port {} with cause {}",
                                RestAPIVerticle.class.getSimpleName(),
                                port,
                                result.cause());
                        fut.fail(result.cause());
                    }
                }
            );
    }
}
