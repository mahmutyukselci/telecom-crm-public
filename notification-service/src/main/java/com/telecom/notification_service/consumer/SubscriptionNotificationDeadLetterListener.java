package com.telecom.notification_service.consumer;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionNotificationDeadLetterListener {

	private static final Logger log = LoggerFactory.getLogger(SubscriptionNotificationDeadLetterListener.class);

	@KafkaListener(topics = "subscription-created-topic-dlt", groupId = "notification-service-dlt")
	public void onDeadLetter(
			SubscriptionCreatedEvent event,
			@Header(value = KafkaHeaders.DLT_EXCEPTION_FQCN, required = false) String exceptionClassName,
			@Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage
	) {
		log.error(
				"Message moved to DLT for subscriptionId={} customerId={} tariffId={} exceptionClass={} exceptionMessage={}",
				event.subscriptionId(),
				event.customerId(),
				event.tariffId(),
				exceptionClassName,
				exceptionMessage
		);
	}
}
