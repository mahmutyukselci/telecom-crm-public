package com.telecom.commonutils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class RestClientConfig {

    /**
     * 1. RestClientCustomizer:
     * Instead of overriding Spring Boot 3's auto-configured RestClient.Builder,
     * we use a Customizer to inject our security interceptor.
     * This ensures that Spring's default configurations (e.g., JSON converters)
     * are preserved and not accidentally lost.
     */
    @Bean
    public RestClientCustomizer restClientTokenPropagationCustomizer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                request.getHeaders().add(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwtToken.getToken().getTokenValue()
                );
            }

            return execution.execute(request, body);
        });
    }

    /**
     * 2. Fallback ObjectMapper:
     * Since removing Eureka may also remove the auto-configured ObjectMapper bean,
     * we define a fallback bean to ensure it is always available in the context.
     * This guarantees that components like OutboxService can start without errors.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Ensures compatibility with Java 8 Date/Time (e.g., LocalDateTime)
        return mapper;
    }
}