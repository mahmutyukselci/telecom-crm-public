package com.telecom.subscription_service.kafka;

import com.telecom.subscription_service.event.SubscriptionCreatedEvent;
import com.telecom.subscription_service.event.SubscriptionExpiringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSubscriptionCreatedEvent(SubscriptionCreatedEvent event) {
        log.info("Publishing subscription created event for Sub ID: {}", event.subscriptionId());

        Message<SubscriptionCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "subscription-created-topic")
                .build();

        kafkaTemplate.send(message);
    }
    // Publishes an event to Kafka when a subscription is nearing its expiration date
    public void sendSubscriptionExpiringEvent(SubscriptionExpiringEvent event) {
        log.info("📢 Publishing subscription expiring event for Sub ID: {}", event.subscriptionId());
        kafkaTemplate.send("subscription-expiring-topic", event);
    }
}