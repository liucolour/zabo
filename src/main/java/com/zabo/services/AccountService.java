package com.zabo.services;

import com.zabo.account.Role;
import com.zabo.account.UserAccount;
import com.zabo.dao.DBInterface;
import com.zabo.dao.ESDataType;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    public AccountService(DBInterface dbInterface){
        this.dbInterface = dbInterface;
    }

    public void createUserAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.User);
    }

    public void createAdminAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.Admin);
    }

    private void createAccountWithRole(RoutingContext ctx, Role role) {
        createAccount(ctx, role);
    }

    public UserAccount getUserAccountFromDB(String username){
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

        JsonObject account_json = userAccounts.getJsonObject(0);
        UserAccount account = Json.decodeValue(account_json.encode(), UserAccount.class);
        account.setId(account_json.getString("id"));
        return account;
    }

    private void createAccount(RoutingContext ctx, Role role) {
        JsonObject json_param = ctx.getBodyAsJson();
        String username = json_param.getString("username");
        String password = json_param.getString("password");

        // Check for existing username
        if (getUserAccountFromDB(username) != null){
            ctx.fail(HttpResponseStatus.CONFLICT.getCode());
            return;
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

        JsonObject result = dbInterface.write(json_input);

        ctx.response()
                .setStatusCode(HttpResponseStatus.CREATED.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Created account for username : " + username +
                        " with role : " + role.toString() +
                        " and id : " + result.getString("id"));
    }

    public void deleteAccount(RoutingContext ctx) {
        User ctxUser = ctx.user();

        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:User", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    UserAccount user_db = getUserAccountFromDB(contextUser);
                    if(user_db == null) {
                        logger.error("Couldn't find username " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }

                    JsonObject json_input = new JsonObject();
                    json_input.put("id", user_db.getId());
                    json_input.put("ESDataType", ESDataType.Account.toString());
                    dbInterface.delete(json_input);
                    //log out
                    ctx.clearUser();

                    ctx.response()
                            .setStatusCode(HttpResponseStatus.OK.getCode())
                            .putHeader("content-type", "application/text; charset=utf-8")
                            .end("Deleted account of username : " + contextUser);

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

                    UserAccount user_db;

                    // get own account
                    if (username_input == null || contextUser.equals(username_input)) {
                        user_db = getUserAccountFromDB(contextUser);
                        // log out
                        ctx.clearUser();
                    } else
                        // get other user account
                        user_db = getUserAccountFromDB(username_input);

                    if(user_db == null) {
                        logger.error("Couldn't find username " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }

                    JsonObject json_input = new JsonObject();
                    json_input.put("id", user_db.getId());
                    json_input.put("ESDataType", ESDataType.Account.toString());
                    dbInterface.delete(json_input);

                    ctx.response()
                            .setStatusCode(HttpResponseStatus.OK.getCode())
                            .putHeader("content-type", "application/text; charset=utf-8")
                            .end("Deleted account of username : " + contextUser);
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

        UserAccount user_db = getUserAccountFromDB(contextUser);

        if(user_db == null) {
            logger.error("Couldn't find username " + contextUser);
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

        user_db.setPassword(hashedPasswordBase64);
        user_db.setSalt(salt.toBase64());
        user_db.setHash_algo(Sha512Hash.ALGORITHM_NAME);

        JsonObject json_input = new JsonObject(Json.encode(user_db));
        json_input.put("ESDataType", ESDataType.Account.toString());

        dbInterface.update(json_input);

        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated password for username : " + user_db.getUsername());
    }

    public  void updateAccountProfile(RoutingContext ctx) {
        String content = ctx.getBodyAsString();
        UserAccount user_input = null;
        try {
            user_input = Json.decodeValue(content, UserAccount.class);
        } catch (DecodeException e){
            //ignore
        }

        if(user_input == null){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");

        UserAccount user_db = getUserAccountFromDB(contextUser);

        if(user_db == null) {
            logger.error("Couldn't find username " + contextUser);
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        user_db.setProfile(user_input.getProfile());

        JsonObject json_input = new JsonObject(Json.encode(user_db));
        json_input.put("ESDataType", ESDataType.Account.toString());

        dbInterface.update(json_input);

        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated profile for username : " + user_db.getUsername());
    }

    public void getAccount(RoutingContext ctx) {
        User ctxUser = ctx.user();
        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:User", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    UserAccount user_db = getUserAccountFromDB(contextUser);
                    if(user_db == null) {
                        logger.error("Couldn't find username " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }
                    removeSensitiveAccountInfo(user_db);
                    ctx.response()
                            .setStatusCode(HttpResponseStatus.OK.getCode())
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(user_db));
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

                    UserAccount user_db;

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
                            .end(Json.encodePrettily(user_db));
                }
            }
        });
    }

    private  void removeSensitiveAccountInfo(UserAccount userAccount) {
        userAccount.setPassword("");
        userAccount.setSalt("");
        userAccount.setHash_algo("");
        userAccount.setPermission("");
    }

    public void getAllAccountsByRole(RoutingContext ctx){
        String role = ctx.request().getParam("role");
        if(role == null){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String queryRoleStatement = String.format(System.getProperty("query.role.statement"), role);

        JsonObject json_input = new JsonObject();
        json_input.put("query", new JsonObject(queryRoleStatement));
        json_input.put("ESDataType", ESDataType.Account.toString());

        JsonArray result = dbInterface.query(json_input);

        List<JsonObject> accounts_json = result.getList();

        List<UserAccount> userAccounts = accounts_json.stream()
                .map(json->Json.decodeValue(json.encode(), UserAccount.class))
                .collect(Collectors.toList());
        userAccounts.stream().forEach(this::removeSensitiveAccountInfo);
        ctx.response()
                .setStatusCode(HttpResponseStatus.OK.getCode())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(userAccounts));
    }
}
