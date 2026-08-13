package com.telecom.subscription_service.service;

import com.telecom.subscription_service.client.CatalogServiceClient;
import com.telecom.subscription_service.dto.*;
import com.telecom.subscription_service.event.SubscriptionCreatedEvent;
import com.telecom.subscription_service.mapper.SubscriptionMapper;
import com.telecom.subscription_service.model.AddonStatus;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionAddon;
import com.telecom.subscription_service.model.SubscriptionStatus;
import com.telecom.subscription_service.outbox.OutboxService;
import com.telecom.subscription_service.repository.SubscriptionAddonRepository;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

        private static final String TOPIC_SUBSCRIPTION_CREATED = "subscription-created-topic";
        private static final String TOPIC_SUBSCRIPTION_CANCELLED = "subscription-cancelled-topic";
        private static final String AGGREGATE_TYPE_SUBSCRIPTION = "SUBSCRIPTION";

        private final SubscriptionRepository subscriptionRepository;
        private final CatalogServiceClient catalogServiceClient;
        private final SubscriptionMapper subscriptionMapper;
        private final SubscriptionAddonRepository subscriptionAddonRepository;
        private final OutboxService outboxService;

        private final RuntimeService runtimeService;
        private final HistoryService historyService;

        @Transactional
        public SubscriptionResponse createSubscription(SubscriptionRequest request) {
                log.info("Initiating Flowable Workflow for Customer: {}", request.getCustomerId());

                // 1. Prepare variables to pass to the workflow engine
                Map<String, Object> variables = new HashMap<>();
                variables.put("customerId", request.getCustomerId());
                variables.put("tariffId", request.getTariffId());

                // 2. Start the BPMN process using its key (subscriptionCreationProcess)
                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                                "subscriptionCreationProcess",
                                request.getCustomerId(),
                                variables);

                // 3. Retrieve the generated subscription ID set in the delegate
                // (SaveSubscriptionDelegate)
                HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
                                .processInstanceId(processInstance.getId())
                                .variableName("subscriptionId")
                                .singleResult();

                if (historicVariable == null) {
                        throw new RuntimeException(
                                        "Workflow failed or subscriptionId was not generated in the workflow.");
                }

                String generatedSubscriptionId = (String) historicVariable.getValue();

                log.info("Workflow completed successfully. Generated Subscription ID: {}", generatedSubscriptionId);

                // 4. Fetch data from DB to return response and persist Outbox event
                Subscription savedSubscription = subscriptionRepository.findById(generatedSubscriptionId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Subscription not found after workflow execution"));

                // Prepare domain event payload
                SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                                UUID.randomUUID().toString(),
                                savedSubscription.getId(),
                                savedSubscription.getCustomerId(),
                                savedSubscription.getTariffId(),
                                savedSubscription.getStatus().name());

                // 5. Save event to Outbox table within the same DB transaction (Dual-Write
                // prevention)
                outboxService.saveEvent(
                                AGGREGATE_TYPE_SUBSCRIPTION,
                                savedSubscription.getId(),
                                "SUBSCRIPTION_CREATED",
                                TOPIC_SUBSCRIPTION_CREATED,
                                event);

                return subscriptionMapper.toResponse(savedSubscription);
        }

        public List<SubscriptionResponse> getAllSubscriptions() {
                return subscriptionRepository.findAll()
                                .stream()
                                .map(subscriptionMapper::toResponse)
                                .toList();
        }

        public SubscriptionResponse getSubscriptionById(String id) {
                Subscription subscription = subscriptionRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));
                return subscriptionMapper.toResponse(subscription);
        }

        @Transactional
        public void cancelSubscription(String id) {
                Subscription subscription = subscriptionRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));

                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(subscription);

                // Persist cancellation event to Outbox table
                SubscriptionCreatedEvent cancellationEvent = new SubscriptionCreatedEvent(
                                UUID.randomUUID().toString(),
                                subscription.getId(),
                                subscription.getCustomerId(),
                                subscription.getTariffId(),
                                SubscriptionStatus.CANCELLED.name());

                outboxService.saveEvent(
                                AGGREGATE_TYPE_SUBSCRIPTION,
                                subscription.getId(),
                                "SUBSCRIPTION_CANCELLED",
                                TOPIC_SUBSCRIPTION_CANCELLED,
                                cancellationEvent);

                log.info("Subscription cancelled and outbox event saved for ID: {}", id);
        }

        public void addAddon(String subscriptionId, String tariffId) {
                // 1. Perform external HTTP call OUTSIDE of database transaction
                // to prevent HikariCP connection pool holding and lock contention under high concurrency
                catalogServiceClient.getTariffById(tariffId);

                // 2. Perform database transaction in isolated transactional scope
                executeAddAddonTransaction(subscriptionId, tariffId);
        }

        @Transactional
        public void executeAddAddonTransaction(String subscriptionId, String tariffId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                        throw new RuntimeException("Subscription not active");
                }

                if (subscriptionAddonRepository.existsBySubscriptionIdAndTariffIdAndStatus(
                                subscriptionId, tariffId, AddonStatus.ACTIVE)) {
                        throw new RuntimeException("Addon already exists");
                }

                SubscriptionAddon addon = SubscriptionAddon.builder()
                                .subscriptionId(subscriptionId)
                                .tariffId(tariffId)
                                .status(AddonStatus.ACTIVE)
                                .build();

                subscriptionAddonRepository.save(addon);
        }

        @Transactional
        public void removeAddon(String addonId) {
                SubscriptionAddon addon = subscriptionAddonRepository.findById(addonId)
                                .orElseThrow(() -> new RuntimeException("Addon not found"));

                addon.setStatus(AddonStatus.CANCELLED);
                subscriptionAddonRepository.save(addon);
        }

        public List<SubscriptionAddonResponse> getAddonsBySubscriptionId(String subscriptionId) {
                subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                return subscriptionAddonRepository
                                .findBySubscriptionId(subscriptionId)
                                .stream()
                                .map(addon -> new SubscriptionAddonResponse(
                                                addon.getId(),
                                                addon.getSubscriptionId(),
                                                addon.getTariffId(),
                                                addon.getStatus().name()))
                                .toList();
        }

        public List<SubscriptionAddonResponse> getAddons(String subscriptionId, AddonStatus status) {
                List<SubscriptionAddon> addons;

                if (status != null) {
                        addons = subscriptionAddonRepository
                                        .findBySubscriptionIdAndStatus(subscriptionId, status);
                } else {
                        addons = subscriptionAddonRepository
                                        .findBySubscriptionId(subscriptionId);
                }

                return addons.stream()
                                .map(a -> new SubscriptionAddonResponse(
                                                a.getId(),
                                                a.getSubscriptionId(),
                                                a.getTariffId(),
                                                a.getStatus().name()))
                                .toList();
        }

        public List<SubscriptionResponse> getMySubscriptions(String keycloakUserId) {
                return subscriptionRepository.findByKeycloakUserId(keycloakUserId)
                                .stream()
                                .map(subscriptionMapper::toResponse)
                                .toList();
        }
}