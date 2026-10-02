package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.dto.NotificationResponseCount;
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
    public List<NotificationResponse> getAllNotification(Long recipientId) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseCount getNotificationCount(Long recipientId) {
        NotificationResponseCount response = new NotificationResponseCount();
        response.setCount(notificationRepository.countByRecipient_Id(recipientId));
        return response;
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, recipientId)
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
    public List<NotificationResponse> markAllAsRead(Long recipientId) {
        List<Notification> unread = notificationRepository.findByRecipient_IdAndReadAtIsNull(recipientId);
        Instant readAt = Instant.now();
        unread.forEach(notification -> notification.setReadAt(readAt));
        notificationRepository.saveAll(unread);
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient() == null ? null : notification.getRecipient().getId(),
                notification.getActor() == null ? null : notification.getActor().getId(),
                notification.getVideo() == null ? null : notification.getVideo().getVideoId(),
                notification.getComment() == null ? null : notification.getComment().getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
