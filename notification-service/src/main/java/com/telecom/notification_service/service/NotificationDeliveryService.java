package com.telecom.notification_service.service;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.provider.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

	private final NotificationProvider notificationProvider;

	public NotificationDeliveryService(NotificationProvider notificationProvider) {
		this.notificationProvider = notificationProvider;
	}

	@Retry(name = "notificationProvider")
	@CircuitBreaker(name = "notificationProvider")
	public void sendSubscriptionCreatedNotification(SubscriptionCreatedEvent event) {
		log.info(
				"Dispatching subscription notification for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);
		notificationProvider.sendCreated(event);
	}

	@Retry(name = "notificationProvider")
	@CircuitBreaker(name = "notificationProvider")
	public void sendSubscriptionExpiringNotification(SubscriptionExpiringEvent event) {
		log.info(
				"Dispatching subscription EXPIRING notification for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);
		notificationProvider.sendExpiring(event);
	}
}
