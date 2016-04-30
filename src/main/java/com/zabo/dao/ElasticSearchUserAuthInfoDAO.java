package com.zabo.dao;

import com.zabo.auth.Role;
import com.zabo.auth.UserAuthInfo;
import com.zabo.post.JobPost;
import io.vertx.core.json.Json;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhaoboliu on 4/28/16.
 */
public class ElasticSearchUserAuthInfoDAO implements UserAuthInfoDAO {

    public String index;
    public final static String type = "user_auth";

    public ElasticSearchUserAuthInfoDAO(Role role){
        if(role == Role.ADMIN)
            index = "admin_account_index";
        else
            index = "user_account_index";
    }
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
        List<UserAuthInfo> res = new LinkedList<>();
        try {
            SearchResponse response = ElasticSearchDAOFactory.getElasticSearchClient()
                    .prepareSearch(index)
                    .setTypes(type)
                    .setQuery(statement)
                    .execute()
                    .actionGet();
            response.getHits().forEach(c -> {
                UserAuthInfo user = Json.decodeValue(c.getSourceAsString(), UserAuthInfo.class);
                user.setId(c.getId());
                res.add(user);
            });
        }catch (IndexNotFoundException e){
            // ignore
        }

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
