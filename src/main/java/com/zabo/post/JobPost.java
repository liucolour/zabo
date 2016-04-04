package com.zabo.post;

import java.util.Date;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public class JobPost extends Post {
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public float getPay() {
        return pay;
    }

    public void setPay(float pay) {
        this.pay = pay;
    }

    public float getMin_pay() {
        return min_pay;
    }

    public void setMin_pay(float min_pay) {
        this.min_pay = min_pay;
    }

    public float getMax_pay() {
        return max_pay;
    }

    public void setMax_pay(float max_pay) {
        this.max_pay = max_pay;
    }

    public PaymentUnit getUnit() {
        return unit;
    }

    public void setUnit(PaymentUnit unit) {
        this.unit = unit;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public boolean isExperienced_required() {
        return experienced_required;
    }

    public void setExperienced_required(boolean experienced_required) {
        this.experienced_required = experienced_required;
    }

    public int getNum_year_experience() {
        return num_year_experience;
    }

    public void setNum_year_experience(int num_year_experience) {
        this.num_year_experience = num_year_experience;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

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
        OneTime,
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

    public enum PaymentMethod {
        Cash,
        Check,
        CreditCard,
        DirectDeposit
    }
    private Category category;
    private Mode mode;
    private Duration duration;
    private float pay;
    private float min_pay;
    private float max_pay;
    private PaymentUnit unit;
    private PaymentMethod paymentMethod;
    private Date start_time;
    private boolean experienced_required;
    private int num_year_experience;
}
