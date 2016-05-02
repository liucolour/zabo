package com.zabo.dao;


import com.zabo.post.JobPost;
import com.zabo.utils.Utils;
import io.vertx.core.json.Json;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.search.sort.SortOrder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhaoboliu on 3/30/16.
 */
public class ElasticSearchJobDAO implements JobDAO {
    public final static String index = "post_index";
    public final static String type = "job";

    @Override
    public String write(JobPost record) {
        String json = Json.encodePrettily(record);
        IndexResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareIndex(index, type)
                .setSource(json).get();
        return response.getId();
    }

    @Override
    public List<String> bulkWrite(List<JobPost> list) {
        return null;
    }

    @Override
    public JobPost read(String recordId) {
        GetResponse response = ElasticSearchDAOFactory.getElasticSearchClient().prepareGet(index, type, recordId)
                .setOperationThreaded(false)
                .get();
        String json = response.getSourceAsString();
        if(json == null)
            return null;
        JobPost post = Json.decodeValue(json, JobPost.class);
        post.setId(response.getId());
        return post;
    }

    @Override
    public List<JobPost> query(String statement) {
        SearchResponse response = ElasticSearchDAOFactory.getElasticSearchClient()
                    .prepareSearch(index)
                    .setTypes(type)
                    .setQuery(statement)
                    .addSort(System.getProperty("es.post.sort.by"), SortOrder.DESC)
                    .execute()
                    .actionGet();

        List<JobPost> res = new LinkedList<>();
        response.getHits().forEach(c -> {
            JobPost post = Json.decodeValue(c.getSourceAsString(), JobPost.class);
            post.setId(c.getId());
            res.add(post);
        });

        return res;
    }

    @Override
    public void update(String recordId, JobPost record) {
        String json = Json.encodePrettily(record);

        ElasticSearchDAOFactory.getElasticSearchClient()
                .prepareUpdate(index, type, recordId)
                .setDoc(json)
                .get();
    }

    @Override
    public void delete(String recordId) {
        try {
            ElasticSearchDAOFactory.getElasticSearchClient()
                    .prepareDelete(index, type, recordId).get();
        } catch (DocumentMissingException e) {
            // ignore
        }
    }
}
