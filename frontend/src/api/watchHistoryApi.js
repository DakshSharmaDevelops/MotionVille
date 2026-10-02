import { apiRequest } from "./videoApi.js";

export function fetchWatchHistory(userId) {
  return apiRequest(`/users/${userId}/history`);
}

export function recordWatchProgress(videoId, userId, lastPositionSeconds) {
  return apiRequest(`/videos/${videoId}/history`, {
    method: "POST",
    body: JSON.stringify({ userId, lastPositionSeconds }),
  });
}

export function removeWatchHistoryItem(userId, videoId) {
  return apiRequest(`/users/${userId}/history/${videoId}`, {
    method: "DELETE",
  });
}

export function clearWatchHistory(userId) {
  return apiRequest(`/users/${userId}/history`, {
    method: "DELETE",
  });
}
