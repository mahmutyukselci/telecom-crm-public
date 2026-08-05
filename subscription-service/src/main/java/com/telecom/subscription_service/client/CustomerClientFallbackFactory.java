package com.telecom.subscription_service.client;

import com.telecom.subscription_service.exception.CustomerNotFoundException;
import com.telecom.subscription_service.exception.CustomerServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerServiceClient> {

    @Override
    public CustomerServiceClient create(Throwable cause) {
        return customerId -> {
            if (cause instanceof FeignException.NotFound) {
                log.warn("Customer not found: {}", customerId);
                throw new CustomerNotFoundException(customerId);
            }
            log.error("Customer Service unavailable while fetching customerId={}", customerId, cause);
            throw new CustomerServiceUnavailableException(customerId, cause);
        };
    }
}