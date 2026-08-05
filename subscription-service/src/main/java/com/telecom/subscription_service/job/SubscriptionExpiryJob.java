package com.telecom.subscription_service.job;

import com.telecom.subscription_service.event.SubscriptionExpiringEvent;
import com.telecom.subscription_service.kafka.SubscriptionProducer;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class SubscriptionExpiryJob extends QuartzJobBean {

    private final SubscriptionRepository repository;
    private final SubscriptionProducer producer;
    private final int daysAhead;

    // Inject the value from configuration, default to 3 if the property is missing
    public SubscriptionExpiryJob(SubscriptionRepository repository,
                                 SubscriptionProducer producer,
                                 @Value("${subscription.expiry.days-ahead:3}") int daysAhead) {
        this.repository = repository;
        this.producer = producer;
        this.daysAhead = daysAhead;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("⏰ Running Quartz Job: Scanning for subscriptions expiring in {} days...", daysAhead);

        LocalDateTime now = LocalDateTime.now();

        // Define the precise boundary for the target day (00:00:00 to 23:59:59)
        LocalDateTime startOfTargetDay = now.plusDays(daysAhead).with(LocalTime.MIN);
        LocalDateTime endOfTargetDay = now.plusDays(daysAhead).with(LocalTime.MAX);

        List<Subscription> expiringSubs = repository.findByStatusAndEndDateBetween(
                SubscriptionStatus.ACTIVE, startOfTargetDay, endOfTargetDay
        );

        if (expiringSubs.isEmpty()) {
            log.info("💤 No subscriptions found expiring on {}. Going back to sleep.", startOfTargetDay.toLocalDate());
            return;
        }

        // Iterate through expiring subscriptions and push events to Kafka
        for (Subscription sub : expiringSubs) {
            SubscriptionExpiringEvent event = new SubscriptionExpiringEvent(
                    UUID.randomUUID().toString(),
                    sub.getId(), sub.getCustomerId(), sub.getTariffId()
            );
            producer.sendSubscriptionExpiringEvent(event);
        }

        log.info("✅ Successfully processed and pushed {} expiring subscriptions to Kafka.", expiringSubs.size());
    }
}