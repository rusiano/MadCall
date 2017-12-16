package com.example.rusia.madcall;

/**
 * Created by rusia on 11/12/2017.
 */

public class MarkerData {

    private String name;
    private String label;


    public MarkerData(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
