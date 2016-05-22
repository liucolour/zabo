package com.zabo.message;

/**
 * Created by zhaoboliu on 5/1/16.
 */
//TODO: implement message box
public class Message {
    private String sender_username;
    private long created_time;
    private String content;

    public String getSender_username() {
        return sender_username;
    }

    public void setSender_username(String sender_username) {
        this.sender_username = sender_username;
    }

    public long getCreated_time() {
        return created_time;
    }

    public void setCreated_time(long created_time) {
        this.created_time = created_time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
