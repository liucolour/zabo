package com.zabo.data;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.zabo.utils.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoboliu on 3/30/16.
 */
public class ElasticSearchInterfaceImpl implements DBInterface {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInterfaceImpl.class.getName());

    private static Client client = null;

    static {
        String server = System.getProperty("es.host");
        String clusterName = System.getProperty("es.cluster.name");
        Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        try {
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(server), 9300));
            logger.info("ElasticSearch Transport client initialized, server={} cluster name={}", server, clusterName);

            // Create index and mapping
            for (ESDataType type : ESDataType.values()) {
                if (!type.getType().equals("")) {
                    String file_name = type.getIndex() + "_" + type.getType() + "_" + "mapping.json";
                    try (InputStream input = ElasticSearchInterfaceImpl.class
                            .getClassLoader()
                            .getResourceAsStream(file_name)) {
                        if (input == null)
                            continue;
                        String mapping = CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
                        try {
                            client.admin().indices()
                                    .prepareCreate(type.getIndex())
                                    .addMapping(type.getType(), mapping)
                                    .execute()
                                    .actionGet();
                            logger.info("ElasticSearch created index={} type={} with mapping at file {}",
                                    type.getIndex(),
                                    type.getType(),
                                    file_name);
                        } catch (IndexAlreadyExistsException e) {
                            logger.info("ElasticSearch already had index={} type={} with mapping at file {}",
                                    type.getIndex(),
                                    type.getType(),
                                    file_name);
                        }
                    } catch (IOException ex) {
                        logger.error("Failed to read file " + file_name, ex);
                    }
                }
            }
        } catch (UnknownHostException e) {
            logger.error("Initializing elastic search client failed ", e);
            client.close();
        }
    }

    public JsonObject write(JsonObject jsonObject) {
        if (client == null) {
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

    //TODO: implement bulkWrite and bulkDelete
    public List<String> bulkWrite(List<JsonObject> list) {
        return null;
    }

    public List<String> bulkDelete(List<JsonObject> list) {
        return null;
    }

    public JsonArray bulkRead(JsonObject jsonObject) {
        if (client == null) {
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        JsonArray ids_json = jsonObject.getJsonArray("ids");
        List<String> ids_list = ids_json.getList();
        MultiGetResponse multiGetItemResponses = client.prepareMultiGet()
                .add(index, type, ids_list.toArray(new String[ids_list.size()]))
                .get();

        JsonArray result = new JsonArray();
        for (MultiGetItemResponse itemResponse : multiGetItemResponses) {
            GetResponse response = itemResponse.getResponse();
            if (response.isExists()) {
                JsonObject one = new JsonObject((response.getSourceAsString()));
                one.put("id", response.getId());
                result.add(one);
            }
        }

        return result;
    }

    public JsonObject read(JsonObject jsonObject) {
        if (client == null) {
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if (Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");

        GetResponse response = client.prepareGet(index, type, id)
                .setOperationThreaded(false)
                .get();
        String json = response.getSourceAsString();
        if (json == null)
            return null;
        JsonObject jsonRes = new JsonObject(json);
        jsonRes.put("id", response.getId());
        return jsonRes;
    }

    public void update(JsonObject jsonObject) {
        if (client == null) {
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if (Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");

        String script = jsonObject.getString("script");
        JsonObject params = jsonObject.getJsonObject("params");
        Map<String, Object> map = null;
        if (params != null)
            map = params.getMap();

        UpdateRequestBuilder request = client.prepareUpdate(index, type, id);
        if (!Utils.ifStringEmpty(script))
            request.setScript(new Script(script, ScriptService.ScriptType.INLINE, null, map));
        else
            request.setDoc(jsonObject.encode());
        request.setRetryOnConflict(3).get();
    }

    public void delete(JsonObject jsonObject) {
        if (client == null) {
            throw new RuntimeException("ElasticSearch client not found");
        }
        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        String id = jsonObject.getString("id");

        if (Utils.ifStringEmpty(id))
            throw new RuntimeException("Invalid input id");

        try {
            client.prepareDelete(index, type, id).get();
        } catch (DocumentMissingException e) {
            // ignore
        }
    }

    public JsonArray query(JsonObject jsonObject) {
        if (client == null) {
            throw new RuntimeException("ElasticSearch client not found");
        }

        String index = ESDataType.valueOf(jsonObject.getString("ESDataType")).getIndex();
        String type = ESDataType.valueOf(jsonObject.getString("ESDataType")).getType();
        jsonObject.remove("ESDataType");

        JsonObject query = jsonObject.getJsonObject("query");
        String field = jsonObject.getString("sort");
        String from_str = jsonObject.getString("from");
        String size_str = jsonObject.getString("size");

        if (Utils.ifStringEmpty(field)) {
            field = "created_time";
        }
        String statement = query.encode();
        if (Utils.ifStringEmpty(statement))
            throw new RuntimeException("Invalid input statement");

        SearchRequestBuilder queryBuilder = client.prepareSearch(index);

        if (!Utils.ifStringEmpty(type))
            queryBuilder.setTypes(type);

        queryBuilder.setQuery(statement);

        //TODO: refactor to add sort order from input
        if (!Utils.ifStringEmpty(field))
            queryBuilder.addSort(field, SortOrder.DESC);

        if (!Utils.ifStringEmpty(from_str)) {
            queryBuilder.setFrom(Integer.parseInt(from_str));
        }

        if (!Utils.ifStringEmpty(size_str)) {
            queryBuilder.setSize(Integer.parseInt(size_str));
        }

        //TODO: add aggregation support
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
