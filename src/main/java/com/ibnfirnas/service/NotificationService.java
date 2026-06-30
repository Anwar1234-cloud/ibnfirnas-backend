package com.ibnfirnas.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ibnfirnas.entity.DeviceToken;
import com.ibnfirnas.entity.Notification;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.DeviceTokenRepository;
import com.ibnfirnas.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Notification sendNotification(Long id) {

        Notification notification = getNotificationById(id);

        List<DeviceToken> tokens = deviceTokenRepository.findByIsActiveTrue();

        int successCount = 0;

        for (DeviceToken deviceToken : tokens) {

            try {

                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(notification.getTitle())
                                        .setBody(notification.getMessage())
                                        .setImage(notification.getImageUrl())
                                        .build()
                        )
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);

                log.info("Successfully sent message: {}", response);
                successCount++;

            } catch (FirebaseMessagingException e) {

                log.error("Failed to send notification to token {}",
                        deviceToken.getToken(), e);

            }
        }

        log.info("Notification sent to {}/{} devices", successCount, tokens.size());

        notification.setIsSent(true);
        notification.setIsDraft(false);

        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}