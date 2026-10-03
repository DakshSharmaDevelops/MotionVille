package org.example.motionville.services.notification;

import org.example.motionville.entity.notification.Notification;
import org.example.motionville.entity.notification.enums.NotificationType;
import org.example.motionville.repo.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationsServiceImplementsTest {

    private NotificationRepository repository;
    private NotificationsServiceImplements service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationsServiceImplements(repository);
    }

    @Test
    void listsNotificationsNewestFirst() {
        Notification newest = notification(2L);
        Notification oldest = notification(1L);
        when(repository.findByRecipient_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of(newest, oldest));

        var result = service.getAllNotification(7L);

        assertEquals(List.of(2L, 1L), result.stream().map(item -> item.getId()).toList());
        verify(repository).findByRecipient_IdOrderByCreatedAtDesc(7L);
    }

    @Test
    void countsNotificationsForRecipient() {
        when(repository.countByRecipient_IdAndReadAtIsNull(7L)).thenReturn(4L);

        var response = service.getNotificationCount(7L);

        assertEquals(4L, response.getCount());
        verify(repository).countByRecipient_IdAndReadAtIsNull(7L);
    }

    @Test
    void marksOneNotificationRead() {
        Notification notification = notification(1L);
        when(repository.findByIdAndRecipient_Id(1L, 7L)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        var result = service.markAsRead(1L, 7L);

        assertNotNull(notification.getReadAt());
        assertEquals(notification.getReadAt(), result.getReadAt());
    }

    @Test
    void returnsNotFoundForMissingNotification() {
        when(repository.findByIdAndRecipient_Id(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.markAsRead(99L, 7L));
    }

    @Test
    void doesNotAllowReadingNotificationOwnedByAnotherRecipient() {
        when(repository.findByIdAndRecipient_Id(1L, 8L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.markAsRead(1L, 8L));
        verify(repository, never()).save(any());
    }

    @Test
    void marksUnreadNotificationsReadAndLeavesReadOnesUnchanged() {
        Instant alreadyReadAt = Instant.parse("2026-01-01T00:00:00Z");
        Notification unread = notification(1L);
        Notification alreadyRead = notification(2L);
        alreadyRead.setReadAt(alreadyReadAt);
        when(repository.findByRecipient_IdAndReadAtIsNull(7L)).thenReturn(List.of(unread));
        when(repository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(repository.findByRecipient_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of(unread, alreadyRead));

        var result = service.markAllAsRead(7L);

        assertNotNull(unread.getReadAt());
        assertEquals(alreadyReadAt, alreadyRead.getReadAt());
        assertEquals(unread.getReadAt(), result.get(0).getReadAt());
        assertEquals(alreadyReadAt, result.get(1).getReadAt());
        verify(repository).saveAll(List.of(unread));
        verify(repository).findByRecipient_IdAndReadAtIsNull(7L);
    }

    private static Notification notification(Long id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType(NotificationType.NEW_COMMENT);
        notification.setMessage("New comment");
        notification.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        return notification;
    }
}
