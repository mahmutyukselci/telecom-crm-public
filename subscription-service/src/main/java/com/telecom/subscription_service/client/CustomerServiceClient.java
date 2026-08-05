// CustomerServiceClient.java
package com.telecom.subscription_service.client;

import com.telecom.subscription_service.dto.CustomerIdentityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", fallbackFactory = CustomerClientFallbackFactory.class)
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{id}/identity")
    CustomerIdentityResponse getCustomerIdentity(@PathVariable("id") String id);
}