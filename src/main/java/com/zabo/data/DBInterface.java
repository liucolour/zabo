package com.zabo.data;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by zhaoboliu on 5/7/16.
 */
public interface DBInterface {
    JsonObject write(JsonObject jsonObject);
    List<String> bulkWrite(List<JsonObject> list);
    JsonArray bulkRead(JsonObject jsonObject);
    JsonObject read(JsonObject jsonObject);
    void update(JsonObject jsonObject);
    void delete(JsonObject jsonObject);
    JsonArray query(JsonObject jsonObject);
}