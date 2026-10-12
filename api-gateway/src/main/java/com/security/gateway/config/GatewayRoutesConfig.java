package com.security.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("sensitive-data-route", r -> r
                        .path("/api/v1/sensitive-data/**")
                        .filters(f -> f.removeRequestHeader("Cookie"))
                        .uri("lb://sensitivedata-service")
                )
                .build();
    }
}
