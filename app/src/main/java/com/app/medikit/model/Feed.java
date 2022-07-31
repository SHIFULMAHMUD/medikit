package com.app.medikit.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Feed implements Serializable {

    @SerializedName("created_at")
    @Expose
    private String created_at;
    @SerializedName("entry_id")
    @Expose
    private int entry_id;
    @SerializedName("field1")
    @Expose
    private String field1;
    @SerializedName("field2")
    @Expose
    private String field2;
    @SerializedName("field3")
    @Expose
    private String field3;

    public Feed() {}

    public Feed(double pulse, double oxygen, double temperature) {
        this.field1 = String.valueOf(pulse);
        this.field2 = String.valueOf(oxygen);
        this.field3 = String.valueOf(temperature);
    }

    public String getCreated_at() {
        return created_at;
    }

    public int getEntry_id() {
        return entry_id;
    }

    public void setPulse(double pulse) {
        this.field1 = String.valueOf(pulse);
    }

    public String getPulse() {
        return field1;
    }

    public void setOxygen(double oxygen) {
        this.field2 = String.valueOf(oxygen);
    }

    public String getOxygen() {
        return field2;
    }

    public void setTemperature(double temperature) {
        this.field3 = String.valueOf(temperature);
    }

    public String getTemperature() {
        return field3;
    }
}
