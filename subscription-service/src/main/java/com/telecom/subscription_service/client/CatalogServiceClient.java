package com.telecom.subscription_service.client;

import com.telecom.subscription_service.dto.TariffResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class CatalogServiceClient {

    private final RestClient restClient;


    public CatalogServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${CATALOG_SERVICE_URL:http://localhost:8083}") String catalogUrl) {

        this.restClient = restClientBuilder
                .baseUrl(catalogUrl)
                .build();
    }

    public TariffResponse getTariffById(String tariffId) {
        log.info("Fetching tariff details for ID: {}", tariffId);

        return restClient.get()
                .uri("/api/v1/tariffs/{id}", tariffId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("Tariff not found or invalid request: {}", response.getStatusCode());
                    throw new RuntimeException("Tariff not found with id: " + tariffId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("Catalog service unavailable: {}", response.getStatusCode());
                    throw new RuntimeException("Catalog service is currently unavailable");
                })
                .body(TariffResponse.class);
    }
}