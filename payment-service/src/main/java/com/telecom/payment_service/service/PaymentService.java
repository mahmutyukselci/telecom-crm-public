package com.telecom.payment_service.service;

import com.telecom.payment_service.dto.PaymentRequest;
import com.telecom.payment_service.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing simulated payment for subscription: {}, amount: {}", request.subscriptionId(), request.amount());

        if (request.simulateDelay()) {
            try {
                log.info("Simulating network delay / bank timeout (15 seconds delay)...");
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Simulated Bank POS transaction completed successfully: {}", transactionId);

        return new PaymentResponse(
            transactionId,
            request.subscriptionId(),
            request.amount(),
            "SUCCESS",
            LocalDateTime.now()
        );
    }
}
