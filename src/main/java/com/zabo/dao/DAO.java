package com.zabo.dao;

import java.util.List;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public interface DAO<E> {
    String write(E record);
    List<String> bulkWrite(List<E> list);
    E read(String recordId);
    void update(String recordId, E record);
    void delete(E record);
}
