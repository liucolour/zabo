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
        String retPost = null;
        try {
            retPost = processPostCreation(category, content);
            if(retPost==null || retPost.isEmpty())
                routingContext.fail(500);
        }catch (Throwable t) {
            logger.error("Adding one post failed ", t);
            routingContext.fail(500);
            return;
        }
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(retPost);
    }

    public static void getOne(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");

        Post post = null;
        try {
            DAO dao = getDAO(category);
            if(dao == null)
                routingContext.fail(500);
            post = (Post) dao.read(id);
            if(post == null)
                routingContext.fail(500);
        }catch (Throwable t) {
            logger.error("Getting one post failed ", t);
            routingContext.fail(500);
            return;
        }
        routingContext.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(post));
    }

    public static void updateOne(RoutingContext routingContext) {
        routingContext.fail(501);
    }

    public static void deleteOne(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");

        try {
            DAO dao = getDAO(category);
            if(dao == null)
                routingContext.fail(500);
            dao.delete(id);
        }catch (Throwable t){
            logger.error("Deleting one post failed ", t);
            routingContext.fail(500);
            return;
        }
        routingContext.response().setStatusCode(204).end();
    }

    public static void query(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String type = routingContext.request().getParam("type");
        String content = routingContext.getBodyAsString();
        List<Post> json = null;
        try {
            DAO dao = getDAOByQuery(category, type);
            if(dao == null)
                routingContext.fail(500);
            json = dao.query(content);
        }catch (Throwable t){
            logger.error("Querying failed ", t);
            routingContext.fail(500);
            return;
        }
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
