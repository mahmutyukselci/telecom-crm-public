package com.telecom.subscription_service.fraud.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an active subscription operation occurs (e.g. SIM swap, tariff change, purchase).
 */
public record SubscriptionActivityEvent(
        String eventId,
        UUID subscriptionId,
        UUID customerId,
        String actionType,
        String ipAddress,
        String locationCity,
        double latitude,
        double longitude,
        LocalDateTime timestamp
) {}
