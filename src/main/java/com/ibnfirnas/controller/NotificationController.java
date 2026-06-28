package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Notification;
import com.ibnfirnas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success("Notifications fetched",
                        notificationService.getAllNotifications()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> create(
            @RequestBody Notification notification) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification created",
                        notificationService.saveNotification(notification)));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<Notification>> send(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Notification sent",
                        notificationService.sendNotification(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}