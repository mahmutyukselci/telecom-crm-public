package com.telecom.subscription_service.eventsourcing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.subscription_service.eventsourcing.dto.ReconstitutedSubscriptionState;
import com.telecom.subscription_service.eventsourcing.model.SubscriptionDomainEvent;
import com.telecom.subscription_service.eventsourcing.repository.SubscriptionEventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing append-only event sourcing and point-in-time state reconstitution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEventSourcingService {

    private final SubscriptionEventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void appendEvent(UUID aggregateId, String eventType, Object payload, long nextVersion) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            SubscriptionDomainEvent event = SubscriptionDomainEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventPayload(payloadJson)
                    .version(nextVersion)
                    .occurredAt(LocalDateTime.now())
                    .build();

            eventStoreRepository.save(event);
            log.info("📝 [EVENT SOURCING] Appended immutable event '{}' (v{}) for aggregate: {}",
                    eventType, nextVersion, aggregateId);
        } catch (Exception e) {
            log.error("Failed to append event for aggregate: {}", aggregateId, e);
            throw new RuntimeException("Event append error", e);
        }
    }

    /**
     * Point-in-time state reconstitution:
     * Replays the event log chronologically up to pointInTime, rebuilding the exact aggregate state.
     */
    @Transactional(readOnly = true)
    public ReconstitutedSubscriptionState reconstituteStateAt(UUID aggregateId, LocalDateTime pointInTime) {
        List<SubscriptionDomainEvent> events = eventStoreRepository.findHistoryUpTo(aggregateId, pointInTime);

        if (events.isEmpty()) {
            throw new NoSuchElementException("No event history found for subscription: " + aggregateId + " at " + pointInTime);
        }

        UUID customerId = null;
        String tariffId = null;
        String status = "UNKNOWN";
        List<String> activeAddons = new ArrayList<>();
        long currentVersion = 0;

        for (SubscriptionDomainEvent event : events) {
            currentVersion = event.getVersion();
            try {
                Map<String, Object> payload = objectMapper.readValue(
                        event.getEventPayload(), new TypeReference<Map<String, Object>>() {});

                switch (event.getEventType()) {
                    case "SUBSCRIPTION_CREATED" -> {
                        customerId = payload.get("customerId") != null ? UUID.fromString((String) payload.get("customerId")) : null;
                        tariffId = (String) payload.get("tariffId");
                        status = "ACTIVE";
                    }
                    case "TARIFF_UPGRADED" -> tariffId = (String) payload.get("newTariffId");
                    case "ADDON_ATTACHED" -> {
                        String addonId = (String) payload.get("addonId");
                        if (addonId != null && !activeAddons.contains(addonId)) {
                            activeAddons.add(addonId);
                        }
                    }
                    case "ADDON_REMOVED" -> {
                        String addonId = (String) payload.get("addonId");
                        activeAddons.remove(addonId);
                    }
                    case "SUBSCRIPTION_SUSPENDED" -> status = "SUSPENDED";
                    case "SUBSCRIPTION_RESUMED" -> status = "ACTIVE";
                    case "SUBSCRIPTION_CANCELLED" -> status = "CANCELLED";
                }
            } catch (Exception e) {
                log.error("Error parsing event payload during state replay for event: {}", event.getId(), e);
            }
        }

        log.info("🔄 [STATE REPLAY] Successfully reconstituted state for {} at {} (Replayed {} events, Final Status: {})",
                aggregateId, pointInTime, events.size(), status);

        return new ReconstitutedSubscriptionState(
                aggregateId,
                customerId,
                tariffId,
                status,
                activeAddons,
                currentVersion,
                pointInTime,
                events.size()
        );
    }
}
