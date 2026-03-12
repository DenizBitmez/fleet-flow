package com.denizbitmez.trackingservice.service;

import com.denizbitmez.common.dto.LocationUpdateDTO;
import com.denizbitmez.common.event.CourierAssignedEvent;
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

    @RabbitListener(queues = "courier.location.queue")
    public void handleLocationUpdate(LocationUpdateDTO locationUpdate) {
        messagingTemplate.convertAndSend("/topic/locations", locationUpdate);
    }

    @RabbitListener(queues = "courier.assigned.queue")
    public void handleCourierAssigned(CourierAssignedEvent event) {
        messagingTemplate.convertAndSend("/topic/assignments", event);
        log.info("Assignment event pushed to WS: {}", event.getOrderId());
    }
}
