package com.telecom.subscription_service.repository;

import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    List<Subscription> findByKeycloakUserId(String keycloakUserId);
    // Fetches active subscriptions where the end date falls strictly within the given start and end timestamps
    List<Subscription>
    findByStatusAndEndDateBetween
            (SubscriptionStatus status, LocalDateTime start, LocalDateTime end);
}