package com.denizbitmez.apigateway.filter;

import com.denizbitmez.apigateway.util.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;

    public AuthenticationFilter(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    public static class Config {
        // Configuration properties can be added here
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String token = null;
            
            // 1. Try to read from HttpOnly Cookie
            org.springframework.http.HttpCookie cookie = request.getCookies().getFirst("JWT");
            if (cookie != null) {
                token = cookie.getValue();
            } 
            // 2. Fallback to Authorization header (for Postman/cURL tests)
            else if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null) {
                return onError(exchange, "Missing Authentication Token (Cookie or Header)", HttpStatus.UNAUTHORIZED);
            }

            if (!jwtUtils.validateToken(token)) {
                return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
            }

            // Propagate user ID to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User", jwtUtils.getClaims(token).getSubject())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
