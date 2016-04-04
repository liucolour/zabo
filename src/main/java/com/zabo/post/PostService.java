package com.zabo.post;

import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class PostService {
    private static final Map<String, Class> categoryClassMap = new HashMap<>();
    static {
        categoryClassMap.put("job", JobPost.class);
    }

    public static void getAll(RoutingContext routingContext){

        routingContext.response().end("getAll is not implemented");
    }

    public static void addOne(RoutingContext routingContext){
        final String category = routingContext.request().getParam("category");
        String content = routingContext.getBodyAsString();
        String retPost = processPostCreation(category, content);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(retPost);
    }

    public static void getOne(RoutingContext routingContext){
        routingContext.response().end("getOne is not implemented");
    }

    public static void updateOne(RoutingContext routingContext){
        routingContext.response().end("updateOne is not implemented");
    }

    public static void deleteOne(RoutingContext routingContext){
        routingContext.response().end("deleteOne is not implemented");
    }

    private static String processPostCreation(String category, String content) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        Post post = (Post) Json.decodeValue(content, clazz);
        long currentTime = System.currentTimeMillis();
        post.setCreated_Time(currentTime);
        post.setModified_Time(currentTime);
        DAOFactory daoFactory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = daoFactory.getDAO(clazz);
        String id = dao.write(post);
        post.setId(id);
        return Json.encodePrettily(post);
    }
}
