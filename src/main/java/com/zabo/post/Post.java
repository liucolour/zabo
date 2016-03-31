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
    private String message;
    private City city;
    private boolean isProvider; // false is requester
}
