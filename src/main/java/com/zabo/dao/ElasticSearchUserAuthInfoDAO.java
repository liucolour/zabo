package com.zabo.dao;

import com.zabo.auth.UserAuthInfo;
import com.zabo.post.JobPost;
import io.vertx.core.json.Json;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.sort.SortOrder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhaoboliu on 4/28/16.
 */
public class ElasticSearchUserAuthInfoDAO implements UserAuthInfoDAO {
    public final static String index = "account_index";
    public final static String type = "user_auth";

    @Override
    public String write(UserAuthInfo record) {
        String json = Json.encodePrettily(record);
        IndexResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareIndex(index, type)
                .setSource(json).get();
        return response.getId();
    }

    @Override
    public List<String> bulkWrite(List<UserAuthInfo> list) {
        return null;
    }

    @Override
    public UserAuthInfo read(String recordId) {
        GetResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareGet(index, type, recordId)
                .setOperationThreaded(false)
                .get();
        UserAuthInfo user = Json.decodeValue(response.getSourceAsString(), UserAuthInfo.class);
        user.setId(response.getId());
        return user;
    }

    @Override
    public List<UserAuthInfo> query(String statement) {
        SearchResponse response = ElasticSearchDAOFactory.getElasticSearchClient()
                .prepareSearch(index)
                .setTypes(type)
                .setQuery(statement)
                .execute()
                .actionGet();

        List<UserAuthInfo> res = new LinkedList<>();
        response.getHits().forEach(c -> {
            UserAuthInfo user = Json.decodeValue(c.getSourceAsString(), UserAuthInfo.class);
            user.setId(c.getId());
            res.add(user);
        });

        return res;
    }

    @Override
    public void update(String recordId, UserAuthInfo record) {
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