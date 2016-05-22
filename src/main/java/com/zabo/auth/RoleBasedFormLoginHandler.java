package com.zabo.auth;

import com.zabo.services.AccountService;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by zhaoboliu on 4/30/16.
 */
public class RoleBasedFormLoginHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(RoleBasedFormLoginHandler.class);
    /**
     * The default value of the form attribute which will contain the username
     */
    String DEFAULT_USERNAME_PARAM = "username";

    /**
     * The default value of the form attribute which will contain the password
     */
    String DEFAULT_PASSWORD_PARAM = "password";

    /**
     * The default value of the form attribute which will contain the return url
     */
    String DEFAULT_RETURN_URL_PARAM = "return_url";

    private final AuthProvider authProvider;
    private final AccountService accountService;

    private String usernameParam = DEFAULT_USERNAME_PARAM;
    private String passwordParam = DEFAULT_PASSWORD_PARAM;
    private String returnURLParam = DEFAULT_RETURN_URL_PARAM;
    private String directLoggedInOKURL;
    private String requiredRole; //shiro role format: role:Admin

    public RoleBasedFormLoginHandler(AuthProvider authProvider, String role, AccountService accountService) {
        this.authProvider = authProvider;
        this.requiredRole = role;
        this.accountService = accountService;
    }

    public RoleBasedFormLoginHandler setUsernameParam(String usernameParam) {
        this.usernameParam = usernameParam;
        return this;
    }

    public RoleBasedFormLoginHandler setPasswordParam(String passwordParam) {
        this.passwordParam = passwordParam;
        return this;
    }

    public RoleBasedFormLoginHandler setReturnURLParam(String returnURLParam) {
        this.returnURLParam = returnURLParam;
        return this;
    }

    public RoleBasedFormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL) {
        this.directLoggedInOKURL = directLoggedInOKURL;
        return this;
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest req = context.request();
        if (req.method() != HttpMethod.POST) {
            context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED.getCode()); // Must be a POST
            return;
        }

        if (!req.isExpectMultipart()) {
            throw new IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?");
        }

        MultiMap params = req.formAttributes();
        String username = params.get(usernameParam);
        String password = params.get(passwordParam);

        if (username == null || password == null) {
            logger.warn("No username or password provided in form - did you forget to include a BodyHandler?");
            context.fail(HttpResponseStatus.BAD_REQUEST.getCode());
            return;
        }

        Session session = context.session();
        JsonObject authInfo = new JsonObject().put("username", username).put("password", password);
        authProvider.authenticate(authInfo, res -> {
            if (res.succeeded()) {
                User user = res.result();
                context.setUser(user);

                user.isAuthorised(requiredRole, authzRes -> {
                    if(authzRes.succeeded()) {
                        boolean hasRole = authzRes.result();
                        if (hasRole) {
                            if (session != null) {
                                JsonObject account = accountService.getUserAccountFromDBByUsername(username);
                                session.put("user_db_id", account.getString("id"));
                                String returnURL = session.remove(returnURLParam);
                                if (returnURL != null) {
                                    // Now redirect back to the original url
                                    doRedirect(req.response(), returnURL);
                                    return;
                                }
                            }
                            // Either no session or no return url
                            if (directLoggedInOKURL != null) {
                                // Redirect to the default logged in OK page - this would occur
                                // if the user logged in directly at this URL without being redirected here first from another
                                // url
                                doRedirect(req.response(), directLoggedInOKURL);
                            } else {
                                // Just show a basic page
                                req.response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE);
                            }
                            return;
                        }
                        else {
                            logger.error("User doesn't have required role to login");
                            context.fail(HttpResponseStatus.FORBIDDEN.getCode());
                        }
                    } else {
                        logger.error("Authorization test failed during login");
                        context.fail(HttpResponseStatus.FORBIDDEN.getCode());
                    }
                });
            } else {
                logger.error("Authentication failed during login");
                context.fail(HttpResponseStatus.FORBIDDEN.getCode());
            }
        });
    }

    //TODO: test 307
    private void doRedirect(HttpServerResponse response, String url) {
        response.putHeader("location", url).setStatusCode(302).end();
    }

    private static final String DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "" +
            "<html><body><h1>Login successful</h1></body></html>";
}