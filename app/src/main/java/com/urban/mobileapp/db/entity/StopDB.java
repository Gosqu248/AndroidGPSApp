package com.urban.mobileapp.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;


@Entity(tableName = "stops")
public class StopDB {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private String line;
    private String name;
    private double lat;
    private double lon;
    private float bearing;

    public StopDB(String line, String name, double lat, double lon, float bearing) {
        this.line = line;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.bearing = bearing;
    }

    // Gettery
    public Long getId() { return id; }
    public String getLine() { return line; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public float getBearing() { return bearing; }

    // Settery
    public void setId(Long id) { this.id = id; }
    public void setLine(String line) { this.line = line; }
    public void setName(String name) { this.name = name; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLon(double lon) { this.lon = lon; }
    public void setBearing(float bearing) { this.bearing = bearing; }

}