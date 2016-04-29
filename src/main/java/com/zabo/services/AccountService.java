package com.zabo.services;

import com.zabo.auth.Role;
import com.zabo.auth.UserAuthInfo;
import com.zabo.dao.DAO;
import com.zabo.dao.DAOFactory;
import com.zabo.dao.UserAuthInfoDAO;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;

/**
 * Created by zhaoboliu on 4/27/16.
 */
public class AccountService {
    public static void createUserAccount(RoutingContext ctx) {
        JsonObject json = ctx.getBodyAsJson();
        String user_id = json.getString("user_id");
        String password = json.getString("password");

        if(user_id == null || password == null)
            ctx.fail(HttpResponseStatus.BAD_REQUEST.getCode());

        //TODO: check for existing user_id
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        ByteSource salt = rng.nextBytes();

        //TODO: add iteration
        Hash hash = new SimpleHash(Sha512Hash.ALGORITHM_NAME, password.toCharArray(), salt);
        String hashedPasswordBase64 = hash.toBase64();

        UserAuthInfo user = new UserAuthInfo(user_id, hashedPasswordBase64, Role.USER, null, Sha512Hash.ALGORITHM_NAME);
        user.setSalt(salt.toBase64());
        DAOFactory factory = DAOFactory.getDAOFactorybyConfig();
        DAO dao = factory.getDAO(UserAuthInfoDAO.class);
        String id = dao.write(user);

        ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/text; charset=utf-8")
                .end("Account id : " + id);
    }
}
