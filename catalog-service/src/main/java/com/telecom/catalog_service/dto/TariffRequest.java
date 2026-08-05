package com.telecom.catalog_service.dto;

import java.math.BigDecimal;

public record TariffRequest(
        String name,
        String description,
        BigDecimal price,
        Integer dataLimitGb,
        Integer voiceLimitMinutes,
        Integer smsLimit,
        Integer validityDays
) {}