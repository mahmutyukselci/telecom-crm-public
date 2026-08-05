package com.telecom.subscription_service.exception;

public class TariffNotFoundException extends RuntimeException {
    public TariffNotFoundException(String tariffId) {
        super("Tariff not found: " + tariffId);
    }
}