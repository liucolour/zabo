package com.zabo.services;

import com.zabo.account.Role;
import com.zabo.account.UserAccount;
import com.zabo.data.DBInterface;
import com.zabo.data.ESDataType;
import com.zabo.utils.Utils;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.internal.SystemPropertyUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zhaoboliu on 4/27/16.
 */

/**
 * Test Cases:
 * . User Account can't login admin page
 * . Admin can delete user account
 * . User can't delete admin's account
 * . Only user/admin can update their own account
 * . Default admin account is admin@gmail.com
 * . authentication for admin is required for creating new admin account
 * . authenticated User can't create admin account
 * . authentication is not required for creating new user account
 * . Can't create new account with same existing username
 * . update password
 **/
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private DBInterface dbInterface;

    public AccountService(DBInterface dbInterface) {
        this.dbInterface = dbInterface;
    }

    public void createUserAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.User);
    }

    public void createAdminAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.Admin);
    }

    public JsonObject getUserAccountFromDBByUsername(String username) {
        String queryUserStatement = String.format(System.getProperty("query.user.statement"), username);

        JsonObject json_input = new JsonObject();
        json_input.put("query", new JsonObject(queryUserStatement));
        json_input.put("ESDataType", ESDataType.Account.toString());

        JsonArray userAccounts = dbInterface.query(json_input);

        if (userAccounts == null || userAccounts.size() == 0) {
            logger.warn("No account found for user " + username);
            return null;
        }

        if (userAccounts.size() > 1) {
            logger.warn("More than one user found for user " + username);
            throw new RuntimeException("More than one user found for user " + username);
        }

        return userAccounts.getJsonObject(0);
    }

    public JsonObject getUserAccountFromDBByUserid(String id) {
        JsonObject json_input = new JsonObject();
        json_input.put("id", id);
        json_input.put("ESDataType", ESDataType.Account.toString());

        return dbInterface.read(json_input);
    }

    private void createAccountWithRole(RoutingContext ctx, Role role) {
        ctx.vertx().executeBlocking(fut -> {
            JsonObject json_param = ctx.getBodyAsJson();
            String username = json_param.getString("username");
            String password = json_param.getString("password");

            JsonObject newAccount = createAccount(username, password, role);

            // createAccount return only id for successfully write
            // but returning a whole account field means existed
            if (!Utils.ifStringEmpty(newAccount.getString("username"))) {
                ctx.fail(HttpResponseStatus.CONFLICT.getCode());
                return;
            }

            ctx.response()
                    .setStatusCode(HttpResponseStatus.CREATED.getCode())
                    .putHeader("content-type", "application/text; charset=utf-8")
                    .end("Created account for username : " + username +
                            " with role : " + role.toString() +
                            " and id : " + newAccount.getString("id"));
        }, false, res -> {
            if (res.failed()) {
                logger.error("CreateAccountWithRole failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });
    }

    public JsonObject createAccount(String username, String password, Role role) {
        // Check for existing username
        JsonObject user_db = getUserAccountFromDBByUsername(username);
        if (user_db != null) {
            logger.warn("Skip account creation as account existed for username " + username);
            return user_db;
        }

        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

        UserAccount user = new UserAccount(username, hashedPasswordBase64, role, null, Sha512Hash.ALGORITHM_NAME);
        user.setSalt(salt.toBase64());
        user.setCreated_time(System.currentTimeMillis());

        JsonObject json_input = new JsonObject(Json.encode(user));
        json_input.put("ESDataType", ESDataType.Account.toString());

        return dbInterface.write(json_input);
    }

    public void deleteAccount(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            User ctxUser = ctx.user();

            String contextUser = ctxUser.principal().getString("username");

            ctxUser.isAuthorised("role:User", res -> {
                if (res.succeeded()) {
                    boolean hasRole = res.result();
                    if (hasRole) {
                        JsonObject json_input = new JsonObject();
                        json_input.put("id", (String) ctx.session().get("user_db_id"));
                        json_input.put("ESDataType", ESDataType.Account.toString());
                        dbInterface.delete(json_input);
                        //log out
                        ctx.clearUser();

                        //TODO: delete all posts by this user
                        ctx.response()
                                .setStatusCode(HttpResponseStatus.OK.getCode())
                                .putHeader("content-type", "application/text; charset=utf-8")
                                .end("Deleted account of username : " + contextUser);
                    }
                }
            });

            ctxUser.isAuthorised("role:Admin", res -> {
                if (res.succeeded()) {
                    boolean hasRole = res.result();
                    if (hasRole) {
                        String username_input = contextUser;
                        try {
                            username_input = ctx.getBodyAsJson().getString("username");
                        } catch (DecodeException e) {
                            //ignore
                        }

                        String user_db_id = ctx.session().get("user_db_id");

                        // get own account
                        if (contextUser.equals(username_input)) {
                            // log out
                            ctx.clearUser();
                        } else {
                            // get other user account
                            JsonObject user_db = getUserAccountFromDBByUsername(username_input);
                            if (user_db == null) {
                                logger.error("Couldn't find username " + username_input);
                                ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                                return;
                            }
                            user_db_id = user_db.getString("id");
                        }

                        JsonObject json_input = new JsonObject();
                        json_input.put("id", user_db_id);
                        json_input.put("ESDataType", ESDataType.Account.toString());
                        dbInterface.delete(json_input);

                        ctx.response()
                                .setStatusCode(HttpResponseStatus.OK.getCode())
                                .putHeader("content-type", "application/text; charset=utf-8")
                                .end("Deleted account of username : " + username_input);
                    }
                }
            });
        }, false, res -> {
            if (res.failed()) {
                logger.error("DeleteAccount failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });
    }

    public void updateAccountPassword(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            String password = ctx.request().formAttributes().get("password");

            if (password == null || password.equals("")) {
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }
            // Only current user can update his/her own account
            String contextUser = ctx.user().principal().getString("username");

            RandomNumberGenerator rng = new SecureRandomNumberGenerator();
            ByteSource salt = rng.nextBytes();

            //TODO: add iteration
            String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt)
                    .toBase64();

            JsonObject json_input = new JsonObject();
            json_input.put("id", (String) ctx.session().get("user_db_id"));
            json_input.put("password", hashedPasswordBase64);
            json_input.put("salt", salt.toBase64());
            json_input.put("hash_algo", Sha512Hash.ALGORITHM_NAME);

            json_input.put("ESDataType", ESDataType.Account.toString());

            dbInterface.update(json_input);

            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/text; charset=utf-8")
                    .end("Updated password for username : " + contextUser);
        }, false, res -> {
            if (res.failed()) {
                logger.error("UpdateAccountPassword failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });

    }

    public void updateAccountProfile(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            String body = ctx.getBodyAsString();
            JsonObject json_input = Utils.ifStringEmpty(body) ? new JsonObject() : new JsonObject(body);

            try {
                //validate fields
                Json.decodeValue(body, UserAccount.class);
            } catch (Exception e) {
                logger.error(e);
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            // Only current user can update his/her own account
            String contextUser = ctx.user().principal().getString("username");

            json_input.put("id", (String) ctx.session().get("user_db_id"));
            json_input.put("ESDataType", ESDataType.Account.toString());

            dbInterface.update(json_input);

            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/text; charset=utf-8")
                    .end("Updated profile for username : " + contextUser);
        }, false, res -> {
            if (res.failed()) {
                logger.error("UpdateAccountProfile failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });
    }

    public void getAccount(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            User ctxUser = ctx.user();
            String contextUser = ctxUser.principal().getString("username");

            ctxUser.isAuthorised("role:User", res -> {
                if (res.succeeded()) {
                    boolean hasRole = res.result();
                    if (hasRole) {
                        JsonObject user_db = getUserAccountFromDBByUserid(ctx.session().get("user_db_id"));
                        if (user_db == null) {
                            logger.error("Couldn't find username " + contextUser);
                            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                            return;
                        }
                        removeSensitiveAccountInfo(user_db);
                        ctx.response()
                                .setStatusCode(HttpResponseStatus.OK.getCode())
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(user_db.encodePrettily());
                    }
                }
            });

            ctxUser.isAuthorised("role:Admin", res -> {
                if (res.succeeded()) {
                    boolean hasRole = res.result();
                    if (hasRole) {
                        String username_input = contextUser;
                        try {
                            username_input = ctx.getBodyAsJson().getString("username");
                        } catch (DecodeException e) {
                            //ignore
                        }

                        JsonObject user_db;

                        // get own account
                        if (contextUser.equals(username_input))
                            user_db = getUserAccountFromDBByUserid(ctx.session().get("user_db_id"));
                        else
                            // get other user account
                            user_db = getUserAccountFromDBByUsername(username_input);

                        if (user_db == null) {
                            logger.error("Couldn't find username " + contextUser);
                            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                            return;
                        }
                        removeSensitiveAccountInfo(user_db);
                        ctx.response()
                                .setStatusCode(HttpResponseStatus.OK.getCode())
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(user_db.encodePrettily());
                    }
                }
            });
        }, false, res -> {
            if (res.failed()) {
                logger.error("GetAccount failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });

    }

    private void removeSensitiveAccountInfo(JsonObject userAccount) {
        userAccount.remove("password");
        userAccount.remove("salt");
        userAccount.remove("hash_algo");
        userAccount.remove("permission");
    }

    public void getAllAccountsByRole(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            String role = ctx.request().getParam("role");
            String body = ctx.getBodyAsString(); // {"from": 21, "size": 20}

            if (role == null) {
                ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                return;
            }

            String queryRoleStatement = String.format(System.getProperty("query.role.statement"), role);

            JsonObject json_input = Utils.ifStringEmpty(body) ? new JsonObject() : new JsonObject(body);
            json_input.put("query", new JsonObject(queryRoleStatement));
            json_input.put("ESDataType", ESDataType.Account.toString());

            JsonArray result = dbInterface.query(json_input);

            if (result == null) {
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                return;
            }

            @SuppressWarnings("unchecked")
            List<JsonObject> accounts_json = result.getList();

            accounts_json.stream().forEach(this::removeSensitiveAccountInfo);

            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(result.encodePrettily());
        }, false, res -> {
            if (res.failed()) {
                logger.error("GetAllAccountsByRole failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });
    }

    public void updateAccountConversationList(RoutingContext ctx, List<String> user_list, String conversation_id) {
        for (String username : user_list) {
            String user_id;
            long read_time = System.currentTimeMillis();
            if (username.equals(ctx.user().principal().getString("username"))) {
                user_id = ctx.session().get("user_db_id");
            } else {
                JsonObject user_db = getUserAccountFromDBByUsername(username);
                if (user_db == null) {
                    logger.error("Couldn't find username " + username);
                    ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
                    return;
                }
                user_id = user_db.getString("id");
                read_time = 0; // indicate not read yet for non logged in user
            }

            JsonObject json = new JsonObject();
            json.put("id", user_id);
            json.put("ESDataType", ESDataType.Account.toString());
            json.put("script", System.getProperty("account.conversation.add.script"));

            Map<String, Object> chatRecord = new HashMap<>();
            chatRecord.put("conversation_id", conversation_id);
            chatRecord.put("last_read_time", read_time);

            Map<String, Object> new_chat = new HashMap<>();
            new_chat.put("new_chat", chatRecord);

            json.put("params", new_chat);
            dbInterface.update(json);
            //TODO: send email to user
        }
    }

    public void getAccountChatList(RoutingContext ctx) {
        ctx.vertx().executeBlocking(fut -> {
            JsonObject account = getUserAccountFromDBByUserid(ctx.session().get("user_db_id"));
            JsonArray chat_list = account.getJsonArray("chat_list");

            List<String> id_list = chat_list.stream()
                    .map(o -> {
                        JsonObject ob = (JsonObject) o;
                        return ob.getString("conversation_id");
                    })
                    .collect(Collectors.toList());

            JsonArray id_list_json = new JsonArray(id_list);
            JsonObject json_input = new JsonObject();
            json_input.put("ids", id_list_json);
            json_input.put("ESDataType", ESDataType.Message.toString());

            JsonArray result = dbInterface.bulkRead(json_input);
            List<Object> conversation_list = result.getList();
            int i = 0;
            int j = 0;
            int size_chat_record = id_list.size();
            int size_conversation_list = conversation_list.size();
            // There may be some conversation_ids missed in DB
            while (i < size_chat_record && j < size_conversation_list) {
                JsonObject chat = chat_list.getJsonObject(i);
                JsonObject con = (JsonObject) conversation_list.get(j);

                if (chat.getString("conversation_id").equals(con.getString("id"))) {
                    con.remove("messages");
                    con.remove("id");
                    con.mergeIn(chat);
                    i++;
                    j++;
                } else
                    i++;
            }
            ctx.response()
                    .setStatusCode(HttpResponseStatus.OK.getCode())
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(result.encodePrettily());
        }, false, res -> {
            if (res.failed()) {
                logger.error("GetAccountChatList failed: ", res.cause());
                ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            }
        });
    }

    public void updateLastMessageReadTime(String user_db_id, String conversation_id) {
        JsonObject account = getUserAccountFromDBByUserid(user_db_id);
        JsonArray chat_list = account.getJsonArray("chat_list");
        for (Object o : chat_list) {
            JsonObject ob = (JsonObject) o;
            if (ob.getString("conversation_id").equals(conversation_id)) {
                ob.put("last_read_time", System.currentTimeMillis());
                break;
            }
        }

        JsonObject json_input = new JsonObject();
        json_input.put("id", user_db_id);
        json_input.put("chat_list", chat_list);
        json_input.put("ESDataType", ESDataType.Account.toString());

        dbInterface.update(json_input);
    }

    public void deleteChatRecord(String user_db_id, String conversation_id) {
        JsonObject account = getUserAccountFromDBByUserid(user_db_id);
        JsonArray chat_list = account.getJsonArray("chat_list");

        JsonObject found = null;
        for (Object o : chat_list) {
            JsonObject ob = (JsonObject) o;
            if (ob.getString("conversation_id").equals(conversation_id)) {
                found = ob;
                break;
            }
        }

        if(found == null){
            logger.info("Couldn't find matched conversation id for delete");
            return;
        }

        JsonObject json_input = new JsonObject();
        json_input.put("id", user_db_id);
        json_input.put("ESDataType", ESDataType.Account.toString());
        json_input.put("script", System.getProperty("account.conversation.remove.script"));

        Map<String, Object> chat = new HashMap<>();
        chat.put("chat", found.getMap());
        json_input.put("params", chat);
        dbInterface.update(json_input);
    }
}
