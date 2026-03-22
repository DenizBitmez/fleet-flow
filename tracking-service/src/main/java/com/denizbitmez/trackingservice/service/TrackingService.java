package com.denizbitmez.trackingservice.service;

import com.denizbitmez.common.dto.LocationUpdateDTO;
import com.denizbitmez.common.event.CourierAssignedEvent;
import com.denizbitmez.common.event.OrderStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @RabbitListener(queues = "courier.location.queue")
    public void handleLocationUpdate(LocationUpdateDTO locationUpdate) {
        // Calculate ETA if there's an active assignment
        String assignment = redisTemplate.opsForValue().get("assignment:courier:" + locationUpdate.getCourierId());
        if (assignment != null) {
            try {
                String[] coords = assignment.split(",");
                double destLat = Double.parseDouble(coords[0]);
                double destLon = Double.parseDouble(coords[1]);

                double distanceKm = calculateDistance(
                        locationUpdate.getLatitude(), locationUpdate.getLongitude(),
                        destLat, destLon
                );

                // Dynamic speed based on Weather
                double speedKmH = 20.0;
                String weather = "CLEAR";
                if (coords.length > 2) {
                    weather = coords[2];
                    if ("RAIN".equals(weather)) speedKmH = 15.0;
                    else if ("SNOW".equals(weather)) speedKmH = 10.0;
                    else if ("STORM".equals(weather)) speedKmH = 5.0;
                }
                
                double etaMinutes = (distanceKm / speedKmH) * 60.0;
                
                locationUpdate.setEta(Math.round(etaMinutes * 10.0) / 10.0); // Round to 1 decimal
                locationUpdate.setWeather(weather);
            } catch (Exception e) {
                log.error("Error calculating ETA: {}", e.getMessage());
            }
        }

        messagingTemplate.convertAndSend("/topic/locations", locationUpdate);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @RabbitListener(queues = "courier.assigned.queue")
    public void handleCourierAssigned(CourierAssignedEvent event) {
        messagingTemplate.convertAndSend("/topic/assignments", event);
        log.info("Assignment event pushed to WS: {}", event.getOrderId());
        
        // Push initial ETA immediately without waiting for the next GPS update
        try {
            String assignment = redisTemplate.opsForValue().get("assignment:courier:" + event.getCourierId());
            if (assignment != null) {
                // Get current location from Redis Geo (returns Point with X=lon, Y=lat)
                java.util.List<org.springframework.data.geo.Point> positions = 
                        redisTemplate.opsForGeo().position("courier:locations", event.getCourierId());
                
                if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
                    org.springframework.data.geo.Point courierLocation = positions.get(0);
                    
                    String[] coords = assignment.split(",");
                    double destLat = Double.parseDouble(coords[0]);
                    double destLon = Double.parseDouble(coords[1]);

                    String weather = "CLEAR";
                    double speedKmH = 20.0;
                    if (coords.length > 2) {
                        weather = coords[2];
                        if ("RAIN".equals(weather)) speedKmH = 15.0;
                        else if ("SNOW".equals(weather)) speedKmH = 10.0;
                        else if ("STORM".equals(weather)) speedKmH = 5.0;
                    }

                    double distanceKm = calculateDistance(
                            courierLocation.getY(), courierLocation.getX(),
                            destLat, destLon
                    );

                    double etaMinutes = (distanceKm / speedKmH) * 60.0;
                    
                    LocationUpdateDTO initialUpdate = LocationUpdateDTO.builder()
                            .courierId(event.getCourierId())
                            .latitude(courierLocation.getY())
                            .longitude(courierLocation.getX())
                            .timestamp(System.currentTimeMillis())
                            .eta(Math.round(etaMinutes * 10.0) / 10.0)
                            .weather(weather)
                            .build();
                            
                    messagingTemplate.convertAndSend("/topic/locations", initialUpdate);
                }
            }
        } catch (Exception e) {
            log.error("Failed to push initial ETA: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "order.status.queue")
    public void handleOrderStatusUpdate(OrderStatusUpdatedEvent event) {
        messagingTemplate.convertAndSend("/topic/order-status", event);
        log.info("Order status update pushed to WS: {} -> {}", event.getOrderId(), event.getStatus());
    }
}
