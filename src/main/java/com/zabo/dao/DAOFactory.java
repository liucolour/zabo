package com.zabo.dao;

import com.zabo.utils.Utils;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public abstract class DAOFactory {
    public enum DBType {
        ElasticSearch
    }

    public abstract CarDAO getCarDAO();
    public abstract JobDAO getJobDAO();
    public abstract RentalDAO getRentalDAO();
    public abstract TradeDAO getTradeDAO();


    public static DAOFactory getDAOFactorybyConfig() {
        String configDBType = Utils.getProperty("database.default");
        return getDAOFactory(DBType.valueOf(configDBType));
    }

    public static DAOFactory getDAOFactory(DBType type) {
        switch (type) {
            case ElasticSearch: return new ElasticSearchDAOFactory();
            default: return null;
        }
    }
}
