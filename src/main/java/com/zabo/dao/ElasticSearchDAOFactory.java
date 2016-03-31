package com.zabo.dao;

import com.zabo.utils.Utils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public class ElasticSearchDAOFactory extends DAOFactory {
    private Client client = null;

    public ElasticSearchDAOFactory() {
        String server = Utils.getProperty("es.host");
        String clusterName = Utils.getProperty("es.cluster.name");
        Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        try {
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(server), 9300));
            // TODO: replace with log4j
            System.out.println("ElasticSearch Transport client initialized");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            client.close();
        }
    }
    public Client getElasticSearchClient() {
        return client;
    }

    @Override
    public CarDAO getCarDAO() {
        return new ElasticSearchCarDAO();
    }

    @Override
    public JobDAO getJobDAO() {
        return null;
    }

    @Override
    public RentalDAO getRentalDAO() {
        return null;
    }

    @Override
    public TradeDAO getTradeDAO() {
        return null;
    }
}
