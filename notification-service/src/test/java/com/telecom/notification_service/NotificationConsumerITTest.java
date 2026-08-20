package com.telecom.notification_service;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.provider.NotificationProvider;
import com.telecom.notification_service.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=com.telecom.notification_service.event",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.listener.immediate-start=true"
})
@ActiveProfiles("test")
class NotificationConsumerITTest {
        @MockitoBean
        private JavaMailSender javaMailSender;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        @MockitoBean(name = "defaultRetryTopicKafkaTemplate")
        private KafkaTemplate<Object, Object> defaultRetryTopicKafkaTemplate;

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Container
        @ServiceConnection
        static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));

        @Autowired
        private KafkaTemplate<String, SubscriptionCreatedEvent> kafkaTemplate;

        @Autowired
        private ProcessedEventRepository processedEventRepository;

        @MockitoBean
        private NotificationProvider notificationProvider;

        @MockitoBean
        private StringRedisTemplate redisTemplate;

        @MockitoBean
        private ValueOperations<String, String> valueOperations;

        @BeforeEach
        void setUp() {
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        // ========================================================================
        // 1. HAPPY PATH (E2E)
        // ========================================================================
        @Test
        @DisplayName("E2E-1: Should consume event from Kafka topic, persist event ID to DB, and invoke provider")
        void shouldConsumeKafkaEventAndPersistIdempotencyRecord() {
                // Given
                String eventId = "e2e-event-uuid-888";
                String redisKey = "idempotency:notification:" + eventId;

                when(valueOperations.setIfAbsent(eq(redisKey), anyString(), any(Duration.class)))
                                .thenReturn(Boolean.TRUE);
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                                .thenReturn(Boolean.TRUE);

                SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                                eventId, "sub-888", "cust-888", "tariff-pro-max", "ACTIVE");

                // When
                kafkaTemplate.send("subscription-created-topic", event.customerId(), event);

                // Then
                await()
                                .atMost(Duration.ofSeconds(15))
                                .pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> {
                                        assertThat(processedEventRepository.existsById(eventId))
                                                        .as("Event ID should be recorded in the processed_events table")
                                                        .isTrue();

                                        verify(notificationProvider, atLeastOnce()).sendCreated(event);
                                });
        }

        // ========================================================================
        // 2. DUPLICATE EVENT HANDLING (E2E)
        // ========================================================================
        @Test
        @DisplayName("E2E-2: When duplicate Kafka messages arrive, provider should be invoked only ONCE")
        void shouldIgnoreDuplicateKafkaEvents() {
                // Given
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                                .thenReturn(Boolean.TRUE);

                String eventId = "e2e-duplicate-uuid-999";
                SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                                eventId, "sub-999", "cust-999", "tariff-pro-max", "ACTIVE");

                // When: Send the same message twice to Kafka
                kafkaTemplate.send("subscription-created-topic", event.customerId(), event);
                kafkaTemplate.send("subscription-created-topic", event.customerId(), event);

                // Then: DB record should exist and provider should be called ONLY ONCE
                await()
                                .atMost(Duration.ofSeconds(10))
                                .pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> {
                                        assertThat(processedEventRepository.existsById(eventId)).isTrue();
                                        verify(notificationProvider, times(1)).sendCreated(event);
                                });
        }

        // ========================================================================
        // 3. REDIS LOCK FAILURE (E2E)
        // ========================================================================
        @Test
        @DisplayName("E2E-3: When Redis lock cannot be acquired, message processing should be skipped")
        void shouldSkipProcessingWhenRedisLockFails() {
                // Given: Redis lock could not be acquired (e.g., another instance holds it)
                when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                                .thenReturn(Boolean.FALSE);

                String eventId = "e2e-redis-lock-fail-777";
                SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                                eventId, "sub-777", "cust-777", "tariff-pro-max", "ACTIVE");

                // When
                kafkaTemplate.send("subscription-created-topic", event.customerId(), event);

                // Then: Provider should never be called and no DB record should exist
                await()
                                .atMost(Duration.ofSeconds(5))
                                .pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> {
                                        verify(notificationProvider, never()).sendCreated(any());
                                        assertThat(processedEventRepository.existsById(eventId)).isFalse();
                                });
        }

        // ========================================================================
        // 4. PROVIDER EXCEPTION & RETRY BEHAVIOR (E2E)
        // ========================================================================
        @Test
        @DisplayName("E2E-4: When provider throws exception, Redis lock should be deleted to allow retry")
        void shouldDeleteRedisLockWhenProviderThrowsException() {
                // Given
                String redisKey = "idempotency:notification:e2e-error-uuid-555";
                when(valueOperations.setIfAbsent(eq(redisKey), anyString(), any(Duration.class)))
                                .thenReturn(Boolean.TRUE);

                String eventId = "e2e-error-uuid-555";
                SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                                eventId, "sub-555", "cust-555", "tariff-pro-max", "ACTIVE");

                // Simulate provider failure
                doThrow(new RuntimeException("External Gateway Timeout"))
                                .when(notificationProvider).sendCreated(any());

                // When
                kafkaTemplate.send("subscription-created-topic", event.customerId(), event);

                // Then: Redis lock should be deleted to allow retry
                await()
                                .atMost(Duration.ofSeconds(10))
                                .pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> {
                                        verify(notificationProvider, atLeastOnce()).sendCreated(any());
                                        verify(redisTemplate, atLeastOnce()).delete(redisKey);
                                });
        }
}
