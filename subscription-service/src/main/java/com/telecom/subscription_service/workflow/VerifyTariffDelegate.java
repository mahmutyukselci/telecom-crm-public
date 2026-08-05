package com.telecom.subscription_service.workflow;

import com.telecom.subscription_service.client.CatalogServiceClient;
import com.telecom.subscription_service.dto.TariffResponse;
import com.telecom.subscription_service.exception.CatalogServiceUnavailableException;
import com.telecom.subscription_service.exception.TariffNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("verifyTariffDelegate")
@RequiredArgsConstructor
@Slf4j
public class VerifyTariffDelegate implements JavaDelegate {

    private final CatalogServiceClient catalogServiceClient;

    @Override
    public void execute(DelegateExecution execution) {
        String tariffId = (String) execution.getVariable("tariffId");
        log.info("⚙️ [Workflow] Executing VerifyTariffDelegate for Tariff ID: {}", tariffId);

        TariffResponse tariff;
        try {
            tariff = catalogServiceClient.getTariffById(tariffId);
        } catch (TariffNotFoundException e) {
            throw new BpmnError("TARIFF_NOT_FOUND", e.getMessage());
        } catch (CatalogServiceUnavailableException e) {
            throw new BpmnError("CATALOG_SERVICE_UNAVAILABLE", e.getMessage());
        }

        if (!tariff.isActive()) {
            log.warn("❌ [Workflow] Tariff is inactive. tariffId={}", tariffId);
            throw new BpmnError("TARIFF_INACTIVE", "Tariff is not currently active: " + tariffId);
        }

        execution.setVariable("tariffValidityDays", tariff.validityDays());
        log.info("✅ [Workflow] Tariff verified successfully. validityDays={}", tariff.validityDays());
    }
}