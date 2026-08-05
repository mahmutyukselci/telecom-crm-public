package com.telecom.notification_service.provider;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.exception.NotificationDispatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockNotificationProvider implements NotificationProvider {

	private static final Logger log = LoggerFactory.getLogger(MockNotificationProvider.class);

	private final boolean fail;
	private final long latencyMs;

	public MockNotificationProvider(
			@Value("${notification.mock.fail:false}") boolean fail,
			@Value("${notification.mock.latency-ms:0}") long latencyMs
	) {
		this.fail = fail;
		this.latencyMs = latencyMs;
	}

	@Override
	public void sendCreated(SubscriptionCreatedEvent event) {
		simulateExternalCall();

		if (fail) {
			throw new NotificationDispatchException(
					"Mock notification provider failed for subscriptionId=" + event.subscriptionId()
			);
		}

		log.info(
				"Mock SMS sent (SUBSCRIPTION CREATED) for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);
	}

	@Override
	public void sendExpiring(SubscriptionExpiringEvent event) {
		simulateExternalCall();

		if (fail) {
			throw new NotificationDispatchException(
					"Mock notification provider failed for subscriptionId=" + event.subscriptionId()
			);
		}

		log.info(
				"Mock SMS sent (SUBSCRIPTION EXPIRING) for subscriptionId={} customerId={} tariffId={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId()
		);
	}

	private void simulateExternalCall() {
		if (latencyMs <= 0) {
			return;
		}

		try {
			Thread.sleep(latencyMs);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new NotificationDispatchException("Mock notification provider was interrupted", exception);
		}
	}
}