package com.zabo.services;

import com.zabo.data.DBInterface;
import com.zabo.data.ESDataType;
import com.zabo.utils.Utils;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zhaoboliu on 3/22/16.
 */
//TODO: listPostedPost and listDraftedPostForUser
//TODO: introduce post save and submit for view
//TODO: one more layer before data like send post to cache
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
            if(query == null || query.isEmpty()) {
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
                "<form action=\"/api/upload/image\" method=\"post\" enctype=\"multipart/form-data\">\n" +
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

    // TODO: refer to BodyHandlerImpl, move file upload here
    // https://github.com/vert-x3/vertx-examples/blob/master/core-examples/src/main/java/io/vertx/example/core/http/simpleformupload/SimpleFormUploadServer.java
    public void uploadImage(RoutingContext ctx) {

        Iterator<FileUpload> it = ctx.fileUploads().iterator();
        List<String> new_files = new ArrayList<>();
        while(it.hasNext()){
            FileUpload file = it.next();
            //http://www.mkyong.com/java/how-to-resize-an-image-in-java/
            String uploadedFileName = file.uploadedFileName();

            String big_file_name = uploadedFileName + "_b.jpg";
            String small_file_name = uploadedFileName + "_s.jpg";
            int big_width = Utils.getPropertyInt("image.big.width", 600);
            int big_height = Utils.getPropertyInt("image.big.height", 450);
            int small_width = Utils.getPropertyInt("image.small.width", 50);
            int small_height = Utils.getPropertyInt("image.small.height", 50);

            //TODO: file reupload to s3
            try {
                BufferedImage original_image = ImageIO.read(new File(uploadedFileName));

                // convert png to jpg first, may use other way to check if png file
                // http://stackoverflow.com/questions/11425521/how-to-get-the-formatexjpen-png-gif-of-image-file-bufferedimage-in-java
                if(file.fileName().endsWith(".png")){
                    BufferedImage pngBufferedImage = new BufferedImage(original_image.getWidth(),
                            original_image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    pngBufferedImage.createGraphics().drawImage(original_image, 0, 0, Color.WHITE, null);
                    original_image = pngBufferedImage;

                }
                BufferedImage resized_image_big = resizeImage(original_image, big_width, big_height);
                ImageIO.write(resized_image_big, "jpg", new File(big_file_name));

                BufferedImage resized_image_small = resizeImage(original_image, small_width, small_height);
                ImageIO.write(resized_image_small, "jpg", new File(small_file_name));
                new_files.add(uploadedFileName);
            } catch (IOException e) {
                logger.error("Read file {} failed with exception ", uploadedFileName, e);
                continue;
            }
            FileSystem fileSystem = ctx.vertx().fileSystem();
            fileSystem.exists(uploadedFileName, existResult -> {
                if (existResult.failed()) {
                    it.remove();
                    logger.warn("Could not detect if image exists, not deleting: " + uploadedFileName, existResult.cause());
                } else if (existResult.result()) {
                    fileSystem.delete(uploadedFileName, deleteResult -> {
                        if (deleteResult.failed()) {
                            logger.warn("Deleting image failed: " + uploadedFileName, deleteResult.cause());
                            return;
                        }
                        it.remove();
                    });
                }
            });
        }
        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .end(Json.encodePrettily(new_files));
    }

    private BufferedImage resizeImage(BufferedImage image, int width, int height) {

        BufferedImage resizedImage = new BufferedImage(width, height,
                                        image.getType()==0? BufferedImage.TYPE_INT_ARGB : image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        return resizedImage;
    }
}
