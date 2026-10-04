package org.example.motionville.repo.notification;

import org.example.motionville.entity.notification.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"recipient", "actor", "video", "comment"})
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    @EntityGraph(attributePaths = {"recipient", "actor", "video", "comment"})
    List<Notification> findByRecipient_IdAndReadAtIsNull(Long recipientId);

    @EntityGraph(attributePaths = {"recipient", "actor", "video", "comment"})
    Optional<Notification> findByIdAndRecipient_Id(Long notificationId, Long recipientId);

    long countByRecipient_Id(Long recipientId);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    void deleteAllByVideo_VideoId(Long videoId);

    void deleteAllByComment_Video_VideoId(Long videoId);
}
