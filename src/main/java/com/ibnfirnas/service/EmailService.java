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

    public void sendContactNotificationToAdmin(String adminEmail, String name,
            String submitterEmail, String phone, String message) {
        sendEmail(adminEmail,
                "New Contact Form Submission - IBN Firnas",
                "New contact form submission:\n\n" +
                        "Name: " + name + "\n" +
                        "Email: " + submitterEmail + "\n" +
                        "Phone: " + (phone == null || phone.isBlank() ? "-" : phone) + "\n\n" +
                        "Message:\n" + message);
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