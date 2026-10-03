package org.example.motionville.services.notification;

import org.example.motionville.dto.notification.NotificationResponse;
import org.example.motionville.dto.notification.NotificationResponseCount;

import java.util.List;

public interface NotificationsService {

    List<NotificationResponse> getAllNotification(Long recipientId);

    NotificationResponseCount getNotificationCount(Long recipientId);

    NotificationResponse markAsRead(Long notificationId, Long recipientId);

    List<NotificationResponse> markAllAsRead(Long recipientId);
}
