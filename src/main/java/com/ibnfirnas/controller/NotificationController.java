package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.NotificationRequest;
import com.ibnfirnas.dto.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success("Notifications fetched",
                        notificationService.getAllNotifications()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Notification>> create(
            @RequestBody Notification notification,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        notification.setCreatedBy(user);
        notification.setIsDraft(false);
        notification.setIsSent(false);

        return ResponseEntity.ok(
                ApiResponse.success("Notification created",
                        notificationService.saveNotification(notification)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Notification>> send(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification sent",
                        notificationService.sendNotification(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}