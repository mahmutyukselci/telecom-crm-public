package com.telecom.subscription_service;

import com.telecom.subscription_service.client.CatalogServiceClient;
import com.telecom.subscription_service.client.CustomerServiceClient;
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
import com.telecom.subscription_service.service.SubscriptionService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionAddonRepository subscriptionAddonRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("createSubscription: Should start workflow, save Outbox event, and return response")
    void createSubscription_shouldExecuteWorkflowAndSaveOutboxEvent() {
        // Given
        SubscriptionRequest request = new SubscriptionRequest("cust-100", "tar-200");
        String processId = "proc-inst-01";
        String generatedSubId = "sub-uuid-123";

        ProcessInstance mockProcessInstance = mock(ProcessInstance.class);
        when(mockProcessInstance.getId()).thenReturn(processId);
        when(runtimeService.startProcessInstanceByKey(eq("subscriptionCreationProcess"), eq("cust-100"), any()))
                .thenReturn(mockProcessInstance);

        HistoricVariableInstanceQuery mockQuery = mock(HistoricVariableInstanceQuery.class);
        HistoricVariableInstance mockVariable = mock(HistoricVariableInstance.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(mockQuery);
        when(mockQuery.processInstanceId(processId)).thenReturn(mockQuery);
        when(mockQuery.variableName("subscriptionId")).thenReturn(mockQuery);
        when(mockQuery.singleResult()).thenReturn(mockVariable);
        when(mockVariable.getValue()).thenReturn(generatedSubId);

        Subscription savedSubscription = Subscription.builder()
                .id(generatedSubId)
                .customerId("cust-100")
                .tariffId("tar-200")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findById(generatedSubId)).thenReturn(Optional.of(savedSubscription));

        SubscriptionResponse expectedResponse = new SubscriptionResponse(
                generatedSubId, "cust-100", "tar-200", LocalDateTime.now(), SubscriptionStatus.ACTIVE
        );
        when(subscriptionMapper.toResponse(savedSubscription)).thenReturn(expectedResponse);

        // When
        SubscriptionResponse actualResponse = subscriptionService.createSubscription(request);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getId()).isEqualTo(generatedSubId);

        verify(outboxService, times(1)).saveEvent(
                eq("SUBSCRIPTION"),
                eq(generatedSubId),
                eq("SUBSCRIPTION_CREATED"),
                eq("subscription-created-topic"),
                any(SubscriptionCreatedEvent.class)
        );
        verify(runtimeService, times(1)).startProcessInstanceByKey(anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("createSubscription: Should throw exception when workflow fails to generate subscriptionId")
    void createSubscription_whenWorkflowVariableMissing_shouldThrowException() {
        // Given
        SubscriptionRequest request = new SubscriptionRequest("cust-100", "tar-200");
        String processId = "proc-inst-error";

        ProcessInstance mockProcessInstance = mock(ProcessInstance.class);
        when(mockProcessInstance.getId()).thenReturn(processId);
        when(runtimeService.startProcessInstanceByKey(anyString(), anyString(), any())).thenReturn(mockProcessInstance);

        HistoricVariableInstanceQuery mockQuery = mock(HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(mockQuery);
        when(mockQuery.processInstanceId(processId)).thenReturn(mockQuery);
        when(mockQuery.variableName("subscriptionId")).thenReturn(mockQuery);
        when(mockQuery.singleResult()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Workflow failed or subscriptionId was not generated");

        verifyNoInteractions(outboxService);
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("getSubscriptionById: Should return response when subscription exists")
    void getSubscriptionById_whenExists_shouldReturnResponse() {
        // Given
        String subId = "sub-100";
        Subscription subscription = Subscription.builder().id(subId).customerId("cust-1").build();
        SubscriptionResponse expected = new
                SubscriptionResponse(subId, "cust-1", "tar-1", null, SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));
        when(subscriptionMapper.toResponse(subscription)).thenReturn(expected);

        // When
        SubscriptionResponse actual = subscriptionService.getSubscriptionById(subId);

        // Then
        assertThat(actual.getId()).isEqualTo(subId);
        verify(subscriptionRepository, times(1)).findById(subId);
    }

    @Test
    @DisplayName("getSubscriptionById: Should throw exception when subscription does not exist")
    void getSubscriptionById_whenNotFound_shouldThrowException() {
        // Given
        when(subscriptionRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionById("invalid-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Subscription not found with id: invalid-id");
    }

    @Test
    @DisplayName("getMySubscriptions: Should return subscriptions matching Keycloak user ID")
    void getMySubscriptions_shouldReturnListForKeycloakUser() {
        // Given
        String keycloakUserId = "kc-user-500";
        Subscription sub1 = Subscription.builder().id("sub-1").keycloakUserId(keycloakUserId).build();
        Subscription sub2 = Subscription.builder().id("sub-2").keycloakUserId(keycloakUserId).build();

        when(subscriptionRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(List.of(sub1, sub2));
        when(subscriptionMapper.toResponse(any())).thenReturn(mock(SubscriptionResponse.class));

        // When
        List<SubscriptionResponse> results = subscriptionService.getMySubscriptions(keycloakUserId);

        // Then
        assertThat(results).hasSize(2);
        verify(subscriptionRepository, times(1)).findByKeycloakUserId(keycloakUserId);
    }

    @Test
    @DisplayName("cancelSubscription: Should update subscription status to CANCELLED and record Outbox event")
    void cancelSubscription_whenExists_shouldSetStatusToCancelledAndSaveOutboxEvent() {
        // Given
        String subId = "sub-100";
        Subscription activeSub = Subscription.builder()
                .id(subId)
                .customerId("cust-1")
                .tariffId("tar-1")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSub));

        // When
        subscriptionService.cancelSubscription(subId);

        // Then
        assertThat(activeSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(subscriptionRepository, times(1)).save(activeSub);
        verify(outboxService, times(1)).saveEvent(
                eq("SUBSCRIPTION"),
                eq(subId),
                eq("SUBSCRIPTION_CANCELLED"),
                eq("subscription-cancelled-topic"),
                any(SubscriptionCreatedEvent.class)
        );
    }

    @Test
    @DisplayName("cancelSubscription: Should throw exception when ID does not exist")
    void cancelSubscription_whenNotFound_shouldThrowException() {
        // Given
        when(subscriptionRepository.findById("missing-id")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription("missing-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Subscription not found");
        verify(subscriptionRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("addAddon: Should successfully save addon when subscription is ACTIVE and addon is new")
    void addAddon_whenValid_shouldSaveAddon() {
        // Given
        String subId = "sub-100";
        String tariffId = "addon-tariff-50";
        Subscription activeSub = Subscription.builder().id(subId).status(SubscriptionStatus.ACTIVE).build();

        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSub));
        when(subscriptionAddonRepository.existsBySubscriptionIdAndTariffIdAndStatus(subId, tariffId, AddonStatus.ACTIVE))
                .thenReturn(false);
        when(catalogServiceClient.getTariffById(tariffId))
                .thenReturn(new TariffResponse(
                        tariffId,
                        "5GB Booster",
                        "Extra 5GB data package",
                        new BigDecimal("50.00"),
                        5,
                        0,
                        0,
                        true,
                        30
                ));

        // When
        subscriptionService.addAddon(subId, tariffId);

        // Then
        verify(subscriptionAddonRepository, times(1)).save(any(SubscriptionAddon.class));
        verify(catalogServiceClient, times(1)).getTariffById(tariffId);
    }

    @Test
    @DisplayName("addAddon: Should throw exception when subscription is NOT ACTIVE")
    void addAddon_whenSubscriptionNotActive_shouldThrowException() {
        // Given
        String subId = "sub-101";
        Subscription cancelledSub = Subscription.builder().id(subId).status(SubscriptionStatus.CANCELLED).build();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(cancelledSub));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.addAddon(subId, "tariff-500"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Subscription not active");

        verify(catalogServiceClient, times(1)).getTariffById("tariff-500");
        verify(subscriptionAddonRepository, never()).save(any());
    }

    @Test
    @DisplayName("addAddon: Should throw exception when addon already exists with ACTIVE status")
    void addAddon_whenAddonAlreadyExists_shouldThrowException() {
        // Given
        String subId = "sub-102";
        String tariffId = "tariff-500";
        Subscription activeSub = Subscription.builder().id(subId).status(SubscriptionStatus.ACTIVE).build();

        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(activeSub));
        when(subscriptionAddonRepository.existsBySubscriptionIdAndTariffIdAndStatus(subId, tariffId, AddonStatus.ACTIVE))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.addAddon(subId, tariffId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Addon already exists");

        verify(catalogServiceClient, times(1)).getTariffById(tariffId);
        verify(subscriptionAddonRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeAddon: Should set addon status to CANCELLED when addon exists")
    void removeAddon_whenExists_shouldCancelAddon() {
        // Given
        String addonId = "addon-uuid-01";
        SubscriptionAddon activeAddon = SubscriptionAddon.builder()
                .id(addonId)
                .status(AddonStatus.ACTIVE)
                .build();
        when(subscriptionAddonRepository.findById(addonId)).thenReturn(Optional.of(activeAddon));

        // When
        subscriptionService.removeAddon(addonId);

        // Then
        assertThat(activeAddon.getStatus()).isEqualTo(AddonStatus.CANCELLED);
        verify(subscriptionAddonRepository, times(1)).save(activeAddon);
    }
}