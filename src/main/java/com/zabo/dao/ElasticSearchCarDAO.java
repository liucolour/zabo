package com.zabo.dao;

import com.zabo.post.CarPost;

import java.util.List;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public class ElasticSearchCarDAO implements CarDAO{
    @Override
    public void write(CarPost record) {

    }

    @Override
    public void bulkWrite(List<CarPost> list) {

    }

    @Override
    public CarPost read(String recordId) {
        return null;
    }

    @Override
    public void update(String recordId, CarPost record) {

    }

    @Override
    public void delete(CarPost record) {

    }
}
