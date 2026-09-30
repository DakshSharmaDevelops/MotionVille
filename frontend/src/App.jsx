import { useEffect, useMemo, useState } from "react";

const VIDEO_URL = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";
const SEED_CHANNELS = [
  {
    channelId: 1,
    ownerId: 1,
    name: "MotionVille Studio",
    handle: "@motionvillestudio",
    description: "A little inspiration for wherever you're headed.",
    bannerUrl: "",
    avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&h=160&fit=crop&crop=faces",
    createdAt: "2025-01-12T10:00:00Z",
    subscriptions: 28400,
  },
  {
    channelId: 2,
    ownerId: 2,
    name: "Northstar Films",
    handle: "@northstarfilms",
    description: "Small stories. Big skies.",
    bannerUrl: "",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&h=160&fit=crop&crop=faces",
    createdAt: "2025-02-03T10:00:00Z",
    subscriptions: 9160,
  },
  {
    channelId: 3,
    ownerId: 3,
    name: "The Sunday Table",
    handle: "@sundaytable",
    description: "Recipes for slowing down.",
    bannerUrl: "",
    avatarUrl: "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=160&h=160&fit=crop&crop=faces",
    createdAt: "2025-03-20T10:00:00Z",
    subscriptions: 52600,
  },
];

const SEED_VIDEOS = [
  {
    videoId: 101,
    channelId: 1,
    title: "A slower morning in the mountains",
    description: "A few quiet moments from a weekend up north. Sometimes the best plan is no plan at all.",
    thumbnailUrl: "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=1000&h=563&fit=crop",
    durationSeconds: 638,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Travel & Places",
    tags: ["travel", "nature", "slow living"],
    createdAt: "2026-08-20T10:00:00Z",
    views: 18400,
    likeCount: 926,
  },
  {
    videoId: 102,
    channelId: 2,
    title: "Finding the last light on the coast",
    description: "Chasing the last warm light of the day along the Pacific coast.",
    thumbnailUrl: "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?w=1000&h=563&fit=crop",
    durationSeconds: 492,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Travel & Places",
    tags: ["ocean", "cinematic"],
    createdAt: "2026-08-18T10:00:00Z",
    views: 92700,
    likeCount: 3400,
  },
  {
    videoId: 103,
    channelId: 3,
    title: "The perfect Sunday tomato pasta",
    description: "A simple, no-fuss pasta with ripe tomatoes, basil and a little patience.",
    thumbnailUrl: "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=1000&h=563&fit=crop",
    durationSeconds: 756,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Food & Cooking",
    tags: ["recipe", "pasta", "cooking"],
    createdAt: "2026-08-17T10:00:00Z",
    views: 241000,
    likeCount: 12800,
  },
  {
    videoId: 104,
    channelId: 1,
    title: "A tiny desk reset that changed my week",
    description: "Clearing the clutter, setting an intention, and getting back into a good rhythm.",
    thumbnailUrl: "https://images.unsplash.com/photo-1497366754035-f200968a6e72?w=1000&h=563&fit=crop",
    durationSeconds: 381,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Lifestyle",
    tags: ["workspace", "routine"],
    createdAt: "2026-08-14T10:00:00Z",
    views: 31900,
    likeCount: 1400,
  },
  {
    videoId: 105,
    channelId: 2,
    title: "A quiet walk through old Kyoto",
    description: "Early morning streets, wooden doorways, and nowhere particular to be.",
    thumbnailUrl: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1000&h=563&fit=crop",
    durationSeconds: 824,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Travel & Places",
    tags: ["japan", "travel", "walking"],
    createdAt: "2026-08-12T10:00:00Z",
    views: 63800,
    likeCount: 2810,
  },
  {
    videoId: 106,
    channelId: 3,
    title: "Five lunches I make on repeat",
    description: "Easy lunch ideas for busy weekdays, made with things already in the kitchen.",
    thumbnailUrl: "https://images.unsplash.com/photo-1547592180-85f173990554?w=1000&h=563&fit=crop",
    durationSeconds: 604,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Food & Cooking",
    tags: ["food", "meal prep"],
    createdAt: "2026-08-10T10:00:00Z",
    views: 126000,
    likeCount: 6570,
  },
  {
    videoId: 107,
    channelId: 1,
    title: "The little details of a rainy city",
    description: "Reflections, street lights and the lovely hush after the rain.",
    thumbnailUrl: "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1000&h=563&fit=crop",
    durationSeconds: 437,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Arts & Culture",
    tags: ["city", "cinematic"],
    createdAt: "2026-08-08T10:00:00Z",
    views: 15700,
    likeCount: 608,
  },
  {
    videoId: 108,
    channelId: 2,
    title: "Build a weekend cabin playlist with me",
    description: "A soundtrack for packing a bag and getting out of town.",
    thumbnailUrl: "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1000&h=563&fit=crop",
    durationSeconds: 1080,
    visibility: "PUBLIC",
    processingStatus: "UPLOADED",
    assetUrl: VIDEO_URL,
    mimeType: "video/mp4",
    category: "Music",
    tags: ["music", "playlist"],
    createdAt: "2026-08-06T10:00:00Z",
    views: 48300,
    likeCount: 2100,
  },
];

const CATEGORIES = ["All", "Travel & Places", "Food & Cooking", "Lifestyle", "Arts & Culture", "Music"];

function loadLocal(key, fallback) {
  const saved = localStorage.getItem(key);
  if (!saved) return fallback;
  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) ? parsed : fallback;
  } catch {
    return fallback;
  }
}

function Icon({ name, size = 21, filled = false }) {
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
  };
  return <svg {...shared}>{paths[name]}</svg>;
}

function formatViews(value = 0) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1).replace(/\.0$/, "")}M views`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(value >= 10_000 ? 0 : 1).replace(/\.0$/, "")}K views`;
  return `${value} views`;
}

function formatAge(value) {
  const days = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000));
  if (days < 1) return "Today";
  if (days < 30) return `${days} days ago`;
  if (days < 365) return `${Math.floor(days / 30)} months ago`;
  return `${Math.floor(days / 365)} years ago`;
}

function formatDuration(value = 0) {
  const minutes = Math.floor(value / 60);
  const seconds = String(value % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function Avatar({ src, name, size = "normal" }) {
  return src
    ? <img className={`avatar avatar-${size}`} src={src} alt="" />
    : <span className={`avatar avatar-fallback avatar-${size}`}>{(name || "M").slice(0, 1).toUpperCase()}</span>;
}

function VideoCard({ video, channel, onSelect, index }) {
  return (
    <article className="video-card" style={{ "--card-order": index }}>
      <button className="video-thumb" onClick={() => onSelect(video)} aria-label={`Watch ${video.title}`}>
        {video.thumbnailUrl
          ? <img src={video.thumbnailUrl} alt="" loading="lazy" />
          : <div className={`thumb-gradient gradient-${index % 5}`}><Icon name="play" size={34} /></div>}
        <span className="duration-badge">{formatDuration(video.durationSeconds)}</span>
        <span className="thumb-play"><Icon name="play" size={22} filled /></span>
      </button>
      <div className="video-card-info">
        <Avatar src={channel?.avatarUrl} name={channel?.name} />
        <div className="video-card-text">
          <button className="video-card-title" onClick={() => onSelect(video)}>{video.title}</button>
          <button className="channel-link">{channel?.name || "MotionVille creator"}</button>
          <p>{formatViews(video.views)} <span>·</span> {formatAge(video.createdAt)}</p>
        </div>
        <button className="more-button" aria-label="More options"><Icon name="more" size={19} /></button>
      </div>
    </article>
  );
}

function Modal({ children, onClose, className = "" }) {
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

function CreateChannelDialog({ onClose, onCreate }) {
  const [form, setForm] = useState({ name: "", handle: "", description: "", bannerUrl: "" });
  const [error, setError] = useState("");

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name === "handle"
        ? `@${value.replace(/^@/, "").toLowerCase().replace(/[^a-z0-9._-]/g, "")}`
        : value,
    }));
  }

  function submit(event) {
    event.preventDefault();
    if (!form.name.trim() || !form.handle.trim()) {
      setError("Add a channel name and handle to continue.");
      return;
    }
    onCreate(form);
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="channel-dialog-title">
        <div className="dialog-topline"><span className="dialog-step"><Icon name="sparkle" size={15} /> Your creator space</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <h2 id="channel-dialog-title">Create your channel</h2>
        <p className="dialog-subtitle">Choose a name and make this space yours. You can add a banner and description too.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Channel name<input name="name" value={form.name} onChange={update} maxLength="100" placeholder="e.g. Daksh Makes Things" required /></label>
          <label>Handle<input name="handle" value={form.handle} onChange={update} maxLength="50" placeholder="@yourchannel" required /><small>Unique name viewers can use to find you.</small></label>
          <label>Description <span className="optional">Optional</span><textarea name="description" value={form.description} onChange={update} maxLength="500" rows="3" placeholder="What will you share on your channel?" /></label>
          <label>Banner image URL <span className="optional">Optional</span><input name="bannerUrl" type="url" value={form.bannerUrl} onChange={update} placeholder="https://example.com/banner.jpg" /></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="dialog-actions"><button type="button" className="text-button" onClick={onClose}>Cancel</button><button className="button button-primary" type="submit">Create channel <Icon name="chevron" size={17} /></button></div>
        </form>
      </section>
    </Modal>
  );
}

function CreateVideoDialog({ channels, onClose, onCreate }) {
  const [form, setForm] = useState({
    title: "",
    description: "",
    thumbnailUrl: "",
    assetUrl: "",
    mimeType: "video/mp4",
    category: "Travel & Places",
    channelId: channels[0]?.channelId ? String(channels[0].channelId) : "",
    visibility: "PUBLIC",
  });
  const [error, setError] = useState("");
  const [previewError, setPreviewError] = useState(false);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    if (event.target.name === "thumbnailUrl") setPreviewError(false);
  }

  function submit(event) {
    event.preventDefault();
    if (!form.channelId) {
      setError("Create a channel first, then you can post a video.");
      return;
    }
    try {
      const parsed = new URL(form.assetUrl);
      if (!["http:", "https:"].includes(parsed.protocol)) throw new Error("URL must use HTTP or HTTPS.");
    } catch {
      setError("Enter a valid direct video URL (MP4 or WebM).");
      return;
    }
    onCreate(form);
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog video-dialog" role="dialog" aria-modal="true" aria-labelledby="video-dialog-title">
        <div className="dialog-topline"><span className="dialog-step"><Icon name="video" size={15} /> Creator studio</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <h2 id="video-dialog-title">Post a video</h2>
        <p className="dialog-subtitle">Add a direct video URL. MotionVille saves the link and details, not the video file.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Video title<input name="title" value={form.title} onChange={update} maxLength="255" placeholder="Give your video a title" required /></label>
          <label>Video URL<input name="assetUrl" type="url" value={form.assetUrl} onChange={update} placeholder="https://example.com/my-video.mp4" required /><small>Use a direct, publicly accessible MP4 or WebM link.</small></label>
          <div className="form-two-col">
            <label>Channel<select name="channelId" value={form.channelId} onChange={update} required>
              {channels.map((channel) => <option key={channel.channelId} value={channel.channelId}>{channel.name} · {channel.handle}</option>)}
            </select></label>
            <label>Video format<select name="mimeType" value={form.mimeType} onChange={update}><option value="video/mp4">MP4</option><option value="video/webm">WebM</option></select></label>
          </div>
          <div className="form-two-col">
            <label>Category<select name="category" value={form.category} onChange={update}>{CATEGORIES.slice(1).map((category) => <option key={category}>{category}</option>)}</select></label>
            <label>Visibility<select name="visibility" value={form.visibility} onChange={update}><option value="PUBLIC">Public</option><option value="UNLISTED">Unlisted</option><option value="PRIVATE">Private</option></select></label>
          </div>
          <label>Thumbnail URL <span className="optional">Optional</span><input name="thumbnailUrl" type="url" value={form.thumbnailUrl} onChange={update} placeholder="https://example.com/thumbnail.jpg" /></label>
          {form.thumbnailUrl && !previewError && <img className="thumbnail-preview" src={form.thumbnailUrl} alt="Thumbnail preview" onError={() => setPreviewError(true)} />}
          <label>Description <span className="optional">Optional</span><textarea name="description" value={form.description} onChange={update} rows="3" maxLength="5000" placeholder="Tell viewers a little about this video" /></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="dialog-actions"><button type="button" className="text-button" onClick={onClose}>Cancel</button><button className="button button-primary" type="submit">Post video <Icon name="chevron" size={17} /></button></div>
        </form>
      </section>
    </Modal>
  );
}

function WatchDialog({ video, channel, onClose, onLike, onSubscribe, onComment, liked, subscribed }) {
  const [comment, setComment] = useState("");
  const comments = video.comments || [];
  const [playbackError, setPlaybackError] = useState(false);

  function addComment(event) {
    event.preventDefault();
    if (!comment.trim()) return;
    onComment(video.videoId, {
      id: Date.now(),
      author: { displayName: "Daksh", username: "daksh", avatarUrl: "" },
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
              {video.assetUrl && !playbackError
                ? <video controls autoPlay playsInline poster={video.thumbnailUrl || undefined} onError={() => setPlaybackError(true)}><source src={video.assetUrl} type={video.mimeType || "video/mp4"} />Your browser does not support video playback.</video>
                : <div className="player-unavailable"><Icon name="video" size={34} /><strong>Video URL isn't playable</strong><span>Check that the link is a direct MP4 or WebM file and is publicly accessible.</span></div>}
            </div>
            <h1 className="watch-title">{video.title}</h1>
            <div className="watch-meta-row">
              <div className="watch-channel"><Avatar src={channel?.avatarUrl} name={channel?.name} size="large" /><div><strong>{channel?.name || "MotionVille creator"}</strong><span>{channel?.handle || "@creator"}</span></div><button className={`button subscribe-button ${subscribed ? "button-subscribed" : "button-dark"}`} onClick={() => onSubscribe(channel?.channelId)}>{subscribed ? "Subscribed" : "Subscribe"}</button></div>
              <div className="watch-actions">
                <button className={`action-pill ${liked ? "action-pill-selected" : ""}`} onClick={() => onLike(video.videoId)}><Icon name="like" size={18} filled={liked} /><span>{formatViews(video.likeCount || 0).replace(" views", "")}</span></button>
                <button className="action-pill" onClick={() => navigator.clipboard?.writeText(video.assetUrl)}><Icon name="share" size={17} /><span>Share</span></button>
              </div>
            </div>
            <div className="watch-description"><span>{formatViews(video.views)} · {formatAge(video.createdAt)} · {video.category}</span><p>{video.description || "No description added yet."}</p>{video.tags?.length > 0 && <div className="tag-row">{video.tags.map((tag) => <span key={tag}>#{tag.replace(/\s+/g, "")}</span>)}</div>}</div>
            <section className="comments-section">
              <h2>{comments.length} Comments</h2>
              <form className="comment-form" onSubmit={addComment}><Avatar name="You" /><input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Add a comment…" aria-label="Add a comment" /><button disabled={!comment.trim()} type="submit">Comment</button></form>
              <div className="comment-list">{comments.map((item) => <article className="comment" key={item.id}><Avatar src={item.author?.avatarUrl} name={item.author?.displayName || item.author?.username} /><div><strong>{item.author?.displayName || item.author?.username || "Viewer"} <span>· {formatAge(item.createdAt)}</span></strong><p>{item.body}</p><button className="comment-like"><Icon name="like" size={15} /> Like <span>Reply</span></button></div></article>)}</div>
            </section>
          </div>
          <aside className="watch-next"><p className="sidebar-heading">Up next</p>{video.description && <div className="next-description">{video.description}</div>}</aside>
        </div>
      </section>
    </Modal>
  );
}

export default function App() {
  const [videos, setVideos] = useState(() => loadLocal("motionville.videos", SEED_VIDEOS));
  const [channels, setChannels] = useState(() => loadLocal("motionville.channels", SEED_CHANNELS));
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [view, setView] = useState("Home");
  const [createDialog, setCreateDialog] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [toast, setToast] = useState("");
  const [likedVideos, setLikedVideos] = useState(() => loadLocal("motionville.likes", []));
  const [history, setHistory] = useState(() => loadLocal("motionville.history", []));
  const [subscriptions, setSubscriptions] = useState(() => loadLocal("motionville.subscriptions", []));
  const [activeChannelId, setActiveChannelId] = useState(null);

  useEffect(() => localStorage.setItem("motionville.videos", JSON.stringify(videos)), [videos]);
  useEffect(() => localStorage.setItem("motionville.channels", JSON.stringify(channels)), [channels]);
  useEffect(() => localStorage.setItem("motionville.likes", JSON.stringify(likedVideos)), [likedVideos]);
  useEffect(() => localStorage.setItem("motionville.history", JSON.stringify(history)), [history]);
  useEffect(() => localStorage.setItem("motionville.subscriptions", JSON.stringify(subscriptions)), [subscriptions]);
  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(""), 2600);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const channelById = useMemo(() => new Map(channels.map((channel) => [Number(channel.channelId), channel])), [channels]);

  const visibleVideos = useMemo(() => {
    const currentChannel = channelById.get(Number(activeChannelId));
    let items = videos.filter((video) => {
      if (view === "Your channel") {
        const ownedIds = channels.filter((channel) => channel.ownerId === 1).map((channel) => Number(channel.channelId));
        return ownedIds.includes(Number(video.channelId));
      }
      if (view === "Channel") {
        return Number(video.channelId) === Number(activeChannelId)
          && (video.visibility !== "PRIVATE" || currentChannel?.ownerId === 1);
      }
      if (view === "History") return history.includes(video.videoId);
      if (view === "Liked videos") return likedVideos.includes(video.videoId);
      if (view === "Subscriptions") return subscriptions.includes(Number(video.channelId));
      return video.visibility === "PUBLIC";
    });
    if (view === "Trending") items = [...items].sort((a, b) => (b.views || 0) - (a.views || 0));
    else if (view === "Recently added" || view === "History") items = [...items].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    if (activeCategory !== "All") items = items.filter((video) => video.category === activeCategory);
    const normalized = search.trim().toLowerCase();
    if (normalized) {
      items = items.filter((video) => {
        const channel = channelById.get(Number(video.channelId));
        return `${video.title} ${video.description || ""} ${channel?.name || ""} ${(video.tags || []).join(" ")}`.toLowerCase().includes(normalized);
      });
    }
    return items;
  }, [activeCategory, activeChannelId, channelById, channels, history, likedVideos, search, subscriptions, videos, view]);

  function createChannel(form) {
    const handle = form.handle.startsWith("@") ? form.handle : `@${form.handle}`;
    if (channels.some((channel) => channel.handle.toLowerCase() === handle.toLowerCase())) {
      setToast("That channel handle is already taken.");
      return;
    }
    const channel = {
      channelId: Date.now(),
      ownerId: 1,
      name: form.name.trim(),
      handle,
      description: form.description.trim(),
      bannerUrl: form.bannerUrl.trim(),
      avatarUrl: "",
      createdAt: new Date().toISOString(),
      subscriptions: 0,
    };
    setChannels((current) => [channel, ...current]);
    setCreateDialog(null);
    setToast("Your channel is ready.");
    setView("Your channel");
  }

  function createVideo(form) {
    const channelId = Number(form.channelId);
    const tags = form.title.toLowerCase().split(/\s+/).filter((word) => word.length > 3).slice(0, 4);
    const video = {
      videoId: Date.now(),
      channelId,
      title: form.title.trim(),
      description: form.description.trim(),
      thumbnailUrl: form.thumbnailUrl.trim(),
      durationSeconds: 0,
      visibility: form.visibility,
      processingStatus: "UPLOADED",
      assetUrl: form.assetUrl.trim(),
      mimeType: form.mimeType,
      category: form.category,
      tags,
      likeCount: 0,
      comments: [],
      createdAt: new Date().toISOString(),
      views: 0,
    };
    setVideos((current) => [video, ...current]);
    setCreateDialog(null);
    setToast("Video added to your MotionVille feed.");
    setActiveCategory("All");
    if (video.visibility === "PRIVATE") {
      setActiveChannelId(channelId);
      setView("Channel");
    } else {
      setActiveChannelId(null);
      setView("Home");
    }
  }

  function toggleLike(videoId) {
    const delta = likedVideos.includes(videoId) ? -1 : 1;
    setLikedVideos((current) => current.includes(videoId)
      ? current.filter((id) => id !== videoId)
      : [...current, videoId]);
    setVideos((current) => current.map((video) => video.videoId === videoId
      ? { ...video, likeCount: Math.max(0, (video.likeCount || 0) + delta) }
      : video));
    setSelectedVideo((current) => current?.videoId === videoId
      ? { ...current, likeCount: Math.max(0, (current.likeCount || 0) + delta) }
      : current);
  }

  function toggleSubscription(channelId) {
    if (!channelId) return;
    const delta = subscriptions.includes(Number(channelId)) ? -1 : 1;
    setSubscriptions((current) => current.includes(Number(channelId))
      ? current.filter((id) => id !== Number(channelId))
      : [...current, Number(channelId)]);
    setChannels((current) => current.map((channel) => Number(channel.channelId) === Number(channelId)
      ? { ...channel, subscriptions: Math.max(0, (channel.subscriptions || 0) + delta) }
      : channel));
  }

  function selectVideo(video) {
    const opened = { ...video, views: (video.views || 0) + 1 };
    setSelectedVideo(opened);
    setVideos((current) => current.map((item) => item.videoId === video.videoId ? opened : item));
    setHistory((current) => [video.videoId, ...current.filter((id) => id !== video.videoId)]);
  }

  function addComment(videoId, comment) {
    setVideos((current) => current.map((video) => video.videoId === videoId
      ? { ...video, comments: [comment, ...(video.comments || [])] }
      : video));
    setSelectedVideo((current) => current?.videoId === videoId
      ? { ...current, comments: [comment, ...(current.comments || [])] }
      : current);
  }

  function chooseView(nextView) {
    setView(nextView);
    setActiveChannelId(null);
    setActiveCategory("All");
    setSidebarOpen(false);
  }

  function showChannel(channel) {
    setView("Channel");
    setActiveChannelId(channel.channelId);
    setActiveCategory("All");
    setSidebarOpen(false);
  }

  const activeChannel = view === "Channel" ? channelById.get(Number(activeChannelId)) : null;
  const feedTitle = view === "Trending"
    ? "Popular right now"
    : view === "Recently added"
      ? "Freshly posted"
      : view === "Your channel"
        ? "Your videos"
        : view === "Channel"
          ? `${activeChannel?.name || "Channel"} videos`
          : view === "Liked videos"
            ? "Videos you liked"
            : view === "History"
              ? "Recently watched"
              : view === "Subscriptions"
                ? "From your subscriptions"
                : activeCategory === "All"
                  ? "A little inspiration"
                  : activeCategory;

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-brand-area">
          <button className="icon-button menu-trigger" onClick={() => setSidebarOpen((open) => !open)} aria-label="Toggle menu"><Icon name="menu" /></button>
          <a className="brand" href="#" onClick={(event) => { event.preventDefault(); chooseView("Home"); }} aria-label="MotionVille home">
            <span className="brand-symbol"><Icon name="play" size={14} filled /></span><span>Motion<span className="brand-red">Ville</span></span>
          </a>
        </div>
        <form className="search-form" onSubmit={(event) => event.preventDefault()}>
          <div className="search-input-wrap"><Icon name="search" size={19} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search videos, creators, and more" aria-label="Search" />{search && <button type="button" className="search-clear" onClick={() => setSearch("")}><Icon name="close" size={16} /></button>}</div>
          <button className="search-submit" type="submit" aria-label="Search"><Icon name="search" /></button>
        </form>
        <div className="topbar-actions">
          <button className="create-button" onClick={() => setCreateDialog(channels.length ? "video" : "channel")}><Icon name="plus" size={18} /><span>Create</span></button>
          <button className="icon-button notification-button" aria-label="Notifications"><Icon name="bell" /></button>
          <Avatar name="Daksh" size="small" />
        </div>
      </header>

      {sidebarOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setSidebarOpen(false)} />}
      <div className="app-body">
        <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
          <nav className="nav-group" aria-label="Main navigation">
            <button className={`nav-item ${view === "Home" ? "nav-active" : ""}`} onClick={() => chooseView("Home")}><Icon name="home" filled={view === "Home"} /><span>Home</span></button>
            <button className={`nav-item ${view === "Explore" ? "nav-active" : ""}`} onClick={() => chooseView("Explore")}><Icon name="compass" /><span>Explore</span></button>
            <button className={`nav-item ${view === "Trending" ? "nav-active" : ""}`} onClick={() => chooseView("Trending")}><Icon name="shorts" /><span>Trending</span></button>
            <button className={`nav-item ${view === "Subscriptions" ? "nav-active" : ""}`} onClick={() => chooseView("Subscriptions")}><Icon name="subscriptions" /><span>Subscriptions</span></button>
          </nav>
          <div className="sidebar-rule" />
          <nav className="nav-group" aria-label="Your library">
            <button className={`nav-item ${view === "Your channel" ? "nav-active" : ""}`} onClick={() => chooseView("Your channel")}><Icon name="library" /><span>Your channel</span><Icon name="chevron" size={16} /></button>
            <button className={`nav-item ${view === "History" ? "nav-active" : ""}`} onClick={() => chooseView("History")}><Icon name="history" /><span>History</span></button>
            <button className={`nav-item ${view === "Liked videos" ? "nav-active" : ""}`} onClick={() => chooseView("Liked videos")}><Icon name="like" /><span>Liked videos</span></button>
          </nav>
          <div className="sidebar-rule" />
          <section className="sidebar-channels"><div className="sidebar-section-heading"><span>Channels</span><button className="small-add" onClick={() => setCreateDialog("channel")} aria-label="Create channel"><Icon name="plus" size={17} /></button></div>
            {channels.slice(0, 5).map((channel) => <button className="nav-item channel-nav-item" key={channel.channelId} onClick={() => showChannel(channel)}><Avatar src={channel.avatarUrl} name={channel.name} size="tiny" /><span>{channel.name}</span></button>)}
            <button className="nav-item add-channel-nav" onClick={() => setCreateDialog("channel")}><span className="add-channel-icon"><Icon name="plus" size={16} /></span><span>Create a channel</span></button>
          </section>
          <div className="sidebar-bottom"><div className="sidebar-note-icon"><Icon name="sparkle" size={18} /></div><p>Good videos make a good day.</p><span>MotionVille · Create your corner</span></div>
        </aside>

        <main className="main-content">
          {view === "Home" || view === "Explore" ? (
            <div className="welcome-strip">
              <div className="welcome-copy"><span className="welcome-kicker"><Icon name="sparkle" size={14} /> YOUR SPACE TO WATCH & SHARE</span><h1>Find your next <em>favorite.</em></h1><p>Stories, ideas, and little moments from creators worth following.</p></div>
              <div className="welcome-art" aria-hidden="true"><div className="art-sun" /><div className="art-arch"><div className="art-land art-land-one" /><div className="art-land art-land-two" /><div className="art-water" /></div><span className="art-spark art-spark-one">✳</span><span className="art-spark art-spark-two">✦</span></div>
            </div>
          ) : view === "Channel" && activeChannel ? (
            <section className="channel-banner">
              {activeChannel.bannerUrl && <img src={activeChannel.bannerUrl} alt="" />}
              <div className="channel-banner-content"><Avatar src={activeChannel.avatarUrl} name={activeChannel.name} size="banner" /><div><h1>{activeChannel.name}</h1><p>{activeChannel.handle} · {formatViews(activeChannel.subscriptions || 0).replace(" views", " subscribers")}</p><span>{activeChannel.description}</span></div></div>
            </section>
          ) : null}

          <div className="category-scroller" aria-label="Video categories">
            {CATEGORIES.map((category, index) => <button key={category} className={`category-chip ${activeCategory === category ? "category-selected" : ""}`} onClick={() => setActiveCategory(category)}>{index === 0 && <Icon name="grid" size={14} />}{category}</button>)}
          </div>

          <section className="feed-section">
            <div className="feed-heading"><div><p className="section-eyebrow">{view === "Home" ? "Picked for you" : view}</p><h2>{feedTitle}</h2></div><button className="feed-filter" onClick={() => chooseView("Recently added")}><Icon name="filter" size={16} /> Recent</button></div>
            {visibleVideos.length ? <div className="video-grid">{visibleVideos.map((video, index) => <VideoCard key={video.videoId} video={video} channel={channelById.get(Number(video.channelId))} onSelect={selectVideo} index={index} />)}</div>
              : <div className="empty-feed"><span><Icon name="video" size={25} /></span><h2>No videos yet</h2><p>{channels.length ? "Try another category or search, or add a video with a direct URL." : "Create your first channel, then post a video to see it here."}</p><button className="button button-primary" onClick={() => setCreateDialog(channels.length ? "video" : "channel")}><Icon name="plus" size={17} />{channels.length ? "Post a video" : "Create a channel"}</button></div>}
          </section>
        </main>
      </div>

      {createDialog === "channel" && <CreateChannelDialog onClose={() => setCreateDialog(null)} onCreate={createChannel} />}
      {createDialog === "video" && <CreateVideoDialog channels={channels} onClose={() => setCreateDialog(null)} onCreate={createVideo} />}
      {selectedVideo && <WatchDialog video={selectedVideo} channel={channelById.get(Number(selectedVideo.channelId))} onClose={() => setSelectedVideo(null)} onLike={toggleLike} onSubscribe={toggleSubscription} onComment={addComment} liked={likedVideos.includes(selectedVideo.videoId)} subscribed={subscriptions.includes(Number(selectedVideo.channelId))} />}
      {toast && <div className="toast"><Icon name="check" size={17} />{toast}</div>}
    </div>
  );
}
