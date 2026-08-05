package com.telecom.subscription_service.dto;

import com.telecom.subscription_service.model.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String id;
    private String customerId;
    private String tariffId;
    private LocalDateTime startDate;
    private SubscriptionStatus status;
}