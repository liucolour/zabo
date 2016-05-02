package com.zabo.services;

import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import com.zabo.post.JobPost;
import com.zabo.post.Post;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoboliu on 3/22/16.
 */

//TODO: introduce post save and submit for view
//TODO: one more layer before dao like send post to cache
//TODO: or refactor to make DAO async and use jsonObject to DB directly without POJO post class, referring to
// https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/angularjs/Server.java
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class.getName());

    private static final String queryType = System.getProperty("query.type");

    private static final Map<String, Class> categoryClassMap = new HashMap<>();


    static {
        categoryClassMap.put("job", JobPost.class);
    }

    public static void addPost(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        if(category == null) {
            routingContext.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String content = routingContext.getBodyAsString();
        String retPost = null;
        try {
            retPost = processPostCreation(routingContext, category, content);
            if(retPost == null || retPost.isEmpty()) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }
        }catch (Throwable t) {
            logger.error("Adding post failed ", t);
            routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }
        routingContext.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(retPost);
    }

    public static void getPostById(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");
        if(category == null || id == null) {
            routingContext.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        Post post = null;
        try {
            DAO dao = getDAO(category);
            if(dao == null) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }
            post = (Post) dao.read(id);
            if(post == null) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }
        }catch (Throwable t) {
            logger.error("Getting post failed ", t);
            routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }
        routingContext.response()
            .setStatusCode(HttpResponseStatus.OK.getCode())
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(post));
    }

    public static void queyUserPosts(RoutingContext routingContext) {
        User ctxUser = routingContext.user();
        ctxUser.isAuthorised("role:USER", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    //TODO: support any category
                    DAO dao = getDAO("job");
                    if(dao == null) {
                        routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                        return;
                    }
                    // for user, use login username
                    String username = routingContext.user().principal().getString("username");
                    queyPostsByUserId(routingContext, username);
                }
            }
        });

        ctxUser.isAuthorised("role:ADMIN", res -> {
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    // for admin, use username from json body in request
                    String username = routingContext.getBodyAsJson().getString("username");
                    queyPostsByUserId(routingContext, username);
                }
            }
        });
    }

    private static void queyPostsByUserId(RoutingContext routingContext, String username){
        String queryUserStatement = String.format(System.getProperty("query.user.statement"), username);
        //TODO: support any category
        queryPostsONDAO(routingContext, "job", queryType, queryUserStatement);
    }

    //TODO: update post
    public static void updatePost(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");
        if(category == null || id == null) {
            routingContext.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String content = routingContext.getBodyAsString();
        String retPost = null;
        try {
            retPost = processPostUpdate(routingContext, category, content, id);
            if(retPost == null || retPost.isEmpty()) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }
        }catch (Throwable t) {
            logger.error("Updating post failed ", t);
            routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }
        routingContext.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(retPost);
    }

    public static void deletePostWithRole(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");
        final String id = routingContext.request().getParam("id");

        if(category == null || id == null) {
            routingContext.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        User ctxUser = routingContext.user();
        ctxUser.isAuthorised("role:USER", res -> {
            //User can only delete post created by him/her
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    DAO dao = getDAO(category);
                    if(dao == null) {
                        routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                        return;
                    }
                    Post post = (Post)dao.read(id);
                    if(post == null) {
                        routingContext.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }
                    if(!post.getUsername().equals(ctxUser.principal().getString("username"))) {
                        routingContext.fail(HttpResponseStatus.FORBIDDEN.getCode());
                        return;
                    }
                    deletePost(routingContext, category,id);
                }
            }
        });

        ctxUser.isAuthorised("role:ADMIN", res -> {
            //Admin can delete anyone's post
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    deletePost(routingContext,category,id);
                }
            }
        });
    }

    private static void deletePost(RoutingContext routingContext, String category, String id) {
        try {
            DAO dao = getDAO(category);
            if (dao == null) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }
            dao.delete(id);
        } catch (Throwable e){
            logger.error("Deleting post failed ", e);
            routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }
        routingContext.response().setStatusCode(HttpResponseStatus.OK.getCode()).end("Deleted post id :"+id);
    }

    public static void queryPosts(RoutingContext routingContext) {
        final String category = routingContext.request().getParam("category");

        if(category == null) {
            routingContext.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String content = routingContext.getBodyAsString();
        queryPostsONDAO(routingContext, category, queryType, content);
    }

    private static void queryPostsONDAO(RoutingContext routingContext, String category, String type, String statement){
        List<Post> posts = new ArrayList<>();
        try {
            DAO dao = getDAOByQuery(category, type);
            if(dao == null) {
                routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
            posts = dao.query(statement);
        }catch (Throwable t){
            logger.error("Querying posts failed ", t);
            routingContext.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }

        routingContext.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(posts));
    }

    private static String processPostCreation(RoutingContext routingContext,String category, String content) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        DAO dao = getDAO(category);

        Post post = (Post) Json.decodeValue(content, clazz);
        User ctxUser = routingContext.user();
        post.setUsername(ctxUser.principal().getString("username"));

        long currentTime = System.currentTimeMillis();
        post.setCreated_time(currentTime);
        post.setModified_time(currentTime);

        String id = dao.write(post);
        post.setId(id);
        return Json.encodePrettily(post);
    }

    private static String processPostUpdate(RoutingContext routingContext,String category, String content, String id) {
        String cleanCategory = category.trim().toLowerCase();
        Class clazz = categoryClassMap.get(cleanCategory);
        DAO dao = getDAO(category);

        Post post = (Post) Json.decodeValue(content, clazz);
        post.setId(id);

        User ctxUser = routingContext.user();
        post.setUsername(ctxUser.principal().getString("username"));

        long currentTime = System.currentTimeMillis();
        post.setModified_time(currentTime);

        //TODO: may define my Document missing exception
        dao.update(id, post);
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
        if(daoFactory == null){
            logger.error("Couldn't find DAO factory");
            return null;
        }
        return daoFactory.getDAO(clazz);
    }

    // Test only
    public static void getUploadUI(RoutingContext routingContext) {
        routingContext.response().putHeader("content-type", "text/html").end(
                "<form action=\"/api/upload/form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                        "    <div>\n" +
                        "        <label for=\"name\">Select a file:</label>\n" +
                        "        <input type=\"file\" name=\"file\" />\n" +
                        "    </div>\n" +
                        "    <div class=\"button\">\n" +
                        "        <button type=\"submit\">Send</button>\n" +
                        "    </div>" +
                "</form>\n" +
                "<div>\n" +
                "   <img src=/image/dab76837-69e0-4b7e-9ba2-e6476ee0b295 alt=\"\" />\n" +
                "</div>\n"
        );
    }

    // Test Only
    public static void uploadForm(RoutingContext routingContext) {


        for (FileUpload f : routingContext.fileUploads()) {
            routingContext.response().putHeader("Content-Type", "text/html");
            routingContext.response().setChunked(true);
            int indx = f.uploadedFileName().lastIndexOf("/");
            String id = f.uploadedFileName().substring(indx+1);
            routingContext.response().write("<div>\n");
            routingContext.response().write("<p>Uploaded File id: " + id + "</p>");
            routingContext.response().write("<p>File Name: " + f.fileName() + "</p>");
            routingContext.response().write("<p>Size: " + f.size() + "</p>");
            routingContext.response().write("<p>CharSet: " + f.charSet() + "</p>");
            routingContext.response().write("<p>Content Transfer Encoding: " + f.contentTransferEncoding() + "</p>");
            routingContext.response().write("<p>Content Type: " + f.contentType() + "</p>");
            routingContext.response().write("</div>\n");
            routingContext.response().write("<div>\n");
            routingContext.response().write("<img src=/image/"+ id + " alt=\"Image Not Found\" />\n");
            routingContext.response().write("</div>\n");
        }

        routingContext.response().end();
    }
}
