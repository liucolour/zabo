package com.zabo.post;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class Post {
    private String id;
    private String user_id;
    private long created_Time;
    private long modified_Time;
    private String title;
    private String description;
    private City city;
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

    public long getCreated_Time() {
        return created_Time;
    }

    public void setCreated_Time(long created_Time) {
        this.created_Time = created_Time;
    }

    public long getModified_Time() {
        return modified_Time;
    }

    public void setModified_Time(long modified_Time) {
        this.modified_Time = modified_Time;
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

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public boolean getIsProvider() {
        return isProvider;
    }

    public void setIsProvider(boolean isProvider) {
        this.isProvider = isProvider;
    }
}
