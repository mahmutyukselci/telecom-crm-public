package com.telecom.payment_service.dto;

import java.math.BigDecimal;

public record PaymentRequest(
    String subscriptionId,
    String customerId,
    String promoCode,
    BigDecimal amount,
    boolean simulateDelay
) {}
