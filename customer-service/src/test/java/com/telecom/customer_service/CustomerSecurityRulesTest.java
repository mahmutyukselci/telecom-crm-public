package com.telecom.customer_service;

import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.repository.CustomerRepository;
import com.telecom.customer_service.security.CustomerSecurityRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerSecurityRulesTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CustomerSecurityRules securityRules;

    @Test
    @DisplayName("isOwner: Should return true when authenticated Keycloak ID matches customer record")
    void isOwner_whenKeycloakIdMatches_shouldReturnTrue() {
        // Given
        String customerId = "cust-001";
        String keycloakUserId = "kc-user-999";

        Customer customer = Customer.builder()
                .id(customerId)
                .keycloakUserId(keycloakUserId)
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(keycloakUserId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // When
        boolean isOwner = securityRules.isOwner(customerId, authentication);

        // Then
        assertThat(isOwner).isTrue();
    }

    @Test
    @DisplayName("isOwner: Should return false when authenticated user tries to access another customer's record")
    void isOwner_whenKeycloakIdMismatch_shouldReturnFalse() {
        // Given
        String customerId = "cust-001";
        Customer customer = Customer.builder()
                .id(customerId)
                .keycloakUserId("kc-real-owner")
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("kc-attacker");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // When
        boolean isOwner = securityRules.isOwner(customerId, authentication);

        // Then
        assertThat(isOwner).isFalse();
    }

    @Test
    @DisplayName("isOwner: Should return false when authentication principal is not a JWT token")
    void isOwner_whenNotJwtAuthentication_shouldReturnFalse() {
        // Given
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        // When
        boolean isOwner = securityRules.isOwner("cust-001", authentication);

        // Then
        assertThat(isOwner).isFalse();
        verifyNoInteractions(customerRepository);
    }
}