package org.example.motionville.services;

import org.example.motionville.entity.notification.Notification;

import java.util.List;

public interface NotificationsService {
    List<Notification> getAllNotification();

    Notification markAsRead(Long notificationId);

    List<Notification> markAllAsRead();
}
