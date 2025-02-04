package com.urban.mobileapp.model;

import com.urban.mobileapp.db.entity.StopDB;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stop {
    private int id;
    private String line;
    private String name;
    private double latitude;
    private double longitude;
    private float bearing;

    public StopDB toStopDB() {
        return new StopDB(
                this.line,
                this.name,
                this.latitude,
                this.longitude,
                this.bearing
        );
    }
}
