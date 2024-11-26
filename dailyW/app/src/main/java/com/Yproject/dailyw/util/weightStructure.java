package com.Yproject.dailyw.util;

import java.util.Date;

public class weightStructure {
    private float weight;
    private Date date;
    private String dateStr;

    public weightStructure(float weight, Date date, String dateStr) {
        this.weight = weight;
        this.date = date;
        this.dateStr = dateStr;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }
}
