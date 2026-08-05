package com.telecom.notification_service.dto;

import java.util.List;

public record TextBeeSmsRequest(
        List<String> recipients,
        String message
) {}