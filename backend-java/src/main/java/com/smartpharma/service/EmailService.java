package com.smartpharma.service;

import com.smartpharma.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends order notification emails to suppliers. Failures (including no SMTP
 * credentials configured) are logged, never thrown - email is a notification
 * on top of the in-app order list, not the source of truth.
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderNotification(Order order) {
        String supplierEmail = order.getSupplier().getEmail();
        if (mailUsername == null || mailUsername.isBlank()) {
            log.info("MAIL_USERNAME not configured - skipping order notification email to {}", supplierEmail);
            return;
        }
        if (supplierEmail == null || supplierEmail.isBlank()) {
            log.warn("Supplier {} has no email on file - skipping order notification",
                    order.getSupplier().getUsername());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(supplierEmail);
            message.setSubject("SmartPharma: new order #" + order.getId());
            message.setText(buildBody(order));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send order notification email for order {}", order.getId(), e);
        }
    }

    private String buildBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("You have a new order from SmartPharma (#").append(order.getId()).append(").\n\n");
        order.getItems().forEach(item ->
                body.append("- ").append(item.getProductName())
                        .append(": ").append(item.getRequestedQty()).append(" units\n"));
        body.append("\nLog in to SmartPharma to review and respond to this order.");
        return body.toString();
    }
}
