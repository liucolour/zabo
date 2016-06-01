package com.zabo.services;

import com.zabo.data.DBInterface;
import com.zabo.data.ESDataType;
import com.zabo.message.Conversation;
import com.zabo.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoboliu on 5/21/16.
 */
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private DBInterface dbInterface;
    private AccountService accountService;

    public MessageService(DBInterface dbInterface, AccountService accountService) {
        this.dbInterface = dbInterface;
        this.accountService = accountService;
    }

    public void createConversation(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            String body = ctx.getBodyAsString();
            if (Utils.ifStringEmpty(body)) {
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            try {
                Json.decodeValue(body, Conversation.class);
            } catch (Exception e) {
                logger.error(e);
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }
            JsonObject json_input = new JsonObject(body);

            JsonArray messages = json_input.getJsonArray("messages");
            if (messages == null || messages.size() != 1) {
                logger.error("Received empty or more than one messages");
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            json_input.put("ESDataType", ESDataType.Message.toString());
            long timestamp = System.currentTimeMillis();
            json_input.put("created_time", timestamp);
            json_input.put("modified_time", timestamp);

            String curr_username = ctx.user().principal().getString("username");

            messages.getJsonObject(0).put("sender_username", curr_username);
            messages.getJsonObject(0).put("created_time", timestamp);
            json_input.put("messages", messages);

            JsonArray usernames = json_input.getJsonArray("usernames");
            if (usernames == null || usernames.isEmpty()) {
                logger.error("Received empty usernames");
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            if (!usernames.contains(curr_username))
                usernames.add(curr_username);

            json_input.put("usernames", usernames);
            JsonObject result = dbInterface.write(json_input);

            String conversation_id = result.getString("id");
            List<String> user_list = usernames.getList();
            accountService.updateAccountConversationList(ctx, user_list, conversation_id);
            ctx.response()
                    .setStatusCode(HttpResponseStatus.CREATED.getCode())
                    .putHeader("content-type", "application/text; charset=utf-8")
                    .end("Created new conversion with id " + conversation_id);
        }, false, null);
    }

    public void replyMessage(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            final String conversation_id = ctx.request().getParam("id");

            JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            String content = body.getString("content");
            if (Utils.ifStringEmpty(content)) {
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            JsonObject json = new JsonObject();
            json.put("id", conversation_id);

            long ts = System.currentTimeMillis();
            String curr_user = ctx.user().principal().getString("username");
            Map<String, Object> message = new HashMap<>();
            message.put("sender_username", curr_user);
            message.put("created_time", ts);
            message.put("content", content);

            Map<String, Object> new_message = new HashMap<>();
            new_message.put("new_message", message);
            new_message.put("new_time", ts);

            json.put("script", System.getProperty("conversation.message.script"));
            json.put("params", new_message);
            json.put("ESDataType", ESDataType.Message.toString());
            dbInterface.update(json);
            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/text; charset=utf-8")
                    .end(curr_user + " replied to conversion id " + conversation_id);
        }, false, null);
    }

    public void readMessages(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            final String conversation_id = ctx.request().getParam("id");
            final String user_db_id = ctx.session().get("user_db_id");
            JsonObject json_input = new JsonObject();
            json_input.put("id", conversation_id);
            json_input.put("ESDataType", ESDataType.Message.toString());
            JsonObject conversation = dbInterface.read(json_input);
            JsonArray messages = conversation.getJsonArray("messages");
            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(messages.encodePrettily());

            accountService.updateAccountChatRecord(user_db_id, conversation_id, false);
        }, false, null);
    }

}
