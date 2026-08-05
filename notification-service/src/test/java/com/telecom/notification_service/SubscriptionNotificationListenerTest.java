package com.telecom.notification_service;

import com.telecom.notification_service.consumer.SubscriptionNotificationListener;
import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.repository.ProcessedEventRepository;
import com.telecom.notification_service.service.NotificationDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionNotificationListenerTest {

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SubscriptionNotificationListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========================================================================
    // 1. SUBSCRIPTION CREATED EVENT TESTS
    // ========================================================================

    @Test
    @DisplayName("onSubscriptionCreated: Should process event successfully and mark Redis lock as DONE")
    void onSubscriptionCreated_whenSuccessful_shouldDeliverAndMarkDone() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                "evt-100", "sub-200", "cust-300", "tar-400", "ACTIVE"
        );
        String redisKey = "idempotency:notification:evt-100";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(processedEventRepository.existsById("evt-100")).thenReturn(false);

        // When
        listener.onSubscriptionCreated(event);

        // Then
        verify(processedEventRepository, times(1)).saveAndFlush(any());
        verify(notificationDeliveryService, times(1)).sendSubscriptionCreatedNotification(event);
        verify(valueOperations, times(1)).set(eq(redisKey), eq("DONE"), any(Duration.class));
    }

    @Test
    @DisplayName("onSubscriptionCreated: Should skip processing when duplicate detected in Redis")
    void onSubscriptionCreated_whenRedisDuplicate_shouldSkipProcessing() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                "evt-101", "sub-201", "cust-301", "tar-401", "ACTIVE"
        );
        String redisKey = "idempotency:notification:evt-101";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.FALSE); // Lock already exists in Redis

        // When
        listener.onSubscriptionCreated(event);

        // Then
        verifyNoInteractions(processedEventRepository);
        verifyNoInteractions(notificationDeliveryService);
        verify(valueOperations, never()).set(anyString(), eq("DONE"), any(Duration.class));
    }

    @Test
    @DisplayName("onSubscriptionCreated: Should skip delivery and mark DONE when duplicate detected in DB")
    void onSubscriptionCreated_whenDbDuplicate_shouldSkipDeliveryAndMarkDone() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                "evt-102", "sub-202", "cust-302", "tar-402", "ACTIVE"
        );
        String redisKey = "idempotency:notification:evt-102";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(processedEventRepository.existsById("evt-102")).thenReturn(true); // Already processed in DB

        // When
        listener.onSubscriptionCreated(event);

        // Then
        verify(processedEventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationDeliveryService);
        verify(valueOperations, times(1)).set(eq(redisKey), eq("DONE"), any(Duration.class));
    }

    @Test
    @DisplayName("onSubscriptionCreated: Should remove Redis lock and rethrow exception on delivery failure")
    void onSubscriptionCreated_whenDeliveryFails_shouldDeleteRedisLockAndThrow() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                "evt-103", "sub-203", "cust-303", "tar-403", "ACTIVE"
        );
        String redisKey = "idempotency:notification:evt-103";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(processedEventRepository.existsById("evt-103")).thenReturn(false);
        doThrow(new RuntimeException("SMS Provider Down"))
                .when(notificationDeliveryService).sendSubscriptionCreatedNotification(event);

        // When & Then
        assertThatThrownBy(() -> listener.onSubscriptionCreated(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMS Provider Down");

        verify(redisTemplate, times(1)).delete(redisKey);
        verify(valueOperations, never()).set(anyString(), eq("DONE"), any(Duration.class));
    }

    @Test
    @DisplayName("onSubscriptionCreated: Should catch DataIntegrityViolationException and mark DONE gracefully")
    void onSubscriptionCreated_whenDataIntegrityViolation_shouldMarkDoneGracefully() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                "evt-104", "sub-204", "cust-304", "tar-404", "ACTIVE"
        );
        String redisKey = "idempotency:notification:evt-104";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(processedEventRepository.existsById("evt-104")).thenReturn(false);
        when(processedEventRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        // When
        listener.onSubscriptionCreated(event);

        // Then
        verifyNoInteractions(notificationDeliveryService);
        verify(valueOperations, times(1)).set(eq(redisKey), eq("DONE"), any(Duration.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    // ========================================================================
    // 2. SUBSCRIPTION EXPIRING EVENT TESTS
    // ========================================================================

    @Test
    @DisplayName("onSubscriptionExpiring: Should process expiring event successfully")
    void onSubscriptionExpiring_whenSuccessful_shouldDeliverAndMarkDone() {
        // Given
        SubscriptionExpiringEvent event = new SubscriptionExpiringEvent(
                "evt-200", "sub-300", "cust-400", "tar-500"
        );
        String redisKey = "idempotency:notification:evt-200";

        when(valueOperations.setIfAbsent(eq(redisKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(processedEventRepository.existsById("evt-200")).thenReturn(false);

        // When
        listener.onSubscriptionExpiring(event);

        // Then
        verify(processedEventRepository, times(1)).saveAndFlush(any());
        verify(notificationDeliveryService, times(1)).sendSubscriptionExpiringNotification(event);
        verify(valueOperations, times(1)).set(eq(redisKey), eq("DONE"), any(Duration.class));
    }
}