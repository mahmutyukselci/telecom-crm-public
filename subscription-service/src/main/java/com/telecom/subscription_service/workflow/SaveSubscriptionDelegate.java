package com.telecom.subscription_service.workflow;

import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("saveSubscriptionDelegate")
@RequiredArgsConstructor
@Slf4j
public class SaveSubscriptionDelegate implements JavaDelegate {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public void execute(DelegateExecution execution) {
        String customerId = (String) execution.getVariable("customerId");
        String tariffId = (String) execution.getVariable("tariffId");
        Integer validityDays = (Integer) execution.getVariable("tariffValidityDays");

        log.info("⚙️ [Workflow] Executing SaveSubscriptionDelegate...");

        LocalDateTime start = LocalDateTime.now();

        String customerKeycloakUserId = (String) execution.getVariable("customerKeycloakUserId");

        Subscription subscription = Subscription.builder()
                .customerId(customerId)
                .tariffId(tariffId)
                .keycloakUserId(customerKeycloakUserId)
                .startDate(start)
                .endDate(validityDays != null ? start.plusDays(validityDays) : null)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        execution.setVariable("subscriptionId", savedSubscription.getId());

        log.info("✅ [Workflow] Subscription saved to DB with ID: {} (endDate={})",
                savedSubscription.getId(), savedSubscription.getEndDate());
    }
}