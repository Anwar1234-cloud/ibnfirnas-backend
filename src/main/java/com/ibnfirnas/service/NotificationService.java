package com.ibnfirnas.service;

import com.ibnfirnas.entity.Notification;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

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
        notification.setIsSent(true);
        notification.setIsDraft(false);
        // TODO: integrate Firebase FCM here
        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
