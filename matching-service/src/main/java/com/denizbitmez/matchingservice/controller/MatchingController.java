package com.denizbitmez.matchingservice.controller;

import com.denizbitmez.common.dto.OrderRequestDTO;
import com.denizbitmez.matchingservice.entity.Order;
import com.denizbitmez.matchingservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PutMapping("/order/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = matchingService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getRecentOrders() {
        return ResponseEntity.ok(matchingService.getRecentOrders());
    }
}
