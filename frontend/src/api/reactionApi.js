import { API_BASE_URL, readResponseError } from "./videoApi.js";

const USER_ID = 1;

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    throw new Error(await readResponseError(response));
  }

  if (response.status === 204 || options.method === "DELETE") return null;
  return response.json();
}

export function fetchVideoReaction(videoId) {
    return request(`/videos/${videoId}/reaction-summary?userId=${USER_ID}`);
}

export function setVideoReaction(videoId, reaction) {
    return request(`/videos/${videoId}/${reaction.toLowerCase()}?userId=${USER_ID}`, {
        method: "POST",
    });
}

export function removeVideoReaction(videoId) {
    return request(`/videos/${videoId}/reaction?userId=${USER_ID}`, {
        method: "DELETE",
    });
}

export function fetchCommentReaction(commentId) {
    return request(`/comments/${commentId}/reaction-summary?userId=${USER_ID}`);
}

export function setCommentReaction(commentId, reaction) {
    return request(`/comments/${commentId}/${reaction.toLowerCase()}?userId=${USER_ID}`, {
        method: "POST",
    });
}

export function removeCommentReaction(commentId) {
    return request(`/comments/${commentId}/reaction?userId=${USER_ID}`, {
        method: "DELETE",
    });
}