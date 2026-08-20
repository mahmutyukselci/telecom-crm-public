package com.telecom.subscription_service.eventsourcing.repository;

import com.telecom.subscription_service.eventsourcing.model.SubscriptionDomainEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository accessing the partitioned subscription event store.
 */
@Repository
public interface SubscriptionEventStoreRepository extends JpaRepository<SubscriptionDomainEvent, SubscriptionDomainEvent.EventId> {

    /**
     * Fetches all sequential events for an aggregate up to a specified historical point in time.
     * Takes advantage of PostgreSQL Partition Pruning by indexing (aggregate_id, occurred_at).
     */
    @Query("SELECT e FROM SubscriptionDomainEvent e WHERE e.aggregateId = :aggregateId AND e.occurredAt <= :pointInTime ORDER BY e.version ASC")
    List<SubscriptionDomainEvent> findHistoryUpTo(
            @Param("aggregateId") UUID aggregateId,
            @Param("pointInTime") LocalDateTime pointInTime
    );

    List<SubscriptionDomainEvent> findByAggregateIdOrderByVersionAsc(UUID aggregateId);
}
