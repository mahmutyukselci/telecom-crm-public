package com.telecom.notification_service.controller;

import com.telecom.notification_service.service.DocumentNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/notifications/documents")
public class DocumentMailController {

    private final DocumentNotificationService documentNotificationService;

    public DocumentMailController(DocumentNotificationService documentNotificationService) {
        this.documentNotificationService = documentNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendDocumentsViaEmail(
            @RequestParam("email") String email,
            @RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided.");
        }

        documentNotificationService.processAndSendDocuments(email, files);

        return ResponseEntity.ok("Document processing initiated. System will automatically choose Attachment or Cloud Links based on size.");
    }
}