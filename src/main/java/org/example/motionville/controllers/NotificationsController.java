package org.example.motionville.controllers;


import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.services.NotificationsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        return notificationsService.getAllNotification();
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse readNotification(
            @PathVariable("notificationId") Long notificationId) {
        return notificationsService.markAsRead(notificationId);
    }

    @PatchMapping("/read-all")
    public List<NotificationResponse> readAllNotification() {
        return notificationsService.markAllAsRead();
    }
}
