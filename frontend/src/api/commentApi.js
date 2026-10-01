import { API_BASE_URL, readResponseError } from "./videoApi.js";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    throw new Error(await readResponseError(response));
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function fetchComments(videoId, options) {
  return request(`/videos/${videoId}/comments`, options);
}

export function createComment(videoId, authorId, body, options) {
  return request(`/videos/${videoId}/comments`, {
    ...options,
    method: "POST",
    body: JSON.stringify({ authorId, body }),
  });
}

export function updateComment(commentId, body, options) {
  return request(`/comments/${commentId}`, {
    ...options,
    method: "PUT",
    body: JSON.stringify({ body }),
  });
}

export function deleteComment(commentId, options) {
  return request(`/comments/${commentId}`, {
    ...options,
    method: "DELETE",
  });
}

export function fetchReplies(commentId, options) {
  return request(`/comments/${commentId}/replies`, options);
}

export function createReply(commentId, authorId, body, options) {
  return request(`/comments/${commentId}/replies`, {
    ...options,
    method: "POST",
    body: JSON.stringify({ authorId, body }),
  });
}
