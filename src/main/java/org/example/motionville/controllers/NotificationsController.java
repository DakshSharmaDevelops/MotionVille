package org.example.motionville.controllers;


import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.dto.NotificationResponseCount;
import org.example.motionville.services.NotificationsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping
    public List<NotificationResponse> getNotifications(
            @RequestParam Long userId) {
        return notificationsService.getAllNotification(userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping("/count")
    public NotificationResponseCount getNotificationCount(
            @RequestParam Long userId) {
        return notificationsService.getNotificationCount(userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PatchMapping("/{notificationId}/read")
    public NotificationResponse readNotification(
            @PathVariable("notificationId") Long notificationId,
            @RequestParam Long userId) {
        return notificationsService.markAsRead(notificationId, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PatchMapping("/read-all")
    public List<NotificationResponse> readAllNotification(
            @RequestParam Long userId) {
        return notificationsService.markAllAsRead(userId);
    }
}