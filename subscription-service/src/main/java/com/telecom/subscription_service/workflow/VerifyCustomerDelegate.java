package com.telecom.subscription_service.workflow;

import com.telecom.subscription_service.client.CustomerServiceClient;
import com.telecom.subscription_service.dto.CustomerIdentityResponse;
import com.telecom.subscription_service.exception.CustomerNotFoundException;
import com.telecom.subscription_service.exception.CustomerServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("verifyCustomerDelegate")
@RequiredArgsConstructor
@Slf4j
public class VerifyCustomerDelegate implements JavaDelegate {

    private final CustomerServiceClient customerServiceClient;

    @Override
    public void execute(DelegateExecution execution) {
        String customerId = (String) execution.getVariable("customerId");
        log.info("⚙️ [Workflow] Executing VerifyCustomerDelegate for Customer ID: {}", customerId);

        CustomerIdentityResponse identity;
        try {
            identity = customerServiceClient.getCustomerIdentity(customerId);
        } catch (CustomerNotFoundException e) {
            throw new BpmnError("CUSTOMER_NOT_FOUND", e.getMessage());
        } catch (CustomerServiceUnavailableException e) {
            throw new BpmnError("CUSTOMER_SERVICE_UNAVAILABLE", e.getMessage());
        }

        execution.setVariable("customerKeycloakUserId", identity.keycloakUserId());
        log.info("✅ [Workflow] Customer verified successfully.");
    }
}