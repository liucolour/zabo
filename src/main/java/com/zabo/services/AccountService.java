package com.zabo.services;

import com.zabo.account.Role;
import com.zabo.account.UserAccount;
import com.zabo.dao.DBInterface;
import com.zabo.dao.ESDataType;
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

import java.util.List;

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

    public AccountService(DBInterface dbInterface){
        this.dbInterface = dbInterface;
    }

    public void createUserAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.User);
    }

    public void createAdminAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.Admin);
    }

    public JsonObject getUserAccountFromDB(String username){
        String queryUserStatement = String.format(System.getProperty("query.user.statement"), username);

        JsonObject json_input = new JsonObject();
        json_input.put("query", new JsonObject(queryUserStatement));
        json_input.put("ESDataType", ESDataType.Account.toString());

        JsonArray userAccounts = dbInterface.query(json_input);

        if(userAccounts == null || userAccounts.size() == 0) {
            logger.warn("No account found for user " + username);
            return null;
        }

        if(userAccounts.size() > 1){
            logger.warn("More than one user found for user " + username);
            throw new RuntimeException("More than one user found for user " + username);
        }

        return userAccounts.getJsonObject(0);
    }

    private void createAccountWithRole(RoutingContext ctx, Role role) {
        JsonObject json_param = ctx.getBodyAsJson();
        String username = json_param.getString("username");
        String password = json_param.getString("password");

        JsonObject newAccount = createAccount(username, password, role);

        // createAccount return only id for successfully write
        // but returning a whole account field means existed
        if(!Utils.ifStringEmpty(newAccount.getString("username"))) {
            ctx.fail(HttpResponseStatus.CONFLICT.getCode());
            return;
        }

        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Created account for username : " + username +
                        " with role : " + role.toString() +
                        " and id : " + newAccount.getString("id"));
    }

    public JsonObject createAccount(String username, String password, Role role){
        // Check for existing username
        JsonObject user_db = getUserAccountFromDB(username);
        if(user_db != null){
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
        User ctxUser = ctx.user();

        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:User", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    JsonObject user_db = getUserAccountFromDB(contextUser);
                    if(user_db == null) {
                        logger.error("Couldn't find username " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }

                    JsonObject json_input = new JsonObject();
                    json_input.put("id", user_db.getString("id"));
                    json_input.put("ESDataType", ESDataType.Account.toString());
                    dbInterface.delete(json_input);
                    //log out
                    ctx.clearUser();

                    ctx.response()
                            .setStatusCode(HttpResponseStatus.OK.getCode())
                            .putHeader("content-type", "application/text; charset=utf-8")
                            .end("Deleted account of username : " + user_db.getString("username"));

                }
            }
        });

        ctxUser.isAuthorised("role:Admin", res -> {
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    String username_input = contextUser;
                    try {
                        username_input = ctx.getBodyAsJson().getString("username");
                    } catch (DecodeException e) {
                        //ignore
                    }

                    JsonObject user_db;

                    // get own account
                    if (contextUser.equals(username_input)) {
                        user_db = getUserAccountFromDB(contextUser);
                        // log out
                        ctx.clearUser();
                    } else {
                        // get other user account
                        user_db = getUserAccountFromDB(username_input);
                    }

                    if(user_db == null) {
                        logger.error("Couldn't find username " + username_input);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }

                    JsonObject json_input = new JsonObject();
                    json_input.put("id", user_db.getString("id"));
                    json_input.put("ESDataType", ESDataType.Account.toString());
                    dbInterface.delete(json_input);

                    ctx.response()
                            .setStatusCode(HttpResponseStatus.OK.getCode())
                            .putHeader("content-type", "application/text; charset=utf-8")
                            .end("Deleted account of username : " + user_db.getString("username"));
                }
            }
        });
    }

    public  void updateAccountPassword(RoutingContext ctx) {
        String password = ctx.request().formAttributes().get("password");

        if(password == null || password.equals("")){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }
        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");

        JsonObject user_db = getUserAccountFromDB(contextUser);

        if(user_db == null) {
            logger.error("Couldn't find username " + contextUser);
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

        JsonObject json_input = user_db.copy();
        json_input.put("password",hashedPasswordBase64);
        json_input.put("salt",salt.toBase64());
        json_input.put("hash_algo",Sha512Hash.ALGORITHM_NAME);

        json_input.put("ESDataType", ESDataType.Account.toString());

        dbInterface.update(json_input);

        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated password for username : " + contextUser);
    }

    public void updateAccountProfile(RoutingContext ctx) {
        String body = ctx.getBodyAsString();
        JsonObject json_input = Utils.ifStringEmpty(body)? new JsonObject() : new JsonObject(body);

        try {
            Json.decodeValue(body, UserAccount.class);
        } catch (Exception e){
            logger.error(e);
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");

        JsonObject user_db = getUserAccountFromDB(contextUser);

        if(user_db == null) {
            logger.error("Couldn't find username " + contextUser);
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        json_input.put("id", user_db.getString("id"));
        json_input.put("ESDataType", ESDataType.Account.toString());

        dbInterface.update(json_input);

        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated profile for username : " + contextUser);
    }

    public void getAccount(RoutingContext ctx) {
        User ctxUser = ctx.user();
        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:User", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    JsonObject user_db = getUserAccountFromDB(contextUser);
                    if(user_db == null) {
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
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    String username_input = null;
                    try {
                        username_input = ctx.getBodyAsJson().getString("username");
                    } catch (DecodeException e) {
                        //ignore
                    }

                    JsonObject user_db;

                    // get own account
                    if (username_input == null || contextUser.equals(username_input))
                        user_db = getUserAccountFromDB(contextUser);
                    else
                        // get other user account
                        user_db = getUserAccountFromDB(username_input);

                    if(user_db == null) {
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
    }

    private  void removeSensitiveAccountInfo(JsonObject userAccount) {
        userAccount.remove("password");
        userAccount.remove("salt");
        userAccount.remove("hash_algo");
        userAccount.remove("permission");
    }

    public void getAllAccountsByRole(RoutingContext ctx){
        String role = ctx.request().getParam("role");
        String body = ctx.getBodyAsString(); // {"from": 21, "size": 20}

        if(role == null){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String queryRoleStatement = String.format(System.getProperty("query.role.statement"), role);

        JsonObject json_input = Utils.ifStringEmpty(body)? new JsonObject() : new JsonObject(body);
        json_input.put("query", new JsonObject(queryRoleStatement));
        json_input.put("ESDataType", ESDataType.Account.toString());

        JsonArray result = dbInterface.query(json_input);

        List<JsonObject> accounts_json = result.getList();

        accounts_json.stream().forEach(this::removeSensitiveAccountInfo);

        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(result.encodePrettily());
    }
}
