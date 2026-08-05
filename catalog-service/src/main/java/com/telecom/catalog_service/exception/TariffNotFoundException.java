
package com.telecom.catalog_service.exception;

import com.telecom.commonutils.exception.ResourceNotFoundException;

public class TariffNotFoundException extends ResourceNotFoundException {
    public TariffNotFoundException(String id) {
        super("Tariff", id);
    }
}