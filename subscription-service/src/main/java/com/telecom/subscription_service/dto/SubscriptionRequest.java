package com.telecom.subscription_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;
    @NotBlank(message = "Tariff ID must not be blank")
    private String tariffId;
}