import { API_BASE_URL, readResponseError } from "./videoApi.js";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    throw new Error(await readResponseError(response));
  }

  if (response.status === 204 || options.method === "DELETE") return null;
  return response.json();
}

export function fetchVideoReaction(videoId, userId) {
    const query = userId == null ? "" : `?userId=${encodeURIComponent(userId)}`;
    return request(`/videos/${videoId}/reaction-summary${query}`);
}

export function setVideoReaction(videoId, reaction, userId) {
    return request(`/videos/${videoId}/${reaction.toLowerCase()}?userId=${userId}`, {
        method: "POST",
    });
}

export function removeVideoReaction(videoId, userId) {
    return request(`/videos/${videoId}/reaction?userId=${userId}`, {
        method: "DELETE",
    });
}

export function fetchCommentReaction(commentId, userId) {
    const query = userId == null ? "" : `?userId=${encodeURIComponent(userId)}`;
    return request(`/comments/${commentId}/reaction-summary${query}`);
}

export function setCommentReaction(commentId, reaction, userId) {
    return request(`/comments/${commentId}/${reaction.toLowerCase()}?userId=${userId}`, {
        method: "POST",
    });
}

export function removeCommentReaction(commentId, userId) {
    return request(`/comments/${commentId}/reaction?userId=${userId}`, {
        method: "DELETE",
    });
}