package com.telecom.subscription_service.eventsourcing.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the calculated deterministic subscription state at an exact point in time.
 */
public record ReconstitutedSubscriptionState(
        UUID subscriptionId,
        UUID customerId,
        String tariffId,
        String status,
        List<String> activeAddons,
        long version,
        LocalDateTime asOfTime,
        int totalEventsReplayed
) {}
