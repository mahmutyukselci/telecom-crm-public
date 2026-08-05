package com.telecom.api_gateway.config;

import com.telecom.commonutils.security.KeycloakRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

                JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
                jwtAuthConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

                ReactiveJwtAuthenticationConverterAdapter reactiveAdapter = new ReactiveJwtAuthenticationConverterAdapter(
                                jwtAuthConverter);

                http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/webjars/swagger-ui/**",
                                                                "/actuator/**",
                                                                "/eureka/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveAdapter)));

                return http.build();
        }
}