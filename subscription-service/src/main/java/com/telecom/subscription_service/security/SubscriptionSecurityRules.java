package com.telecom.subscription_service.security;

import com.telecom.subscription_service.client.CustomerServiceClient;
import com.telecom.subscription_service.dto.CustomerIdentityResponse;
import com.telecom.subscription_service.model.Subscription;
import com.telecom.subscription_service.repository.SubscriptionAddonRepository;
import com.telecom.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("subscriptionSecurityRules")
@RequiredArgsConstructor
public class SubscriptionSecurityRules {

    private final SubscriptionRepository subscriptionRepository;

    public boolean isOwner(String subscriptionId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        String currentKeycloakUserId = jwt.getSubject();

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);

        return subscriptionOpt
                .map(subscription -> currentKeycloakUserId.equals(subscription.getKeycloakUserId()))
                .orElse(false);
    }

    private final SubscriptionAddonRepository subscriptionAddonRepository;

    public boolean isOwnerOfAddon(String addonId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return false;
        }
        return subscriptionAddonRepository.findById(addonId)
                .map(addon -> isOwner(addon.getSubscriptionId(), authentication))
                .orElse(false);
    }

    private final CustomerServiceClient customerServiceClient;

    public boolean isOwnerOfCustomer(String customerId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        String currentKeycloakUserId = jwt.getSubject();
        try {
            CustomerIdentityResponse identity = customerServiceClient.getCustomerIdentity(customerId);
            return currentKeycloakUserId.equals(identity.keycloakUserId());
        } catch (Exception e) {
            return false; // customer bulunamadı ya da servis erişilemedi -> reddet
        }
    }
}