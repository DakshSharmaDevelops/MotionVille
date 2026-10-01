package org.example.motionville.controllers;

import org.example.motionville.entity.notification.Notification;
import org.example.motionville.services.NotificationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationsControllerTest {

    private StubNotificationsService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new StubNotificationsService();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new NotificationsController(service)).build();
    }

    @Test
    void listsNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("New comment"));
    }

    @Test
    void marksOneNotificationAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.readAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void marksAllNotificationsAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$[1].readAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundWhenNotificationDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/notifications/99/read"))
                .andExpect(status().isNotFound());
    }

    private static class StubNotificationsService implements NotificationsService {
        private final List<Notification> notifications = List.of(
                notification(1L, "New comment"),
                notification(2L, "New video"));

        @Override
        public List<Notification> getAllNotification() {
            return notifications;
        }

        @Override
        public Notification markAsRead(Long notificationId) {
            return notifications.stream()
                    .filter(notification -> notification.getId().equals(notificationId))
                    .findFirst()
                    .map(notification -> {
                        notification.setReadAt(Instant.parse("2026-01-01T00:00:00Z"));
                        return notification;
                    })
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Notification not found"));
        }

        @Override
        public List<Notification> markAllAsRead() {
            notifications.forEach(notification ->
                    notification.setReadAt(Instant.parse("2026-01-01T00:00:00Z")));
            return notifications;
        }

        private static Notification notification(Long id, String message) {
            Notification notification = new Notification();
            notification.setId(id);
            notification.setMessage(message);
            return notification;
        }
    }
}
