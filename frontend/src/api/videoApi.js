export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
export const MAX_VIDEO_BYTES = 500 * 1024 * 1024;

export class ApiError extends Error {
  constructor(message, status, data = null, details = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
    this.details = Array.isArray(details) ? details : [];
  }

  get isUnauthorized() {
    return this.status === 401;
  }

  get isForbidden() {
    return this.status === 403;
  }

  get isValidationError() {
    return this.status === 400;
  }
}

export function getCsrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

let isRefreshingPromise = null;

async function silentRefresh() {
  if (!isRefreshingPromise) {
    isRefreshingPromise = (async () => {
      const csrfToken = getCsrfToken();
      const headers = {};
      if (csrfToken) {
        headers["X-XSRF-TOKEN"] = csrfToken;
      }
      const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: "POST",
        credentials: "include",
        headers,
      });
      if (!res.ok) {
        throw new Error("Token refresh failed");
      }
      return res.json();
    })().finally(() => {
      isRefreshingPromise = null;
    });
  }
  return isRefreshingPromise;
}

/**
 * Parses JSON error envelope from backend (timestamp, status, error, message, details)
 */
async function parseApiError(response) {
  let message = `Request failed (${response.status}).`;
  let data = null;
  let details = [];

  try {
    data = await response.json();
    if (data && typeof data === "object") {
      details = Array.isArray(data.details) ? data.details : [];
      if (details.length > 0) {
        message = `${data.message || "Validation failed"}: ${details.join("; ")}`;
      } else {
        message = data.message || data.error || message;
      }
    }
  } catch {
    // Non-JSON response body
  }

  // Prevent internal stack trace / system detail exposure on 5xx errors
  if (response.status >= 500) {
    message = "An unexpected server error occurred. Please try again later.";
  } else if (response.status === 401 && (!data || !data.message)) {
    message = "Your session has expired or authentication is required. Please log in.";
  } else if (response.status === 403 && (!data || !data.message)) {
    message = "Access denied: You do not have permission to access or modify this resource.";
  } else if (response.status === 404 && (!data || !data.message)) {
    message = "The requested resource was not found.";
  }

  return new ApiError(message, response.status, data, details);
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const csrfToken = getCsrfToken();

  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...options.headers,
  };

  // Attach XSRF token for mutating requests
  if (csrfToken && !["GET", "HEAD", "OPTIONS"].includes(method)) {
    headers["X-XSRF-TOKEN"] = csrfToken;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: "include",
      ...options,
      headers,
    });
  } catch (networkError) {
    // Handle network drop, offline state, or unreachable server gracefully
    throw new ApiError(
      "Unable to connect to the server. Please check your network connection.",
      0,
      null,
      [networkError.message]
    );
  }

  if (!response.ok) {
    if (
      response.status === 401 &&
      !options._retry &&
      !["/auth/login", "/auth/refresh", "/auth/logout"].includes(path)
    ) {
      try {
        await silentRefresh();
        return await apiRequest(path, { ...options, _retry: true });
      } catch (refreshErr) {
        // Silent refresh failed; proceed to regular 401 error dispatch
      }
    }

    const error = await parseApiError(response);

    // Validation failure (400): notify app of invalid input
    if (error.isValidationError) {
      window.dispatchEvent(new CustomEvent("motionville:validation-error", { detail: error }));
    }

    // Authentication failure (401): notify app to reset auth state (except when checking /auth/me)
    if (error.isUnauthorized && !path.includes("/auth/me")) {
      window.dispatchEvent(new CustomEvent("motionville:unauthorized", { detail: error }));
    }

    // Authorization failure (403): wrong owner or insufficient permissions
    if (error.isForbidden) {
      window.dispatchEvent(new CustomEvent("motionville:forbidden", { detail: error }));
    }

    throw error;
  }

  if (response.status === 204 || response.status === 202) return null;
  const body = await response.text();
  return body ? JSON.parse(body) : null;
}

export async function readResponseError(response) {
  const error = await parseApiError(response);
  return error.message;

}

export function buildVideoQuery({ search, page, sort, channelId, categoryId, publicOnly, trending }) {
  if (trending) {
    return `/videos/trending?page=${page}&size=20`;
  }
  const params = new URLSearchParams({
    page: String(page),
    size: "20",
    sort,
  });
  if (search.trim()) params.set("search", search.trim());
  if (channelId) params.set("channelId", String(channelId));
  if (categoryId) params.set("categoryId", String(categoryId));
  if (publicOnly) params.set("publicOnly", "true");
  return `/videos?${params.toString()}`;
}

export function mapApiVideo(video, categories = []) {
  return {
    videoId: video.id,
    channelId: video.channelId,
    categoryId: video.categoryId,
    title: video.title,
    description: video.description || "",
    thumbnailUrl: video.thumbnailUrl || "",
    durationSeconds: video.durationSeconds || 0,
    visibility: video.visibility,
    processingStatus: video.processingStatus,
    createdAt: video.createdAt,
    updatedAt: video.updatedAt,
    publishedAt: video.publishedAt,
    category: categories.find((category) => Number(category.id) === Number(video.categoryId))?.name || null,
    serverVideo: true,
  };
}

export function mapApiChannel(channel) {
  const profileImage = channel.profileImageUrl || channel.avatarUrl || "";
  return {
    channelId: channel.id,
    ownerId: channel.ownerId,
    backendChannel: true,
    name: channel.name,
    handle: channel.handle,
    description: channel.description || "",
    bannerUrl: channel.bannerUrl || "",
    avatarUrl: profileImage,
    profileImageUrl: profileImage,
    createdAt: channel.createdAt,
    videoCount: Number(channel.videoCount || 0),
    viewCount: Number(channel.viewCount || 0),
  };
}

export function updateChannelApi(channelId, data) {
  return apiRequest(`/channels/${channelId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export function mapApiCategory(category) {
  return { id: category.id, name: category.name };
}

export function fetchChannels(search) {
  const query = search ? `?search=${encodeURIComponent(search)}` : "";
  return apiRequest(`/channels${query}`);
}

export function fetchTags() {
  return apiRequest("/tags");
}

export function fetchVideoTags(videoId) {
  return apiRequest(`/videos/${videoId}/tags`);
}

export function addTagToVideo(videoId, tagId) {
  return apiRequest(`/videos/${videoId}/tags/${tagId}`, { method: "POST" });
}

export function removeTagFromVideo(videoId, tagId) {
  return apiRequest(`/videos/${videoId}/tags/${tagId}`, { method: "DELETE" });
}

export function fetchVideosByTag(tagId) {
  return apiRequest(`/tags/${tagId}/videos`);
}

export function createTag(name) {
  return apiRequest("/tags", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export function uploadFile(uploadUrl, file, mimeType, onProgress) {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open("PUT", uploadUrl);
    request.setRequestHeader("Content-Type", mimeType);
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    request.onload = () => {
      if (request.status >= 200 && request.status < 300) resolve();
      else reject(new Error(`R2 upload failed (${request.status}). Check the bucket CORS configuration.`));
    };
    request.onerror = () => reject(new Error("Could not reach R2. Check the bucket CORS configuration."));
    request.onabort = () => reject(new Error("Video upload was cancelled."));
    request.send(file);
  });
}

function makeTitleThumbnail(title) {
  const canvas = document.createElement("canvas");
  canvas.width = 640;
  canvas.height = 360;
  const context = canvas.getContext("2d");
  if (!context) throw new Error("This browser cannot create a video thumbnail.");
  const gradient = context.createLinearGradient(0, 0, 640, 360);
  gradient.addColorStop(0, "#ad7762");
  gradient.addColorStop(1, "#34343a");
  context.fillStyle = gradient;
  context.fillRect(0, 0, 640, 360);
  context.fillStyle = "rgba(20, 20, 24, .36)";
  context.fillRect(0, 0, 640, 360);
  context.fillStyle = "#ffffff";
  context.font = "700 30px sans-serif";
  context.textAlign = "center";
  const words = (title.trim() || "MotionVille").split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (context.measureText(next).width > 540 && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) lines.push(line);
  const visibleLines = lines.slice(0, 3);
  visibleLines.forEach((text, index) => {
    context.fillText(text, 320, 180 + (index - (visibleLines.length - 1) / 2) * 42, 540);
  });
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob);
      else reject(new Error("Could not generate a thumbnail."));
    }, "image/jpeg", 0.86);
  });
}

export async function createVideoThumbnail(file, title) {
  const sourceUrl = URL.createObjectURL(file);
  const video = document.createElement("video");
  video.muted = true;
  video.playsInline = true;
  video.preload = "metadata";
  try {
    await new Promise((resolve, reject) => {
      video.onloadedmetadata = resolve;
      video.onerror = () => reject(new Error("Could not read video metadata."));
      video.src = sourceUrl;
    });
    await new Promise((resolve, reject) => {
      video.onseeked = resolve;
      video.onerror = () => reject(new Error("Could not decode a video frame."));
      video.currentTime = Math.min(1, Math.max(0, video.duration / 2));
    });
    const canvas = document.createElement("canvas");
    canvas.width = 640;
    canvas.height = 360;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("This browser cannot create a video thumbnail.");
    const scale = Math.max(canvas.width / video.videoWidth, canvas.height / video.videoHeight);
    const width = video.videoWidth * scale;
    const height = video.videoHeight * scale;
    context.drawImage(video, (canvas.width - width) / 2, (canvas.height - height) / 2, width, height);
    return await new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (blob) resolve(blob);
        else reject(new Error("Could not create a thumbnail from the video."));
      }, "image/jpeg", 0.86);
    });
  } catch {
    return makeTitleThumbnail(title);
  } finally {
    video.removeAttribute("src");
    video.load();
    URL.revokeObjectURL(sourceUrl);
  }
}