package com.telecom.customer_service;

import com.telecom.customer_service.controller.CustomerWebhookController;
import com.telecom.customer_service.model.Customer;
import com.telecom.customer_service.outbox.OutboxRelayScheduler;
import com.telecom.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(roles = "SERVICE")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=customer_schema",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "outbox.relay.interval-ms=99999999"
})
class CustomerWebhookITTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OutboxRelayScheduler outboxRelayScheduler;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerWebhookController webhookController;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("E2E-1: Webhook should insert new customer and update existing customer without duplicating records")
    void shouldInsertAndUpdateCustomerViaWebhookSync() {
        // 1. GIVEN: Simulate initial Keycloak user creation webhook payload
        String testKeycloakId = "kc-webhook-user-777";
        var createPayload = new CustomerWebhookController.WebhookSyncRequest(
                testKeycloakId, "John", "Doe", "john@test.com", "5550009988"
        );

        // 2. WHEN: Invoke upsert webhook
        webhookController.upsert(createPayload);

        // 3. THEN: Assert customer is created in DB
        Customer createdCustomer = customerRepository.findByKeycloakUserId(testKeycloakId).orElseThrow();
        assertThat(createdCustomer.getFirstName()).isEqualTo("John");
        assertThat(createdCustomer.getEmail()).isEqualTo("john@test.com");

        // 4. WHEN: Keycloak sends updated information for the same user
        var updatePayload = new CustomerWebhookController.WebhookSyncRequest(
                testKeycloakId, "Johnathan", "Doe-Updated", "john.updated@test.com", "5551112233"
        );
        webhookController.upsert(updatePayload);

        // 5. THEN: Assert existing DB record is updated and NO duplicates are created
        long count = customerRepository.findAll().stream()
                .filter(c -> testKeycloakId.equals(c.getKeycloakUserId()))
                .count();

        assertThat(count).isEqualTo(1);
        Customer updatedCustomer = customerRepository.findByKeycloakUserId(testKeycloakId).orElseThrow();
        assertThat(updatedCustomer.getFirstName()).isEqualTo("Johnathan");
        assertThat(updatedCustomer.getPhone()).isEqualTo("5551112233");
    }

    @Test
    @DisplayName("E2E-2: Webhook DELETE endpoint should remove customer from PostgreSQL database")
    void shouldDeleteCustomerViaWebhook() {
        // 1. GIVEN: Create an existing customer in DB
        String targetKeycloakId = "kc-to-be-deleted-888";
        webhookController.upsert(new CustomerWebhookController.WebhookSyncRequest(
                targetKeycloakId, "Temporary", "User", "temp@test.com", "5559990000"
        ));
        assertThat(customerRepository.findByKeycloakUserId(targetKeycloakId)).isPresent();

        // 2. WHEN: Keycloak triggers delete webhook
        webhookController.delete(targetKeycloakId);

        // 3. THEN: Assert record is removed from DB
        Optional<Customer> deleted = customerRepository.findByKeycloakUserId(targetKeycloakId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("E2E-3: Webhook should return Bad Request when keycloakUserId is null or blank")
    void shouldReturnBadRequestWhenKeycloakUserIdIsInvalid() {
        // Given: Empty keycloakUserId payload
        var invalidPayload = new CustomerWebhookController.WebhookSyncRequest(
                "", "John", "Doe", "invalid@test.com", "5550001122"
        );

        // When
        var response = webhookController.upsert(invalidPayload);

        // Then: Should return 400 Bad Request and NOT save to DB
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(customerRepository.findAll()).isEmpty();
    }
}