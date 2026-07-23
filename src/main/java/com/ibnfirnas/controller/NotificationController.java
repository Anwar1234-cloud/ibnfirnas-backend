package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.NotificationRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.NotificationResponse;
import com.ibnfirnas.entity.Notification;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(
                ApiResponse.success("Notifications fetched", notifications));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @RequestBody Notification notification,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        notification.setCreatedBy(user);
        notification.setIsDraft(false);
        notification.setIsSent(false);

        Notification saved = notificationService.saveNotification(notification);
        return ResponseEntity.ok(
                ApiResponse.success("Notification created", toResponse(saved)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(@PathVariable Long id) {
        Notification sent = notificationService.sendNotification(id);
        return ResponseEntity.ok(
                ApiResponse.success("Notification sent", toResponse(sent)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .imageUrl(notification.getImageUrl())
                .isSent(notification.getIsSent())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}