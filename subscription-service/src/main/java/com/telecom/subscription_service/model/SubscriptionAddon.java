package com.telecom.subscription_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_addons", schema = "subscription_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "subscription_id", nullable = false)
    private String subscriptionId;

    @Column(name = "tariff_id", nullable = false)
    private String tariffId;

    @Enumerated(EnumType.STRING)
    private AddonStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AddonStatus.ACTIVE;
        }
    }


}