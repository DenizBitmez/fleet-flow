package com.denizbitmez.matchingservice.service;

import com.denizbitmez.common.dto.OrderRequestDTO;
import com.denizbitmez.common.event.CourierAssignedEvent;
import com.denizbitmez.common.event.OrderStatusUpdatedEvent;
import com.denizbitmez.matchingservice.config.RabbitMQConfig;
import com.denizbitmez.matchingservice.entity.Order;
import com.denizbitmez.matchingservice.repository.OrderRepository;
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
    private final OrderRepository orderRepository; // Fixed order

    private static final String REDIS_KEY = "courier:locations";
    private static final double SEARCH_RADIUS_KM = 5.0;

    @Transactional
    public Order createOrder(OrderRequestDTO orderRequest) {
        Order order = Order.builder()
                .customerId(orderRequest.getCustomerId())
                .pickupLatitude(orderRequest.getLatitude())
                .pickupLongitude(orderRequest.getLongitude())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        // Find nearest courier
        String courierId = findNearestCourier(orderRequest.getLongitude(), orderRequest.getLatitude());

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
    private String findNearestCourier(double lon, double lat) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                REDIS_KEY,
                new Circle(new Point(lon, lat), new Distance(SEARCH_RADIUS_KM, RedisGeoCommands.DistanceUnit.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending().limit(1)
        );

        if (results != null && !results.getContent().isEmpty()) {
            return results.getContent().get(0).getContent().getName();
        }
        return null;
    }

    public String courierSearchFallback(double lon, double lat, Throwable t) {
        log.error("Circuit breaker for courier search opened! Fallback triggered. Error: {}", t.getMessage());
        // Fallback strategy: return null or a default courier if desired
        return null;
    }
}
