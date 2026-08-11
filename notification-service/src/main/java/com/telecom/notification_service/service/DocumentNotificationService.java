package com.telecom.notification_service.service;

import com.telecom.notification_service.provider.EmailNotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentNotificationService.class);

    // SMTP email attachment limit: 3 MB (in bytes)
    private static final long MAX_ATTACHMENT_SIZE = 3 * 1024 * 1024;

    private final EmailNotificationProvider emailProvider;

    public DocumentNotificationService(EmailNotificationProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    public void processAndSendDocuments(String toEmail, MultipartFile[] files) {
        long totalSize = 0;

        for (MultipartFile file : files) {
            totalSize += file.getSize();
        }

        log.info("Total size of incoming documents: {} bytes", totalSize);

        if (totalSize <= MAX_ATTACHMENT_SIZE && files.length == 1) {
            // Scenario 1: Only 1 file and it is under 3 MB. Send it directly.
            sendSingleFile(toEmail, files[0]);

        } else {
            // Scenario 2: Multiple files or total size > 3 MB. Try compressing them into a ZIP archive.
            try {
                File zippedFile = createZipArchive(files);
                log.info("Zipped file size: {} bytes", zippedFile.length());

                if (zippedFile.length() <= MAX_ATTACHMENT_SIZE) {
                    log.info("Zip file is under the 3 MB threshold. Sending as an attachment.");

                    emailProvider.sendWithAttachment(
                            toEmail,
                            "Your Requested Documents",
                            "Please find the compressed documents attached.",
                            zippedFile
                    );

                } else {
                    // Scenario 3: Even the ZIP file is > 3 MB. Upload to Cloud Storage and send links.
                    log.warn("Zip file exceeds the 3 MB threshold. Switching to Cloud Storage mode.");

                    List<String> cloudLinks = uploadToCloudStorage(files);

                    emailProvider.sendWithCloudLinks(
                            toEmail,
                            "Your Requested Documents (Cloud Links)",
                            cloudLinks
                    );
                }

                // Clean up the temporary ZIP file
                zippedFile.delete();

            } catch (IOException e) {
                log.error("Error during document processing: {}", e.getMessage());
                throw new RuntimeException("Document processing failed", e);
            }
        }
    }

    private void sendSingleFile(String toEmail, MultipartFile file) {
        try {
            File tempFile = File.createTempFile("doc-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            emailProvider.sendWithAttachment(
                    toEmail,
                    "Your Requested Document",
                    "Please find the document attached.",
                    tempFile
            );

            tempFile.delete();

        } catch (IOException e) {
            log.error("Failed to process single file: {}", e.getMessage());
        }
    }

    // Server-side compression using Java's built-in ZipOutputStream
    private File createZipArchive(MultipartFile[] files) throws IOException {
        File zipFile = File.createTempFile("documents-archive-", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (MultipartFile file : files) {
                ZipEntry zipEntry = new ZipEntry(
                        file.getOriginalFilename() != null
                                ? file.getOriginalFilename()
                                : "document-" + UUID.randomUUID()
                );

                zos.putNextEntry(zipEntry);
                zos.write(file.getBytes());
                zos.closeEntry();
            }
        }

        return zipFile;
    }

    // Mock method for Object Storage (AWS S3, MinIO)
    private List<String> uploadToCloudStorage(MultipartFile[] files) {
        log.info("Uploading {} files to Cloud Object Storage...", files.length);

        List<String> generatedLinks = new ArrayList<>();

        // Normally, the AWS S3 SDK (s3Client.putObject) would be used here.
        // A pre-signed URL (e.g., a secure link valid for 1 day) would be generated.
        for (MultipartFile file : files) {
            String mockSignedUrl =
                    "https://s3.telecom-crm.com/docs/"
                            + UUID.randomUUID()
                            + "?expires=86400&signature=mockSign";

            generatedLinks.add(mockSignedUrl);
        }

        return generatedLinks;
    }
}