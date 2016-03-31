package com.zabo.post;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class Duration {
    public enum Unit {
        Day,
        Month,
        Year
    }
    private int number;  // -1 means no require
    private Unit unit;
}
