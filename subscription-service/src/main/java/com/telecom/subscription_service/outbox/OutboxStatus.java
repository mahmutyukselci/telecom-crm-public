package com.telecom.subscription_service.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}