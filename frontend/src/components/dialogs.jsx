import { useEffect, useState } from "react";
import { API_BASE_URL, createVideoThumbnail, MAX_VIDEO_BYTES, readResponseError } from "../api/videoApi.js";
import { formatAge } from "../utils/format.js";
import { Avatar, Icon, Modal, VideoCard } from "./ui.jsx";
import {fetchComments, createComment, fetchReplies, createReply,} from "../api/commentApi.js";

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

export function WatchDialog({ video, channel, recommendations, onSelectRecommendation, onClose, onLike, onSubscribe, onComment, liked, subscribed }) {
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
  const [playbackError, setPlaybackError] = useState(false);

  const [playbackUrl, setPlaybackUrl] = useState("");
  const [playbackMimeType, setPlaybackMimeType] = useState("video/mp4");
  const [posterUrl, setPosterUrl] = useState(video.thumbnailUrl || "");

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
    setPlaybackError(false);
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
        const response = await fetch(`${API_BASE_URL}/videos/${video.videoId}/playback`, { signal: controller.signal });
        if (!response.ok) throw new Error(await readResponseError(response));
        const playback = await response.json();
        if (active) {
          setPlaybackUrl(playback.assetUrl);
          setPlaybackMimeType(playback.mimeType);
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

  function addComment(event) {
    event.preventDefault();
    if (!comment.trim()) return;
    onComment(video.videoId, {
      id: Date.now(),
      author: { displayName: "You", username: "you", avatarUrl: "" },
      body: comment.trim(),
      createdAt: new Date().toISOString(),
    });
    setComment("");
  }

  return (
    <Modal onClose={onClose} className="watch-backdrop">
      <section className="watch-modal" role="dialog" aria-modal="true" aria-label={video.title}>
        <div className="watch-bar"><a className="brand brand-small"><span className="brand-symbol"><Icon name="play" size={14} filled /></span><span>Motion<span className="brand-red">Ville</span></span></a><button className="icon-button" onClick={onClose} aria-label="Close player"><Icon name="close" /></button></div>
        <div className="watch-content">
          <div className="watch-primary">
            <div className="player-wrap">
              {playbackUrl && !playbackError
                ? <video controls autoPlay playsInline poster={posterUrl || undefined} onError={() => setPlaybackError(true)}><source src={playbackUrl} type={playbackMimeType} />Your browser does not support video playback.</video>
                : video.serverVideo && !playbackError
                  ? <div className="player-preparing" role="status" aria-label="Preparing video for playback">
                    {posterUrl && <img src={posterUrl} alt="" onError={() => setPosterUrl("")} />}
                    <div className="player-loading">
                      <span className="loading-spinner" aria-hidden="true" />
                      <strong>Preparing video…</strong>
                    </div>
                  </div>
                  : <div className="player-unavailable"><Icon name="video" size={34} /><strong>Video isn't playable</strong><span>{playbackError ? "Could not prepare this video. Check backend processing logs; the file may be damaged or use an unsupported codec." : "The video is being prepared for playback."}</span></div>}
            </div>
            <h1 className="watch-title">{video.title}</h1>
            <div className="watch-meta-row">
              <div className="watch-channel"><Avatar src={channel?.avatarUrl} name={channel?.name} size="large" /><div><strong>{channel?.name || "MotionVille creator"}</strong><span>{channel?.handle || "@creator"}</span></div><button className={`button subscribe-button ${subscribed ? "button-subscribed" : "button-dark"}`} onClick={() => onSubscribe(channel?.channelId)}>{subscribed ? "Subscribed" : "Subscribe"}</button></div>
              <div className="watch-actions">
                <button className={`action-pill ${liked ? "action-pill-selected" : ""}`} onClick={() => onLike(video.videoId)}><Icon name="like" size={18} filled={liked} /><span>{liked ? "Liked" : "Like"}</span></button>
                <button className="action-pill" onClick={() => navigator.clipboard?.writeText(playbackUrl)}><Icon name="share" size={17} /><span>Share</span></button>10. Frontend
Build:
• Comments/replies.
• Like/dislike UI.
• Playlist creation/management.
• Add/remove/reorder videos.
• Watch history and resume playback.
• Notifications and read status.
• Report video/comment.
              </div>
            </div>
            <div className="watch-description"><span>{formatAge(video.createdAt)}{video.category ? ` · ${video.category}` : ""}</span><p>{video.description || "No description added yet."}</p>{video.tags?.length > 0 && <div className="tag-row">{video.tags.map((tag) => <span key={tag}>#{tag.replace(/\s+/g, "")}</span>)}</div>}</div>
            <section className="comments-section">
              <h2>{comments.length} Comments</h2>
              <form className="comment-form" onSubmit={addComment}><Avatar name="You" /><input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Add a comment…" aria-label="Add a comment" /><button disabled={!comment.trim()} type="submit">Comment</button></form>
              <div className="comment-list">{comments.map((item) => <article className="comment" key={item.id}><Avatar src={item.author?.avatarUrl} name={item.author?.displayName || item.author?.username} /><div><strong>{item.author?.displayName || item.author?.username || "Viewer"} <span>· {formatAge(item.createdAt)}</span></strong><p>{item.body}</p><button className="comment-like"><Icon name="like" size={15} /> Like <span>Reply</span></button></div></article>)}</div>
            </section>
          </div>
          <section className="watch-recommendations" aria-label="More videos">
            <div className="recommendations-heading"><span>More videos</span><span>{recommendations.length} videos</span></div>
            {recommendations.length
              ? <div className="recommendations-grid">{recommendations.map((item, index) => (
                <VideoCard key={item.videoId} video={item} channel={item.channel}
                  onSelect={onSelectRecommendation} onManage={() => {}} onDelete={() => {}} index={index} />
              ))}</div>
              : <p className="recommendations-empty">No other videos to show yet.</p>}
          </section>
        </div>
      </section>
    </Modal>
  );
}
