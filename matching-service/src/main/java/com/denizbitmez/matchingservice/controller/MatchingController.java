package com.denizbitmez.matchingservice.controller;

import com.denizbitmez.common.dto.OrderRequestDTO;
import com.denizbitmez.matchingservice.entity.Order;
import com.denizbitmez.matchingservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/order")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        Order order = matchingService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(order);
    }
}
