package com.telecom.subscription_service.dto;

public record SubscriptionAddonResponse(
        String id,
        String subscriptionId,
        String tariffId,
        String status
) {}