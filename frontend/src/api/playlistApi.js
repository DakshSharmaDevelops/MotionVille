import { apiRequest } from "./videoApi.js";

export function fetchPlaylists(userId) {
  return apiRequest(`/playlists?userId=${userId}`);
}

export function createPlaylist(playlist) {
  return apiRequest("/playlists", {
    method: "POST",
    body: JSON.stringify(playlist),
  });
}

export function updatePlaylist(playlistId, playlist) {
  return apiRequest(`/playlists/${playlistId}`, {
    method: "PUT",
    body: JSON.stringify(playlist),
  });
}

export function deletePlaylist(playlistId) {
  return apiRequest(`/playlists/${playlistId}`, { method: "DELETE" });
}

export function fetchPlaylistVideos(playlistId) {
  return apiRequest(`/playlists/${playlistId}/videos`);
}

export function addVideoToPlaylist(playlistId, videoId) {
  return apiRequest(`/playlists/${playlistId}/videos/${videoId}`, { method: "POST" });
}

export function removeVideoFromPlaylist(playlistId, videoId) {
  return apiRequest(`/playlists/${playlistId}/videos/${videoId}`, { method: "DELETE" });
}

export function reorderPlaylistVideos(playlistId, videoIds) {
  return apiRequest(`/playlists/${playlistId}/videos/reorder`, {
    method: "PUT",
    body: JSON.stringify({ videoIds }),
  });
}
