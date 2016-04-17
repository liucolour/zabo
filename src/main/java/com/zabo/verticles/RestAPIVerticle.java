package com.zabo.verticles;

import com.zabo.post.PostService;
import com.zabo.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by zhaoboliu on 4/3/16.
 */
public class RestAPIVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(RestAPIVerticle.class.getName());

    @Override
    public void start(Future<Void> fut) {
        Router router = Router.router(vertx);

        router.route("/api/*").consumes("application/json").handler(BodyHandler.create());

        router.post("/api/posts/:category").handler(PostService::addOne);
        router.get("/api/posts/:category/:id").handler(PostService::getOne);
        router.put("/api/posts/:category/:id").handler(PostService::updateOne);
        router.delete("/api/posts/:category/:id").handler(PostService::deleteOne);
        router.post("/api/posts/:category/query/:type").handler(PostService::query);

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
