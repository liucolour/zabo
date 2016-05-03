package com.zabo.account;

/**
 * Created by zhaoboliu on 4/27/16.
 */
public class UserProfile {
    private String email;
    private String phone;
    private String wechat;
    private String zipcode;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWeChat() {
        return wechat;
    }

    public void setWeChat(String weChat) {
        this.wechat = weChat;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }
}
