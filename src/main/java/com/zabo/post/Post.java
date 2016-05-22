package com.zabo.post;

import java.util.List;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class Post {

    private enum Status {
        Drafted,
        Posted,
        Expired,
    }

    private String id;
    private String username;
    private long created_time;
    private long modified_time;
    private String title;
    private String description;
    private boolean is_provider; // false is requester
    private Status status;
    private List<String> image_url_list;
    private Location location;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean getIs_provider() {
        return is_provider;
    }

    public void setIs_provider(boolean is_provider) {
        this.is_provider = is_provider;
    }

    public List<String> getImage_url_list() {
        return image_url_list;
    }

    public void setImage_url_list(List<String> image_url_list) {
        this.image_url_list = image_url_list;
    }
}
