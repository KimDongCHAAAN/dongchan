package com.Yproject.dailyw.util;

import java.util.Date;

// 로컬에 있는 체중 데이터를 쉽게 사용하기 위해 만들어둔 구조(로컬에서 체중 데이터를 가져온 후 이러한 형식으로 만들어서 사용)
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
