package com.denizbitmez.matchingservice.service;

import com.denizbitmez.common.dto.OrderRequestDTO;
import com.denizbitmez.common.event.CourierAssignedEvent;
import com.denizbitmez.common.event.OrderStatusUpdatedEvent;
import com.denizbitmez.matchingservice.config.RabbitMQConfig;
import com.denizbitmez.matchingservice.entity.Order;
import com.denizbitmez.matchingservice.repository.OrderRepository;
import com.denizbitmez.matchingservice.client.WeatherClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository; 
    private final WeatherClient weatherClient;

    private static final String REDIS_KEY = "courier:locations";
    private static final double SEARCH_RADIUS_KM = 5.0;

    @Transactional
    public Order createOrder(OrderRequestDTO orderRequest) {
        Order order = Order.builder()
                .customerId(orderRequest.getCustomerId())
                .pickupLatitude(orderRequest.getLatitude())
                .pickupLongitude(orderRequest.getLongitude())
                .deliveryLatitude(orderRequest.getDeliveryLatitude())
                .deliveryLongitude(orderRequest.getDeliveryLongitude())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        WeatherClient.WeatherState weather = weatherClient.getCurrentWeather(orderRequest.getLatitude(), orderRequest.getLongitude());
        log.info("Current weather at pickup: {}", weather);

        // Find nearest courier or best pooled courier
        String courierId = findNearestCourier(orderRequest.getLongitude(), orderRequest.getLatitude(), orderRequest.getDeliveryLatitude(), orderRequest.getDeliveryLongitude(), weather);

        if (courierId != null) {
            order.setCourierId(courierId);
            order.setStatus("MATCHED");
            order.setMatchedAt(LocalDateTime.now());
            order = orderRepository.save(order);

            // Publish event
            CourierAssignedEvent event = CourierAssignedEvent.builder()
                    .orderId(order.getId().toString())
                    .courierId(courierId)
                    .customerId(order.getCustomerId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Store active assignment in Redis for TrackingService to calculate ETA
            // Key: assignment:courier:{courierId}, Value: destLat,destLon,weather
            String destValue = order.getDeliveryLatitude() + "," + order.getDeliveryLongitude() + "," + weather.name();
            redisTemplate.opsForValue().set("assignment:courier:" + courierId, destValue);

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Order {} matched with courier {}", order.getId(), courierId);
        } else {
            log.info("No courier found for order {}", order.getId());
        }

        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        order.setStatus(status);
        order = orderRepository.save(order);

        // Publish status update event
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                .orderId(orderId.toString())
                .status(status)
                .timestamp(System.currentTimeMillis())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.status", event);
        log.info("Order {} status updated to {}", orderId, status);
        
        return order;
    }

    public List<Order> getRecentOrders() {
        return orderRepository.findTop10ByOrderByIdDesc();
    }

    @CircuitBreaker(name = "courierSearch", fallbackMethod = "courierSearchFallback")
    private String findNearestCourier(double pickupLon, double pickupLat, double deliveryLat, double deliveryLon, WeatherClient.WeatherState weather) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                REDIS_KEY,
                new Circle(new Point(pickupLon, pickupLat), new Distance(SEARCH_RADIUS_KM, RedisGeoCommands.DistanceUnit.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates()
        );

        if (results == null || results.getContent().isEmpty()) {
            return null;
        }

        String bestCourierId = null;
        double bestScore = Double.MAX_VALUE;
        
        int maxCapacity = (weather == WeatherClient.WeatherState.SNOW || weather == WeatherClient.WeatherState.STORM) ? 1 : 3;
        if (weather == WeatherClient.WeatherState.RAIN) maxCapacity = 2;

        for (var result : results.getContent()) {
            String courierId = result.getContent().getName();
            double distanceToPickupKm = result.getDistance().getValue();

            // Fetch active orders for this courier
            List<Order> activeOrders = orderRepository.findByCourierIdAndStatusIn(
                    courierId, List.of("MATCHED", "PREPARING", "ON_THE_WAY"));

            if (activeOrders.size() >= maxCapacity) {
                continue; // Capacity full constraint based on weather
            }

            double score = distanceToPickupKm; // Base penalty

            // Pooling Logic (Heuristic Override)
            if (!activeOrders.isEmpty()) {
                double minDeliveryDistance = Double.MAX_VALUE;
                for (Order active : activeOrders) {
                    double distToNewDelivery = calculateDistance(
                            active.getDeliveryLatitude(), active.getDeliveryLongitude(),
                            deliveryLat, deliveryLon);
                    if (distToNewDelivery < minDeliveryDistance) {
                        minDeliveryDistance = distToNewDelivery;
                    }
                }

                if (minDeliveryDistance <= 2.0) { // Same neighborhood!
                    log.info("Pooling target! Courier {} is {}km from new delivery.", courierId, minDeliveryDistance);
                    score -= 10.0; // Massive bonus (negative penalty)
                } else {
                    score += 5.0; // Penalty for picking up a detouring order
                }
            }

            if (score < bestScore) {
                bestScore = score;
                bestCourierId = courierId;
            }
        }

        return bestCourierId;
    }

    public String courierSearchFallback(double pickupLon, double pickupLat, double deliveryLat, double deliveryLon, WeatherClient.WeatherState weather, Throwable t) {
        log.error("Circuit breaker for courier search opened! Fallback triggered. Error: {}", t.getMessage());
        return null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
