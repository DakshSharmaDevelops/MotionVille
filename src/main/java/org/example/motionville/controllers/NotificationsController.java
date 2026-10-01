package org.example.motionville.controllers;


import lombok.RequiredArgsConstructor;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.services.NotificationsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationsService.getAllNotification();
    }

    @PatchMapping("/{notificationId}/read")
    public Notification readNotification(
            @PathVariable("notificationId") Long notificationId) {
        return notificationsService.markAsRead(notificationId);
    }

    @PatchMapping("/read-all")
    public List<Notification> readAllNotification() {
        return notificationsService.markAllAsRead();
    }
}
