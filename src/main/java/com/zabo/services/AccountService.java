package com.zabo.services;

import com.zabo.auth.Role;
import com.zabo.auth.UserAuthInfo;
import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import com.zabo.dao.UserAuthInfoDAO;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;

/**
 * Created by zhaoboliu on 4/27/16.
 */
public class AccountService {
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


    private static void createAccountWithRole(RoutingContext ctx, Role role) {
        if(role == Role.ADMIN) {
            User user = ctx.user();
            boolean isAuthenticated = ctx.user() != null;
            if(!isAuthenticated) {
                ctx.fail(HttpResponseStatus.UNAUTHORIZED.getCode());
                return;
            }

            user.isAuthorised("role:ADMIN", res -> {
                if(res.succeeded()) {
                    boolean hasRole = res.result();
                    if (hasRole){
                        createAccount(ctx, role);
                        return;
                    }
                }
                ctx.fail(HttpResponseStatus.UNAUTHORIZED.getCode());
            });
        } else {
            createAccount(ctx, role);
        }
    }

    private static void createAccount(RoutingContext ctx, Role role) {
        JsonObject json = ctx.getBodyAsJson();
        String user_id = json.getString("user_id");
        String password = json.getString("password");

        if(user_id == null || password == null)
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO(role);

        if (ifUserExisted(dao, user_id)){
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
                .end("Created account id : " + id);
    }

    public static void deleteAccountWithRole(RoutingContext ctx, Role role) {
        final String id = ctx.request().getParam("id");

        if(id == null) {
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO(role);

        UserAuthInfo user_db = (UserAuthInfo)dao.read(id);

        User ctxUser = ctx.user();

        ctxUser.isAuthorised("role:USER", res -> {
            if(res.succeeded()){
                if(!user_db.getUser_id().equals(ctx.user().principal().getString("user_id"))) {
                    ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
                    return;
                }

                deleteAccount(ctx, dao, id);
            }
        });

        ctxUser.isAuthorised("role:ADMIN", res -> {
            if(res.succeeded())
                deleteAccount(ctx, dao, id);
        });
    }

    public static void updateAccountWithRole(RoutingContext ctx, Role role) {
        final String id = ctx.request().getParam("id");
        UserAuthInfo user_input = Json.decodeValue(ctx.getBodyAsString(), UserAuthInfo.class);

        if(id == null || user_input == null) {
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        String contextUser = ctx.user().principal().getString("user_id");
        if(!user_input.getUser_id().equals(contextUser)){
            ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
            return;
        }

        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getUserAuthInfoDAO(role);

        UserAuthInfo user_db = (UserAuthInfo)dao.read(id);
        if(!user_db.getUser_id().equals(contextUser)){
            ctx.fail(HttpResponseStatus.FORBIDDEN.getCode());
            return;
        }

        if(user_input.getPassword() != null) {

            RandomNumberGenerator rng = new SecureRandomNumberGenerator();
            ByteSource salt = rng.nextBytes();

            //TODO: add iteration
            String hashedPasswordBase64 = new SimpleHash(Sha512Hash.ALGORITHM_NAME, user_input.getPassword().toCharArray(), salt).toBase64();

            user_db.setPassword(hashedPasswordBase64);
            user_db.setSalt(salt.toBase64());
            user_db.setHash_algo(Sha512Hash.ALGORITHM_NAME);
        }

        dao.update(user_input.getId(), user_db);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Updated Account id : " + user_input.getId());
    }

    private static boolean ifUserExisted(DAO dao, String user_id) {
        //TODO: make query generic and not bound to elasticsearch here
        String queryUserStatement = "{" +
                " \"query\": {" +
                "   \"constant_score\": {" +
                "     \"filter\": {" +
                "        \"term\": " +
                "            {\"user_id\": \"" + user_id + "\"}" +
                "     }" +
                "   }" +
                " }" +
                "}";

        List<UserAuthInfo> authInfos;
        authInfos = dao.query(queryUserStatement);
        if (authInfos.size() > 0)
            return true;
        return false;
    }

    private static void deleteAccount(RoutingContext ctx, DAO dao, String id) {
        dao.delete(id);

        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Deleted Account id : " + id);
    }
}
