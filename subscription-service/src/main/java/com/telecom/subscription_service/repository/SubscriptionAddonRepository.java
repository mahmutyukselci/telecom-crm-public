package com.telecom.subscription_service.repository;

import com.telecom.subscription_service.model.AddonStatus;
import com.telecom.subscription_service.model.SubscriptionAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionAddonRepository
        extends JpaRepository<SubscriptionAddon, String> {

    List<SubscriptionAddon> findBySubscriptionId(String subscriptionId);

    boolean existsBySubscriptionIdAndTariffIdAndStatus(
            String subscriptionId,
            String TariffId,
            AddonStatus status
    );

    List<SubscriptionAddon> findBySubscriptionIdAndStatus(
            String subscriptionId,
            AddonStatus status
    );
}