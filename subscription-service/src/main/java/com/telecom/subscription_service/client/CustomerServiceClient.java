package com.telecom.subscription_service.client;

import com.telecom.subscription_service.dto.CustomerIdentityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class CustomerServiceClient {

    private final RestClient restClient;

    public CustomerServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${CUSTOMER_SERVICE_URL:http://localhost:8084}") String customerUrl) {

        this.restClient = restClientBuilder
                .baseUrl(customerUrl)
                .build();
    }

    public CustomerIdentityResponse getCustomerIdentity(String id) {
        log.info("Fetching customer identity details for ID: {}", id);

        return restClient.get()
                .uri("/api/v1/customers/{id}/identity", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("Customer identity not found or invalid request for ID {}: {}", id, response.getStatusCode());
                    throw new RuntimeException("Customer identity not found with id: " + id);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("Customer service unavailable when fetching identity for ID {}: {}", id, response.getStatusCode());
                    throw new RuntimeException("Customer service is currently unavailable");
                })
                .body(CustomerIdentityResponse.class);
    }
}