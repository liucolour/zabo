package com.zabo.post;

import java.util.Date;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class JobPost extends Post {
    public enum Category {
        Restaurant,
        BabySister,
        Repair,
        Accountant,
        Engineer,
        Gardening,
        Labor
    }
    public enum Mode {
        FullTime,
        PartTime
    }
    public enum PaymentUnit {
        Once,
        Hour,
        Day,
        Month,
        Year
    }
    private Category category;
    private Duration duration;
    private float pay;
    private float min_pay;
    private float max_pay;
    private PaymentUnit unit;
    private Date start_time;
    private boolean experienced_required;
    private int num_year_experience;
}
