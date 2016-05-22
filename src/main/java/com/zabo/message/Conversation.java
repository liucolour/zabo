package com.zabo.message;

import java.util.List;

/**
 * Created by zhaoboliu on 5/16/16.
 */
public class Conversation {
    private String id;
    private String subject;
    private List<String> usernames;
    private long created_time;
    private long modified_time;
    private List<String> deleted_usernames;
    private List<Message> messages;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public long getCreated_time() {
        return created_time;
    }

    public void setCreated_time(long created_time) {
        this.created_time = created_time;
    }

    public long getModified_time() {
        return modified_time;
    }

    public void setModified_time(long modified_time) {
        this.modified_time = modified_time;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<String> getDeleted_usernames() {
        return deleted_usernames;
    }

    public void setDeleted_usernames(List<String> deleted_usernames) {
        this.deleted_usernames = deleted_usernames;
    }
}
