package com.zabo.services;

import com.zabo.dao.DBInterface;
import com.zabo.dao.ESDataType;
import com.zabo.utils.Utils;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by zhaoboliu on 3/22/16.
 */
//TODO: listPostedPost and listDraftedPostForUser
//TODO: introduce post save and submit for view
//TODO: one more layer before dao like send post to cache
//TODO: use vertx.executionBlock to make async call, refer to ShiroAuthProviderImpl
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class.getName());

    private DBInterface dbInterface;

    public PostService(DBInterface dbInterface){
        this.dbInterface = dbInterface;
    }

    private JsonObject convertRequestInJsonObject(final RoutingContext ctx){
        final String category = ctx.request().getParam("category");

        String body = ctx.getBodyAsString();
        JsonObject jsonObject = Utils.ifStringEmpty(body)? new JsonObject() : new JsonObject(body);

        JsonObject query = jsonObject.getJsonObject("query");
        if(!Utils.ifStringEmpty(category)) {
            jsonObject.put("ESDataType", category);
            if( query.isEmpty()) {
                try {
                    // category string has to be the same class name
                    Class clazz = Class.forName("com.zabo.post." + category);

                    // validate matched fields
                    if (!Utils.ifStringEmpty(body))
                        Json.decodeValue(body, clazz);

                } catch (Exception e) {
                    logger.error(e);
                    ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                    return null;
                }
            }
        } else {
            jsonObject.put("ESDataType", ESDataType.AllPost.toString());
        }

        final String id = ctx.request().getParam("id");
        if(!Utils.ifStringEmpty(id))
            jsonObject.put("id", id.trim());


        return jsonObject;
    }

    public void addPost(RoutingContext ctx) {
        Vertx vertx = ctx.vertx();
        JsonObject json_input = convertRequestInJsonObject(ctx);
        if(json_input == null) {
            return;
        }
        User ctxUser = ctx.user();
        String username = ctxUser.principal().getString("username");

        json_input.put("username",username);
        long timestamp = System.currentTimeMillis();
        json_input.put("created_time", timestamp);
        json_input.put("modified_time", timestamp);

        JsonObject result = dbInterface.write(json_input);
        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(result.encodePrettily());
    }

    public void getPost(RoutingContext ctx) {
        JsonObject post = dbInterface.read(convertRequestInJsonObject(ctx));
        if(post == null)
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(post));
    }

    public void updatePost(RoutingContext ctx) {
        User ctxUser = ctx.user();
        JsonObject json_input = convertRequestInJsonObject(ctx);

        if(json_input == null)
            return;

        //use copy of json_input as ElasticSearchInterfaceImpl API remove "ESDataType" entry
        // where the subsequent update still need this info
        JsonObject post_db = dbInterface.read(json_input.copy());


        if(post_db == null) {
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        // Only logged in user can update self post
        if (!post_db.getString("username").equals(ctxUser.principal().getString("username"))) {
            ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
            return;
        }

        json_input.put("modified_time", System.currentTimeMillis());
        dbInterface.update(json_input);
        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated post id " + post_db.getString("id"));
    }


    public void deletePost(RoutingContext ctx) {

        User ctxUser = ctx.user();
        final JsonObject json_input = convertRequestInJsonObject(ctx);

        if(json_input == null)
            return;

        ctxUser.isAuthorised("role:User", res -> {
            //User can only delete post created by self
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    JsonObject post_db = dbInterface.read(json_input.copy());
                    if(post_db == null) {
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }

                    if(!post_db.getString("username").equals(ctxUser.principal().getString("username"))) {
                        ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
                        return;
                    }
                    deleteThenResponse(ctx, json_input);
                }
            }
        });

        ctxUser.isAuthorised("role:Admin", res -> {
            //Admin can delete anyone's post
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    deleteThenResponse(ctx, json_input);
                }
            }
        });
    }

    private void deleteThenResponse(RoutingContext ctx, JsonObject jsonObject){
        dbInterface.delete(jsonObject);
        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Deleted post id " + jsonObject.getString("id"));
    }

    public void queryPosts(RoutingContext ctx) {
        JsonObject json_input = convertRequestInJsonObject(ctx);
        queryThenResponse(ctx, json_input);
    }

    public void queyUserPosts(RoutingContext ctx) {
        final JsonObject json_input = convertRequestInJsonObject(ctx);

        if(json_input == null)
            return;

        User ctxUser = ctx.user();
        ctxUser.isAuthorised("role:User", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    String username = ctx.user().principal().getString("username");
                    json_input.put("query",
                            new JsonObject(String.format(System.getProperty("query.user.statement"), username)));
                    queryThenResponse(ctx, json_input);
                }
            }
        });

        ctxUser.isAuthorised("role:Admin", res -> {
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    // for admin, use username from json body in request
                    String username = ctx.getBodyAsJson().getString("username");
                    json_input.put("query",
                            new JsonObject(String.format(System.getProperty("query.user.statement"), username)));
                    queryThenResponse(ctx, json_input);
                }
            }
        });
    }

    private void queryThenResponse(RoutingContext ctx, JsonObject json_input) {
        JsonArray posts = dbInterface.query(json_input);
        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(posts.encodePrettily());
    }

    // Test only
    public void getUploadUI(RoutingContext routingContext) {
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
    public void uploadForm(RoutingContext routingContext) {


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
