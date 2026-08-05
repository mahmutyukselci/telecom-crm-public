package com.telecom.subscription_service.event;

public record SubscriptionCreatedEvent(
        String eventId,
        String subscriptionId,
        String customerId,
        String tariffId,
        String status
) {
}