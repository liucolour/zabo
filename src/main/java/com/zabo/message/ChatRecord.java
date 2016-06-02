package com.zabo.message;

/**
 * Created by zhaoboliu on 5/22/16.
 */

public class ChatRecord {
    private String conversation_id;
    private long last_read_time;

    public String getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(String conversation_id) {
        this.conversation_id = conversation_id;
    }

    public long getLast_read_time() {
        return last_read_time;
    }

    public void setLast_read_time(long last_read_time) {
        this.last_read_time = last_read_time;
    }
}
