package com.zabo.data;

/**
 * Created by zhaoboliu on 5/5/16.
 */
public enum ESDataType {
    //give the name exact the same as class name
    AllPost("post_index", ""),
    JobPost("post_index", "job"),
    CarPost("post_index", "car"),
    Account("account_index", "account");

    ESDataType(String index, String type){
        this.index = index;
        this.type = type;
    }
    private String index;
    private String type;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
