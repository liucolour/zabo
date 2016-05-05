package com.zabo.verticles;

import com.zabo.auth.DBShiroAuthorizingRealm;
import com.zabo.auth.RoleBasedFormLoginHandler;
import com.zabo.services.AccountService;
import com.zabo.services.PostService;
import com.zabo.services.PostService2;
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
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.realm.AuthorizingRealm;

import java.io.File;

/**
 * Created by zhaoboliu on 4/3/16.
 */
//TODO: use system property for "role:Admin" "role:User"
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
                System.getProperty("basedir") + "/" + System.getProperty("image.dir")));

        //TODO: change to clustered session
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        AuthorizingRealm userRealm = new DBShiroAuthorizingRealm();
        HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
        credentialsMatcher.setHashAlgorithmName(Sha512Hash.ALGORITHM_NAME);
        credentialsMatcher.setStoredCredentialsHexEncoded(false);
        //TODO: credentialsMatcher.setHashIterations(10);
        userRealm.setCredentialsMatcher(credentialsMatcher);

        AuthProvider authProvider = ShiroAuth.create(vertx, userRealm);

        // We need a user session handler too to make sure the user is stored in the session between requests
        router.route().handler(UserSessionHandler.create(authProvider));

        // public API without authentication required
        router.get("/api/posts/category/:category/:id").handler(PostService2::getPost);
        router.get("/api/upload/ui").handler(PostService::getUploadUI);
        router.post("/api/query/posts/:category").handler(PostService::queryPosts);
        router.post("/api/user/accounts").handler(AccountService::createUserAccount);

        // Handle logout
        router.route("/api/logout").handler(context -> {
            context.clearUser();
            // Redirect back to the index page
            context.response().putHeader("location", "/").setStatusCode(302).end();
        });

        String loginPage = "/login.html";
        String adminLoginPage = "/adminLogin.html";

        // deleteAccount, user can only delete own account, admin can delete anyone's account
        router.delete("/api/accounts").handler(RedirectAuthHandler.create(authProvider,loginPage));

        // getAccount, logged in user can only get its own profile, admin can get anyone's profile
        router.post("/api/accounts/*").handler(RedirectAuthHandler.create(authProvider,loginPage));

        // updateAccountPassword and updateAccountProfile, user or admin can only update own password
        router.put("/api/accounts/*").handler(RedirectAuthHandler.create(authProvider,loginPage));

        // getAllAccountsByRole
        router.get("/api/accounts/:role").handler(RedirectAuthHandler.create(authProvider,loginPage).addAuthority("role:Admin"));

        // queyUserPosts
        router.post("/api/posts/user/*").handler(RedirectAuthHandler.create(authProvider, loginPage));

        // deletePostWithRole, user can only delete own post, admin can delete anyone's post
        router.delete("/api/posts/category/*").handler(RedirectAuthHandler.create(authProvider, loginPage));

        // updatePost
        router.put("/api/posts/category/*").handler(RedirectAuthHandler.create(authProvider, loginPage).addAuthority("role:User"));

        // addPost
        router.post("/api/posts/category/*").handler(RedirectAuthHandler.create(authProvider, loginPage).addAuthority("role:User"));

        // uploadForm
        router.post("/api/upload/*").handler(RedirectAuthHandler.create(authProvider, loginPage).addAuthority("role:User"));

        // createAdminAccount, default admin is admin@zabo.com, can only logged in admin can create a new admin account
        router.post("/api/admin/accounts").handler(RedirectAuthHandler.create(authProvider, adminLoginPage).addAuthority("role:Admin"));

        // Handle the user login
        router.route("/api/user/login").handler(new RoleBasedFormLoginHandler(authProvider, "role:User")
                .setDirectLoggedInOKURL("/index.html")
                .setReturnURLParam(null));

        // Handle the admin login
        router.route("/api/admin/login").handler(new RoleBasedFormLoginHandler(authProvider, "role:Admin")
                .setDirectLoggedInOKURL("/admin.html")
                .setReturnURLParam(null));

        // public API with authentication required
        router.post("/api/posts/user").handler(PostService::queyUserPosts);
        router.post("/api/posts/category/:category").handler(PostService2::addPost);
        router.put("/api/posts/category/:category/:id").handler(PostService::updatePost);
        router.delete("/api/posts/category/:category/:id").handler(PostService::deletePostWithRole);
        router.post("/api/upload/form").handler(PostService::uploadForm);

        router.delete("/api/accounts").handler(AccountService::deleteAccount);
        router.post("/api/accounts").handler(AccountService::getAccount);
        router.put("/api/accounts/password").handler(AccountService::updateAccountPassword);
        router.put("/api/accounts/profile").handler(AccountService::updateAccountProfile);
        router.get("/api/accounts/:role").handler(AccountService::getAllAccountsByRole);

        router.post("/api/admin/accounts").handler(AccountService::createAdminAccount);

        router.route().handler(StaticHandler
                .create()
                .setAllowRootFileSystemAccess(true)
                .setWebRoot(System.getProperty("basedir") + "/webroot"));

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
