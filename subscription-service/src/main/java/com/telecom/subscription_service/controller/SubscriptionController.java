package com.telecom.subscription_service.controller;

import com.telecom.subscription_service.dto.SubscriptionAddonResponse;
import com.telecom.subscription_service.dto.SubscriptionRequest;
import com.telecom.subscription_service.dto.SubscriptionResponse;
import com.telecom.subscription_service.model.AddonStatus;
import com.telecom.subscription_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwnerOfCustomer(#request.customerId, authentication))")
    public ResponseEntity<SubscriptionResponse> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable String id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<Void> cancelSubscription(@PathVariable String id) {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/addons")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<Void> addAddon(
            @Valid @PathVariable String id,
            @Valid @RequestParam String tariffId) {

        subscriptionService.addAddon(id, tariffId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/addons/{addonId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwnerOfAddon(#addonId, authentication))")
    public ResponseEntity<Void> removeAddon(@PathVariable String addonId) {
        subscriptionService.removeAddon(addonId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/addons")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and " +
            "@subscriptionSecurityRules.isOwner(#id, authentication))")
    public ResponseEntity<List<SubscriptionAddonResponse>> getAddons(
            @PathVariable String id,
            @RequestParam(required = false) AddonStatus status) {

        return ResponseEntity.ok(subscriptionService.getAddons(id, status));
    }


}