// CatalogServiceClient.java
package com.telecom.subscription_service.client;

import com.telecom.subscription_service.dto.TariffResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", path = "/api/v1/tariffs", fallbackFactory = CatalogClientFallbackFactory.class)
public interface CatalogServiceClient {

    @GetMapping("/{id}")
    TariffResponse getTariffById(@PathVariable("id") String id);
}