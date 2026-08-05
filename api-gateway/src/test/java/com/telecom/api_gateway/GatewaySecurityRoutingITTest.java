package com.telecom.api_gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySecurityRoutingITTest {

        @MockitoBean
        private ReactiveJwtDecoder reactiveJwtDecoder;

        @Autowired
        private WebTestClient webTestClient;

        // ========================================================================
        // HELPER: Creates a realistic valid JWT mock object
        // ========================================================================
        private Jwt createMockJwt() {
                return Jwt.withTokenValue("valid.jwt.token")
                                .header("alg", "none")
                                .claim("sub", "keycloak-user-123")
                                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                                .build();
        }

        // ========================================================================
        // 1. PUBLIC ENDPOINTS (PERMIT ALL)
        // ========================================================================

        @Test
        @DisplayName("Security: Should allow unauthenticated access to Actuator endpoints")
        void shouldAllowUnauthenticatedAccessToActuator() {
                webTestClient
                                .get()
                                .uri("/actuator")
                                .exchange()
                                .expectStatus().is2xxSuccessful();
        }

        @Test
        @DisplayName("Security: Should allow unauthenticated access to Swagger / OpenAPI endpoints")
        void shouldAllowUnauthenticatedAccessToSwagger() {
                webTestClient
                                .get()
                                .uri("/v3/api-docs")
                                .exchange()
                                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
        }

        // ========================================================================
        // 2. PROTECTED API ENDPOINTS (UNAUTHENTICATED / INVALID AUTH)
        // ========================================================================

        @Test
        @DisplayName("Security: Should reject unauthenticated requests to protected customer-service routes with 401")
        void shouldRejectUnauthenticatedAccessToCustomerService() {
                webTestClient
                                .get()
                                .uri("/api/v1/customers")
                                .exchange()
                                .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Security: Should reject requests with malformed or forged JWT Bearer token with 401")
        void shouldRejectMalformedJwtTokenWithUnauthorized() {
                when(reactiveJwtDecoder.decode("invalid.forged.jwt-token"))
                                .thenReturn(Mono.error(new BadJwtException("Invalid JWT token")));

                webTestClient
                                .get()
                                .uri("/api/v1/tariffs")
                                .header("Authorization", "Bearer invalid.forged.jwt-token")
                                .exchange()
                                .expectStatus().isUnauthorized();
        }

        // ========================================================================
        // 3. PROTECTED API ENDPOINTS (VALID AUTHENTICATION - HAPPY PATH)
        // ========================================================================

        @Test
        @DisplayName("Security: Should pass authentication filter when a valid JWT token is provided")
        void shouldAllowAccessWhenValidJwtIsProvided() {
                // When a valid token is provided, ensure the decoder returns a successful Jwt
                when(reactiveJwtDecoder.decode("valid.jwt.token"))
                                .thenReturn(Mono.just(createMockJwt()));

                webTestClient
                                .get()
                                .uri("/api/v1/tariffs")
                                .header("Authorization", "Bearer valid.jwt.token")
                                .exchange()
                                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
        }

        // ========================================================================
        // 4. EDGE CASES & ROUTING BEHAVIOR
        // ========================================================================

        @Test
        @DisplayName("Routing: Should return 404 Not Found when an authenticated request targets an unregistered route path")
        void shouldReturnNotFoundForUnknownRoute() {
                when(reactiveJwtDecoder.decode("valid.jwt.token"))
                                .thenReturn(Mono.just(createMockJwt()));

                webTestClient
                                .get()
                                .uri("/api/v1/unknown-non-existent-service")
                                .header("Authorization", "Bearer valid.jwt.token")
                                .exchange()
                                .expectStatus().isNotFound();
        }
}