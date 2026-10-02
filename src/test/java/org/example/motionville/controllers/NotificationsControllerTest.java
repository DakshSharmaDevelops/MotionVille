package org.example.motionville.controllers;

import org.example.motionville.dto.NotificationResponse;
import org.example.motionville.dto.NotificationResponseCount;
import org.example.motionville.entity.notification.enums.NotificationType;
import org.example.motionville.services.NotificationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new NotificationsController(new StubNotificationsService())).build();
    }

    @Test
    void listsNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications").param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("New comment"));
    }

    @Test
    void returnsNotificationCountForUser() throws Exception {
        mockMvc.perform(get("/api/notifications/count").param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void marksOneNotificationAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read").param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.readAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void marksAllNotificationsAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$[1].readAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundWhenNotificationDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/notifications/99/read").param("userId", "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresRecipientIdForNotificationRequests() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest());
    }

    private static class StubNotificationsService implements NotificationsService {
        @Override
        public List<NotificationResponse> getAllNotification(Long recipientId) {
            if (recipientId != 7L) return List.of();
            return List.of(
                    notification(1L, "New comment", null),
                    notification(2L, "New video", null));
        }

        @Override
        public NotificationResponseCount getNotificationCount(Long recipientId) {
            NotificationResponseCount response = new NotificationResponseCount();
            response.setCount(recipientId == 7L ? 2L : 0L);
            return response;
        }

        @Override
        public NotificationResponse markAsRead(Long notificationId, Long recipientId) {
            return notificationId == 1L && recipientId == 7L
                    ? notification(1L, "New comment", Instant.parse("2026-01-01T00:00:00Z"))
                    : throwNotFound();
        }

        @Override
        public List<NotificationResponse> markAllAsRead(Long recipientId) {
            if (recipientId != 7L) return List.of();
            Instant readAt = Instant.parse("2026-01-01T00:00:00Z");
            return List.of(
                    notification(1L, "New comment", readAt),
                    notification(2L, "New video", readAt));
        }

        private NotificationResponse throwNotFound() {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Notification not found");
        }

        private static NotificationResponse notification(Long id, String message, Instant readAt) {
            return new NotificationResponse(id, 7L, 3L, null, 9L,
                    NotificationType.NEW_COMMENT, message,
                    Instant.parse("2026-01-01T00:00:00Z"), readAt);
        }
    }
}
