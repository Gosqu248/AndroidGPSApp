package com.urban.mobileapp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Bus {

    private Long id;
    private String model;
    private int capacity;
    private String androidId;

    private List<Stop> stops;

}
