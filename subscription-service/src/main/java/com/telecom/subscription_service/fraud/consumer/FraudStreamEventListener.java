package com.telecom.subscription_service.fraud.consumer;

import com.telecom.subscription_service.fraud.engine.FraudDetectionEngine;
import com.telecom.subscription_service.fraud.event.FraudAlertEvent;
import com.telecom.subscription_service.fraud.event.SubscriptionActivityEvent;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Real-time event listener streaming subscription activity into the CEP fraud engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudStreamEventListener {

    private final FraudDetectionEngine fraudDetectionEngine;
    private final SubscriptionRepository subscriptionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "subscription-activity-topic",
            groupId = "telecom-fraud-detection-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onActivityEvent(SubscriptionActivityEvent event) {
        log.info("📊 [TELECOM TELEMETRY] Ingesting activity event: {} for customer: {}", event.actionType(), event.customerId());

        Optional<FraudAlertEvent> fraudAlert = fraudDetectionEngine.processEvent(event);

        fraudAlert.ifPresent(alert -> {
            log.error("🛑 [FRAUD ALERT DISPATCHED] Alert ID: {}, Score: {}, Reason: {}",
                    alert.alertId(), alert.riskScore(), alert.violationType());

            // 1. Publish to high-priority security topic for SOC monitoring
            kafkaTemplate.send("fraud-alerts-topic", alert.customerId().toString(), alert);

            // 2. Automated Safeguard: Suspend compromised subscription if risk score > 85
            if (alert.riskScore() >= 85 && alert.subscriptionId() != null) {
                subscriptionRepository.findById(alert.subscriptionId().toString()).ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.SUSPENDED);
                    subscriptionRepository.save(sub);
                    log.warn("🔒 [AUTOMATED SAFEGUARD] Subscription {} automatically suspended due to critical fraud risk!", sub.getId());
                });
            }
        });
    }
}
