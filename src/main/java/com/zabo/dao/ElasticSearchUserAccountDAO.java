package com.zabo.dao;

import com.zabo.account.UserAccount;
import io.vertx.core.json.Json;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhaoboliu on 4/28/16.
 */
public class ElasticSearchUserAccountDAO implements UserAccountDAO {

    public String index = "account_auth_index";
    public final static String type = "user_auth";

    @Override
    public String write(UserAccount record) {
        String json = Json.encodePrettily(record);
        IndexResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareIndex(index, type)
                .setSource(json).get();
        return response.getId();
    }

    @Override
    public List<String> bulkWrite(List<UserAccount> list) {
        return null;
    }

    @Override
    public UserAccount read(String recordId) {
        GetResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareGet(index, type, recordId)
                .setOperationThreaded(false)
                .get();
        UserAccount user = Json.decodeValue(response.getSourceAsString(), UserAccount.class);
        user.setId(response.getId());
        return user;
    }

    @Override
    public List<UserAccount> query(String statement) {
        List<UserAccount> res = new LinkedList<>();
        try {
            SearchResponse response = ElasticSearchDAOFactory.getElasticSearchClient()
                    .prepareSearch(index)
                    .setTypes(type)
                    .setQuery(statement)
                    .execute()
                    .actionGet();
            response.getHits().forEach(c -> {
                UserAccount user = Json.decodeValue(c.getSourceAsString(), UserAccount.class);
                user.setId(c.getId());
                res.add(user);
            });
        }catch (IndexNotFoundException e){
            // ignore
        }

        return res;
    }

    @Override
    public void update(String recordId, UserAccount record) {
        String json = Json.encodePrettily(record);
        ElasticSearchDAOFactory.getElasticSearchClient()
                .prepareUpdate(index, type, recordId)
                .setDoc(json)
                .get();
    }

    @Override
    public void delete(String recordId) {
        ElasticSearchDAOFactory.getElasticSearchClient()
                .prepareDelete(index, type, recordId).get();
    }
}
