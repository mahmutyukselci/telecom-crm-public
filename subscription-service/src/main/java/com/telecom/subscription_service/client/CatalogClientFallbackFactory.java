package com.telecom.subscription_service.client;

import com.telecom.subscription_service.exception.CatalogServiceUnavailableException;
import com.telecom.subscription_service.exception.TariffNotFoundException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogClientFallbackFactory implements FallbackFactory<CatalogServiceClient> {

    @Override
    public CatalogServiceClient create(Throwable cause) {
        return tariffId -> {
            if (cause instanceof FeignException.NotFound) {
                log.warn("Tariff not found: {}", tariffId);
                throw new TariffNotFoundException(tariffId);
            }
            log.error("Catalog Service unavailable while fetching tariffId={}", tariffId, cause);
            throw new CatalogServiceUnavailableException(tariffId, cause);
        };
    }
}