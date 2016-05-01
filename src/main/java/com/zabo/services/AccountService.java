package com.zabo.services;

import com.zabo.auth.Role;
import com.zabo.auth.UserAuthInfo;
import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import io.vertx.core.json.DecodeException;
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

    public static void updateUserAccount(RoutingContext ctx) {
        updateAccountWithRole(ctx, Role.USER);
    }

    public static void updateAdminAccount(RoutingContext ctx) {
        updateAccountWithRole(ctx, Role.ADMIN);
    }

    public static void deleteUserAccount(RoutingContext ctx) {
        deleteAccountWithRole(ctx, Role.USER);
    }

    public static void deleteAdminAccount(RoutingContext ctx) {
        deleteAccountWithRole(ctx, Role.ADMIN);
    }

    public static void getAllUserAccounts(RoutingContext ctx) { getAllAccountsWithRole(ctx, Role.USER); }

    public static void getAllAdminAccounts(RoutingContext ctx) { getAllAccountsWithRole(ctx, Role.ADMIN); }

    private static void createAccountWithRole(RoutingContext ctx, Role role) {
        createAccount(ctx, role);
    }

    private static void createAccount(RoutingContext ctx, Role role) {
        JsonObject json = ctx.getBodyAsJson();
        String user_id = json.getString("user_id");
        String password = json.getString("password");

        if(user_id == null || password == null)
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO();

        // Check for existing user_id
        if (getUserByID(dao, user_id) != null){
            ctx.fail(HttpResponseStatus.CONFLICT.getCode());
            return;
        }
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

        UserAuthInfo user = new UserAuthInfo(user_id, hashedPasswordBase64, role, null, Sha512Hash.ALGORITHM_NAME);
        user.setSalt(salt.toBase64());

        String id = dao.write(user);

        ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Created account id : " + id + "with role :" + role.toString());
    }

    public static void deleteAccountWithRole(RoutingContext ctx, Role role) {
        final String id = ctx.request().getParam("id");

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO();

        User ctxUser = ctx.user();
        String contextUser = ctxUser.principal().getString("username");

        ctxUser.isAuthorised("role:USER", res -> {
            if(res.succeeded()){
                boolean hasRole = res.result();
                if(hasRole) {
                    UserAuthInfo user_db = getUserByID(dao, contextUser);
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

                    UserAuthInfo user_db;

                    // delete own account
                    if (user_id_input == null || contextUser.equals(user_id_input))
                        user_db = getUserByID(dao, contextUser);
                    else
                        // delete user account
                        user_db = getUserByID(dao, user_id_input);
                    deleteAccount(ctx, dao, user_db.getId());
                }
            }
        });
    }

    public static void updateAccountWithRole(RoutingContext ctx, Role role) {
        final String id = ctx.request().getParam("id");
        final String password = ctx.request().formAttributes().get("password");
        //UserAuthInfo user_input = Json.decodeValue(ctx.getBodyAsString(), UserAuthInfo.class);

//        if(id == null) {
//            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
//            return;
//        }

        // Only current user can update his/her own account
        String contextUser = ctx.user().principal().getString("username");
//        if(!user_input.getUser_id().equals(contextUser)){
//            ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
//            return;
//        }

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO();

        UserAuthInfo user_db;
        if(id != null)
            user_db = (UserAuthInfo)dao.read(id);
        else
            user_db = getUserByID(dao, contextUser);

        if(password != null) {

            RandomNumberGenerator rng = new SecureRandomNumberGenerator();
            ByteSource salt = rng.nextBytes();

            //TODO: add iteration
            String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt).toBase64();

            user_db.setPassword(hashedPasswordBase64);
            user_db.setSalt(salt.toBase64());
            user_db.setHash_algo(Sha512Hash.ALGORITHM_NAME);
        }

        dao.update(user_db.getId(), user_db);

        if(password != null){
            logger.info("Account password was changed for id : {}", user_db.getId());
        }

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated Account id : " + user_db.getId());
    }

    private static UserAuthInfo getUserByID(DAO dao, String user_id) {
        String queryUserStatement = String.format(System.getProperty("query.user.statement"), user_id);

        List<UserAuthInfo> authInfos;
        authInfos = dao.query(queryUserStatement);
        if (authInfos.size() > 1) {
            logger.error("Found duplicated user in database with id " + user_id);
            throw new RuntimeException("Found duplicated user id in database");
        }

        if(authInfos.size() == 0)
            return null;
        return authInfos.get(0);
    }

    private static void deleteAccount(RoutingContext ctx, DAO dao, String id) {
        dao.delete(id);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Deleted Account id : " + id);
    }

    private static void getAllAccountsWithRole(RoutingContext ctx, Role role){
        ctx.fail(HttpResponseStatus.NOT_IMPLEMENTED.getCode());
    }
}
