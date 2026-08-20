package com.telecom.subscription_service.fraud.controller;

import com.telecom.subscription_service.fraud.engine.FraudDetectionEngine;
import com.telecom.subscription_service.fraud.event.FraudAlertEvent;
import com.telecom.subscription_service.fraud.event.SubscriptionActivityEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST API for telemetry simulation and real-time fraud diagnostics.
 */
@RestController
@RequestMapping("/api/v1/fraud-diagnostics")
@RequiredArgsConstructor
@Tag(name = "Fraud Diagnostics API", description = "Endpoints for analyzing stream activity and CEP fraud detection")
public class FraudManagementController {

    private final FraudDetectionEngine fraudDetectionEngine;

    @PostMapping("/evaluate-activity")
    @Operation(summary = "Simulate and evaluate a subscription activity event against the CEP sliding window")
    public ResponseEntity<FraudAlertEvent> evaluateEvent(@RequestBody SubscriptionActivityEvent event) {
        Optional<FraudAlertEvent> alert = fraudDetectionEngine.processEvent(event);
        return alert.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
