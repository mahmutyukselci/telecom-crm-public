package com.telecom.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    String transactionId,
    String subscriptionId,
    BigDecimal amount,
    String status,
    LocalDateTime timestamp
) {}
