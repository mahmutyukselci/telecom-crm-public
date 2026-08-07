package com.telecom.subscription_service.job;

import com.telecom.subscription_service.event.SubscriptionExpiringEvent;
import com.telecom.subscription_service.kafka.SubscriptionProducer;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryJob {

    private final SubscriptionRepository repository;
    private final SubscriptionProducer producer;

    @Value("${subscription.expiry.days-ahead:3}")
    private int daysAhead;

    // PRO TIP: In production, use "0 0 0 * * ?" (runs every midnight).
    // For development/testing convenience, it currently runs every minute.
    @Scheduled(cron = "0 0 18-22 * * ?")
    @SchedulerLock(name = "subscriptionExpiryJob", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void run() {
        log.info("⏰ Scanning for subscriptions expiring in {} days...", daysAhead);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfTargetDay = now.plusDays(daysAhead).with(LocalTime.MIN);
        LocalDateTime endOfTargetDay = now.plusDays(daysAhead).with(LocalTime.MAX);

        List<Subscription> expiringSubs = repository.findByStatusAndEndDateBetween(
                SubscriptionStatus.ACTIVE, startOfTargetDay, endOfTargetDay
        );

        if (expiringSubs.isEmpty()) {
            log.info("💤 No subscriptions found expiring on {}. Going back to sleep.",
                    startOfTargetDay.toLocalDate());
            return;
        }

        for (Subscription sub : expiringSubs) {
            String deterministicEventId = "EXP-" + sub.getId() + "-" + startOfTargetDay.toLocalDate();

            SubscriptionExpiringEvent event = new SubscriptionExpiringEvent(
                    deterministicEventId,
                    sub.getId(),
                    sub.getCustomerId(),
                    sub.getTariffId()
            );

            producer.sendSubscriptionExpiringEvent(event);
        }

        log.info("✅ Successfully processed and pushed {} expiring subscriptions to Kafka.",
                expiringSubs.size());
    }
}