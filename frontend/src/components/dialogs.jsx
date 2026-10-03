import { useEffect, useRef, useState } from "react";
import { API_BASE_URL, createVideoThumbnail, MAX_VIDEO_BYTES, readResponseError } from "../api/videoApi.js";
import { fetchWatchHistory, recordWatchProgress } from "../api/watchHistoryApi.js";
import { formatAge } from "../utils/format.js";
import { Avatar, Icon, Modal, VideoCard } from "./ui.jsx";

import {
  createComment,
  createReply,
  deleteComment,
  fetchComments,
  fetchReplies,
  updateComment,
} from "../api/commentApi.js";

import {
  fetchVideoReaction,
  setVideoReaction,
  removeVideoReaction,
  fetchCommentReaction,
  setCommentReaction,
  removeCommentReaction,
} from "../api/reactionApi.js";
import { createReport } from "../api/reportApi.js";
import { recordVideoView } from "../api/videoViewApi.js";

function formatPlayerTime(seconds = 0) {
  const totalSeconds = Math.floor(Math.max(0, seconds));
  const minutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = totalSeconds % 60;
  return `${minutes}:${String(remainingSeconds).padStart(2, "0")}`;
}

export function CreateChannelDialog({ onClose, onCreate }) {
  const [form, setForm] = useState({ name: "", handle: "", description: "", bannerUrl: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name === "handle"
        ? `@${value.replace(/^@/, "").toLowerCase().replace(/[^a-z0-9._-]/g, "")}`
        : value,
    }));
  }

  async function submit(event) {
    event.preventDefault();
    if (!form.name.trim() || !form.handle.trim()) {
      setError("Add a channel name and handle to continue.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await onCreate(form);
    } catch (createError) {
      setError(createError.message || "Could not create the channel.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="channel-dialog-title">
        <div className="dialog-topline"><span className="dialog-step"><Icon name="sparkle" size={15} /> Your creator space</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <h2 id="channel-dialog-title">Create your channel</h2>
        <p className="dialog-subtitle">Choose a name and make this space yours. You can add a banner and description too.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Channel name<input name="name" value={form.name} onChange={update} maxLength="100" placeholder="Choose a channel name" required /></label>
          <label>Handle<input name="handle" value={form.handle} onChange={update} maxLength="50" placeholder="@yourchannel" required /><small>Unique name viewers can use to find you.</small></label>
          <label>Description <span className="optional">Optional</span><textarea name="description" value={form.description} onChange={update} maxLength="500" rows="3" placeholder="What will you share on your channel?" /></label>
          <label>Banner image URL <span className="optional">Optional</span><input name="bannerUrl" type="url" value={form.bannerUrl} onChange={update} placeholder="https://example.com/banner.jpg" /></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="dialog-actions"><button type="button" className="text-button" onClick={onClose} disabled={busy}>Cancel</button><button className="button button-primary" type="submit" disabled={busy}>{busy ? "Creating…" : "Create channel"} {!busy && <Icon name="chevron" size={17} />}</button></div>
        </form>
      </section>
    </Modal>
  );
}

export function CreateVideoDialog({ channels, categories, onClose, onCreate }) {
  const uploadChannels = channels.filter((channel) => channel.backendChannel);
  const [form, setForm] = useState({
    title: "",
    description: "",
    thumbnailUrl: "",
    categoryId: "",
    channelId: uploadChannels[0]?.channelId ? String(uploadChannels[0].channelId) : "",
    visibility: "PUBLIC",
  });
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [previewError, setPreviewError] = useState(false);
  const [generatedThumbnail, setGeneratedThumbnail] = useState(null);
  const [generatedThumbnailUrl, setGeneratedThumbnailUrl] = useState("");
  const [generatingThumbnail, setGeneratingThumbnail] = useState(false);
  const [busy, setBusy] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  useEffect(() => () => {
    if (generatedThumbnailUrl) URL.revokeObjectURL(generatedThumbnailUrl);
  }, [generatedThumbnailUrl]);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    if (event.target.name === "thumbnailUrl") {
      setPreviewError(false);
      if (event.target.value.trim()) setGeneratedThumbnail(null);
    }
  }

  async function selectVideoFile(event) {
    const selectedFile = event.target.files?.[0] || null;
    setFile(selectedFile);
    setGeneratedThumbnail(null);
    setGeneratedThumbnailUrl("");
    if (!selectedFile || form.thumbnailUrl.trim()) return;
    setGeneratingThumbnail(true);
    setError("");
    try {
      const thumbnail = await createVideoThumbnail(selectedFile, form.title);
      setGeneratedThumbnail(thumbnail);
      setGeneratedThumbnailUrl(URL.createObjectURL(thumbnail));
    } catch (thumbnailError) {
      setError(thumbnailError.message || "Could not generate a thumbnail.");
    } finally {
      setGeneratingThumbnail(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    if (generatingThumbnail) {
      setError("Wait for the thumbnail preview to finish generating.");
      return;
    }
    if (!form.channelId || !uploadChannels.some((channel) => String(channel.channelId) === form.channelId)) {
      setError("Create a channel first, then you can upload a video.");
      return;
    }
    if (!file) {
      setError("Choose a video file.");
      return;
    }
    const mimeType = file.type || "application/octet-stream";
    if (!mimeType.startsWith("video/") && !["application/octet-stream", "application/ogg"].includes(mimeType)) {
      setError("Choose a video file such as MP4, WebM, MOV, MKV, or AVI.");
      return;
    }
    if (file.size <= 0 || file.size > MAX_VIDEO_BYTES) {
      setError("Choose a video smaller than 500 MB.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      await onCreate({ ...form, mimeType }, file, setUploadProgress, generatedThumbnail);
    } catch (uploadError) {
      setError(uploadError.message || "Video upload failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog video-dialog" role="dialog" aria-modal="true" aria-labelledby="video-dialog-title">
        <div className="dialog-topline"><span className="dialog-step"><Icon name="video" size={15} /> Creator studio</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <h2 id="video-dialog-title">Post a video</h2>
        <p className="dialog-subtitle">Your video uploads to R2, then converts for playback. Leave the thumbnail URL blank to create one automatically from the video. Maximum file size: 500 MB.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Video title<input name="title" value={form.title} onChange={update} maxLength="255" placeholder="Give your video a title" required /></label>
          <label>Video file<input type="file" accept="video/*,.mp4,.webm,.mov,.mkv,.avi,.m4v,.mpeg,.mpg,.wmv,.flv,.3gp,.3g2,.ts,.mts,.m2ts,.ogv" onChange={selectVideoFile} required /><small>{file ? `${file.name} · ${(file.size / (1024 * 1024)).toFixed(1)} MB` : "MP4, WebM, MOV, MKV, AVI and other video formats, up to 500 MB."}</small></label>
          <div className="form-two-col">
            <label>Channel<select name="channelId" value={form.channelId} onChange={update} required disabled={!uploadChannels.length}>
              <option value="">Select a channel</option>
              {uploadChannels.map((channel) => <option key={channel.channelId} value={channel.channelId}>{channel.name} · {channel.handle}</option>)}
            </select></label>
          </div>
          <div className="form-two-col">
            <label>Category<select name="categoryId" value={form.categoryId} onChange={update}><option value="">No category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
            <label>Visibility<select name="visibility" value={form.visibility} onChange={update}><option value="PUBLIC">Public</option><option value="UNLISTED">Unlisted</option><option value="PRIVATE">Private</option></select></label>
          </div>
          <label>Thumbnail URL <span className="optional">Optional</span><input name="thumbnailUrl" type="url" value={form.thumbnailUrl} onChange={update} placeholder="https://example.com/thumbnail.jpg" /></label>
          {form.thumbnailUrl && !previewError && <img className="thumbnail-preview" src={form.thumbnailUrl} alt="Thumbnail preview" onError={() => setPreviewError(true)} />}
          {!form.thumbnailUrl.trim() && generatedThumbnailUrl && <div className="generated-thumbnail-preview"><img className="thumbnail-preview" src={generatedThumbnailUrl} alt="Automatically generated video thumbnail preview" /><small>Thumbnail generated from your video</small></div>}
          <label>Description <span className="optional">Optional</span><textarea name="description" value={form.description} onChange={update} rows="3" maxLength="5000" placeholder="Tell viewers a little about this video" /></label>
          {!uploadChannels.length && <p className="inline-error" role="alert">Create a channel first.</p>}
          {error && <p className="inline-error" role="alert">{error}</p>}
          {generatingThumbnail && <p className="upload-progress" role="status">Generating thumbnail preview…</p>}
          {busy && <p className="upload-progress" role="status">{uploadProgress < 100 ? `Uploading to R2: ${uploadProgress}%` : "Finishing upload…"}</p>}
          <div className="dialog-actions"><button type="button" className="text-button" onClick={onClose} disabled={busy || generatingThumbnail}>Cancel</button><button className="button button-primary" type="submit" disabled={busy || generatingThumbnail}>{busy ? "Uploading…" : "Post video"} {!busy && <Icon name="chevron" size={17} />}</button></div>
        </form>
      </section>
    </Modal>
  );
}

export function ManageVideoDialog({ video, channels, categories, onClose, onSave, onPublish, onUnpublish, onDelete }) {
  const [form, setForm] = useState({
    title: video.title,
    description: video.description || "",
    thumbnailUrl: video.thumbnailUrl || "",
    categoryId: video.categoryId ? String(video.categoryId) : "",
    visibility: video.visibility,
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function run(action) {
    setBusy(true);
    setError("");
    try {
      const updatedVideo = await action();
      if (updatedVideo) {
        setForm((current) => ({
          ...current,
          title: updatedVideo.title ?? current.title,
          description: updatedVideo.description ?? "",
          thumbnailUrl: updatedVideo.thumbnailUrl ?? "",
          categoryId: updatedVideo.categoryId ? String(updatedVideo.categoryId) : "",
          visibility: updatedVideo.visibility ?? current.visibility,
        }));
      }
    } catch (actionError) {
      setError(actionError.message || "Could not update this video.");
    } finally {
      setBusy(false);
    }
  }

  function submit(event) {
    event.preventDefault();
    const channel = channels.find((item) => Number(item.channelId) === Number(video.channelId));
    if (!channel) {
      setError("The video's channel is not available.");
      return;
    }
    run(() => onSave(video, { ...form, channelId: channel.channelId }));
  }

  const isPublished = Boolean(video.publishedAt);
  const isReady = video.processingStatus === "READY" || video.processingStatus === "UPLOADED";

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="manage-video-title">
        <div className="dialog-topline"><span className="dialog-step"><Icon name="video" size={15} /> Video settings</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <h2 id="manage-video-title">Manage video</h2>
        <p className="dialog-subtitle">Status: {video.processingStatus} · {isPublished ? "Published" : "Not published"}</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Video title<input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} maxLength="255" required /></label>
          <label>Description<textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} rows="3" maxLength="5000" /></label>
          <label>Thumbnail URL<input type="text" value={form.thumbnailUrl} onChange={(event) => setForm((current) => ({ ...current, thumbnailUrl: event.target.value }))} maxLength="255" /></label>
          <label>Category<select value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}><option value="">No category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <label>Visibility<select value={form.visibility} onChange={(event) => setForm((current) => ({ ...current, visibility: event.target.value }))}><option value="PUBLIC">Public</option><option value="UNLISTED">Unlisted</option><option value="PRIVATE">Private</option></select></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="video-management-actions">
            <button type="button" className="text-button danger-text" disabled={busy} onClick={() => {
              if (window.confirm("Delete this video permanently?")) run(() => onDelete(video));
            }}>Delete</button>
            {isPublished
              ? <button type="button" className="text-button" disabled={busy} onClick={() => run(() => onUnpublish(video))}>Unpublish</button>
              : <button type="button" className="text-button" disabled={busy || !isReady || form.visibility === "PRIVATE"} onClick={() => {
                const channel = channels.find((item) => Number(item.channelId) === Number(video.channelId));
                if (!channel) {
                  setError("The video's channel is not available.");
                  return;
                }
                run(() => onPublish(video, { ...form, channelId: channel.channelId }));
              }}>Publish</button>}
            {!isPublished && form.visibility === "PRIVATE" && <small className="publish-hint">Choose Public or Unlisted and save before publishing.</small>}
            <button className="button button-primary" type="submit" disabled={busy}>{busy ? "Saving…" : "Save changes"}</button>
          </div>
        </form>
      </section>
    </Modal>
  );
}

export function ReportDialog({ userId, target, onClose, onSubmitted }) {
  const [reason, setReason] = useState("OTHER");
  const [details, setDetails] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await createReport({
        reporterId: Number(userId),
        ...target,
        reason,
        details: details.trim() || null,
      });
      onSubmitted();
    } catch (submitError) {
      setError(submitError.message || "Could not submit your report.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="report-dialog-title">
        <div className="dialog-topline">
          <span className="dialog-step">Community report</span>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close" disabled={busy}>
            <Icon name="close" />
          </button>
        </div>
        <h2 id="report-dialog-title">Report {target.videoId != null ? "video" : "comment"}</h2>
        <p className="dialog-subtitle">Tell us what is wrong. Reports are reviewed by the MotionVille team.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>
            Reason
            <select value={reason} onChange={(event) => setReason(event.target.value)}>
              <option value="SPAM">Spam</option>
              <option value="HARASSMENT">Harassment</option>
              <option value="HATE">Hate speech</option>
              <option value="VIOLENCE">Violence</option>
              <option value="COPYRIGHT">Copyright</option>
              <option value="OTHER">Other</option>
            </select>
          </label>
          <label>
            Details <span className="optional">Optional</span>
            <textarea
              value={details}
              onChange={(event) => setDetails(event.target.value)}
              maxLength={10000}
              rows={4}
              placeholder="Add context to help us review this report."
            />
          </label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="dialog-actions">
            <button type="button" className="text-button" onClick={onClose} disabled={busy}>Cancel</button>
            <button className="button button-primary" type="submit" disabled={busy}>
              {busy ? "Submitting…" : "Submit report"}
            </button>
          </div>
        </form>
      </section>
    </Modal>
  );
}

export function WatchDialog({ video, channel, recommendations, onSelectRecommendation, onSelectChannel, onGoHome, onClose, onLike, onSubscribe, onSavePlaylist, onReportVideo, onManageVideo, onDeleteVideo, onReportComment, onWatchProgress, onNotificationsChanged, liked, subscribed, currentUser }) {
  const reactionUserId = Number(currentUser?.id);
  const hasReactionUser = Number.isInteger(reactionUserId) && reactionUserId > 0;
  const [comment, setComment] = useState("");
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentsError, setCommentsError] = useState("");
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [repliesByComment, setRepliesByComment] = useState({});
  const [replyError, setReplyError] = useState("");
  const [repliesLoading, setRepliesLoading] = useState(false);
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [replySubmitting, setReplySubmitting] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentBody, setEditingCommentBody] = useState("");
  const [commentActionId, setCommentActionId] = useState(null);
  const [commentActionError, setCommentActionError] = useState("");
  const [playbackError, setPlaybackError] = useState(false);
  const [viewError, setViewError] = useState("");
  const [videoActionsOpen, setVideoActionsOpen] = useState(false);
  const [playerSettingsOpen, setPlayerSettingsOpen] = useState(false);
  const [playerQualityOpen, setPlayerQualityOpen] = useState(false);
  const [playerSpeedOpen, setPlayerSpeedOpen] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [playerCurrentTime, setPlayerCurrentTime] = useState(0);
  const [playerDuration, setPlayerDuration] = useState(0);
  const [playerPaused, setPlayerPaused] = useState(true);
  const videoActionsRef = useRef(null);
  const playerSettingsRef = useRef(null);

  const [playbackUrl, setPlaybackUrl] = useState("");
  const [playbackMimeType, setPlaybackMimeType] = useState("video/mp4");
  const [availableQualities, setAvailableQualities] = useState([]);
  const [selectedQuality, setSelectedQuality] = useState("auto");
  const [qualityLoading, setQualityLoading] = useState(false);
  const [qualityError, setQualityError] = useState("");
  const [posterUrl, setPosterUrl] = useState(video.thumbnailUrl || "");

  const [videoReaction, setVideoReactionState] = useState({
    likeCount: 0,
    dislikeCount: 0,
    userReaction: null,
  });
  const [videoReactionLoading, setVideoReactionLoading] = useState(false);
  const [videoReactionBusy, setVideoReactionBusy] = useState(false);
  const [reactionError, setReactionError] = useState("");
  const [commentReactions, setCommentReactions] = useState({});
  const [commentReactionError, setCommentReactionError] = useState("");
  const [commentReactionBusyId, setCommentReactionBusyId] = useState(null);

  useEffect(() => {
    if (!videoActionsOpen) return undefined;
    function closeOutside(event) {
      if (!videoActionsRef.current?.contains(event.target)) {
        setVideoActionsOpen(false);
      }
    }
    document.addEventListener("pointerdown", closeOutside);
    return () => document.removeEventListener("pointerdown", closeOutside);
  }, [videoActionsOpen]);

  useEffect(() => {
    if (!playerSettingsOpen) return undefined;
    function closeSettingsOutside(event) {
      if (!playerSettingsRef.current?.contains(event.target)) {
        setPlayerSettingsOpen(false);
        setPlayerQualityOpen(false);
        setPlayerSpeedOpen(false);
      }
    }
    document.addEventListener("pointerdown", closeSettingsOutside);
    return () => document.removeEventListener("pointerdown", closeSettingsOutside);
  }, [playerSettingsOpen]);

  const playerRef = useRef(null);
  const lastPlaybackPosition = useRef(0);
  const playerMetadataReady = useRef(false);
  const resumeApplied = useRef(false);
  const pendingQualitySeek = useRef(null);
  const pendingQualityPlay = useRef(false);
  const saveInProgress = useRef(false);
  const viewSessionId = useRef(crypto.randomUUID());
  const viewRequestAttempted = useRef(false);
  const viewRequestInProgress = useRef(false);
  const [resumePosition, setResumePosition] = useState(0);
  const [resumeReady, setResumeReady] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(!currentUser?.id || !video.serverVideo);
  const [historyError, setHistoryError] = useState("");
  const lastSavedPosition = useRef(0);

  useEffect(() => {
    if (playbackUrl) playerRef.current?.load();
  }, [playbackUrl]);

  useEffect(() => {
    if (!currentUser?.id || !video.serverVideo) {
      resumeApplied.current = false;
      setResumeReady(false);
      setHistoryLoaded(true);
      return undefined;
    }

    let active = true;
    resumeApplied.current = false;
    setResumeReady(false);
    setHistoryLoaded(false);
    setHistoryError("");
    setResumePosition(0);
    lastSavedPosition.current = 0;
    fetchWatchHistory(currentUser.id)
      .then((items) => {
        if (!active) return;
        const item = items.find((entry) => Number(entry.videoId) === Number(video.videoId));
        const position = item?.lastPositionSeconds || 0;
        setResumePosition(position);
        lastSavedPosition.current = position;
        lastPlaybackPosition.current = position;
      })
      .catch((error) => {
        if (active) setHistoryError(`Could not load watch progress: ${error.message}`);
      })
      .finally(() => {
        if (active) setHistoryLoaded(true);
      });

    return () => {
      active = false;
    };
  }, [currentUser?.id, video.serverVideo, video.videoId]);

  async function saveProgress(position, force = false) {
    if (!currentUser?.id || !video.serverVideo || !resumeApplied.current
        || !Number.isFinite(position) || saveInProgress.current) return;

    const seconds = Math.floor(position);
    if (!force && Math.abs(seconds - lastSavedPosition.current) < 10) return;

    saveInProgress.current = true;
    try {
      const savedProgress = await recordWatchProgress(video.videoId, currentUser.id, seconds);
      lastSavedPosition.current = seconds;
      setHistoryError("");
      onWatchProgress?.(savedProgress);
    } catch (error) {
      setHistoryError(`Could not save watch progress: ${error.message}`);
    } finally {
      saveInProgress.current = false;
    }
  }

  async function recordViewAtTime(player) {
    if (!video.serverVideo || viewRequestAttempted.current || viewRequestInProgress.current
        || !Number.isFinite(player.duration) || player.duration <= 0) return;

    const durationSeconds = Math.floor(player.duration);
    const requiredSeconds = durationSeconds < 30
      ? Math.ceil(durationSeconds / 2)
      : 30;
    const watchedSeconds = Math.floor(player.currentTime);
    if (watchedSeconds < requiredSeconds) return;

    viewRequestInProgress.current = true;
    try {
      const result = await recordVideoView(
        video.videoId,
        watchedSeconds,
        currentUser?.id ? Number(currentUser.id) : null,
        viewSessionId.current,
      );
      viewRequestAttempted.current = result.counted;
    } catch (error) {
      viewRequestAttempted.current = true;
      setViewError(error.message);
    } finally {
      viewRequestInProgress.current = false;
    }
  }

  function onPlayerMetadata() {
    playerMetadataReady.current = true;
    const player = playerRef.current;
    if (player) setPlayerDuration(player.duration);
    if (player && pendingQualitySeek.current !== null) {
      player.currentTime = Math.min(
        pendingQualitySeek.current,
        Math.max(0, player.duration - 0.5)
      );
      lastPlaybackPosition.current = player.currentTime;
      const shouldResume = pendingQualityPlay.current;
      pendingQualitySeek.current = null;
      pendingQualityPlay.current = false;
      if (shouldResume) {
        player.play().catch((error) => setQualityError(`Could not resume playback: ${error.message}`));
      }
      return;
    }
    if (!historyLoaded) return;
    resumePlayback();
  }

  function togglePlayback() {
    const player = playerRef.current;
    if (!player) return;
    if (player.paused) {
      player.play().catch((error) => setQualityError(`Could not start playback: ${error.message}`));
    } else {
      player.pause();
    }
  }

  function seekPlayback(event) {
    const position = Number(event.target.value);
    const player = playerRef.current;
    if (!player || !Number.isFinite(position)) return;
    player.currentTime = position;
    setPlayerCurrentTime(position);
    lastPlaybackPosition.current = position;
  }

  function changePlaybackSpeed(speed) {
    const player = playerRef.current;
    if (!player) return;
    player.playbackRate = speed;
    setPlaybackSpeed(speed);
    setPlayerSpeedOpen(false);
  }

  function togglePlayerFullscreen() {
    const playerContainer = playerRef.current?.parentElement;
    if (!playerContainer) return;
    if (document.fullscreenElement) {
      document.exitFullscreen().catch((error) => setQualityError(`Could not exit fullscreen: ${error.message}`));
    } else {
      playerContainer.requestFullscreen().catch((error) => setQualityError(`Could not enter fullscreen: ${error.message}`));
    }
  }

  function resumePlayback() {
    const player = playerRef.current;
    if (!player || !playerMetadataReady.current || !historyLoaded || resumeApplied.current) return;

    resumeApplied.current = true;
    if (resumePosition > 0 && resumePosition < player.duration - 5) {
      player.currentTime = resumePosition;
      lastPlaybackPosition.current = resumePosition;
      setResumeReady(true);
      return;
    }
    lastPlaybackPosition.current = player.currentTime;
    setResumeReady(true);
    saveProgress(player.currentTime, true);
  }

  useEffect(() => {
    resumePlayback();
  }, [historyLoaded, resumePosition]);

  useEffect(() => {
    const userId = currentUser?.id;
    const videoId = video.videoId;
    return () => {
      const position = lastPlaybackPosition.current;
      if (!userId || !video.serverVideo || !resumeApplied.current
          || !Number.isFinite(position)) return;
      recordWatchProgress(videoId, userId, Math.floor(position)).catch((error) => {
        console.error(`Could not save watch progress while closing the player: ${error.message}`);
      });
    };
  }, [currentUser?.id, video.serverVideo, video.videoId]);

  useEffect(() => {
    let active = true;
    setVideoReactionLoading(true);
    setReactionError("");
    setVideoReactionState({ likeCount: 0, dislikeCount: 0, userReaction: null });
    fetchVideoReaction(video.videoId, hasReactionUser ? reactionUserId : null)
      .then((summary) => {
        if (active) {
          setVideoReactionState(summary);
          onLike?.(video.videoId, summary.userReaction === "LIKE");
        }
      })
      .catch((error) => {
        if (active) setReactionError(error.message);
      })
      .finally(() => {
        if (active) setVideoReactionLoading(false);
      });

    return () => {
      active = false;
    };
  }, [hasReactionUser, reactionUserId, video.videoId]);

  async function reactToVideo(reaction) {
    if (!hasReactionUser) {
      setReactionError("Create a profile before reacting to videos.");
      return;
    }
    if (videoReactionBusy) return;
    setVideoReactionBusy(true);
    setReactionError("");

    try {
      if (videoReaction.userReaction === reaction) {
        await removeVideoReaction(video.videoId, reactionUserId);
      } else {
        await setVideoReaction(video.videoId, reaction, reactionUserId);
      }

      const summary = await fetchVideoReaction(video.videoId, reactionUserId);
      setVideoReactionState(summary);
      onLike?.(video.videoId, summary.userReaction === "LIKE");
      onNotificationsChanged?.();
    } catch (error) {
      setReactionError(error.message);
    } finally {
      setVideoReactionBusy(false);
    }
  }

  async function loadCommentReactionSummaries(items) {
    const results = await Promise.all(items.map(async (item) => {
      try {
        return [
          item.id,
          await fetchCommentReaction(item.id, hasReactionUser ? reactionUserId : null),
        ];
      } catch (error) {
        setCommentReactionError(error.message);
        return [item.id, { likeCount: 0, dislikeCount: 0, userReaction: null }];
      }
    }));
    setCommentReactions((current) => ({ ...current, ...Object.fromEntries(results) }));
  }

  useEffect(() => {
    const controller = new AbortController();

    setComments([]);
    setCommentReactions({});
    setCommentsError("");
    setCommentsLoading(true);

    fetchComments(video.videoId, { signal: controller.signal })
      .then((result) => {
        setComments(result);
        loadCommentReactionSummaries(result);
      })
      .catch((error) => {
        if (!controller.signal.aborted) setCommentsError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setCommentsLoading(false);
      });

    return () => controller.abort();
  }, [hasReactionUser, reactionUserId, video.videoId]);

  useEffect(() => {
    if (!video.serverVideo) return undefined;
    let active = true;
    setPosterUrl("");
    fetch(`${API_BASE_URL}/videos/${video.videoId}/thumbnail`)
      .then(async (response) => {
        if (!response.ok) throw new Error(await readResponseError(response));
        return response.json();
      })
      .then((thumbnail) => {
        if (active) setPosterUrl(thumbnail.thumbnailUrl);
      })
      .catch(() => {
        if (active) setPosterUrl("");
      });
    return () => {
      active = false;
    };
  }, [video.serverVideo, video.thumbnailUrl, video.videoId]);

  useEffect(() => {
    if (!video.serverVideo) return undefined;
    let active = true;
    let timer;
    const controller = new AbortController();
    setPlaybackUrl("");
    setAvailableQualities([]);
    setSelectedQuality("auto");
    setQualityError("");
    setPlaybackError(false);
    setPlayerCurrentTime(0);
    setPlayerDuration(0);
    setPlayerPaused(true);
    setPlaybackSpeed(1);
    async function preparePlayback() {
      try {
        const statusResponse = await fetch(`${API_BASE_URL}/videos/${video.videoId}/status`, { signal: controller.signal });
        if (!statusResponse.ok) throw new Error(await readResponseError(statusResponse));
        const status = await statusResponse.json();
        if (!active) return;
        if (status.processingStatus === "FAILED") throw new Error("Video conversion failed");
        if (status.processingStatus !== "READY" && status.processingStatus !== "UPLOADED") {
          timer = setTimeout(preparePlayback, 3000);
          return;
        }
        const [playbackResponse, assetsResponse] = await Promise.all([
          fetch(`${API_BASE_URL}/videos/${video.videoId}/playback`, { signal: controller.signal }),
          fetch(`${API_BASE_URL}/videos/${video.videoId}/assets`, { signal: controller.signal }),
        ]);
        if (!playbackResponse.ok) throw new Error(await readResponseError(playbackResponse));
        if (!assetsResponse.ok) throw new Error(await readResponseError(assetsResponse));
        const [playback, assets] = await Promise.all([
          playbackResponse.json(),
          assetsResponse.json(),
        ]);
        if (active) {
          setPlaybackUrl(playback.assetUrl);
          setPlaybackMimeType(playback.mimeType);
          setAvailableQualities(
            [...new Set(
              assets
                .map((asset) => asset.quality)
                .filter((quality) => /^(360|480|720|1080)p$/.test(quality))
            )].sort((a, b) => Number(b.slice(0, -1)) - Number(a.slice(0, -1)))
          );
        }
      } catch (error) {
        if (active && error.name !== "AbortError") setPlaybackError(true);
      }
    }
    preparePlayback();
    return () => {
      active = false;
      clearTimeout(timer);
      controller.abort();
    };
  }, [video.serverVideo, video.videoId]);

  useEffect(() => {
    if (!playbackUrl || playbackMimeType !== "application/vnd.apple.mpegurl") {
      return undefined;
    }
    const player = playerRef.current;
    if (!player) return undefined;

    if (player.canPlayType("application/vnd.apple.mpegurl")) {
      player.src = playbackUrl;
      return () => {
        player.removeAttribute("src");
        player.load();
      };
    }

    let active = true;
    let hls;
    import("hls.js")
      .then(({ default: Hls }) => {
        if (!active) return;
        if (!Hls.isSupported()) {
          setPlaybackError(true);
          return;
        }
        hls = new Hls();
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) setPlaybackError(true);
        });
        hls.loadSource(playbackUrl);
        hls.attachMedia(player);
      })
      .catch(() => {
        if (active) setPlaybackError(true);
      });
    return () => {
      active = false;
      hls?.destroy();
    };
  }, [playbackMimeType, playbackUrl]);

  async function changePlaybackQuality(quality) {
    if (quality === selectedQuality) return;
    const player = playerRef.current;
    if (quality !== "auto") {
      const available = availableQualities.includes(quality);
      if (!available) return;
    }
    pendingQualitySeek.current = player?.currentTime ?? 0;
    pendingQualityPlay.current = Boolean(player && !player.paused);
    setQualityLoading(true);
    setQualityError("");
    try {
      const response = await fetch(
        `${API_BASE_URL}/videos/${video.videoId}/playback?quality=${encodeURIComponent(quality)}`
      );
      if (!response.ok) throw new Error(await readResponseError(response));
      const playback = await response.json();
      setPlaybackUrl(playback.assetUrl);
      setPlaybackMimeType(playback.mimeType);
      setSelectedQuality(quality);
      setPlayerQualityOpen(false);
      setVideoActionsOpen(false);
    } catch (error) {
      pendingQualitySeek.current = null;
      pendingQualityPlay.current = false;
      setQualityError(error.message);
    } finally {
      setQualityLoading(false);
    }
  }

  async function addComment(event) {
    event.preventDefault();
    const body = comment.trim();
    if (!body) return;

    const authorId = Number(currentUser?.id);
    if (!Number.isInteger(authorId) || authorId <= 0) {
      setCommentsError("Create a profile before commenting.");
      return;
    }

    setCommentSubmitting(true);
    setCommentsError("");

    try {
      const savedComment = await createComment(
          video.videoId,
          authorId,
          body,
      );
      setComments((current) => [...current, savedComment]);
      loadCommentReactionSummaries([savedComment]);
      setComment("");
    } catch (error) {
      setCommentsError(error.message);
    } finally {
      setCommentSubmitting(false);
    }
  }
  async function toggleReplies(commentId) {
    setReplyError("");

    if (replyingTo === commentId) {
      setReplyingTo(null);
      return;
    }

    setReplyingTo(commentId);

    if (Object.hasOwn(repliesByComment, commentId)) return;

    setRepliesLoading(true);
    try {
      const replies = await fetchReplies(commentId);
      setRepliesByComment((current) => ({ ...current, [commentId]: replies }));
      loadCommentReactionSummaries(replies);
    } catch (error) {
      setReplyError(error.message);
    } finally {
      setRepliesLoading(false);
    }
  }

  async function reactToComment(commentId, currentReaction, nextReaction) {
    if (!hasReactionUser) {
      setCommentReactionError("Create a profile before reacting to comments.");
      return;
    }
    if (commentReactionBusyId === commentId) return;
    setCommentReactionBusyId(commentId);
    setCommentReactionError("");
    try {
      if (currentReaction === nextReaction) {
        await removeCommentReaction(commentId, reactionUserId);
      } else {
        await setCommentReaction(commentId, nextReaction, reactionUserId);
      }

      const summary = await fetchCommentReaction(commentId, reactionUserId);
      setCommentReactions((current) => ({
        ...current,
        [commentId]: summary,
      }));
      onNotificationsChanged?.();
    } catch (error) {
      setCommentReactionError(error.message);
    } finally {
      setCommentReactionBusyId(null);
    }
  }

  async function submitReply(event) {
    event.preventDefault();
    const body = replyText.trim();
    if (!body || replyingTo === null) return;

    const authorId = Number(currentUser?.id);
    if (!Number.isInteger(authorId) || authorId <= 0) {
      setReplyError("Create a profile before replying.");
      return;
    }

    setReplySubmitting(true);
    setReplyError("");

    try {
      const savedReply = await createReply(replyingTo, authorId, body);
      setRepliesByComment((current) => ({
        ...current,
        [replyingTo]: [...(current[replyingTo] || []), savedReply],
      }));
      loadCommentReactionSummaries([savedReply]);
      setReplyText("");
    } catch (error) {
      setReplyError(error.message);
    } finally {
      setReplySubmitting(false);
    }
  }

  function beginEdit(item) {
    setEditingCommentId(item.id);
    setEditingCommentBody(item.body);
    setCommentActionError("");
  }

  function replaceComment(updatedComment, parentCommentId) {
    if (parentCommentId === null) {
      setComments((current) => current.map((item) =>
        item.id === updatedComment.id ? updatedComment : item));
      return;
    }

    setRepliesByComment((current) => ({
      ...current,
      [parentCommentId]: (current[parentCommentId] || []).map((item) =>
        item.id === updatedComment.id ? updatedComment : item),
    }));
  }

  async function saveCommentEdit(event, parentCommentId = null) {
    event.preventDefault();
    const body = editingCommentBody.trim();
    if (!body || editingCommentId === null) return;

    setCommentActionId(editingCommentId);
    setCommentActionError("");
    try {
      const updatedComment = await updateComment(editingCommentId, body);
      replaceComment(updatedComment, parentCommentId);
      setEditingCommentId(null);
      setEditingCommentBody("");
    } catch (error) {
      setCommentActionError(error.message);
    } finally {
      setCommentActionId(null);
    }
  }

  async function removeComment(item, parentCommentId = null) {
    if (!window.confirm("Delete this comment?")) return;

    setCommentActionId(item.id);
    setCommentActionError("");
    try {
      await deleteComment(item.id);
      if (parentCommentId === null) {
        setComments((current) => current.map((commentItem) =>
          commentItem.id === item.id
            ? { ...commentItem, deleted: true, body: "This comment was deleted" }
            : commentItem));
      } else {
        setRepliesByComment((current) => ({
          ...current,
          [parentCommentId]: (current[parentCommentId] || [])
            .map((commentItem) => commentItem.id === item.id
              ? { ...commentItem, deleted: true, body: "This comment was deleted" }
              : commentItem),
        }));
      }
      if (editingCommentId === item.id) {
        setEditingCommentId(null);
        setEditingCommentBody("");
      }
    } catch (error) {
      setCommentActionError(error.message);
    } finally {
      setCommentActionId(null);
    }
  }

  return (
    <Modal onClose={onClose} className="watch-backdrop">
      <section className="watch-modal" role="dialog" aria-modal="true" aria-label={video.title}>
        <div className="watch-bar"><a className="brand brand-small" href="/" onClick={(event) => { event.preventDefault(); onGoHome(); }} aria-label="MotionVille home"><span className="brand-symbol"><Icon name="play" size={14} filled /></span><span>Motion<span className="brand-red">Ville</span></span></a><button className="icon-button" onClick={onClose} aria-label="Close player"><Icon name="close" /></button></div>
        <div className="watch-content">
          <div className="watch-primary">
            <div className="player-wrap">
              {playbackUrl && !playbackError
                ? <video
                      ref={playerRef}
                      controls={false}
                      controlsList="nodownload noremoteplayback"
                      disableRemotePlayback
                      autoPlay={resumeReady}
                      playsInline
                      poster={posterUrl || undefined}
                      onClick={togglePlayback}
                      onLoadedMetadata={onPlayerMetadata}
                      onPlay={() => setPlayerPaused(false)}
                      onContextMenu={(event) => event.preventDefault()}
                      onTimeUpdate={(event) => {
                        setPlayerCurrentTime(event.currentTarget.currentTime);
                        lastPlaybackPosition.current = event.currentTarget.currentTime;
                        recordViewAtTime(event.currentTarget);
                        saveProgress(event.currentTarget.currentTime);
                      }}
                      onPause={(event) => {
                        setPlayerPaused(true);
                        lastPlaybackPosition.current = event.currentTarget.currentTime;
                        saveProgress(event.currentTarget.currentTime, true);
                      }}
                      onEnded={() => {
                        setPlayerPaused(true);
                        setPlayerCurrentTime(0);
                        lastPlaybackPosition.current = 0;
                        saveProgress(0, true);
                      }}
                      onError={() => setPlaybackError(true)}
                  >
                    {playbackMimeType !== "application/vnd.apple.mpegurl"
                      && <source src={playbackUrl} type={playbackMimeType} />}
                    Your browser does not support video playback.
                  </video>
                : video.serverVideo && !playbackError
                  ? <div className="player-preparing" role="status" aria-label="Preparing video for playback">
                    {posterUrl && <img src={posterUrl} alt="" onError={() => setPosterUrl("")} />}
                    <div className="player-loading">
                      <span className="loading-spinner" aria-hidden="true" />
                      <strong>Preparing video…</strong>
                    </div>
                  </div>
                  : <div className="player-unavailable"><Icon name="video" size={34} /><strong>Video isn't playable</strong><span>{playbackError ? "Could not prepare this video. Check backend processing logs; the file may be damaged or use an unsupported codec." : "The video is being prepared for playback."}</span></div>}
              {playbackUrl && !playbackError && (
                <div className="custom-player-controls">
                  <input
                    className="player-seek"
                    type="range"
                    min="0"
                    max={playerDuration || 0}
                    step="0.1"
                    value={Math.min(playerCurrentTime, playerDuration || 0)}
                    onChange={seekPlayback}
                    aria-label="Seek video"
                  />
                  <div className="player-control-row">
                    <button
                      className="player-control-button"
                      type="button"
                      onClick={togglePlayback}
                      aria-label={playerPaused ? "Play video" : "Pause video"}
                    >
                      {playerPaused ? <Icon name="play" size={17} filled /> : <span className="player-pause-icon">Ⅱ</span>}
                    </button>
                    <span className="player-time">
                      {formatPlayerTime(playerCurrentTime)} / {formatPlayerTime(playerDuration)}
                    </span>
                    <div className="player-control-spacer" />
                    <div className="player-settings" ref={playerSettingsRef}>
                      <button
                        className="player-control-button"
                        type="button"
                        aria-label="Playback settings"
                        aria-expanded={playerSettingsOpen}
                        onClick={() => {
                          setPlayerSettingsOpen((open) => !open);
                          setPlayerQualityOpen(false);
                          setPlayerSpeedOpen(false);
                        }}
                      >
                        <Icon name="more" size={20} />
                      </button>
                      {playerSettingsOpen && (
                        <div className="player-settings-menu" role="menu" aria-label="Playback settings">
                          <button
                            className="video-actions-menu-item"
                            type="button"
                            role="menuitem"
                            aria-expanded={playerSpeedOpen}
                            onClick={() => {
                              setPlayerSpeedOpen((open) => !open);
                              setPlayerQualityOpen(false);
                            }}
                          >
                            Playback speed <span>{playbackSpeed === 1 ? "Normal" : `${playbackSpeed}x`}</span>
                          </button>
                          {playerSpeedOpen && (
                            <div className="video-quality-options" role="group" aria-label="Playback speed">
                              {[0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((speed) => (
                                <button
                                  key={speed}
                                  className={`video-actions-menu-item ${playbackSpeed === speed ? "video-quality-selected" : ""}`}
                                  type="button"
                                  role="menuitemradio"
                                  aria-checked={playbackSpeed === speed}
                                  onClick={() => changePlaybackSpeed(speed)}
                                >
                                  {speed === 1 ? "Normal" : `${speed}x`}
                                  {playbackSpeed === speed ? " ✓" : ""}
                                </button>
                              ))}
                            </div>
                          )}
                          {availableQualities.length > 0 ? (
                            <>
                              <button
                                className="video-actions-menu-item"
                                type="button"
                                role="menuitem"
                                aria-expanded={playerQualityOpen}
                                onClick={() => setPlayerQualityOpen((open) => !open)}
                              >
                                Quality <span>{selectedQuality === "auto" ? "Auto" : selectedQuality}</span>
                              </button>
                              {playerQualityOpen && (
                                <div className="video-quality-options" role="group" aria-label="Video quality">
                                  {["auto", ...availableQualities].map((quality) => (
                                    <button
                                      key={quality}
                                      className={`video-actions-menu-item ${selectedQuality === quality ? "video-quality-selected" : ""}`}
                                      type="button"
                                      role="menuitemradio"
                                      aria-checked={selectedQuality === quality}
                                      disabled={qualityLoading}
                                      onClick={() => changePlaybackQuality(quality)}
                                    >
                                      {quality === "auto" ? "Auto" : quality}
                                      {selectedQuality === quality ? " ✓" : ""}
                                    </button>
                                  ))}
                                </div>
                              )}
                            </>
                          ) : (
                            <span className="player-quality-unavailable">Quality options unavailable</span>
                          )}
                          {qualityError && <p className="video-quality-error" role="alert">{qualityError}</p>}
                        </div>
                      )}
                    </div>
                    <button
                      className="player-control-button"
                      type="button"
                      onClick={togglePlayerFullscreen}
                      aria-label="Toggle fullscreen"
                    >
                      <span className="player-fullscreen-icon">⛶</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
            {historyError && <p className="inline-error" role="alert">{historyError}</p>}
            {viewError && <p className="inline-error" role="alert">Could not record this video view: {viewError}</p>}
            <h1 className="watch-title">{video.title}</h1>
            <div className="watch-meta-row">
              <div className="watch-channel">
                <button
                  type="button"
                  className="watch-channel-identity"
                  onClick={() => channel && onSelectChannel?.(channel)}
                  disabled={!channel}
                >
                  <Avatar src={channel?.avatarUrl} name={channel?.name} size="large" />
                  <span>
                    <strong>{channel?.name || "MotionVille creator"}</strong>
                    <span>{channel?.handle || "@creator"}</span>
                  </span>
                </button>
                <button className={`button subscribe-button ${subscribed ? "button-subscribed" : "button-dark"}`} onClick={() => onSubscribe(channel?.channelId)}>{subscribed ? "Subscribed" : "Subscribe"}</button>
              </div>
              <div className="watch-actions">
                <button
                    className={`action-pill ${videoReaction.userReaction === "LIKE" ? "action-pill-selected" : ""}`}
                    onClick={() => reactToVideo("LIKE")}
                    disabled={videoReactionLoading || videoReactionBusy}
                >
                  <Icon name="like" size={18} filled={videoReaction.userReaction === "LIKE"} />
                  <span>Like {videoReaction.likeCount}</span>
                </button>

                <button
                    className={`action-pill ${videoReaction.userReaction === "DISLIKE" ? "action-pill-selected" : ""}`}
                    onClick={() => reactToVideo("DISLIKE")}
                    aria-label="Dislike video"
                    disabled={videoReactionLoading || videoReactionBusy}
                >
                  <Icon name="dislike" size={18} filled={videoReaction.userReaction === "DISLIKE"} />
                  <span>Dislike {videoReaction.dislikeCount}</span>
                </button>

                {video.serverVideo && (
                  <div className="watch-video-menu" ref={videoActionsRef}>
                    <button
                      className="action-pill"
                      type="button"
                      aria-label="More video options"
                      aria-expanded={videoActionsOpen}
                      onClick={() => setVideoActionsOpen((open) => !open)}
                    >
                      <Icon name="more" size={19} />
                    </button>
                    {videoActionsOpen && (
                      <div className="video-actions-menu" role="menu">
                        {onSavePlaylist && (
                          <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setVideoActionsOpen(false); onSavePlaylist(video); }}>
                            Save to playlist
                          </button>
                        )}
                        {onReportVideo && (
                          <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setVideoActionsOpen(false); onReportVideo(video); }}>
                            Report
                          </button>
                        )}
                        {Number(channel?.ownerId) === Number(currentUser?.id) && onManageVideo && (
                          <>
                            <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setVideoActionsOpen(false); onManageVideo(video); }}>
                              Edit video
                            </button>
                            {onDeleteVideo && (
                              <button className="video-actions-menu-item video-actions-danger" type="button" role="menuitem" onClick={() => { setVideoActionsOpen(false); onDeleteVideo(video); }}>
                                Delete video
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {reactionError && <p className="inline-error" role="alert">{reactionError}</p>}</div>
            </div>
            <div className="watch-description"><span>{formatAge(video.createdAt)}{video.category ? ` · ${video.category}` : ""}</span><p>{video.description || "No description added yet."}</p>{video.tags?.length > 0 && <div className="tag-row">{video.tags.map((tag) => <span key={tag}>#{tag.replace(/\s+/g, "")}</span>)}</div>}</div>

            <section className="comments-section">
              <h2>{comments.length} Comments</h2>

              <form className="comment-form" onSubmit={addComment}>
                <Avatar name="You" />
                <input
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    placeholder="Add a comment…"
                    aria-label="Add a comment"
                    maxLength={10000}
                />
                <button
                    disabled={!comment.trim() || commentSubmitting}
                    type="submit"
                >
                  {commentSubmitting ? "Posting…" : "Comment"}
                </button>
              </form>

              {commentsLoading && <p role="status">Loading comments…</p>}
              {commentsError && <p className="inline-error" role="alert">{commentsError}</p>}
              {commentActionError && <p className="inline-error" role="alert">{commentActionError}</p>}
              {commentReactionError && <p className="inline-error" role="alert">{commentReactionError}</p>}

              <div className="comment-list">
                {comments.map((item) => (
                  <article className="comment" key={item.id}>
                    <Avatar name={item.authorDisplayName || "Viewer"} />
                    <div className="comment-content">
                      <strong className="comment-author">
                        {item.authorDisplayName || "Viewer"}
                        <span> · {formatAge(item.createdAt)}</span>
                      </strong>
                      {editingCommentId === item.id
                        ? <form className="comment-edit-form" onSubmit={saveCommentEdit}>
                            <textarea
                              value={editingCommentBody}
                              onChange={(event) => setEditingCommentBody(event.target.value)}
                              aria-label="Edit comment"
                              maxLength={10000}
                              required
                            />
                            <div className="comment-actions">
                              <button className="comment-action" type="button" onClick={() => setEditingCommentId(null)}>Cancel</button>
                              <button className="comment-action" type="submit" disabled={!editingCommentBody.trim() || commentActionId === item.id}>
                                {commentActionId === item.id ? "Saving…" : "Save"}
                              </button>
                            </div>
                          </form>
                        : <>
                            <p>{item.body}</p>
                            <div className="comment-actions">
                              {!item.deleted && <>
                                <button
                                  className={`comment-action ${commentReactions[item.id]?.userReaction === "LIKE" ? "comment-action-selected" : ""}`}
                                  type="button"
                                  disabled={commentReactionBusyId === item.id}
                                  aria-pressed={commentReactions[item.id]?.userReaction === "LIKE"}
                                  onClick={() => reactToComment(item.id, commentReactions[item.id]?.userReaction, "LIKE")}
                                >
                                  Like {commentReactions[item.id]?.likeCount ?? 0}
                                </button>
                                <button
                                  className={`comment-action ${commentReactions[item.id]?.userReaction === "DISLIKE" ? "comment-action-selected" : ""}`}
                                  type="button"
                                  disabled={commentReactionBusyId === item.id}
                                  aria-pressed={commentReactions[item.id]?.userReaction === "DISLIKE"}
                                  onClick={() => reactToComment(item.id, commentReactions[item.id]?.userReaction, "DISLIKE")}
                                >
                                  Dislike {commentReactions[item.id]?.dislikeCount ?? 0}
                                </button>
                              </>}
                              <button
                                className="comment-action"
                                type="button"
                                onClick={() => toggleReplies(item.id)}
                                aria-expanded={replyingTo === item.id}
                              >
                                {replyingTo === item.id ? "Hide replies" : "Reply"}
                              </button>
                              {!item.deleted && <>
                                <button className="comment-action" type="button" onClick={() => beginEdit(item)}>Edit</button>
                                {onReportComment && (
                                  <button className="comment-action comment-action-report" type="button" onClick={() => onReportComment(item)}>
                                    Report
                                  </button>
                                )}
                                <button
                                  className="comment-action comment-action-delete"
                                  type="button"
                                  disabled={commentActionId === item.id}
                                  onClick={() => removeComment(item)}
                                >
                                  Delete
                                </button>
                              </>}
                            </div>
                          </>}

                      {replyingTo === item.id && (
                        <div className="comment-replies">
                          {repliesLoading && <p className="comment-status" role="status">Loading replies…</p>}
                          {replyError && <p className="inline-error" role="alert">{replyError}</p>}

                          {(repliesByComment[item.id] || []).map((reply) => (
                            <article className="comment comment-reply" key={reply.id}>
                              <Avatar name={reply.authorDisplayName || "Viewer"} />
                              <div className="comment-content">
                                <strong className="comment-author">
                                  {reply.authorDisplayName || "Viewer"}
                                  <span> · {formatAge(reply.createdAt)}</span>
                                </strong>
                                {editingCommentId === reply.id
                                  ? <form className="comment-edit-form" onSubmit={(event) => saveCommentEdit(event, item.id)}>
                                      <textarea
                                        value={editingCommentBody}
                                        onChange={(event) => setEditingCommentBody(event.target.value)}
                                        aria-label="Edit reply"
                                        maxLength={10000}
                                        required
                                      />
                                      <div className="comment-actions">
                                        <button className="comment-action" type="button" onClick={() => setEditingCommentId(null)}>Cancel</button>
                                        <button className="comment-action" type="submit" disabled={!editingCommentBody.trim() || commentActionId === reply.id}>
                                          {commentActionId === reply.id ? "Saving…" : "Save"}
                                        </button>
                                      </div>
                                    </form>
                                  : <>
                                      <p>{reply.body}</p>
                                      <div className="comment-actions">
                                        {!reply.deleted && <>
                                          <button
                                            className={`comment-action ${commentReactions[reply.id]?.userReaction === "LIKE" ? "comment-action-selected" : ""}`}
                                            type="button"
                                            disabled={commentReactionBusyId === reply.id}
                                            aria-pressed={commentReactions[reply.id]?.userReaction === "LIKE"}
                                            onClick={() => reactToComment(reply.id, commentReactions[reply.id]?.userReaction, "LIKE")}
                                          >
                                            Like {commentReactions[reply.id]?.likeCount ?? 0}
                                          </button>
                                          <button
                                            className={`comment-action ${commentReactions[reply.id]?.userReaction === "DISLIKE" ? "comment-action-selected" : ""}`}
                                            type="button"
                                            disabled={commentReactionBusyId === reply.id}
                                            aria-pressed={commentReactions[reply.id]?.userReaction === "DISLIKE"}
                                            onClick={() => reactToComment(reply.id, commentReactions[reply.id]?.userReaction, "DISLIKE")}
                                          >
                                            Dislike {commentReactions[reply.id]?.dislikeCount ?? 0}
                                          </button>
                                          <button className="comment-action" type="button" onClick={() => beginEdit(reply)}>Edit</button>
                                          {onReportComment && (
                                            <button className="comment-action comment-action-report" type="button" onClick={() => onReportComment(reply)}>
                                              Report
                                            </button>
                                          )}
                                          <button
                                            className="comment-action comment-action-delete"
                                            type="button"
                                            disabled={commentActionId === reply.id}
                                            onClick={() => removeComment(reply, item.id)}
                                          >
                                            Delete
                                          </button>
                                        </>}
                                      </div>
                                    </>}
                              </div>
                            </article>
                          ))}

                          <form className="comment-form comment-reply-form" onSubmit={submitReply}>
                            <Avatar name="You" />
                            <input
                              value={replyText}
                              onChange={(event) => setReplyText(event.target.value)}
                              placeholder="Write a reply…"
                              aria-label={`Reply to ${item.authorDisplayName || "comment"}`}
                              maxLength={10000}
                            />
                            <button disabled={!replyText.trim() || replySubmitting} type="submit">
                              {replySubmitting ? "Posting…" : "Reply"}
                            </button>
                          </form>
                        </div>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </div>
          <section className="watch-recommendations" aria-label="More videos">
            <div className="recommendations-heading"><span>More videos</span><span>{recommendations.length} videos</span></div>
            {recommendations.length
              ? <div className="recommendations-grid">{recommendations.map((item, index) => (
                <VideoCard key={item.videoId} video={item} channel={item.channel}
                  onSelect={onSelectRecommendation} onSelectChannel={onSelectChannel} index={index} />
              ))}</div>
              : <p className="recommendations-empty">No other videos to show yet.</p>}
          </section>
        </div>
      </section>
    </Modal>
  );
}
