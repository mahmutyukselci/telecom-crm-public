package com.telecom.customer_service.security;

import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("customerSecurityRules")
@RequiredArgsConstructor
public class CustomerSecurityRules {

    private final CustomerRepository customerRepository;

    public boolean isOwner(String customerId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        String currentKeycloakUserId = jwt.getSubject(); // Keycloak sub claim
        Optional<Customer> customerOpt = customerRepository.findById(customerId);

        return customerOpt
                .map(customer -> currentKeycloakUserId.equals(customer.getKeycloakUserId()))
                .orElse(false);
    }
}