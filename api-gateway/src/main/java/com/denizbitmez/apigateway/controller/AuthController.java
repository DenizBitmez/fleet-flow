package com.denizbitmez.apigateway.controller;

import com.denizbitmez.apigateway.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtils jwtUtils;

    @GetMapping("/login")
    public Mono<Map<String, String>> login(@RequestParam(defaultValue = "demo-user") String username) {
        String token = jwtUtils.generateToken(username);
        return Mono.just(Map.of(
            "token", token,
            "type", "Bearer",
            "info", "Use this token in Authorization header: Bearer " + token
        ));
    }
}
