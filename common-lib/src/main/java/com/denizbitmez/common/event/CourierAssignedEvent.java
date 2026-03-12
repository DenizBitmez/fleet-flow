package com.denizbitmez.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierAssignedEvent {
    private String orderId;
    private String courierId;
    private String customerId;
    private long timestamp;
}
