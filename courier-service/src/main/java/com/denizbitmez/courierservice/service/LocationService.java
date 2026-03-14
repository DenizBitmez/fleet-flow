package com.denizbitmez.courierservice.service;

import com.denizbitmez.common.dto.LocationUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private static final String REDIS_KEY = "courier:locations";
    private static final String EXCHANGE = "fleet.exchange";
    private static final String ROUTING_KEY = "courier.location";

    public void updateLocation(LocationUpdateDTO locationUpdateDTO) {
        log.info("Received location update for courier: {}", locationUpdateDTO.getCourierId());
        try {
            // Save to Redis
            redisTemplate.opsForGeo().add(
                    REDIS_KEY,
                    new Point(locationUpdateDTO.getLongitude(), locationUpdateDTO.getLatitude()),
                    locationUpdateDTO.getCourierId()
            );
            log.debug("Location saved to Redis for courier: {}", locationUpdateDTO.getCourierId());

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, locationUpdateDTO);
            log.debug("Location published to RabbitMQ for courier: {}", locationUpdateDTO.getCourierId());
        } catch (Exception e) {
            log.error("Error updating location for courier {}: {}", locationUpdateDTO.getCourierId(), e.getMessage(), e);
            throw e;
        }
    }

    public List<LocationUpdateDTO> getAllLocations() {
        log.info("Fetching all courier locations from Redis");
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                REDIS_KEY,
                new Circle(new Point(0, 0), new Distance(20000, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeCoordinates()
        );

        List<LocationUpdateDTO> locations = new ArrayList<>();
        if (results != null) {
            results.forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                locations.add(LocationUpdateDTO.builder()
                        .courierId(location.getName())
                        .longitude(location.getPoint().getX())
                        .latitude(location.getPoint().getY())
                        .timestamp(System.currentTimeMillis())
                        .build());
            });
        }
        return locations;
    }
}

