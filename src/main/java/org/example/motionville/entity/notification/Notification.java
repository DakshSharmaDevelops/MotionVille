package org.example.motionville.entity.notification;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.notification.enums.NotificationType;

import java.time.Instant;

@Entity
@Table(name="notifications")
@Getter
@Setter
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AppUser recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_id")
    private AppUser actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comment_id")
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 30)
    private NotificationType type;

    @Column(nullable = false,length = 255)
    private String message;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="read_at")
    private Instant readAt;
}
