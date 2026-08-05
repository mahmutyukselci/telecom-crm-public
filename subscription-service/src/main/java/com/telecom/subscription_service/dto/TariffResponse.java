package com.telecom.subscription_service.dto;

import java.math.BigDecimal;

public record TariffResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer dataLimitGb,
        Integer voiceLimitMinutes,
        Integer smsLimit,
        boolean isActive,
        Integer validityDays
) {}