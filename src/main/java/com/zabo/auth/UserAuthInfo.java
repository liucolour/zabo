package com.zabo.auth;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;

/**
 * Created by zhaoboliu on 4/27/16.
 */

public class UserAuthInfo {
    private String id;
    private String user_id;
    private String password;
    private Role role;
    private String permission;
    private String hash_algo;
    private String salt;

    // for json auto conversion
    public UserAuthInfo(){

    }
    public UserAuthInfo(String user_id, String password, Role role, String permission, String hash_algo) {
        this.user_id = user_id;
        this.password = password;
        this.role = role;
        this.permission = permission;
        this.hash_algo = hash_algo;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getHash_algo() {
        return hash_algo;
    }

    public void setHash_algo(String hash_algo) {
        this.hash_algo = hash_algo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
