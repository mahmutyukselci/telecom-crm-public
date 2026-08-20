package com.telecom.subscription_service;

import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=subscription_schema",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
class SubscriptionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("1. Basic CRUD: Subscription should be saved and retrieved with all columns")
    void savesAndReadsSubscriptionWithAllCurrentColumns() {
        Subscription subscription = Subscription.builder()
                .customerId("test-customer-id")
                .tariffId("test-tariff-id")
                .keycloakUserId("test-keycloak-user-id")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        Subscription found = subscriptionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getKeycloakUserId()).isEqualTo("test-keycloak-user-id");
        assertThat(found.getEndDate()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("2. Custom Query: findByKeycloakUserId should return subscriptions for the given user")
    void findByKeycloakUserId_shouldReturnMatchingSubscriptions() {
        // Given: 2 subscriptions for same user, 1 for another user
        String targetKeycloakId = "keycloak-user-999";

        subscriptionRepository.save(Subscription.builder()
                .customerId("cust-1").tariffId("tar-1").keycloakUserId(targetKeycloakId).status(SubscriptionStatus.ACTIVE).build());
        subscriptionRepository.save(Subscription.builder()
                .customerId("cust-1").tariffId("tar-2").keycloakUserId(targetKeycloakId).status(SubscriptionStatus.ACTIVE).build());
        subscriptionRepository.save(Subscription.builder()
                .customerId("cust-2").tariffId("tar-3").keycloakUserId("other-user-111").status(SubscriptionStatus.ACTIVE).build());

        // When
        List<Subscription> results = subscriptionRepository.findByKeycloakUserId(targetKeycloakId);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(sub -> sub.getKeycloakUserId().equals(targetKeycloakId));
    }

    @Test
    @DisplayName("3. JPA Lifecycle: @PrePersist should set default ACTIVE status and startDate")
    void prePersist_shouldSetDefaultStatusAndStartDate_whenNull() {
        // Given: intentionally leaving status and startDate null
        Subscription subscription = Subscription.builder()
                .customerId("cust-lifecycle")
                .tariffId("tar-lifecycle")
                .keycloakUserId("keycloak-lifecycle")
                .build();

        // When
        Subscription saved = subscriptionRepository.saveAndFlush(subscription);

        // Then
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(saved.getStartDate()).isNotNull();
    }

    @Test
    @DisplayName("4. Update: Status change to CANCELLED should be persisted")
    void updateStatus_shouldPersistNewStatus() {
        // Given
        Subscription saved = subscriptionRepository.save(Subscription.builder()
                .customerId("cust-cancel").tariffId("tar-1").keycloakUserId("kc-1").status(SubscriptionStatus.ACTIVE).build());

        // When
        saved.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.saveAndFlush(saved);

        // Then
        Subscription updated = subscriptionRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("5. Delete: deleteById should remove the record from database")
    void deleteById_shouldRemoveSubscriptionFromDatabase() {
        // Given
        Subscription saved = subscriptionRepository.save(Subscription.builder()
                .customerId("cust-del").tariffId("tar-1").keycloakUserId("kc-1").status(SubscriptionStatus.ACTIVE).build());

        // When
        subscriptionRepository.deleteById(saved.getId());

        // Then
        Optional<Subscription> deleted = subscriptionRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }
}