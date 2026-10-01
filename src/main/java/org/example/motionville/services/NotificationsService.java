package org.example.motionville.services;

import org.example.motionville.dto.NotificationResponse;

import java.util.List;

public interface NotificationsService {

    List<NotificationResponse> getAllNotification();

    NotificationResponse markAsRead(Long notificationId);

    List<NotificationResponse> markAllAsRead();
}
