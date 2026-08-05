package com.telecom.subscription_service.exception;

public class CustomerServiceUnavailableException extends RuntimeException {
    public CustomerServiceUnavailableException(String customerId, Throwable cause) {
        super("Customer Service unavailable while verifying customerId=" + customerId, cause);
    }
}