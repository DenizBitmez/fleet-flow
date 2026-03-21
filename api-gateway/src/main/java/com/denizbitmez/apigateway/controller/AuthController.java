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
    public org.springframework.http.ResponseEntity<Mono<Map<String, String>>> login(@RequestParam(defaultValue = "demo-user") String username) {
        String token = jwtUtils.generateToken(username);
        
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("JWT", token)
                .httpOnly(true)
                .secure(false) // Set true if using HTTPS
                .path("/")
                .maxAge(3600) // 1 hour
                .sameSite("Strict")
                .build();

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Mono.just(Map.of(
                        "message", "Login successful",
                        "info", "JWT stored securely in HttpOnly cookie."
                )));
    }
}
