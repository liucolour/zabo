package com.zabo.services;

import com.zabo.account.Role;
import com.zabo.account.UserAccount;
import com.zabo.account.UserProfile;
import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
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
 * . Can't create new account with same existing user_id
 * . update password
 **/
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    public static void createUserAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.USER);
    }

    public static void createAdminAccount(RoutingContext ctx) {
        createAccountWithRole(ctx, Role.ADMIN);
    }

    public static void deleteUserAccount(RoutingContext ctx) {
        deleteAccountWithRole(ctx, Role.USER);
    }

    public static void deleteAdminAccount(RoutingContext ctx) {
        deleteAccountWithRole(ctx, Role.ADMIN);
    }

   // public static void getAllUserAccounts(RoutingContext ctx) { getAllAccountsOfRole(ctx, Role.USER); }

   // public static void getAllAdminAccounts(RoutingContext ctx) { getAllAccountsOfRole(ctx, Role.ADMIN); }

    private static void createAccountWithRole(RoutingContext ctx, Role role) {
        createAccount(ctx, role);
    }

    private static void createAccount(RoutingContext ctx, Role role) {
        JsonObject json = ctx.getBodyAsJson();
        String user_id = json.getString("user_id").toLowerCase().trim();
        String password = json.getString("password").trim();

        if(user_id == null || password == null)
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUseAccountDAO();

        // Check for existing user_id
        if (getUserByUserID(dao, user_id) != null){
            ctx.fail(HttpResponseStatus.CONFLICT.getCode());
            return;
        }
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

        UserAccount user = new UserAccount(user_id, hashedPasswordBase64, role, null, Sha512Hash.ALGORITHM_NAME);
        user.setSalt(salt.toBase64());

        dao.write(user);

        ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Created account for user id : " + user_id + "with role :" + role.toString());
    }

    public static void deleteAccountWithRole(RoutingContext ctx, Role role) {

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUseAccountDAO();

        User ctxUser = ctx.user();
        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:USER", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    UserAccount user_db = getUserByUserID(dao, contextUser);
                    if(user_db == null) {
                        logger.error("Couldn't find user_id " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }
                    deleteAccount(ctx, dao, user_db.getId());
                }
            }
        });

        ctxUser.isAuthorised("role:ADMIN", res -> {
            if(res.succeeded()) {
                boolean hasRole = res.result();
                if(hasRole) {
                    String user_id_input = null;
                    try {
                        user_id_input = ctx.getBodyAsJson().getString("user_id");
                    } catch (DecodeException e) {
                        //ignore
                    }

                    UserAccount user_db;

                    // delete own account
                    if (user_id_input == null || contextUser.equals(user_id_input))
                        user_db = getUserByUserID(dao, contextUser);
                    else
                        // delete user account
                        user_db = getUserByUserID(dao, user_id_input);

                    if(user_db == null) {
                        logger.error("Couldn't find user_id " + contextUser);
                        ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
                        return;
                    }
                    deleteAccount(ctx, dao, user_db.getId());
                }
            }
        });
    }

    public static void updateAccountPassword(RoutingContext ctx) {
        String password = ctx.request().formAttributes().get("password");

        if(password == null || password.equals("")){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }
        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUseAccountDAO();

        if(dao == null) {
            ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }

        UserAccount user_db;

        user_db = getUserByUserID(dao, contextUser);

        if(user_db == null) {
            logger.error("Couldn't find user_id " + contextUser);
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

        dao.update(user_db.getId(), user_db);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated password for user id : " + user_db.getUser_id());
    }

    public static void updateAccountProfile(RoutingContext ctx) {
        String content = ctx.getBodyAsString();
        UserAccount user_input = null;
        try {
            user_input = Json.decodeValue(content, UserAccount.class);
        }catch (DecodeException e){
            //ignore
        }

        if(user_input == null){
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUseAccountDAO();

        if(dao == null) {
            ctx.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            return;
        }

        UserAccount user_db = getUserByUserID(dao, contextUser);

        if(user_db == null) {
            logger.error("Couldn't find user_id " + contextUser);
            ctx.fail(HttpResponseStatus.NOT_FOUND.getCode());
            return;
        }

        if(user_input != null && user_input.getProfile() != null)
            user_db.setProfile(user_input.getProfile());

        dao.update(user_db.getId(), user_db);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated profile for user id : " + user_db.getUser_id());
    }

    private static UserAccount getUserByUserID(DAO dao, String user_id) {
        String queryUserStatement = String.format(System.getProperty("query.user.statement"), user_id);

        List<UserAccount> userAccounts;
        userAccounts = dao.query(queryUserStatement);
        if (userAccounts.size() > 1) {
            logger.error("Found duplicated user in database with id " + user_id);
            throw new RuntimeException("Found duplicated user id in database");
        }

        if(userAccounts.size() == 0)
            return null;
        return userAccounts.get(0);
    }

    private static void deleteAccount(RoutingContext ctx, DAO dao, String id) {
        dao.delete(id);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Deleted account of user id : " + id);
    }

//    private static void getAllAccountsOfRole(RoutingContext ctx, Role role){
//        String queryRoleStatement = String.format(System.getProperty("query.role.statement"), role.toString());
//
//        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
//        DAO dao = factory.getUserAuthInfoDAO();
//
//        List<UserAccount> authInfos;
//        authInfos = dao.query(queryRoleStatement);
//        ctx.response()
//                .setStatusCode(HttpResponseStatus.OK.getCode())
//                .putHeader("content-type", "application/json; charset=utf-8")
//                .end(Json.encodePrettily(authInfos));
//    }
}
