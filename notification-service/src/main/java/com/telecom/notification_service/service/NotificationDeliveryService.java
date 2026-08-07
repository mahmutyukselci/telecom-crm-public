package com.telecom.notification_service.service;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.provider.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

	// Injected as a List to support multi-channel dispatch (e.g., SMS, Email)
	private final List<NotificationProvider> notificationProviders;

	public NotificationDeliveryService(List<NotificationProvider> notificationProviders) {
		this.notificationProviders = notificationProviders;
	}

	@Retry(name = "notificationProvider")
	@CircuitBreaker(name = "notificationProvider")
	@RateLimiter(name = "notificationProvider")
	public void sendSubscriptionCreatedNotification(SubscriptionCreatedEvent event) {
		log.info(
				"Dispatching subscription CREATED notification for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);

		// Iterate through all active notification channels (e.g., SMS and Email)
		for (NotificationProvider provider : notificationProviders) {
			try {
				provider.sendCreated(event);
			} catch (Exception e) {
				log.error("Failed to dispatch CREATED notification via provider [{}]: {}",
						provider.getClass().getSimpleName(), e.getMessage());
				// Rethrowing the exception triggers the Resilience4j Retry mechanism
				throw e;
			}
		}
	}

	@Retry(name = "notificationProvider")
	@CircuitBreaker(name = "notificationProvider")
	@RateLimiter(name = "notificationProvider")
	public void sendSubscriptionExpiringNotification(SubscriptionExpiringEvent event) {
		log.info(
				"Dispatching subscription EXPIRING notification for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);

		// Iterate through all active notification channels (e.g., SMS and Email)
		for (NotificationProvider provider : notificationProviders) {
			try {
				provider.sendExpiring(event);
			} catch (Exception e) {
				log.error("Failed to dispatch EXPIRING notification via provider [{}]: {}",
						provider.getClass().getSimpleName(), e.getMessage());
				// Rethrowing the exception triggers the Resilience4j Retry mechanism
				throw e;
			}
		}
	}
}