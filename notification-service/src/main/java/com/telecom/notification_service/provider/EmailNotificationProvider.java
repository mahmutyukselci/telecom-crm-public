package com.telecom.notification_service.provider;

import com.telecom.notification_service.event.SubscriptionCreatedEvent;
import com.telecom.notification_service.event.SubscriptionExpiringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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
}

