package org.example.motionville.services;

import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.dto.NotificationResponseCount;

import java.util.List;

public interface NotificationsService {

    List<NotificationResponse> getAllNotification(Long recipientId);

    NotificationResponseCount getNotificationCount(Long recipientId);

    NotificationResponse markAsRead(Long notificationId, Long recipientId);

    List<NotificationResponse> markAllAsRead(Long recipientId);
}
