export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
export const MAX_VIDEO_BYTES = 500 * 1024 * 1024;

async function readApiError(response) {
  try {
    const body = await response.json();
    return body.message || `Request failed (${response.status}).`;
  } catch {
    return `Request failed (${response.status}).`;
  }
}

export async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) throw new Error(await readApiError(response));
  const body = await response.text();
  return body ? JSON.parse(body) : null;
}

export function buildVideoQuery({ search, page, sort, channelId, categoryId, publicOnly }) {
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
  return {
    channelId: channel.id,
    ownerId: channel.ownerId,
    backendChannel: true,
    name: channel.name,
    handle: channel.handle,
    description: channel.description || "",
    bannerUrl: channel.bannerUrl || "",
    avatarUrl: "",
    createdAt: channel.createdAt,
  };
}

export function mapApiCategory(category) {
  return { id: category.id, name: category.name };
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

export async function readResponseError(response) {
  return readApiError(response);
}