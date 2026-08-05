package com.telecom.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ManualSmsRequest(
        @Schema(description = "Target phone number with country code", example = "+905550001122")
        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @Schema(description = "Custom SMS text content", example = "Hello Mahmut, this is a live test from Swagger UI!")
        @NotBlank(message = "Message content is required")
        String message
) {}