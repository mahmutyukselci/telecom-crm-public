package com.telecom.notification_service.provider;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    private final JavaMailSender mailSender;

    // The sender email address (must match the one in application.yml)
    private final String senderEmail = "your-email@gmail.com";

    public EmailNotificationProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendCreated(SubscriptionCreatedEvent event) {
        log.info("Preparing email (Created) - subscriptionId: {}", event.subscriptionId());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        // In a real scenario, this email would be fetched from Customer Service. For now, we simulate it.
        message.setTo("customer_" + event.customerId() + "@example.com");
        message.setSubject("Your Subscription Has Been Successfully Activated!");
        message.setText("Hello,\n\nYour tariff with ID " + event.tariffId() + " has been successfully activated. Thank you for choosing us.");

        mailSender.send(message);
    }

    @Override
    public void sendExpiring(SubscriptionExpiringEvent event) {
        log.info("Preparing email (Expiring) - subscriptionId: {}", event.subscriptionId());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo("customer_" + event.customerId() + "@example.com");
        message.setSubject("Your Subscription Is About to Expire!");
        message.setText("Hello,\n\nYour subscription will expire in 3 days. Please renew your package to avoid any service interruption.");

        mailSender.send(message);
    }
    // --- NEW: Send Email with Attachment ---
    public void sendWithAttachment(String to, String subject, String text, File attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // The true flag indicates a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            FileSystemResource fileResource = new FileSystemResource(attachment);
            helper.addAttachment(fileResource.getFilename(), fileResource);

            mailSender.send(message);
            log.info("Email with attachment sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Mail sending failed", e);
        }
    }

    // --- NEW: Send Email with Cloud Links (HTML) ---
    public void sendWithCloudLinks(String to, String subject, List<String> downloadLinks) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            StringBuilder htmlBody = new StringBuilder();
            htmlBody.append("<h3>Hello,</h3>");
            htmlBody.append("<p>The documents you requested are too large to attach. Please download them using the secure links below (Valid for 24 hours):</p>");
            htmlBody.append("<ul>");

            for (String link : downloadLinks) {
                htmlBody.append("<li><a href='")
                        .append(link)
                        .append("'>Download Document</a></li>");
            }

            htmlBody.append("</ul><p>Telecom CRM System</p>");

            // The true flag indicates that the content is HTML
            helper.setText(htmlBody.toString(), true);

            mailSender.send(message);
            log.info("Email with cloud links sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email with links to {}: {}", to, e.getMessage());
            throw new RuntimeException("Mail sending failed", e);
        }
    }
}

