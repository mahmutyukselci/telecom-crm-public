package com.telecom.customer_service.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}