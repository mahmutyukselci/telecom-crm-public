package com.telecom.notification_service.event;

public record SubscriptionExpiringEvent(
        String eventId,
        String subscriptionId,
        String customerId,
        String tariffId
) {}