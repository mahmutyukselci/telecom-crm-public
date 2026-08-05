package com.telecom.notification_service.dto;

public record SmsResponse(
        boolean success,
        String message,
        String recipient
) {}