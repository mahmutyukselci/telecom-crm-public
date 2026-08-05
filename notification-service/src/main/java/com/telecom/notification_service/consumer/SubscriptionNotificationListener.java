package com.telecom.notification_service.consumer;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import com.telecom.notification_service.model.ProcessedEvent;
import com.telecom.notification_service.repository.ProcessedEventRepository;
import com.telecom.notification_service.service.NotificationDeliveryService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SubscriptionNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(SubscriptionNotificationListener.class);

	private static final Duration REDIS_PROCESSING_TTL = Duration.ofHours(24);
	private static final Duration REDIS_DONE_TTL = Duration.ofDays(7);

	private final NotificationDeliveryService notificationDeliveryService;
	private final ProcessedEventRepository processedEventRepository;
	private final StringRedisTemplate redisTemplate;

	public SubscriptionNotificationListener(
			NotificationDeliveryService notificationDeliveryService,
			ProcessedEventRepository processedEventRepository,
			StringRedisTemplate redisTemplate) {
		this.notificationDeliveryService = notificationDeliveryService;
		this.processedEventRepository = processedEventRepository;
		this.redisTemplate = redisTemplate;
	}

	@RetryableTopic(
			attempts = "5",
			backOff = @BackOff(
					delay = 1000,
					multiplier = 2.0,
					maxDelay = 10000
			)
	)
	@Transactional(rollbackOn = Exception.class)
	@KafkaListener(topics = "subscription-created-topic", groupId = "notification-service")
	public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
		String eventId = event.eventId();
		String redisKey = "idempotency:notification:" + eventId;

		Boolean acquired = redisTemplate.opsForValue()
				.setIfAbsent(redisKey, "PROCESSING", REDIS_PROCESSING_TTL);

		if (!Boolean.TRUE.equals(acquired)) {
			log.warn("Duplicate detected in Redis, skipping. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());
			return;
		}

		try {
			if (processedEventRepository.existsById(eventId)) {
				log.warn("Duplicate detected in DB, skipping. eventId={} subscriptionId={}",
						eventId, event.subscriptionId());
				markDone(redisKey);
				return;
			}

			processedEventRepository.saveAndFlush(
					new ProcessedEvent(eventId, LocalDateTime.now())
			);

			notificationDeliveryService.sendSubscriptionCreatedNotification(event);

			markDone(redisKey);

			log.info("Processed subscription-created event successfully. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());

		} catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
			log.warn("Duplicate detected while saving to DB, skipping. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());

			markDone(redisKey);

		} catch (Exception e) {
			log.error("Processing failed, removing Redis lock for retry. eventId={} subscriptionId={}",
					eventId, event.subscriptionId(), e);

			redisTemplate.delete(redisKey);
			throw e;
		}
	}

	@RetryableTopic(
			attempts = "5",
			backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
	)
	@Transactional(rollbackOn = Exception.class)
	@KafkaListener(topics = "subscription-expiring-topic", groupId = "notification-service")
	public void onSubscriptionExpiring(SubscriptionExpiringEvent event) {
		String eventId = event.eventId();
		String redisKey = "idempotency:notification:" + eventId;

		Boolean acquired = redisTemplate.opsForValue()
				.setIfAbsent(redisKey, "PROCESSING", REDIS_PROCESSING_TTL);

		if (!Boolean.TRUE.equals(acquired)) {
			log.warn("Duplicate detected in Redis, skipping. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());
			return;
		}

		try {
			if (processedEventRepository.existsById(eventId)) {
				log.warn("Duplicate detected in DB, skipping. eventId={} subscriptionId={}",
						eventId, event.subscriptionId());
				markDone(redisKey);
				return;
			}

			processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, LocalDateTime.now()));
			notificationDeliveryService.sendSubscriptionExpiringNotification(event);
			markDone(redisKey);

			log.info("Processed subscription-expiring event successfully. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());

		} catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
			log.warn("Duplicate detected while saving to DB, skipping. eventId={} subscriptionId={}",
					eventId, event.subscriptionId());
			markDone(redisKey);

		} catch (Exception e) {
			log.error("Processing failed, removing Redis lock for retry. eventId={} subscriptionId={}",
					eventId, event.subscriptionId(), e);
			redisTemplate.delete(redisKey);
			throw e;
		}
	}

	private void markDone(String redisKey) {
		redisTemplate.opsForValue().set(redisKey, "DONE", REDIS_DONE_TTL);
	}
}