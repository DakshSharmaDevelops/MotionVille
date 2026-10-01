package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.repo.notification.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationsServiceImplements implements NotificationsService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotification() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public List<NotificationResponse> markAllAsRead() {
        List<Notification> notifications = notificationRepository.findAll();
        Instant readAt = Instant.now();
        notifications.stream()
                .filter(notification -> notification.getReadAt() == null)
                .forEach(notification -> notification.setReadAt(readAt));
        return notificationRepository.saveAll(notifications).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient() == null ? null : notification.getRecipient().getId(),
                notification.getActor() == null ? null : notification.getActor().getId(),
                notification.getComment() == null ? null : notification.getComment().getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
