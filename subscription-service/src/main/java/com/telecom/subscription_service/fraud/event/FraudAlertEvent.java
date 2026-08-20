package com.telecom.subscription_service.fraud.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published to 'fraud-alerts-topic' when high-risk anomalies are detected.
 */
public record FraudAlertEvent(
        String alertId,
        UUID subscriptionId,
        UUID customerId,
        String violationType,
        String description,
        int riskScore,
        LocalDateTime detectedAt
) {}
