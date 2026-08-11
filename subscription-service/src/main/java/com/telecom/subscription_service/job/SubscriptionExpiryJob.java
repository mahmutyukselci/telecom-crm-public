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

@Component
@Slf4j
public class SubscriptionExpiryJob extends QuartzJobBean {

    private final SubscriptionRepository repository;
    private final SubscriptionProducer producer;

    private final int daysAhead;
    private final int bulkLimit;

    // Inject values from configuration, with defaults
    public SubscriptionExpiryJob(SubscriptionRepository repository,
                                 SubscriptionProducer producer,
                                 @Value("${subscription.expiry.days-ahead:3}") int daysAhead,
                                 @Value("${subscription.expiry.bulk-limit:1000}") int bulkLimit) {
        this.repository = repository;
        this.producer = producer;
        this.daysAhead = daysAhead;
        this.bulkLimit = bulkLimit;
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

        int totalSubs = expiringSubs.size();
        int currentHour = now.getHour();

        // Calculate the current run index (0 for 18:00, 1 for 19:00, ..., 4 for 22:00)
        int runIndex = (currentHour >= 18 && currentHour <= 22) ? (currentHour - 18) : 0;

        List<Subscription> chunkToProcess;

        if (totalSubs <= bulkLimit) {
            // If the total is below the limit, process everything at the first run (18:00)
            if (runIndex > 0) {
                log.info("⏭️ Total expiring subscriptions ({}) is under the limit ({}). Already processed at 18:00. Skipping this run.", totalSubs, bulkLimit);
                return;
            }
            chunkToProcess = expiringSubs;
            log.info("📦 Processing all {} subscriptions at once.", totalSubs);
        } else {
            // If the total exceeds the limit, divide it into 5 chunks for the 18:00 - 22:00 timeframe
            int totalRuns = 5;
            int chunkSize = (int) Math.ceil((double) totalSubs / totalRuns);
            int startIndex = runIndex * chunkSize;
            int endIndex = Math.min(startIndex + chunkSize, totalSubs);

            if (startIndex >= totalSubs) {
                log.info("⏭️ No more subscriptions to process in this chunk.");
                return;
            }

            chunkToProcess = expiringSubs.subList(startIndex, endIndex);
            log.info("🔪 Chunking enabled! Total: {}. Processing chunk {}/5 (Index {} to {}).", totalSubs, runIndex + 1, startIndex, endIndex - 1);
        }

        // Iterate through the calculated chunk and push events to Kafka
        for (Subscription sub : chunkToProcess) {
            // Deterministic Event ID to prevent duplicate processing (Idempotency fix)
            String deterministicEventId = "EXP-" + sub.getId() + "-" + startOfTargetDay.toLocalDate();

            SubscriptionExpiringEvent event = new SubscriptionExpiringEvent(
                    deterministicEventId,
                    sub.getId(),
                    sub.getCustomerId(),
                    sub.getTariffId()
            );
            producer.sendSubscriptionExpiringEvent(event);
        }

        log.info("✅ Successfully processed and pushed {} expiring subscriptions to Kafka.", chunkToProcess.size());
    }
}