package com.zabo.dao;


import com.zabo.post.JobPost;
import io.vertx.core.json.Json;
import org.elasticsearch.action.index.IndexResponse;

import java.util.List;

/**
 * Created by zhaoboliu on 3/30/16.
 */
public class ElasticSearchJobDAO implements JobDAO {
    final static String index = "job_index";
    final static String type = "job_post";

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
        return null;
    }

    @Override
    public void update(String recordId, JobPost record) {

    }

    @Override
    public void delete(JobPost record) {

    }
}
