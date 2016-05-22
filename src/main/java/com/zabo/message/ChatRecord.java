package com.zabo.message;

/**
 * Created by zhaoboliu on 5/22/16.
 */
public class ChatRecord {
    private String conversation_id;
    private boolean has_new;
    private int amount_read;

    public String getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(String conversation_id) {
        this.conversation_id = conversation_id;
    }

    public boolean getHas_new() {
        return has_new;
    }

    public void setHas_new(boolean has_new) {
        this.has_new = has_new;
    }

    public int getAmount_read() {
        return amount_read;
    }

    public void setAmount_read(int amount_read) {
        this.amount_read = amount_read;
    }
}
