package com.telecom.api_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class SwaggerRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(SwaggerRouteConfig.class);

    @Bean
    public RouteLocator swaggerRoutes(RouteLocatorBuilder builder,
                                      @Value("${CUSTOMER_SERVICE_URL:http://localhost:8084}") String customerUrl,
                                      @Value("${SUBSCRIPTION_SERVICE_URL:http://localhost:8082}") String subscriptionUrl,
                                      @Value("${CATALOG_SERVICE_URL:http://localhost:8083}") String catalogUrl,
                                      @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8085}") String notificationUrl) {

        return builder.routes()

                // =========================
                // CUSTOMER SERVICE
                // =========================

                // Swagger Documentation
                .route("customer-service-docs", r -> r
                        // FIX 1: Handle both exact path and subpaths in case the request does not end with '/'
                        .path("/v3/api-docs/customer-service", "/v3/api-docs/customer-service/**")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/customer-service(?<segment>.*)", "/v3/api-docs${segment}")
                                .filter(loggingFilter("Customer Service Docs"))
                        )
                        .uri(customerUrl)
                )

                // API Endpoints
                .route("customer-service-api", r -> r
                        .path("/api/v1/customers/**")
                        .filters(f -> f
                                .filter(loggingFilter("Customer Service API"))
                        )
                        .uri(customerUrl)
                )


                // =========================
                // SUBSCRIPTION SERVICE
                // =========================

                .route("subscription-service-docs", r -> r
                        .path("/v3/api-docs/subscription-service", "/v3/api-docs/subscription-service/**")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/subscription-service(?<segment>.*)", "/v3/api-docs${segment}")
                                .filter(loggingFilter("Subscription Service Docs"))
                        )
                        .uri(subscriptionUrl)
                )

                .route("subscription-service-api", r -> r
                        .path("/api/v1/subscriptions/**")
                        .filters(f -> f
                                .filter(loggingFilter("Subscription Service API"))
                        )
                        .uri(subscriptionUrl)
                )
                .route("subscription-service-history", r -> r
                        .path("/api/v1/history/**")
                        .filters(f -> f
                                .filter(loggingFilter("Subscription Service - History API"))
                        )
                        .uri(subscriptionUrl)
                )


                // =========================
                // CATALOG SERVICE
                // =========================

                .route("catalog-service-docs", r -> r
                        .path("/v3/api-docs/catalog-service", "/v3/api-docs/catalog-service/**")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/catalog-service(?<segment>.*)", "/v3/api-docs${segment}")
                                .filter(loggingFilter("Catalog Service Docs"))
                        )
                        .uri(catalogUrl)
                )

                .route("catalog-service-api", r -> r
                        // FIX 2: The actual URL used in curl requests is mapped here
                        .path("/api/v1/tariffs/**")
                        .filters(f -> f
                                .filter(loggingFilter("Catalog Service API"))
                        )
                        .uri(catalogUrl)
                )


                // =========================
                // NOTIFICATION SERVICE
                // =========================

                .route("notification-service-docs", r -> r
                        .path("/v3/api-docs/notification-service", "/v3/api-docs/notification-service/**")
                        .filters(f -> f
                                .rewritePath("/v3/api-docs/notification-service(?<segment>.*)", "/v3/api-docs${segment}")
                                .filter(loggingFilter("Notification Service Docs"))
                        )
                        .uri(notificationUrl)
                )

                .route("notification-service-api", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .filter(loggingFilter("Notification Service API"))
                        )
                        .uri(notificationUrl)
                )

                .build();
    }

    /**
     * Custom Gateway filter that logs:
     * - The outgoing request URI before it is sent to the downstream service
     * - The HTTP response status returned by the downstream service
     */
    private GatewayFilter loggingFilter(String routeName) {
        return (exchange, chain) -> {
            log.info("===================================================");
            log.info("ROUTE INTERCEPTED: {}", routeName);
            log.info("Outgoing Request URI: {}", exchange.getRequest().getURI());
            log.info("===================================================");

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                log.info("RESPONSE STATUS [{}]: {}", routeName, exchange.getResponse().getStatusCode());
            }));
        };
    }
}