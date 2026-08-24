package com.telecom.subscription_service;

import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.outbox.OutboxRelayScheduler;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import com.telecom.subscription_service.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=subscription_schema",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "outbox.relay.interval-ms=99999999",
        "flowable.async-executor-activate=false",
        "spring.kafka.listener.auto-startup=false"
})
class SubscriptionServiceITTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private OutboxRelayScheduler outboxRelayScheduler;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("IT-1: When a subscription is cancelled via the service, its status should be CANCELLED in the real PostgreSQL database")
    void shouldCancelSubscriptionInRealDatabase() {
        // 1. GIVEN: Persist an active subscription directly into the database
        Subscription activeSubscription = Subscription.builder()
                .customerId("customer-int-100")
                .tariffId("tariff-int-200")
                .keycloakUserId("keycloak-int-300")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription saved = subscriptionRepository.saveAndFlush(activeSubscription);
        String subscriptionId = saved.getId();

        // 2. WHEN: Call the cancelSubscription method from the service layer
        subscriptionService.cancelSubscription(subscriptionId);

        // 3. THEN: Fetch the record again from the database and verify the result
        Subscription foundInDb = subscriptionRepository.findById(subscriptionId)
                .orElseThrow();

        assertThat(foundInDb.getStatus())
                .as("When the service issues a cancel command, the DB status should be updated to CANCELLED")
                .isEqualTo(SubscriptionStatus.CANCELLED);
    }
}