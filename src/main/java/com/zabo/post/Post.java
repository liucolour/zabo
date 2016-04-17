package com.zabo.post;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class Post {
    private String id;
    private String user_id;
    private long created_time;
    private long modified_time;
    private String title;
    private String description;
    private Location location;
    private boolean isProvider; // false is requester

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
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

    public boolean getIsProvider() {
        return isProvider;
    }

    public void setIsProvider(boolean isProvider) {
        this.isProvider = isProvider;
    }
}
