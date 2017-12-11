package com.example.rusia.madcall;

/**
 * Created by rusia on 11/12/2017.
 */

public class MarkerData {

    private String name;
    private String description;

    public MarkerData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
