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
