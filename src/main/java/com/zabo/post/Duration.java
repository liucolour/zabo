package com.zabo.post;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class Duration {
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public enum Unit {
        Minute,
        Hour,
        Day,
        Month,
        Year
    }
    private int number;  // -1 means no require
    private Unit unit;
}
