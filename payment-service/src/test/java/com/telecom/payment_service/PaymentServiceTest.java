package com.telecom.payment_service;

import com.telecom.payment_service.dto.PaymentRequest;
import com.telecom.payment_service.dto.PaymentResponse;
import com.telecom.payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private final PaymentService paymentService = new PaymentService();

    @Test
    @DisplayName("processPayment: Should return SUCCESS status and valid transaction ID for prompt payment")
    void processPayment_shouldReturnSuccess() {
        PaymentRequest request = new PaymentRequest("sub-12345", new BigDecimal("199.99"), false);

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.subscriptionId()).isEqualTo("sub-12345");
        assertThat(response.amount()).isEqualByComparingTo("199.99");
        assertThat(response.transactionId()).startsWith("TXN-");
        assertThat(response.timestamp()).isNotNull();
    }
}
