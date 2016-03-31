package com.zabo.post;

import java.util.Date;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class RentalPost extends Post {
    public enum RoomType {
        Unit,
        Condo,
        Apartment,
        TownHouse,
        House
    }

    public enum Gender {
        Male,
        Female,
        Both
    }
    private Integer monthly_price;
    private Date start_time;
    private Date end_time;
    private Duration duration;
    private RoomType type;
    private int numBed;
    private int sqft;
    private boolean isFurnished;
    private boolean isPetOK;
    private boolean isLaundry;
    private boolean isSingle;
    private Gender gender;
    private String address;
}
