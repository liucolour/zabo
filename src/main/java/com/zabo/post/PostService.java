package com.zabo.post;

import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class PostService {
    private static final Map<String, Class> categoryClassMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(PostService.class.getName());
    static {
        categoryClassMap.put("job", JobPost.class);
    }

    public static void addOne(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        String content = routingContext.getBodyAsString();
        String retPost = processPostCreation(category, content);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(retPost);
    }

    public static void getOne(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");
        DAO dao = getDAO(category);
        Post post = (Post) dao.read(id);
        routingContext.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(post));
    }

    public static void updateOne(RoutingContext routingContext) {
        //routingContext.response().setStatusCode(501).end();
        routingContext.fail(501);
    }

    public static void deleteOne(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");
        DAO dao = getDAO(category);
        dao.delete(id);
        routingContext.response().setStatusCode(204).end();
    }

    public static void query(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String type = routingContext.request().getParam("type");
        String content = routingContext.getBodyAsString();
        DAO dao = getDAOByQuery(category, type);
        List<Post> json = dao.query(content);
        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(json));
    }

    private static String processPostCreation(String category, String content) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        DAO dao = getDAO(category);

        Post post = (Post) Json.decodeValue(content, clazz);
        long currentTime = System.currentTimeMillis();
        post.setCreated_time(currentTime);
        post.setModified_time(currentTime);

        //TODO: one more layer before dao like send post to cache
        String id = dao.write(post);
        post.setId(id);
        return Json.encodePrettily(post);
    }

    private static DAO getDAO(String category) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        DAOFactory daoFactory = DAOFactory.getDAOFactorybyConfig();
        return daoFactory.getDAO(clazz);
    }

    private static DAO getDAOByQuery(String category, String type) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        //TODO: throw exception for type not match
        DAOFactory daoFactory = DAOFactory.getDAOFactory(DAOFactory.DBType.valueOf(type));
        return daoFactory.getDAO(clazz);
    }
}
