package com.zabo.dao;

import java.util.List;

/**
 * Created by zhaoboliu on 3/29/16.
 */
//TODO: async APIs
public interface DAO<E> {
    String write(E record);
    List<String> bulkWrite(List<E> list);
    E read(String recordId);
    List<E> query(String statement);
    void update(String recordId, E record);
    void delete(String recordId);
}
