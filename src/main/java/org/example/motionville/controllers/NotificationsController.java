package org.example.motionville.controllers;


import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.dto.NotificationResponseCount;
import org.example.motionville.services.NotificationsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;

    @GetMapping
    public List<NotificationResponse> getNotifications(
                                                @RequestParam Long userId) {
        return notificationsService.getAllNotification(userId);
    }

    @GetMapping("/count")
    public NotificationResponseCount getNotificationCount(
                                                @RequestParam Long userId) {
        return notificationsService.getNotificationCount(userId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse readNotification(
            @PathVariable("notificationId") Long notificationId,
            @RequestParam Long userId) {
        return notificationsService.markAsRead(notificationId, userId);
    }

    @PatchMapping("/read-all")
    public List<NotificationResponse> readAllNotification(
                                            @RequestParam Long userId) {
        return notificationsService.markAllAsRead(userId);
    }
}
