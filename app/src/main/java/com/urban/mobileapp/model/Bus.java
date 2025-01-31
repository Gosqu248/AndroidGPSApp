package com.urban.mobileapp.model;

public class Bus {

    private Long id;
    private String line;
    private String model;
    private int capacity;

    public Bus(Long id, String line, String model, int capacity) {
        this.id = id;
        this.line = line;
        this.model = model;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public String getLineNumber() { return line; }
    public String getModel() { return model; }
    public int getCapacity() { return capacity; }
}
