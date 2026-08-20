package com.telecom.subscription_service.eventsourcing.controller;

import com.telecom.subscription_service.eventsourcing.dto.ReconstitutedSubscriptionState;
import com.telecom.subscription_service.eventsourcing.service.SubscriptionEventSourcingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST API for querying historical subscription state via Event Sourcing replay.
 */
@RestController
@RequestMapping("/api/v1/subscription-audit")
@RequiredArgsConstructor
@Tag(name = "Event Sourcing Audit API", description = "Endpoints for point-in-time state reconstitution and audit trail")
public class SubscriptionEventSourcingController {

    private final SubscriptionEventSourcingService eventSourcingService;

    @GetMapping("/{subscriptionId}/state-at")
    @Operation(summary = "Reconstitute exact subscription state at a specific historical timestamp (Point-in-Time replay)")
    public ResponseEntity<ReconstitutedSubscriptionState> getStateAt(
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {

        LocalDateTime targetTime = (timestamp != null) ? timestamp : LocalDateTime.now();
        ReconstitutedSubscriptionState state = eventSourcingService.reconstituteStateAt(subscriptionId, targetTime);
        return ResponseEntity.ok(state);
    }
}
