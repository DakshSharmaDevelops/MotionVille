package org.example.motionville.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.notification.enums.NotificationType;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Setter
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private Long videoId;
    private Long commentId;
    private NotificationType type;
    private String message;
    private Instant createdAt;
    private Instant readAt;
}
