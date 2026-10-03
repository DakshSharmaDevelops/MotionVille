import { apiRequest } from "./videoApi.js";

export function fetchVideoReaction(videoId, userId) {
    const query = userId == null ? "" : `?userId=${encodeURIComponent(userId)}`;
    return apiRequest(`/videos/${videoId}/reaction-summary${query}`);
}

export function setVideoReaction(videoId, reaction, userId) {
    return apiRequest(`/videos/${videoId}/${reaction.toLowerCase()}?userId=${encodeURIComponent(userId)}`, {
        method: "POST",
    });
}

export function removeVideoReaction(videoId, userId) {
    return apiRequest(`/videos/${videoId}/reaction?userId=${encodeURIComponent(userId)}`, {
        method: "DELETE",
    });
}

export function fetchCommentReaction(commentId, userId) {
    const query = userId == null ? "" : `?userId=${encodeURIComponent(userId)}`;
    return apiRequest(`/comments/${commentId}/reaction-summary${query}`);
}

export function setCommentReaction(commentId, reaction, userId) {
    return apiRequest(`/comments/${commentId}/${reaction.toLowerCase()}?userId=${encodeURIComponent(userId)}`, {
        method: "POST",
    });
}

export function removeCommentReaction(commentId, userId) {
    return apiRequest(`/comments/${commentId}/reaction?userId=${encodeURIComponent(userId)}`, {
        method: "DELETE",
    });
}