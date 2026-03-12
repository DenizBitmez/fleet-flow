package com.denizbitmez.matchingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String customerId;
    private String courierId;
    private String status; // CREATED, MATCHED, DELIVERED
    
    private double pickupLatitude;
    private double pickupLongitude;
    
    private LocalDateTime createdAt;
    private LocalDateTime matchedAt;
}
