import { useEffect, useRef, useState } from "react";
import { formatAge, formatDuration, formatViews } from "../utils/format.js";
import { API_BASE_URL, readResponseError } from "../api/videoApi.js";
import { fetchVideoViews } from "../api/videoViewApi.js";

export function Icon({ name, size = 21, filled = false }) {
  const shared = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: filled ? "currentColor" : "none",
    stroke: "currentColor",
    strokeWidth: "1.8",
    strokeLinecap: "round",
    strokeLinejoin: "round",
    "aria-hidden": true,
  };
  const paths = {
    menu: <><path d="M4 6h16M4 12h16M4 18h16" /></>,
    home: <><path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z" /></>,
    compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5z" /></>,
    shorts: <><path d="M8 3h8l5 7-9 4-9-4zM8 21h8l5-7-9-4-9 4z" /><path d="m10 8 4 2-4 2z" /></>,
    subscriptions: <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="m10 9 5 3-5 3z" /></>,
    library: <><path d="M4 5v14M8 5v14M12 5v14M16 5v14M20 5v14" /></>,
    history: <><path d="M3 12a9 9 0 1 0 2.6-6.4L3 8" /><path d="M3 3v5h5M12 7v5l3 2" /></>,
    like: <><path d="M7 10v11H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2zm0 0 4-8a3 3 0 0 1 2 3v3h5.5a3 3 0 0 1 2.9 3.8l-2 7A3 3 0 0 1 16.5 21H7" /></>,
    dislike: <><path d="M7 14V3H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2zm0 0 4 8a3 3 0 0 0 2-3v-3h5.5a3 3 0 0 0 2.9-3.8l-2-7A3 3 0 0 0 16.5 3H7" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    play: <><path d="m8 5 11 7-11 7z" /></>,
    close: <><path d="m18 6-12 12M6 6l12 12" /></>,
    more: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>,
    share: <><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.6 10.7 6.8-4.4m-6.8 7 6.8 4.1" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    video: <><rect x="3" y="5" width="13" height="14" rx="2" /><path d="m16 10 5-3v10l-5-3z" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    check: <path d="m5 12 4 4L19 6" />,
    sparkle: <><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z" /><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z" /></>,
    filter: <><path d="M4 7h16M7 12h10m-7 5h4" /></>,
    grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /></>,
    edit: <><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></>,
  };
  return <svg {...shared}>{paths[name]}</svg>;
}

export function Avatar({ src, name, size = "normal" }) {
  return src
    ? <img className={`avatar avatar-${size}`} src={src} alt="" />
    : <span className={`avatar avatar-fallback avatar-${size}`}>{(name || "M").slice(0, 1).toUpperCase()}</span>;
}

export function VideoCard({ video, channel, onSelect, onSelectChannel, onManage, onDelete, canManage, onSavePlaylist, onReport, onRemoveHistory, removingHistoryVideoId, index }) {
  const [thumbnailUrl, setThumbnailUrl] = useState(video.thumbnailUrl || "");
  const [actionsOpen, setActionsOpen] = useState(false);
  const [viewCount, setViewCount] = useState(video.views ?? null);
  const actionsRef = useRef(null);

  useEffect(() => {
    let active = true;
    if (viewCount === null && video.videoId) {
      fetchVideoViews(video.videoId)
        .then((cnt) => {
          if (active && typeof cnt === "number") setViewCount(cnt);
        })
        .catch(() => {});
    }
    return () => {
      active = false;
    };
  }, [video.videoId, viewCount]);

  useEffect(() => {
    setThumbnailUrl(video.thumbnailUrl || "");
    if (!video.serverVideo) return undefined;
    let active = true;
    fetch(`${API_BASE_URL}/videos/${video.videoId}/thumbnail`)
      .then(async (response) => {
        if (!response.ok) throw new Error(await readResponseError(response));
        return response.json();
      })
      .then((result) => {
        if (active) setThumbnailUrl(result.thumbnailUrl);
      })
      .catch(() => {
        if (active) setThumbnailUrl("");
      });
    return () => {
      active = false;
    };
  }, [video.serverVideo, video.thumbnailUrl, video.videoId]);

  useEffect(() => {
    if (!actionsOpen) return undefined;
    function closeOutside(event) {
      if (!actionsRef.current?.contains(event.target)) setActionsOpen(false);
    }
    document.addEventListener("pointerdown", closeOutside);
    return () => document.removeEventListener("pointerdown", closeOutside);
  }, [actionsOpen]);

  return (
    <article className="video-card" style={{ "--card-order": index }}>
      <button className="video-thumb" onClick={() => onSelect(video)} aria-label={`Watch ${video.title}`}>
        {thumbnailUrl
          ? <img src={thumbnailUrl} alt="" loading="lazy" onError={() => setThumbnailUrl("")} />
          : <div className={`thumb-gradient gradient-${index % 5}`}><Icon name="play" size={34} /></div>}
        <span className="duration-badge">{formatDuration(video.durationSeconds)}</span>
        <span className="thumb-play"><Icon name="play" size={22} filled /></span>
      </button>
      <div className="video-card-info">
        <Avatar src={channel?.avatarUrl} name={channel?.name} />
        <div className="video-card-text">
          <button className="video-card-title" onClick={() => onSelect(video)}>{video.title}</button>
          <button
            className="channel-link"
            type="button"
            onClick={() => channel && onSelectChannel?.(channel)}
            disabled={!channel}
          >
            {channel?.name || "MotionVille creator"}
          </button>
          <p>{viewCount == null ? formatAge(video.createdAt) : `${formatViews(viewCount)} • ${formatAge(video.createdAt)}`}</p>
          {Number.isFinite(video.resumePositionSeconds) && (
            <div className="watch-progress">
              <span>Continue watching · {formatDuration(video.resumePositionSeconds)}</span>
              <span className="watch-progress-track"><span style={{ width: `${video.watchProgressPercent || 0}%` }} /></span>
            </div>
          )}
        </div>
        <div className="video-card-actions" ref={actionsRef}>
          <button
            className="more-button"
            type="button"
            aria-label={`More options for ${video.title}`}
            aria-expanded={actionsOpen}
            onClick={() => setActionsOpen((open) => !open)}
          >
            <Icon name="more" size={19} />
          </button>
          {actionsOpen && (
            <div className="video-actions-menu" role="menu">
              {onRemoveHistory && (
                <button
                  className="video-actions-menu-item video-actions-danger"
                  type="button"
                  role="menuitem"
                  disabled={removingHistoryVideoId === video.videoId}
                  onClick={() => {
                    setActionsOpen(false);
                    onRemoveHistory(video);
                  }}
                >
                  {removingHistoryVideoId === video.videoId ? "Removing…" : "Remove from history"}
                </button>
              )}
              {video.serverVideo && onSavePlaylist && (
                <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setActionsOpen(false); onSavePlaylist(video); }}>Save to playlist</button>
              )}
              {video.serverVideo && onReport && (
                <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setActionsOpen(false); onReport(video); }}>Report</button>
              )}
              {video.serverVideo && canManage && (
                <>
                  <button className="video-actions-menu-item" type="button" role="menuitem" onClick={() => { setActionsOpen(false); onManage(video); }}>Edit video</button>
                  <button className="video-actions-menu-item video-actions-danger" type="button" role="menuitem" onClick={() => { setActionsOpen(false); onDelete(video); }}>Delete video</button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </article>
  );
}

export function Modal({ children, onClose, className = "" }) {
  useEffect(() => {
    function closeOnEscape(event) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", closeOnEscape);
    document.body.classList.add("modal-open");
    return () => {
      window.removeEventListener("keydown", closeOnEscape);
      document.body.classList.remove("modal-open");
    };
  }, [onClose]);

  return (
    <div className={`modal-backdrop ${className}`} onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      {children}
    </div>
  );
}
