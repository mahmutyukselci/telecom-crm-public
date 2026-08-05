package com.telecom.api_gateway;

import com.telecom.api_gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

class RateLimiterConfigTest {

    private final RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();

    @Test
    @DisplayName("userIpKeyResolver: Should resolve and return the remote client IP address from ServerWebExchange")
    void userIpKeyResolver_shouldResolveRemoteIpAddress() {
        // Given: Simulate a client request originating from IP "192.168.1.100"
        InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.100", 8080);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/tariffs")
                .remoteAddress(remoteAddress)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        KeyResolver keyResolver = rateLimiterConfig.userIpKeyResolver();

        // When & Then: Verify the reactive Mono stream emits the expected IP string
        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("192.168.1.100")
                .verifyComplete();
    }
}