package com.zabo.verticles;

import com.zabo.auth.DBShiroAuthorizingRealm;
import com.zabo.services.AccountService;
import com.zabo.services.PostService;
import com.zabo.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.credential.Sha512CredentialsMatcher;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.realm.AuthorizingRealm;

import java.io.File;

/**
 * Created by zhaoboliu on 4/3/16.
 */
public class RestAPIVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(RestAPIVerticle.class.getName());

    @Override
    public void start(Future<Void> fut) {
        // either already specified from script using option -Dbasedir or take current absolute path running from IDE
        System.setProperty("basedir", System.getProperty("basedir", new File(".").getAbsolutePath()));

        logger.debug("Base dir is {}", System.getProperty("basedir"));

        Router router = Router.router(vertx);

        //log http request
        router.route().handler(LoggerHandler.create());

        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create().setUploadsDirectory(
                System.getProperty("basedir")+"/"+System.getProperty("image.dir")));
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        AuthorizingRealm realm = new DBShiroAuthorizingRealm();
        HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
        credentialsMatcher.setHashAlgorithmName(Sha512Hash.ALGORITHM_NAME);
        credentialsMatcher.setStoredCredentialsHexEncoded(false);
        //TODO: credentialsMatcher.setHashIterations(10);
        realm.setCredentialsMatcher(credentialsMatcher);
        AuthProvider authProvider = ShiroAuth.create(vertx, realm);

        // We need a user session handler too to make sure the user is stored in the session between requests
        router.route().handler(UserSessionHandler.create(authProvider));

        // public API without authentication required
        router.get("/api/posts/:category/:id").handler(PostService::getOne);
        router.get("/api/upload/ui").handler(PostService::getUploadUI);
        router.post("/api/query/posts/:category/:type").handler(PostService::query);
        router.post("/api/account").handler(AccountService::createUserAccount);

        // Implement logout
        router.route("/api/logout").handler(context -> {
            context.clearUser();
            // Redirect back to the index page
            context.response().putHeader("location", "/").setStatusCode(302).end();
        });

        router.route().handler(StaticHandler
                .create()
                .setAllowRootFileSystemAccess(true)
                .setWebRoot(System.getProperty("basedir")+"/webroot"));

        String loginPage = "/login.html";
        router.post("/api/posts/*").handler(RedirectAuthHandler.create(authProvider, loginPage));
        router.post("/api/upload/*").handler(RedirectAuthHandler.create(authProvider, loginPage));
        router.delete("/api/posts/*").handler(RedirectAuthHandler.create(authProvider, loginPage));
        router.put("/api/posts/*").handler(RedirectAuthHandler.create(authProvider, loginPage));

        // Handles the actual login
        router.route("/api/login").handler(FormLoginHandler.create(authProvider)
                .setDirectLoggedInOKURL("/index.html")
                .setReturnURLParam(null));

        // public API with authentication required
        router.post("/api/posts/:category").handler(PostService::addOne);
        router.put("/api/posts/:category/:id").handler(PostService::updateOne);
        router.delete("/api/posts/:category/:id").handler(PostService::deleteOne);
        router.post("/api/upload/form").handler(PostService::uploadForm);


        //doesn't seem to need this as html tag <img src=> can transfer image directly
//        router.get("/image/:id").handler(cxt -> {
//            String id = cxt.request().getParam("id");
//            //TODO: check file exist
//            cxt.response().sendFile("image/" + id);
//        });

        Integer port = Utils.getPropertyInt("http.port");
        if(port == null)
            return;
        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(
                port,
                result -> {
                    if (result.succeeded()) {
                        logger.info("Verticle {} is started successfully at port {}",
                                RestAPIVerticle.class.getSimpleName(),
                                port);
                        fut.complete();
                    } else {
                        logger.error("Verticle {} failed to start at port {} with cause {}",
                                RestAPIVerticle.class.getSimpleName(),
                                port,
                                result.cause());
                        fut.fail(result.cause());
                    }
                }
            );
    }
}
