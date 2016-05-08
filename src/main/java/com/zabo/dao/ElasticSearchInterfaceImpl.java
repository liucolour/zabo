package com.zabo.dao;

import com.zabo.utils.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.search.sort.SortOrder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by zhaoboliu on 3/30/16.
 */
//TODO: refactor to only have postDAO for all post' types
public class ElasticSearchInterfaceImpl implements DBInterface{
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInterfaceImpl.class.getName());

    private static Client client = null;

    static{
        //TODO: add mapping
        String server = System.getProperty("es.host");
        String clusterName = System.getProperty("es.cluster.name");
        Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        try {
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(server), 9300));
            logger.info("ElasticSearch Transport client initialized, server={} cluster name={}", server, clusterName);
        } catch (UnknownHostException e) {
            logger.error("Initializing elastic search client failed ", e);
            client.close();
        }
    }

    public JsonObject  write(JsonObject jsonObject) {
        if(client == null){
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        IndexResponse response = client.prepareIndex(index, type)
                .setSource(jsonObject.encode()).get();

        JsonObject jsonRes = new JsonObject();
        jsonRes.put("id", response.getId());
        return jsonRes;
    }

    public List<String> bulkWrite(List<JsonObject> list) {
        return null;
    }

    public JsonObject read(JsonObject jsonObject) {
        if(client == null){
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if(Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");

        GetResponse response = client.prepareGet(index, type, id)
                .setOperationThreaded(false)
                .get();
        String json = response.getSourceAsString();
        if(json == null)
            return null;
        JsonObject jsonRes = new JsonObject(json);
        jsonRes.put("id", response.getId());
        return jsonRes;
    }

    public void update(JsonObject jsonObject) {
        if(client == null){
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if(Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");

        client.prepareUpdate(index, type, id)
                .setDoc(jsonObject.encode())
                .get();
    }

    public void delete(JsonObject jsonObject) {
        if(client == null){
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if(Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");


        try {
            client.prepareDelete(index, type, id).get();
        } catch (DocumentMissingException e) {
            // ignore
        }

    }

    public JsonArray query(JsonObject jsonObject) {
        if(client == null){
            throw new RuntimeException("ElasticSearch client not found");
        }

        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        JsonObject query = jsonObject.getJsonObject("query");
        String field = jsonObject.getString("sort");

        if(Utils.ifStringEmpty(field)){
            field = "created_time";
        }
        String statement = query.encode();
        if(Utils.ifStringEmpty(statement))
            throw new RuntimeException("Invalid input statement");

        SearchRequestBuilder queryBuilder = client.prepareSearch(index);

        if(!Utils.ifStringEmpty(type))
            queryBuilder.setTypes(type);

        queryBuilder.setQuery(statement);
        //TODO: refactor to add sort order from input
        if(!Utils.ifStringEmpty(field))
            queryBuilder.addSort(field, SortOrder.DESC);

        //TODO: add aggreation support
        SearchResponse response = queryBuilder.execute().actionGet();

        JsonArray list = new JsonArray();
        response.getHits().forEach(c -> {
            JsonObject one = new JsonObject((c.getSourceAsString()));
            one.put("id", c.getId());
            list.add(one);
        });

        return list;
    }
}
