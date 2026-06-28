package com.ibnfirnas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@ibnfirnas.com}")
    private String fromEmail;

    public void sendInquiryConfirmation(String toEmail, String name) {
        sendEmail(toEmail,
                "Inquiry Received - IBN Firnas",
                "Dear " + name + ",\n\nThank you for your inquiry. " +
                        "We will get back to you shortly.\n\nIBN Firnas Team");
    }

    public void sendOrderConfirmation(String toEmail, String orderNumber) {
        sendEmail(toEmail,
                "Order Confirmed - " + orderNumber,
                "Your order " + orderNumber + " has been confirmed.\n\nIBN Firnas Team");
    }

    public void sendWelcomeEmail(String toEmail, String fullName) {
        sendEmail(toEmail,
                "Welcome to IBN Firnas!",
                "Dear " + fullName + ",\n\nWelcome to IBN Firnas. " +
                        "We are glad to have you.\n\nIBN Firnas Team");
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        sendEmail(toEmail,
                "Password Reset - IBN Firnas",
                "Your password reset token is: " + token +
                        "\n\nThis token expires in 1 hour.\n\nIBN Firnas Team");
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}