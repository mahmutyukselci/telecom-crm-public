package com.telecom.notification_service.provider;

import com.telecom.notification_service.dto.SmsResponse;
import com.telecom.notification_service.dto.TextBeeSmsRequest;
import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.exception.NotificationDispatchException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Primary
@Profile("!test") // Test MockNotificationProvider, prod/dev -> TextBee
public class TextBeeNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TextBeeNotificationProvider.class);

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;
    private final String deviceId;

    public TextBeeNotificationProvider(
            RestClient.Builder restClientBuilder,
            @Value("${notification.textbee.api-url}") String apiUrl,
            @Value("${notification.textbee.api-key}") String apiKey,
            @Value("${notification.textbee.device-id}") String deviceId
    ) {
        this.restClient = restClientBuilder.build();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.deviceId = deviceId;
    }

    @Override
    @Retry(name = "notificationProvider", fallbackMethod = "sendCreatedFallback")
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendCreatedFallback")
    public void sendCreated(SubscriptionCreatedEvent event) {
        String message = String.format(
                "Welcome! Your subscription %s (Tariff: %s) is now active.",
                event.subscriptionId(),
                event.tariffId()
        );
        sendRawSms(event.customerId(), message);
        log.info("TextBee SMS sent successfully for created subscriptionId={}", event.subscriptionId());
    }

    @Override
    @Retry(name = "notificationProvider", fallbackMethod = "sendExpiringFallback")
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendExpiringFallback")
    public void sendExpiring(SubscriptionExpiringEvent event) {
        String message = String.format(
                "Reminder: Your subscription %s is expiring soon.",
                event.subscriptionId()
        );
        sendRawSms(event.customerId(), message);
        log.info("TextBee SMS sent successfully for expiring subscriptionId={}", event.subscriptionId());
    }


    @Retry(name = "notificationProvider", fallbackMethod = "sendRawSmsFallback")
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendRawSmsFallback")
    public SmsResponse sendRawSms(String recipientPhone, String messageContent) {
        try {
            if (!isDeviceOnline()) {
                throw new NotificationDispatchException(
                        "TextBee device is OFFLINE / Unreachable (e.g., Airplane Mode). Refusing to queue SMS."
                );
            }

            TextBeeSmsRequest requestPayload = new TextBeeSmsRequest(
                    List.of(recipientPhone),
                    messageContent
            );

            restClient.post()
                    .uri(apiUrl + "/gateway/devices/{deviceId}/send-sms", deviceId)
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Manual TextBee SMS dispatched successfully to recipient={}", recipientPhone);
            return new SmsResponse(true, "SMS dispatched successfully", recipientPhone);

        } catch (Exception e) {
            log.error("Failed to deliver SMS via TextBee API to recipient={}", recipientPhone, e);
            throw new NotificationDispatchException("TextBee SMS API communication failed: " + e.getMessage(), e);
        }
    }

    public SmsResponse sendRawSmsFallback(String recipientPhone, String messageContent, Throwable t) {
        log.warn("CIRCUIT BREAKER / RETRY FALLBACK TRIGGERED for manual SMS to {}. Reason: {}",
                recipientPhone, t.getMessage());

        return new SmsResponse(
                false,
                "SMS could not be delivered. Fallback triggered due to: " + t.getMessage(),
                recipientPhone
        );
    }
    /**
     * Queries the device's live status via the TextBee API.
     * Returns false if the device is in airplane mode or powered off.
     */
    private boolean isDeviceOnline() {
        try {
            // Send a GET request to the TextBee device info endpoint and read the device's status/connection state
            // Note: Depending on the returned JSON structure, we check "status" or "isConnected" fields
            String responseBody = restClient.get()
                    .uri(apiUrl + "/gateway/devices/{deviceId}", deviceId)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .body(String.class);

            // If the device is online (e.g., JSON contains "online" or "connected"), return true
            return responseBody != null && (
                    responseBody.toLowerCase().contains("\"status\":\"online\"") ||
                            responseBody.toLowerCase().contains("\"connected\":true")
            );
        } catch (Exception e) {
            log.warn("Could not check TextBee device status. Assuming OFFLINE.", e);
            return false;
        }
    }

    // ========================================================================
    // RESILIENCE4J FALLBACK METHODS
    // ========================================================================

    public void sendCreatedFallback(SubscriptionCreatedEvent event, Throwable t) {
        log.warn("CIRCUIT BREAKER / RETRY FALLBACK: Could not deliver created notification for subscriptionId={}. Reason: {}",
                event.subscriptionId(), t.getMessage());
        throw new NotificationDispatchException("TextBee Provider fallback triggered after retries exhausted", t);
    }

    public void sendExpiringFallback(SubscriptionExpiringEvent event, Throwable t) {
        log.warn("CIRCUIT BREAKER / RETRY FALLBACK: Could not deliver expiring notification for subscriptionId={}. Reason: {}",
                event.subscriptionId(), t.getMessage());
        throw new NotificationDispatchException("TextBee Provider fallback triggered after retries exhausted", t);
    }
}