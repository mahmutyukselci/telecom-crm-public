package com.telecom.notification_service.event;

public record SubscriptionCreatedEvent(
		String eventId,
		String subscriptionId,
		String customerId,
		String tariffId,
		String status
) {
}
