package com.denizbitmez.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDTO {
    private String courierId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private Double eta; // in minutes
}
