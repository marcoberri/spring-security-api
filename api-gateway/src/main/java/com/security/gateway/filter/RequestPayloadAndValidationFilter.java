package com.security.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway filter enforcing maximum payload size (1MB) on POST/PUT requests
 * and validating JSON Content-Type headers before routing to backend microservices.
 */
@Component
public class RequestPayloadAndValidationFilter implements GlobalFilter, Ordered {

    private static final long MAX_PAYLOAD_BYTES = 1024 * 1024; // 1MB

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            // Check Content-Length if present
            long contentLength = exchange.getRequest().getHeaders().getContentLength();
            if (contentLength > MAX_PAYLOAD_BYTES) {
                return rejectWithPayloadTooLarge(exchange, "Payload exceeds 1MB limit (Content-Length: " + contentLength + ")");
            }

            // Validate Content-Type for POST/PUT
            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType != null && !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                    && !MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
                return rejectWithBadRequest(exchange, "Unsupported Content-Type. Expected application/json");
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> rejectWithPayloadTooLarge(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\": \"PAYLOAD_TOO_LARGE\", \"message\": \"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private Mono<Void> rejectWithBadRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\": \"BAD_REQUEST\", \"message\": \"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
