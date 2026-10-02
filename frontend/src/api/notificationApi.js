import { apiRequest } from "./videoApi.js";

export function fetchNotifications(userId) {
    return apiRequest(`/notifications?userId=${encodeURIComponent(userId)}`, {
        cache: "no-store",
    });
}

export function fetchNotificationCount(userId) {
    return apiRequest(`/notifications/count?userId=${encodeURIComponent(userId)}`, {
        cache: "no-store",
    });
}

export function markNotificationRead(userId, notificationId) {
    return apiRequest(
        `/notifications/${notificationId}/read?userId=${encodeURIComponent(userId)}`,
        { method: "PATCH" },
    );
}

export function markAllNotificationsRead(userId) {
    return apiRequest(
        `/notifications/read-all?userId=${encodeURIComponent(userId)}`,
        { method: "PATCH" },
    );
}