// customer-service/exception/CustomerNotFoundException.java
package com.telecom.customer_service.exception;

import com.telecom.commonutils.exception.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {
    public CustomerNotFoundException(String id) {
        super("Customer", id);
    }
}