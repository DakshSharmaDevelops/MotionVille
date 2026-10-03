import { apiRequest } from "./videoApi.js";

export function fetchComments(videoId, options) {
  return apiRequest(`/videos/${videoId}/comments`, options);
}

export function createComment(videoId, authorId, body, options) {
  return apiRequest(`/videos/${videoId}/comments`, {
    ...options,
    method: "POST",
    body: JSON.stringify({ authorId, body }),
  });
}

export function updateComment(commentId, body, options) {
  return apiRequest(`/comments/${commentId}`, {
    ...options,
    method: "PUT",
    body: JSON.stringify({ body }),
  });
}

export function deleteComment(commentId, options) {
  return apiRequest(`/comments/${commentId}`, {
    ...options,
    method: "DELETE",
  });
}

export function fetchReplies(commentId, options) {
  return apiRequest(`/comments/${commentId}/replies`, options);
}

export function createReply(commentId, authorId, body, options) {
  return apiRequest(`/comments/${commentId}/replies`, {
    ...options,
    method: "POST",
    body: JSON.stringify({ authorId, body }),
  });
}
