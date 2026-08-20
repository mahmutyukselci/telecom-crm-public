package com.telecom.subscription_service.eventsourcing.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable Event Store JPA entity mapped to the range-partitioned 'subscription_event_store' table.
 */
@Entity
@Table(name = "subscription_event_store")
@IdClass(SubscriptionDomainEvent.EventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDomainEvent {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Id
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "version", nullable = false)
    private long version;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventId implements Serializable {
        private UUID id;
        private LocalDateTime occurredAt;
    }
}
