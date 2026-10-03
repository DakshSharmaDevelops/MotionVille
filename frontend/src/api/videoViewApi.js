import { apiRequest } from "./videoApi.js";

export function recordVideoView(videoId, watchedSeconds, viewerId, sessionId) {
  return apiRequest(`/videos/${videoId}/view`, {
    method: "POST",
    body: JSON.stringify({ watchedSeconds, viewerId, sessionId }),
  });
}

export function fetchVideoViews(videoId) {
  return apiRequest(`/videos/${videoId}/views`);
}
