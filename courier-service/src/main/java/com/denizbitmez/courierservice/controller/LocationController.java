package com.denizbitmez.courierservice.controller;

import com.denizbitmez.common.dto.LocationUpdateDTO;
import com.denizbitmez.courierservice.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courier")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(@RequestBody LocationUpdateDTO locationUpdateDTO) {
        locationService.updateLocation(locationUpdateDTO);
        return ResponseEntity.ok().build();
    }
}
