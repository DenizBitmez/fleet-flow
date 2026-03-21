package com.denizbitmez.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frameOptions -> frameOptions.mode(org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().permitAll() // Trusting Gateway filters for business logic security
                )
                .build();
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("anonymous");
        };
    }
}
