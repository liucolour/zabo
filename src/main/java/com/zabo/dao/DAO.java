package com.zabo.dao;

import java.util.List;

/**
 * Created by zhaoboliu on 3/29/16.
 */
public interface DAO<E> {
    void write(E record);
    void bulkWrite(List<E> list);
    E read(String recordId);
    void update(String recordId, E record);
    void delete(E record);
}
