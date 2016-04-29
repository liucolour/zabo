package com.zabo.auth;

import com.zabo.dao.DAOFactory;
import com.zabo.dao.UserAuthInfoDAO;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by zhaoboliu on 4/27/16.
 */
public class DBShiroAuthorizingRealm extends AuthorizingRealm {
    private static final Logger logger = LoggerFactory.getLogger(DBShiroAuthorizingRealm.class);

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //null usernames are invalid
        if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }

        String username = (String) getAvailablePrincipal(principals);

        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.");
        }

        //TODO: make query generic and not bound to elasticsearch here
        String queryStatement = "{" +
                " \"query\": {" +
                "   \"filtered\": {" +
                "     \"filter\": {" +
                "        \"match\": " +
                "            {\"user_id\": \"" + username + "\"}" +
                "     }" +
                "   }" +
                " }" +
                "}";

        List<UserAuthInfo> authInfos;
        try {
            authInfos = DAOFactory.getDAOFactorybyConfig().getUserAuthInfoDAO().query(queryStatement);
        }catch (Throwable e) {
            logger.error("Data Access error: ", e);
            throw new AuthenticationException(e);
        }

        String dbPassword = authInfos.get(0).getPassword();
        String salt = authInfos.get(0).getSalt();

        if (dbPassword == null) {
            throw new UnknownAccountException("No account found for user [" + username + "]");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(username,
                dbPassword.toCharArray(),
                ByteSource.Util.bytes(Base64.decode(salt.getBytes())),
                getName());
        return info;
    }
}
