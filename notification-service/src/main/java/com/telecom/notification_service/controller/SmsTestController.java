package com.telecom.notification_service.controller;

import com.telecom.notification_service.dto.ManualSmsRequest;
import com.telecom.notification_service.dto.SmsResponse;
import com.telecom.notification_service.provider.TextBeeNotificationProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/test")
@RequiredArgsConstructor
@Tag(name = "SMS Test Controller", description = "Manual SMS dispatch endpoint for Swagger UI testing")
@Profile({"!prod & !test"})
public class SmsTestController {

    private final TextBeeNotificationProvider textBeeProvider;

    @PostMapping("/send-sms")
    @Operation(
            summary = "Send a manual SMS via TextBee",
            description = "Allows developers and QA engineers to dispatch custom SMS messages directly from Swagger UI."
    )
    public ResponseEntity<SmsResponse> sendManualSms(@Valid @RequestBody ManualSmsRequest request) {
        SmsResponse response = textBeeProvider.sendRawSms(request.phoneNumber(), request.message());
        return ResponseEntity.ok(response);
    }
}