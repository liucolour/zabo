package com.zabo.account;

/**
 * Created by zhaoboliu on 4/27/16.
 */

public class UserAccount {
    private String id;
    private String username;
    private String password;
    private Role role;
    private String permission;
    private String hash_algo;
    private String salt;
    private long created_time;

    private UserProfile profile;

    // for json auto conversion
    public UserAccount(){

    }
    public UserAccount(String username, String password, Role role, String permission, String hash_algo) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.permission = permission;
        this.hash_algo = hash_algo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public long getCreated_time() {
        return created_time;
    }

    public void setCreated_time(long created_time) {
        this.created_time = created_time;
    }
}
