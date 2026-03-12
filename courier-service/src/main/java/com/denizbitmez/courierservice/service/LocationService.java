package com.denizbitmez.courierservice.service;

import com.denizbitmez.common.dto.LocationUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private static final String REDIS_KEY = "courier:locations";
    private static final String EXCHANGE = "fleet.exchange";
    private static final String ROUTING_KEY = "courier.location";

    public void updateLocation(LocationUpdateDTO locationUpdateDTO) {
        // Save to Redis
        redisTemplate.opsForGeo().add(
                REDIS_KEY,
                new Point(locationUpdateDTO.getLongitude(), locationUpdateDTO.getLatitude()),
                locationUpdateDTO.getCourierId()
        );

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, locationUpdateDTO);
    }
}
