package com.security.gateway.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
public class GatewayHttpClientConfig {

    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                return httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            } catch (SSLException e) {
                throw new RuntimeException("Failed to configure SSL for Gateway HttpClient", e);
            }
        };
    }
}
