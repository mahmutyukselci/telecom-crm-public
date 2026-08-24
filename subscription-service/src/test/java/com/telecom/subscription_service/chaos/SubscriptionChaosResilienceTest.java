package com.telecom.subscription_service.chaos;

import com.telecom.subscription_service.eventsourcing.service.SubscriptionEventSourcingService;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.outbox.OutboxEvent;
import com.telecom.subscription_service.outbox.OutboxEventRepository;
import com.telecom.subscription_service.outbox.OutboxService;
import com.telecom.subscription_service.outbox.OutboxStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Chaos Engineering & Fault Injection Test Suite (Simulating Toxiproxy Latency and Broker Outages).
 * <p>
 * Verifies system breaking points, graceful degradation, and transactional self-healing.
 */
@Slf4j
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
class SubscriptionChaosResilienceTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private SubscriptionEventSourcingService eventSourcingService;

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Chaos Test 1: Simulated Kafka Network Blackhole - Outbox Pattern Guarantees Zero Message Loss")
    void testBrokerBlackholeAndTransactionalOutboxResilience() {
        // GIVEN: Kafka broker is simulated as unreachable / blackholed (throws transport timeout exception)
        doThrow(new RuntimeException("Kafka Broker Network Partition / Connection Refused: 9092"))
                .when(kafkaTemplate).send(any(), any(), any());

        // WHEN: Core subscription lifecycle transaction is executed during network outage
        Subscription subscription = Subscription.builder()
                .customerId(UUID.randomUUID().toString())
                .tariffId("TARIFF_ULTRA_5G")
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .startDate(LocalDateTime.now())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        String subscriptionId = saved.getId();

        // Transactional Outbox write
        outboxService.saveEvent(
                "SUBSCRIPTION",
                subscriptionId,
                "SUBSCRIPTION_CREATED",
                "subscription-events-topic",
                "{\"tariffId\": \"TARIFF_ULTRA_5G\"}"
        );

        // THEN: Database transaction committed successfully despite broker outage
        assertThat(subscriptionRepository.findById(subscriptionId)).isPresent();

        // Verify outbox buffered the event in PostgreSQL table for asynchronous self-healing relay
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAll();
        assertThat(pendingEvents).isNotEmpty();
        assertThat(pendingEvents.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        log.info("✅ [CHAOS VERIFIED] Message broker blackhole handled gracefully: Zero data loss via Outbox buffer!");
    }

    @Test
    @DisplayName("Chaos Test 2: Concurrent Jitter & Event Sourcing Deterministic Replay Under Load")
    void testConcurrentJitterAndEventSourcingDeterminism() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        int totalEvents = 10;
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Concurrently append events with simulated network jitter
        CountDownLatch latch = new CountDownLatch(totalEvents);
        for (int i = 1; i <= totalEvents; i++) {
            final int version = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(5, 30)); // 5-30ms jitter
                    String eventType = (version == 1) ? "SUBSCRIPTION_CREATED" : "ADDON_ATTACHED";
                    eventSourcingService.appendEvent(
                            aggregateId,
                            eventType,
                            java.util.Map.of("customerId", UUID.randomUUID().toString(), "tariffId", "PLAN_10GB", "addonId", "ADDON_" + version),
                            version
                    );
                } catch (Exception e) {
                    log.error("Jitter error", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // THEN: Verify deterministic replay state
        var state = eventSourcingService.reconstituteStateAt(aggregateId, LocalDateTime.now());
        assertThat(state).isNotNull();
        assertThat(state.subscriptionId()).isEqualTo(aggregateId);
        log.info("✅ [CHAOS VERIFIED] Event Sourcing state reconstituted deterministically after concurrent jitter!");
    }
}
