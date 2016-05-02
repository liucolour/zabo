package com.zabo.auth;

import com.zabo.account.Role;
import com.zabo.account.UserAccount;
import com.zabo.dao.DAOFactory;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by zhaoboliu on 4/27/16.
 */
public class DBShiroAuthorizingRealm extends AuthorizingRealm {
    private static final Logger logger = LoggerFactory.getLogger(DBShiroAuthorizingRealm.class);

    private Role role;
    public DBShiroAuthorizingRealm(){
        this.role = null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //null usernames are invalid
        if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }

        String username = (String) getAvailablePrincipal(principals);

        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.");
        }

        String queryUserStatement = String.format(System.getProperty("query.user.statement"), username);

        List<UserAccount> userAccounts;
        try {
            userAccounts = DAOFactory.getDAOFactorybyConfig().getUseAccountDAO().query(queryUserStatement);
        }catch (Throwable e) {
            logger.error("Data Access error: ", e);
            throw new AuthenticationException(e);
        }

        if(userAccounts == null || userAccounts.size() == 0) {
            logger.warn("No account found for user " + username);
            throw new UnknownAccountException("No account found for user [" + username + "]");
        }

        Set<String> roleNames = new HashSet<>();
        roleNames.add(userAccounts.get(0).getRole().toString());

        return new SimpleAuthorizationInfo(roleNames);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.");
        }

        String queryUserStatement = String.format(System.getProperty("query.user.statement"), username);

        List<UserAccount> userAccounts;
        try {
            userAccounts = DAOFactory.getDAOFactorybyConfig().getUseAccountDAO().query(queryUserStatement);
        }catch (Throwable e) {
            logger.error("Data Access error: ", e);
            throw new AuthenticationException(e);
        }

        if(userAccounts == null || userAccounts.size() == 0) {
            logger.warn("No account found for user " + username);
            throw new UnknownAccountException("No account found for user [" + username + "]");
        }

        String dbPassword = userAccounts.get(0).getPassword();
        String salt = userAccounts.get(0).getSalt();

        return new SimpleAuthenticationInfo(
                username,
                dbPassword.toCharArray(),
                ByteSource.Util.bytes(Base64.decode(salt.getBytes())),
                getName());
    }
}
