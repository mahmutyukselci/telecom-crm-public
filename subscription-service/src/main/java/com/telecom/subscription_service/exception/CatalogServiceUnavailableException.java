package com.telecom.subscription_service.exception;

public class CatalogServiceUnavailableException extends RuntimeException {
    public CatalogServiceUnavailableException(String tariffId, Throwable cause) {
        super("Catalog Service unavailable while verifying tariffId=" + tariffId, cause);
    }
}