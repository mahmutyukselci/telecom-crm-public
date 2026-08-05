package com.telecom.customer_service.controller;

import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/webhook")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SERVICE')")
public class CustomerWebhookController {

    private final CustomerRepository customerRepository;

    public record WebhookSyncRequest(
            String keycloakUserId,
            String firstName,
            String lastName,
            String email,
            String phone
    ) {}

    @PutMapping
    public ResponseEntity<Void> upsert(@RequestBody WebhookSyncRequest request) {
        if (request.keycloakUserId() == null || request.keycloakUserId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Customer customer = customerRepository.findByKeycloakUserId(request.keycloakUserId())
                .orElseGet(() -> Customer.builder()
                        .keycloakUserId(request.keycloakUserId())
                        .build());

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        customerRepository.save(customer);
        log.info("Customer synced from Keycloak. keycloakUserId={}", request.keycloakUserId());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{keycloakUserId}")
    public ResponseEntity<Void> delete(@PathVariable String keycloakUserId) {
        customerRepository.findByKeycloakUserId(keycloakUserId)
                .ifPresent(customerRepository::delete);

        log.info("Customer deleted from Keycloak sync. keycloakUserId={}", keycloakUserId);
        return ResponseEntity.noContent().build();
    }
}