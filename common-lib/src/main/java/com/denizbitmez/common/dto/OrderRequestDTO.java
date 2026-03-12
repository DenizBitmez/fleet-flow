package com.denizbitmez.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private String customerId;
    private double latitude;
    private double longitude;
    private String pickupAddress;
    private String deliveryAddress;
}
