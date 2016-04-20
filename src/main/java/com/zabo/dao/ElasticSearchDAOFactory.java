package com.zabo.dao;

import com.zabo.post.JobPost;
import com.zabo.utils.Utils;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchDAOFactory.class.getName());

    private static Client client = null;

    public ElasticSearchDAOFactory() {
        init();
    }

    private static void init() {
        if(client != null)
            return;
        String server = System.getProperty("es.host");
        String clusterName = System.getProperty("es.cluster.name");
        Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        try {
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(server), 9300));
            logger.info("ElasticSearch Transport client initialized, server={} cluster name={}", server, clusterName);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            client.close();
        }
    }
    public static Client getElasticSearchClient() {
        if (client == null)
            init();
        return client;
    }


    @Override
    public JobDAO getJobDAO() {
        return new ElasticSearchJobDAO();
    }

    @Override
    public DAO getDAO(Class clazz) {
        if(clazz.equals(JobPost.class))
            return getJobDAO();
        return null;
    }
}
