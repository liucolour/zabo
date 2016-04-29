package com.zabo.dao;

import com.zabo.auth.UserAuthInfo;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by zhaoboliu on 3/29/16.
 */
// TODO: refactor based on vertx's LoggerFactory
public abstract class DAOFactory {
    private static final Logger logger = LoggerFactory.getLogger(DAOFactory.class.getName());
    private static volatile ElasticSearchDAOFactory esDAOFactory = new ElasticSearchDAOFactory();;

    public enum DBType {
        ElasticSearch
    }

    public abstract JobDAO getJobDAO();

    public abstract DAO getDAO(Class clazz);

    public abstract UserAuthInfoDAO getUserAuthInfoDAO();

    public static DAOFactory getDAOFactorybyConfig() {
        String configDBType = System.getProperty("database.default");
        return getDAOFactory(DBType.valueOf(configDBType));
    }

    public static DAOFactory getDAOFactory(DBType type) {
        switch (type) {
            case ElasticSearch: return esDAOFactory;
            default:
        }
        logger.error("Couldn't find DB type {}", type);
        return null;
    }
}
