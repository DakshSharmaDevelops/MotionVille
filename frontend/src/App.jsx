import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  apiRequest,
  buildVideoQuery,
  createVideoThumbnail,
  mapApiCategory,
  mapApiChannel,
  mapApiVideo,
  uploadFile,
  createTag,
  addTagToVideo,
  fetchTags,
  updateChannelApi,
} from "./api/videoApi.js";

import { Avatar, Icon, VideoCard } from "./components/ui.jsx";

import {
  AccountDialog,
  AccountMenu,
  SubscribersDialog,
} from "./components/account.jsx";

import {
  CreateChannelDialog,
  EditChannelDialog,
  CreateVideoDialog,
  ManageVideoDialog,
  ReportDialog,
  WatchDialog,
} from "./components/dialogs.jsx";
import {
  AddVideosToPlaylistDialog,
  PlaylistDialog,
  PlaylistLibrary,
  SaveToPlaylistDialog,
} from "./components/playlists.jsx";
import {
  addVideoToPlaylist,
  createPlaylist,
  deletePlaylist,
  fetchPlaylistVideos,
  fetchPlaylists,
  removeVideoFromPlaylist,
  reorderPlaylistVideos,
  updatePlaylist,
} from "./api/playlistApi.js";
import { clearWatchHistory, fetchWatchHistory, removeWatchHistoryItem } from "./api/watchHistoryApi.js";
import { formatAge, formatDuration, formatDateJoined, formatViews } from "./utils/format.js";

import {
  fetchNotificationCount,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "./api/notificationApi.js";

import ReportsAdminPanel from "./components/ReportsAdminPanel.jsx";
import { LiveStudio, LiveWatchDialog, LiveBroadcastList } from "./components/live.jsx";

const DEMO_ADMIN_USER_ID = Number(
    import.meta.env.VITE_DEMO_ADMIN_USER_ID || 9,
);
const SESSION_IDLE_TIMEOUT_MS = 30 * 60 * 1000;
const LAST_ACTIVITY_STORAGE_KEY = "motionville.lastActivityAt";

function routeView(pathname) {
  if (pathname === "/explore") return "Explore";
  if (pathname === "/trending") return "Trending";
  if (pathname === "/subscriptions") return "Subscriptions";
  if (pathname === "/playlists" || /^\/playlist\/\d+\/?$/.test(pathname)) return "Playlists";
  if (pathname === "/history") return "History";
  if (pathname === "/liked") return "Liked videos";
  if (pathname === "/your-videos") return "Your channel";
  if (pathname === "/recent") return "Recently added";
  if (/^\/channel\/\d+\/?$/.test(pathname)) return "Channel";
  if (pathname === "/search") return "Search";
  return "Home";
}

function routeForView(view) {
  switch (view) {
    case "Explore": return "/explore";
    case "Trending": return "/trending";
    case "Subscriptions": return "/subscriptions";
    case "Playlists": return "/playlists";
    case "History": return "/history";
    case "Liked videos": return "/liked";
    case "Your channel": return "/your-videos";
    case "Recently added": return "/recent";
    case "Search": return "/search";
    default: return "/";
  }
}

function loadStoredIds(key) {
  try {
    const saved = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [channels, setChannels] = useState([]);
  const [categories, setCategories] = useState([]);

  const [selectedVideo, setSelectedVideo] = useState(null);
  const [manageVideo, setManageVideo] = useState(null);
  const [playlists, setPlaylists] = useState([]);
  const [selectedPlaylist, setSelectedPlaylist] = useState(null);
  const [playlistVideos, setPlaylistVideos] = useState([]);
  const [playlistLoading, setPlaylistLoading] = useState(false);
  const [playlistError, setPlaylistError] = useState("");
  const [playlistEditor, setPlaylistEditor] = useState(null);
  const [playlistPickerVideo, setPlaylistPickerVideo] = useState(null);
  const [playlistAddTarget, setPlaylistAddTarget] = useState(null);
  const [playlistAddVideos, setPlaylistAddVideos] = useState([]);
  const [playlistAddLoading, setPlaylistAddLoading] = useState(false);
  const [playlistAddError, setPlaylistAddError] = useState("");
  const [pendingPlaylistVideo, setPendingPlaylistVideo] = useState(null);

  const [backendLoaded, setBackendLoaded] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [notificationCount, setNotificationCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationsError, setNotificationsError] = useState("");

  const [videoPage, setVideoPage] = useState({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  });

  const [pageIndex, setPageIndex] = useState(0);
  const [sortOrder, setSortOrder] = useState("createdAt,desc");

  const [videosLoading, setVideosLoading] = useState(false);

  const [feedError, setFeedError] = useState("");
  const [channelError, setChannelError] = useState("");
  const [categoryError, setCategoryError] = useState("");

  const [search, setSearch] = useState(() => {
    const q = new URLSearchParams(window.location.search).get("q");
    return window.location.pathname === "/search" && q ? q : "";
  });
  const [searchInput, setSearchInput] = useState(search);
  const [activeCategory, setActiveCategory] = useState("All");
  const [view, setView] = useState(() => routeView(window.location.pathname));

  const [createDialog, setCreateDialog] = useState(null);
  const [editChannelDialog, setEditChannelDialog] = useState(null);
  const [liveStreamDialogOpen, setLiveStreamDialogOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [youPanelOpen, setYouPanelOpen] = useState(false);
  const [toast, setToast] = useState("");

  const [likedVideos, setLikedVideos] = useState(() =>
    loadStoredIds("motionville.likes")
  );

  const [history, setHistory] = useState(() =>
    loadStoredIds("motionville.history")
  );
  const [watchHistory, setWatchHistory] = useState([]);
  const [watchHistoryLoading, setWatchHistoryLoading] = useState(false);
  const [watchHistoryError, setWatchHistoryError] = useState("");
  const [watchHistoryActionError, setWatchHistoryActionError] = useState("");
  const [watchHistoryActionBusy, setWatchHistoryActionBusy] = useState(false);
  const [removingHistoryVideoId, setRemovingHistoryVideoId] = useState(null);

  const [subscriptions, setSubscriptions] = useState(() =>
    loadStoredIds("motionville.subscriptions")
  );

  const [activeChannelId, setActiveChannelId] = useState(null);

  const [currentUser, setCurrentUser] = useState(null);
  const [reportsAdminOpen, setReportsAdminOpen] = useState(false);
  const [reportTarget, setReportTarget] = useState(null);
  const [accountDialog, setAccountDialog] = useState(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [notificationActionBusy, setNotificationActionBusy] = useState(false);
  const accountMenuRef = useRef(null);
  const notificationMenuRef = useRef(null);
  const isDemoAdmin = currentUser?.role === "ADMIN";

  useEffect(() => {
    function closeMenusOutside(event) {
      if (!accountMenuRef.current?.contains(event.target)) {
        setAccountMenuOpen(false);
      }
      if (!notificationMenuRef.current?.contains(event.target)) {
        setNotificationsOpen(false);
      }
    }

    document.addEventListener("pointerdown", closeMenusOutside);
    return () => document.removeEventListener("pointerdown", closeMenusOutside);
  }, []);

  useEffect(() => {
    let active = true;
    const path = location.pathname;
    const nextView = routeView(path);
    setView(nextView);
    setActiveCategory("All");
    setPageIndex(0);

    const channelMatch = path.match(/^\/channel\/(\d+)\/?$/);
    setActiveChannelId(channelMatch ? Number(channelMatch[1]) : null);

    const query = new URLSearchParams(location.search);
    if (path === "/verify-email") {
      const token = query.get("token");
      if (token) {
        apiRequest("/auth/verify-email", {
          method: "POST",
          body: JSON.stringify({ token }),
        })
          .then((res) => {
            setToast(res?.message || "Email verified successfully! You can now log in.");
            setAccountDialog("login");
            navigate("/login", { replace: true });
          })
          .catch((err) => {
            setToast(err?.message || "Verification link is invalid or has expired.");
            setAccountDialog("login");
            navigate("/login", { replace: true });
          });
      } else {
        setAccountDialog("login");
        navigate("/login", { replace: true });
      }
    }
    if (path === "/search") {
      const q = query.get("q") || "";
      setSearch(q);
      setSearchInput(q);
    }
    if (path === "/login") setAccountDialog("login");
    else if (path === "/register") setAccountDialog("register");
    else if (path !== "/login" && path !== "/register") setAccountDialog(null);

    const watchMatch = path.match(/^\/watch\/(\d+)\/?$/);
    if (watchMatch) {
      const videoId = Number(watchMatch[1]);
      setSelectedVideo((current) =>
        current?.videoId === videoId ? current : videos.find(
          (item) => Number(item.videoId) === videoId
        ) || null
      );
    } else {
      setSelectedVideo(null);
    }

    const playlistMatch = path.match(/^\/playlist\/(\d+)\/?$/);
    if (playlistMatch) {
      const playlistId = Number(playlistMatch[1]);
      const playlist = playlists.find((item) => Number(item.id) === playlistId);
      if (playlist && Number(selectedPlaylist?.id) !== playlistId) {
        setSelectedPlaylist(playlist);
        setPlaylistVideos([]);
        setPlaylistLoading(true);
        setPlaylistError("");
        fetchPlaylistVideos(playlistId)
          .then((result) => {
            if (!active) return;
            setPlaylistVideos(result.map((item) => {
            const existing = videos.find((video) => Number(video.videoId) === Number(item.videoId));
            return {
              ...existing,
              ...item,
              videoId: item.videoId,
              channelId: existing?.channelId,
              serverVideo: true,
              createdAt: existing?.createdAt || item.addedAt,
            };
            }));
          })
          .catch((error) => {
            if (active) setPlaylistError(`Could not load playlist videos: ${error.message}`);
          })
          .finally(() => {
            if (active) setPlaylistLoading(false);
          });
      } else if (!playlist) {
        apiRequest(`/playlists/${playlistId}`)
          .then((item) => {
            if (!active) return null;
            setSelectedPlaylist(item);
            return fetchPlaylistVideos(playlistId);
          })
          .then((result) => {
            if (!active || !result) return;
            setPlaylistVideos(result.map((item) => {
              const existing = videos.find((video) => Number(video.videoId) === Number(item.videoId));
              return {
                ...existing,
                ...item,
                videoId: item.videoId,
                channelId: existing?.channelId,
                serverVideo: true,
                createdAt: existing?.createdAt || item.addedAt,
              };
            }));
          })
          .catch((error) => {
            if (active) setPlaylistError(`Could not load playlist: ${error.message}`);
          })
          .finally(() => {
            if (active) setPlaylistLoading(false);
          });
      }
    } else {
      setSelectedPlaylist(null);
      setPlaylistVideos([]);
    }
    return () => {
      active = false;
    };
  }, [location.pathname, location.search, playlists, currentUser?.id]);

  useEffect(() => {
    const match = location.pathname.match(/^\/watch\/(\d+)\/?$/);
    if (!match) return undefined;
    const videoId = Number(match[1]);
    if (videos.some((item) => Number(item.videoId) === videoId)) return undefined;

    let active = true;
    apiRequest(`/videos/${videoId}`)
      .then((item) => {
        if (active) setSelectedVideo(mapApiVideo(item, categories));
      })
      .catch((error) => {
        if (active) setFeedError(`Could not load video: ${error.message}`);
      });
    return () => {
      active = false;
    };
  }, [location.pathname, videos, categories]);

  function closeVideo() {
    setSelectedVideo(null);
    navigate(location.state?.from || "/");
  }

  useEffect(() => {
    if (!currentUser?.id) {
      setNotifications([]);
      setNotificationCount(0);
      setNotificationsError("");
      setNotificationsOpen(false);
      return undefined;
    }

    let active = true;

    async function loadNotifications() {
      try {
        const [items, countResponse] = await Promise.all([
          fetchNotifications(currentUser.id),
          fetchNotificationCount(currentUser.id),
        ]);
        if (active) {
          setNotifications(items);
          setNotificationCount(Number(countResponse.count) || 0);
          setNotificationsError("");
        }
      } catch (error) {
        if (active) setNotificationsError(error.message);
      }
    }

    loadNotifications();
    const intervalId = window.setInterval(loadNotifications, 30_000);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [currentUser?.id]);

  const unreadNotificationCount = notifications.filter(
    (notification) => notification.readAt === null,
  ).length;

  async function handleMarkNotificationRead(notificationId) {
    if (!currentUser?.id) return;
    try {
      const updated = await markNotificationRead(currentUser.id, notificationId);
      setNotifications((items) =>
        items.map((item) => item.id === updated.id ? updated : item),
      );
      setNotificationsError("");
    } catch (error) {
      setNotificationsError(error.message);
    }
  }

  async function handleMarkAllNotificationsRead() {
    if (!currentUser?.id || notificationActionBusy) return;
    setNotificationActionBusy(true);
    try {
      const updated = await markAllNotificationsRead(currentUser.id);
      setNotifications(updated);
      setNotificationsError("");
    } catch (error) {
      setNotificationsError(error.message);
    } finally {
      setNotificationActionBusy(false);
    }
  }

  async function handleNotificationClick(notification) {
    if (notification.readAt === null) {
      await handleMarkNotificationRead(notification.id);
    }
    setNotificationsOpen(false);
    const linkedVideo = videos.find(
      (video) => Number(video.videoId) === Number(notification.videoId),
    );
    if (linkedVideo) selectVideo(linkedVideo);
  }

  async function refreshNotifications() {
    if (!currentUser?.id) return;
    try {
      const [items, countResponse] = await Promise.all([
        fetchNotifications(currentUser.id),
        fetchNotificationCount(currentUser.id),
      ]);
      setNotifications(items);
      setNotificationCount(Number(countResponse.count) || 0);
      setNotificationsError("");
    } catch (error) {
      setNotificationsError(error.message);
    }
  }

  const [myChannels, setMyChannels] = useState([]);
  const [subscriptionChannels, setSubscriptionChannels] = useState([]);

  const [subscriberDialog, setSubscriberDialog] = useState(null);
  const [subscriberCounts, setSubscriberCounts] = useState({});

  useEffect(() => {
    localStorage.setItem(
      "motionville.likes",
      JSON.stringify(likedVideos)
    );
  }, [likedVideos]);

  useEffect(() => {
    localStorage.setItem(
      "motionville.history",
      JSON.stringify(history)
    );
  }, [history]);

  useEffect(() => {
    localStorage.setItem(
      "motionville.subscriptions",
      JSON.stringify(subscriptions)
    );
  }, [subscriptions]);

  useEffect(() => {
    const lastActivityAt = Number(
      localStorage.getItem(LAST_ACTIVITY_STORAGE_KEY)
    );

    if (
      Number.isFinite(lastActivityAt) &&
      lastActivityAt > 0 &&
      Date.now() - lastActivityAt >= SESSION_IDLE_TIMEOUT_MS
    ) {
      localStorage.removeItem("motionville.currentUserId");
      localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);
      return;
    }

    apiRequest("/auth/me")
      .then((user) => {
        localStorage.setItem(
          "motionville.currentUserId",
          String(user.id)
        );
        localStorage.setItem(LAST_ACTIVITY_STORAGE_KEY, String(Date.now()));
        setCurrentUser(user);
      })
      .catch(() => {
        localStorage.removeItem("motionville.currentUserId");
        localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);
        setCurrentUser(null);
      });
  }, []);

  useEffect(() => {
    if (!currentUser?.id) {
      setMyChannels([]);
      setSubscriptionChannels([]);
      setSubscriptions([]);
      return;
    }

    let active = true;

    Promise.all([
      apiRequest(`/users/${currentUser.id}/channels`),
      apiRequest(`/users/${currentUser.id}/subscriptions`),
    ])
      .then(([channelResult, subscriptionResult]) => {
        if (!active) return;

        const mine = channelResult.map(mapApiChannel);
        const subscribed = subscriptionResult.map(mapApiChannel);

        setMyChannels(mine);
        setSubscriptionChannels(subscribed);

        setSubscriptions(
          subscribed.map((channel) =>
            Number(channel.channelId)
          )
        );
      })
      .catch((error) => {
        if (active) {
          setToast(
            `Could not load account data: ${error.message}`
          );
        }
      });

    return () => {
      active = false;
    };
  }, [currentUser?.id]);

  useEffect(() => {
    if (!currentUser?.id) {
      setWatchHistory([]);
      setWatchHistoryLoading(false);
      setWatchHistoryError("");
      return undefined;
    }

    let active = true;
    setWatchHistoryLoading(true);
    setWatchHistoryError("");
    fetchWatchHistory(currentUser.id)
      .then((items) => {
        if (active) setWatchHistory(items);
      })
      .catch((error) => {
        if (active) setWatchHistoryError(`Could not load watch history: ${error.message}`);
      })
      .finally(() => {
        if (active) setWatchHistoryLoading(false);
      });

    return () => {
      active = false;
    };
  }, [currentUser?.id]);

  useEffect(() => {
    if (!currentUser?.id) {
      setPlaylists([]);
      setSelectedPlaylist(null);
      setPlaylistVideos([]);
      setPlaylistError("");
      return undefined;
    }

    let active = true;
    fetchPlaylists(currentUser.id)
      .then((result) => {
        if (active) {
          setPlaylists(result);
          setPlaylistError("");
        }
      })
      .catch((error) => {
        if (active) setPlaylistError(`Could not load playlists: ${error.message}`);
      });

    return () => {
      active = false;
    };
  }, [currentUser?.id]);

  useEffect(() => {
    if (!channels.length) return;

    let active = true;

    Promise.all(
      channels.map(async (channel) => {
        try {
          const count = await apiRequest(
            `/channels/${channel.channelId}/subscriber-count`
          );

          return [
            Number(channel.channelId),
            Number(count),
          ];
        } catch {
          return [
            Number(channel.channelId),
            0,
          ];
        }
      })
    ).then((entries) => {
      if (active) {
        setSubscriberCounts(
          Object.fromEntries(entries)
        );
      }
    });

    return () => {
      active = false;
    };
  }, [channels]);

  useEffect(() => {
    const controller = new AbortController();

    async function loadBackendMetadata() {
      const [
        channelResult,
        categoryResult,
      ] = await Promise.allSettled([
        apiRequest("/channels", {
          signal: controller.signal,
        }),
        apiRequest("/categories", {
          signal: controller.signal,
        }),
      ]);

      if (controller.signal.aborted) return;

      if (channelResult.status === "fulfilled") {
        setChannels(
          channelResult.value.map(mapApiChannel)
        );
        setChannelError("");
      } else {
        setChannelError(
          `Could not load channels: ${channelResult.reason.message}`
        );
      }

      if (categoryResult.status === "fulfilled") {
        setCategories(
          categoryResult.value.map(mapApiCategory)
        );
        setCategoryError("");
      } else {
        setCategoryError(
          `Could not load video categories: ${categoryResult.reason.message}`
        );
      }
    }

    loadBackendMetadata();

    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    const timer = window.setTimeout(
      async () => {
        setVideosLoading(true);

        try {
          const channelId =
            view === "Channel"
              ? activeChannelId
              : null;

          const categoryId =
            categories.find(
              (category) =>
                category.name === activeCategory
            )?.id;

          const publicOnly =
            ![
              "Your channel",
              "Channel",
              "History",
              "Liked videos",
            ].includes(view);

          const result = await apiRequest(
            buildVideoQuery({
              search,
              page: pageIndex,
              sort: sortOrder,
              channelId,
              categoryId,
              publicOnly,
              trending: view === "Trending",
            }),
            {
              signal: controller.signal,
            }
          );

          if (controller.signal.aborted) return;

          setVideos(
            result.content.map((video) =>
              mapApiVideo(video, categories)
            )
          );

          setVideoPage({
            page: result.page,
            size: result.size,
            totalElements: result.totalElements,
            totalPages: result.totalPages,
          });

          setBackendLoaded(true);
          setFeedError("");
        } catch (error) {
          if (!controller.signal.aborted) {
            setFeedError(
              `Could not load saved videos: ${error.message}`
            );
          }
        } finally {
          if (!controller.signal.aborted) {
            setVideosLoading(false);
          }
        }
      },
      search.trim() ? 250 : 0
    );

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [
    activeCategory,
    activeChannelId,
    categories,
    pageIndex,
    search,
    sortOrder,
    view,
  ]);

  useEffect(() => {
    if (!toast) return undefined;

    const timer = window.setTimeout(
      () => setToast(""),
      2600
    );

    return () => window.clearTimeout(timer);
  }, [toast]);

  const channelById = useMemo(
    () =>
      new Map(
        channels.map((channel) => [
          Number(channel.channelId),
          channel,
        ])
      ),
    [channels]
  );

  const matchingChannels = useMemo(() => {
    if (!search.trim()) return [];
    const query = search.trim().toLowerCase();
    return channels.filter((ch) =>
      ch.name.toLowerCase().includes(query) ||
      ch.handle.toLowerCase().includes(query) ||
      (ch.description && ch.description.toLowerCase().includes(query))
    );
  }, [search, channels]);

  const visibleFeedError =
    feedError ||
    channelError ||
    categoryError;

  const visibleVideos = useMemo(() => {
    let items = videos;

    if (view === "Your channel") {
      const channelIds = new Set(
        myChannels.map((channel) =>
          Number(channel.channelId)
        )
      );

      items = items.filter((video) =>
        channelIds.has(Number(video.channelId))
      );
    } else if (view === "Channel") {
      items = items.filter(
        (video) =>
          Number(video.channelId) ===
          Number(activeChannelId)
      );
    } else if (view === "History") {
      if (currentUser?.id) {
        items = watchHistory.map((entry) => {
          const existing = videos.find((video) => Number(video.videoId) === Number(entry.videoId));
          const durationSeconds = Number(entry.durationSeconds || existing?.durationSeconds || 0);
          const resumePositionSeconds = Number(entry.lastPositionSeconds || 0);
          return {
            ...existing,
            videoId: Number(entry.videoId),
            title: entry.videoTitle || existing?.title || "Video",
            thumbnailUrl: entry.thumbnailUrl || existing?.thumbnailUrl || "",
            durationSeconds,
            channelId: existing?.channelId,
            createdAt: entry.lastWatchedAt || existing?.createdAt,
            serverVideo: true,
            resumePositionSeconds,
            watchProgressPercent: durationSeconds > 0
              ? Math.min(100, (resumePositionSeconds / durationSeconds) * 100)
              : 0,
          };
        });
      } else {
        items = items.filter((video) => history.includes(Number(video.videoId)));
      }
    } else if (view === "Liked videos") {
      items = items.filter((video) =>
        likedVideos.includes(video.videoId)
      );
    } else if (view === "Subscriptions") {
      items = items.filter((video) =>
        subscriptions.includes(
          Number(video.channelId)
        )
      );
    }

    if (view === "History" && !currentUser?.id) {
      items = [...items].sort(
        (a, b) =>
          history.indexOf(a.videoId) -
          history.indexOf(b.videoId)
      );
    }

    return items;
  }, [
    activeChannelId,
    currentUser?.id,
    history,
    likedVideos,
    myChannels,
    subscriptions,
    videos,
    watchHistory,
    view,
  ]);

  const watchRecommendations = useMemo(
    () =>
      videos
        .filter(
          (video) =>
            video.videoId !==
            selectedVideo?.videoId
        )
        .map((video) => ({
          ...video,
          channel: channelById.get(
            Number(video.channelId)
          ),
        }))
        .slice(0, 12),
    [
      channelById,
      selectedVideo?.videoId,
      videos,
    ]
  );

  async function refreshBackendVideos(
    targetPage = pageIndex
  ) {
    const channelId =
      view === "Channel"
        ? activeChannelId
        : null;

    const categoryId =
      categories.find(
        (category) =>
          category.name === activeCategory
      )?.id;

    const publicOnly =
      ![
        "Your channel",
        "Channel",
        "History",
        "Liked videos",
      ].includes(view);

    const result = await apiRequest(
      buildVideoQuery({
        search,
        page: targetPage,
        sort: sortOrder,
        channelId,
        categoryId,
        publicOnly,
      })
    );

    setVideos(
      result.content.map((video) =>
        mapApiVideo(video, categories)
      )
    );

    setVideoPage({
      page: result.page,
      size: result.size,
      totalElements: result.totalElements,
      totalPages: result.totalPages,
    });

    setBackendLoaded(true);
    setFeedError("");
  }

  function applyVideoResponse(
    response,
    closeDialog = true
  ) {
    const updatedVideo =
      mapApiVideo(response, categories);

    setVideos((current) =>
      current.some(
        (video) =>
          video.videoId ===
          updatedVideo.videoId
      )
        ? current.map((video) =>
            video.videoId ===
            updatedVideo.videoId
              ? {
                  ...video,
                  ...updatedVideo,
                }
              : video
          )
        : [updatedVideo, ...current]
    );

    setManageVideo((current) =>
      current?.videoId ===
      updatedVideo.videoId
        ? closeDialog
          ? null
          : {
              ...current,
              ...updatedVideo,
            }
        : current
    );

    return updatedVideo;
  }

  async function createChannel(form) {
    if (!currentUser?.id) {
      setAccountDialog("auth");
      setToast("Create a profile first.");
      return;
    }
    if (myChannels.length > 0) {
      setCreateDialog(null);
      setToast("You can create only one channel per account.");
      return;
    }

    const handle = form.handle.startsWith("@")
      ? form.handle
      : `@${form.handle}`;

    const savedChannel =
      await apiRequest("/channels", {
        method: "POST",
        body: JSON.stringify({
          ownerId: Number(currentUser.id),
          name: form.name.trim(),
          handle,
          description: form.description.trim(),
          bannerUrl:
            form.bannerUrl.trim() || null,
          profileImageUrl:
            form.profileImageUrl?.trim() || null,
        }),
      });

    const channel =
      mapApiChannel(savedChannel);

    setChannels((current) => [
      channel,
      ...current.filter(
        (item) =>
          item.channelId !==
          channel.channelId
      ),
    ]);

    setMyChannels((current) => [
      channel,
      ...current.filter(
        (item) =>
          item.channelId !==
          channel.channelId
      ),
    ]);

    setCreateDialog(null);
    setToast("Your channel is ready.");
    setView("Your channel");
    navigate(`/channel/${channel.channelId}`);
  }

  async function handleUpdateChannel(channelId, form) {
    const handle = form.handle.startsWith("@")
      ? form.handle
      : `@${form.handle}`;

    const updated = await updateChannelApi(channelId, {
      name: form.name.trim(),
      handle: handle.trim(),
      description: form.description?.trim() || null,
      bannerUrl: form.bannerUrl?.trim() || null,
      profileImageUrl: form.profileImageUrl?.trim() || null,
    });

    const mapped = mapApiChannel(updated);

    setChannels((current) =>
      current.map((item) =>
        item.channelId === mapped.channelId ? mapped : item
      )
    );

    setMyChannels((current) =>
      current.map((item) =>
        item.channelId === mapped.channelId ? mapped : item
      )
    );

    setEditChannelDialog(null);
    setToast("Channel updated successfully.");
  }

  async function registerUser(form) {
    await apiRequest(
      "/users",
      {
        method: "POST",
        body: JSON.stringify({
          username:
            form.username.trim(),
          email:
            form.email.trim(),
          password:
            form.password,
          displayName:
            form.displayName.trim(),
          avatarUrl:
            form.avatarUrl.trim() || null,
        }),
      }
    );

    setAccountDialog("login");
    navigate("/login");
    setToast(
      "Account created! Please check your email to verify your account before logging in."
    );
  }

  async function loginUser(form) {
    const user = await apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: form.username.trim(),
        password: form.password,
      }),
    });

    localStorage.setItem(
      "motionville.currentUserId",
      String(user.id)
    );
    localStorage.setItem(LAST_ACTIVITY_STORAGE_KEY, String(Date.now()));

    setCurrentUser(user);
    setAccountDialog(null);
    setAccountMenuOpen(false);
    setView("Home");
    navigate("/");
    setYouPanelOpen(false);
    setToast("Signed in successfully.");
  }

  async function updateUser(form) {
    if (!currentUser?.id) return;

    const user = await apiRequest(
      `/users/${currentUser.id}`,
      {
        method: "PUT",
        body: JSON.stringify({
          username:
            form.username.trim(),
          email:
            form.email.trim(),
          password:
            form.password || null,
          displayName:
            form.displayName.trim(),
          avatarUrl:
            form.avatarUrl.trim() || null,
        }),
      }
    );

    setCurrentUser(user);

    localStorage.setItem(
      "motionville.currentUserId",
      String(user.id)
    );

    setToast("Profile updated.");
  }

  async function resendVerificationEmail() {
    try {
      const res = await apiRequest("/auth/resend-verification", {
        method: "POST",
        body: JSON.stringify({ email: currentUser?.email }),
      });
      setToast(res?.message || "Verification email sent.");
    } catch (err) {
      setToast(err?.message || "Could not send verification email. Please try again later.");
    }
  }

  async function logoutUser() {
    try {
      await apiRequest("/auth/logout", { method: "POST" });
    } catch {
    }

    localStorage.removeItem(
      "motionville.currentUserId"
    );
    localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);

    setCurrentUser(null);
    setMyChannels([]);
    setSubscriptionChannels([]);
    setSubscriptions([]);
    setAccountDialog(null);
    setAccountMenuOpen(false);
    setYouPanelOpen(false);
    setView("Home");
    navigate("/");

    setToast("Signed out.");
  }

  const logoutUserRef = useRef(logoutUser);
  logoutUserRef.current = logoutUser;

  useEffect(() => {
    if (!currentUser?.id) return undefined;

    let timeoutId;
    function scheduleLogout() {
      window.clearTimeout(timeoutId);
      const lastActivityAt = Number(
        localStorage.getItem(LAST_ACTIVITY_STORAGE_KEY)
      );
      const elapsed = Date.now() - lastActivityAt;
      const remaining = Math.max(0, SESSION_IDLE_TIMEOUT_MS - elapsed);
      timeoutId = window.setTimeout(() => {
        localStorage.removeItem("motionville.currentUserId");
        localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);
        logoutUserRef.current();
        setToast("You were signed out after 30 minutes of inactivity.");
      }, remaining);
    }

    function recordActivity() {
      localStorage.setItem(LAST_ACTIVITY_STORAGE_KEY, String(Date.now()));
      scheduleLogout();
    }

    function syncSession(event) {
      if (event.key === LAST_ACTIVITY_STORAGE_KEY) {
        scheduleLogout();
      } else if (event.key === "motionville.currentUserId" && !event.newValue) {
        logoutUserRef.current();
      }
    }

    function handleUnauthorized() {
      if (currentUser?.id) {
        logoutUserRef.current();
        setToast("Your session has expired. Please sign in again.");
        setAccountDialog("auth");
      }
    }

    function handleForbidden(event) {
      setToast(event?.detail?.message || "Access denied: You do not have permission for this resource.");
    }

    recordActivity();
    window.addEventListener("pointerdown", recordActivity);
    window.addEventListener("keydown", recordActivity);
    window.addEventListener("touchstart", recordActivity);
    window.addEventListener("scroll", recordActivity, true);
    window.addEventListener("storage", syncSession);
    window.addEventListener("motionville:unauthorized", handleUnauthorized);
    window.addEventListener("motionville:forbidden", handleForbidden);

    return () => {
      window.clearTimeout(timeoutId);
      window.removeEventListener("pointerdown", recordActivity);
      window.removeEventListener("keydown", recordActivity);
      window.removeEventListener("touchstart", recordActivity);
      window.removeEventListener("scroll", recordActivity, true);
      window.removeEventListener("storage", syncSession);
      window.removeEventListener("motionville:unauthorized", handleUnauthorized);
      window.removeEventListener("motionville:forbidden", handleForbidden);
    };
  }, [currentUser?.id]);

  function openReport(target) {
    if (!currentUser?.id) {
      setAccountDialog("auth");
      setToast("Create a profile or sign in to submit a report.");
      return;
    }
    setReportTarget(target);
  }

  function handleReportSubmitted() {
    setReportTarget(null);
    setToast("Your report was submitted.");
  }

  async function subscribeToChannel(channelId) {
    if (!currentUser?.id) {
      setAccountDialog("auth");
      setToast(
        "Create a profile before subscribing."
      );
      return;
    }

    const id = Number(channelId);

    const subscribed =
      subscriptions.includes(id);

    if (subscribed) {
      await apiRequest(
        `/channels/${id}/subscribe?userId=${currentUser.id}`,
        {
          method: "DELETE",
        }
      );

      setSubscriptions((current) =>
        current.filter(
          (item) => item !== id
        )
      );

      setSubscriptionChannels(
        (current) =>
          current.filter(
            (channel) =>
              Number(channel.channelId) !== id
          )
      );

      setSubscriberCounts((current) => ({
        ...current,
        [id]: Math.max(
          0,
          Number(current[id] || 0) - 1
        ),
      }));

      setToast("Unsubscribed.");
    } else {
      await apiRequest(
        `/channels/${id}/subscribe?userId=${currentUser.id}`,
        {
          method: "POST",
        }
      );

      const channel =
        channels.find(
          (item) =>
            Number(item.channelId) === id
        );

      setSubscriptions((current) => [
        ...current,
        id,
      ]);

      if (channel) {
        setSubscriptionChannels(
          (current) => [
            channel,
            ...current.filter(
              (item) =>
                Number(item.channelId) !== id
            ),
          ]
        );
      }

      setSubscriberCounts((current) => ({
        ...current,
        [id]:
          Number(current[id] || 0) + 1,
      }));

      setToast("Subscribed.");
    }
  }

  async function openSubscribers(channel) {
    if (!channel?.channelId) return;

    try {
      const subscribers =
        await apiRequest(
          `/channels/${channel.channelId}/subscribers`
        );

      setSubscriberDialog({
        channel,
        subscribers,
      });
    } catch (error) {
      setToast(
        `Could not load subscribers: ${error.message}`
      );
    }
  }

  async function createVideo(
    form,
    file,
    setProgress,
    generatedThumbnail
  ) {
    const channelId =
      Number(form.channelId);

    const thumbnailToUpload =
      form.thumbnailUrl.trim()
        ? null
        : generatedThumbnail ||
          await createVideoThumbnail(
            file,
            form.title
          );

    const upload =
      await apiRequest(
        "/videos/uploads",
        {
          method: "POST",
          body: JSON.stringify({
            channelId,
            title: form.title.trim(),
            description:
              form.description.trim(),
            thumbnailUrl:
              form.thumbnailUrl.trim() ||
              null,
            categoryId:
              form.categoryId
                ? Number(form.categoryId)
                : null,
            mimeType:
              form.mimeType,
            sizeBytes:
              file.size,
            visibility:
              form.visibility,
          }),
        }
      );

    await uploadFile(
      upload.uploadUrl,
      file,
      upload.mimeType,
      setProgress
    );

    if (
      upload.thumbnailUploadUrl &&
      thumbnailToUpload
    ) {
      await uploadFile(
        upload.thumbnailUploadUrl,
        thumbnailToUpload,
        "image/jpeg",
        () => {}
      );
    }

    setProgress(100);

    await apiRequest(
      `/videos/${upload.videoId}/complete`,
      {
        method: "POST",
      }
    );

    if (Array.isArray(form.tags) && form.tags.length > 0) {
      try {
        const existingTags = await fetchTags();
        for (const tagItem of form.tags) {
          const tagName = String(tagItem).trim();
          if (!tagName) continue;
          let matched = Array.isArray(existingTags) ? existingTags.find((t) => t.name.toLowerCase() === tagName.toLowerCase()) : null;
          let tagId = matched?.id;
          if (!tagId) {
            try {
              const created = await createTag(tagName);
              tagId = created.id;
            } catch {
              // tag might have been created concurrently
            }
          }
          if (tagId) {
            try {
              await addTagToVideo(upload.videoId, tagId);
            } catch {
              // ignore duplicate association
            }
          }
        }
      } catch {
        // ignore tag sync failure so video creation succeeds
      }
    }

    const savedVideo =
      mapApiVideo(
        await apiRequest(
          `/videos/${upload.videoId}`
        ),
        categories
      );

    setVideos((current) => [
      savedVideo,
      ...current.filter(
        (item) =>
          item.videoId !==
          savedVideo.videoId
      ),
    ]);

    setCreateDialog(null);
    setToast(
      "Video uploaded. Preparing it for playback."
    );

    setActiveCategory("All");
    setPageIndex(0);

    if (form.visibility === "PRIVATE") {
      setActiveChannelId(channelId);
      setView("Channel");
      navigate(`/channel/${channelId}`);
    } else {
      setActiveChannelId(null);
      setView("Home");
      navigate("/");
    }
  }

  async function saveVideo(video, form) {
    const response =
      await apiRequest(
        `/videos/${video.videoId}`,
        {
          method: "PUT",
          body: JSON.stringify({
            channelId:
              form.channelId,
            categoryId:
              form.categoryId
                ? Number(form.categoryId)
                : null,
            title:
              form.title.trim(),
            description:
              form.description.trim() ||
              null,
            thumbnailUrl:
              form.thumbnailUrl.trim() ||
              null,
            durationSeconds:
              video.durationSeconds || 0,
            visibility:
              form.visibility,
          }),
        }
      );

    const updatedVideo =
      applyVideoResponse(
        response,
        false
      );

    setToast(
      "Video details saved."
    );

    refreshBackendVideos()
      .catch((error) =>
        setFeedError(
          `Video saved, but the feed could not refresh: ${error.message}`
        )
      );

    return updatedVideo;
  }

  async function publishVideo(
    video,
    form
  ) {
    let candidate = video;

    if (
      video.visibility !==
        form.visibility ||
      video.title !==
        form.title.trim() ||
      video.description !==
        (form.description.trim() || "") ||
      video.thumbnailUrl !==
        (form.thumbnailUrl.trim() ||
          "") ||
      Number(video.categoryId || 0) !==
        Number(form.categoryId || 0)
    ) {
      candidate =
        await saveVideo(
          video,
          form
        );
    }

    if (candidate.publishedAt) {
      setToast("Video published.");
      return candidate;
    }

    const updatedVideo =
      applyVideoResponse(
        await apiRequest(
          `/videos/${video.videoId}/publish`,
          {
            method: "PATCH",
          }
        ),
        false
      );

    setToast("Video published.");

    return updatedVideo;
  }

  async function unpublishVideo(video) {
    const updatedVideo =
      applyVideoResponse(
        await apiRequest(
          `/videos/${video.videoId}/unpublish`,
          {
            method: "PATCH",
          }
        ),
        false
      );

    setToast(
      "Video unpublished. It remains in your channel."
    );

    return updatedVideo;
  }

  async function deleteVideo(video) {
    await apiRequest(
      `/videos/${video.videoId}`,
      {
        method: "DELETE",
      }
    );

    setVideos((current) =>
      current.filter(
        (item) =>
          item.videoId !==
          video.videoId
      )
    );

    const totalElements =
      Math.max(
        0,
        videoPage.totalElements - 1
      );

    const totalPages =
      Math.ceil(
        totalElements /
          videoPage.size
      );

    const lastPage =
      Math.max(
        0,
        totalPages - 1
      );

    setPageIndex(lastPage);

    setVideoPage((current) => ({
      ...current,
      totalElements:
        Math.max(
          0,
          current.totalElements - 1
        ),
      totalPages:
        Math.ceil(
          Math.max(
            0,
            current.totalElements - 1
          ) /
            current.size
        ),
      page:
        Math.min(
          current.page,
          lastPage
        ),
    }));

    setManageVideo(null);
    setToast("Video deleted.");

    refreshBackendVideos(
      lastPage
    ).catch((error) =>
      setFeedError(
        `Video deleted, but the feed could not refresh: ${error.message}`
      )
    );
  }

  function deleteVideoFromCard(video) {
    if (
      !window.confirm(
        `Delete "${video.title}" permanently?`
      )
    ) {
      return;
    }

    deleteVideo(video).catch(
      (error) =>
        setFeedError(
          `Could not delete video: ${error.message}`
        )
    );
  }

  function toggleLike(
    videoId,
    liked
  ) {
    setLikedVideos((current) => {
      const alreadyLiked =
        current.includes(videoId);

      if (
        liked === true &&
        !alreadyLiked
      ) {
        return [
          ...current,
          videoId,
        ];
      }

      if (
        liked === false &&
        alreadyLiked
      ) {
        return current.filter(
          (id) =>
            id !== videoId
        );
      }

      if (
        liked === undefined
      ) {
        return alreadyLiked
          ? current.filter(
              (id) =>
                id !== videoId
            )
          : [
              ...current,
              videoId,
            ];
      }

      return current;
    });
  }

  function toggleSubscription(
    channelId
  ) {
    subscribeToChannel(
      channelId
    ).catch((error) => {
      setToast(
        `Could not update subscription: ${error.message}`
      );
    });
  }

  function selectVideo(video) {
    setSelectedVideo(video);
    navigate(`/watch/${video.videoId}`, {
      state: {
        from: location.state?.from || `${location.pathname}${location.search}`,
      },
    });

    setHistory((current) => [
      video.videoId,
      ...current.filter(
        (id) =>
          id !== video.videoId
      ),
    ]);
  }

  function updateWatchHistory(entry) {
    setWatchHistory((current) => [
      entry,
      ...current.filter((item) => Number(item.videoId) !== Number(entry.videoId)),
    ]);
  }

  async function removeHistoryVideo(video) {
    if (!currentUser?.id || removingHistoryVideoId !== null) return;
    if (!window.confirm(`Remove "${video.title}" from your watch history?`)) return;

    setRemovingHistoryVideoId(video.videoId);
    setWatchHistoryActionError("");
    try {
      await removeWatchHistoryItem(currentUser.id, video.videoId);
      setWatchHistory((current) => current.filter(
        (entry) => Number(entry.videoId) !== Number(video.videoId)
      ));
    } catch (error) {
      setWatchHistoryActionError(`Could not remove video from watch history: ${error.message}`);
    } finally {
      setRemovingHistoryVideoId(null);
    }
  }

  async function clearAllWatchHistory() {
    if (!currentUser?.id || watchHistoryActionBusy) return;
    if (!window.confirm("Clear your entire watch history? This cannot be undone.")) return;

    setWatchHistoryActionBusy(true);
    setWatchHistoryActionError("");
    try {
      await clearWatchHistory(currentUser.id);
      setWatchHistory([]);
    } catch (error) {
      setWatchHistoryActionError(`Could not clear watch history: ${error.message}`);
    } finally {
      setWatchHistoryActionBusy(false);
    }
  }

  function chooseView(nextView) {
    setView(nextView);
    navigate(routeForView(nextView));
    setActiveChannelId(null);
    setActiveCategory("All");
    setPageIndex(0);
    setSidebarOpen(false);
  }

  function showPlaylists() {
    if (!currentUser?.id) {
      setAccountDialog("auth");
      setToast("Create a profile to manage playlists.");
      return;
    }
    setSelectedPlaylist(null);
    setPlaylistVideos([]);
    chooseView("Playlists");
  }

  async function openPlaylist(playlist) {
    setSelectedPlaylist(playlist);
    setPlaylistVideos([]);
    setPlaylistLoading(true);
    setPlaylistError("");
    setView("Playlists");
    setActiveChannelId(null);
    setActiveCategory("All");
    setPageIndex(0);
    setSidebarOpen(false);
    navigate(`/playlist/${playlist.id}`);
    try {
      const result = await fetchPlaylistVideos(playlist.id);
      setPlaylistVideos(result.map((item) => {
        const existing = videos.find((video) => Number(video.videoId) === Number(item.videoId));
        return {
          ...existing,
          ...item,
          videoId: item.videoId,
          channelId: existing?.channelId,
          serverVideo: true,
          createdAt: existing?.createdAt || item.addedAt,
        };
      }));
    } catch (error) {
      setPlaylistError(`Could not load playlist videos: ${error.message}`);
    } finally {
      setPlaylistLoading(false);
    }
  }

  async function savePlaylist(form) {
    if (!currentUser?.id) throw new Error("Create a profile before managing playlists.");

    const wasEditing = Boolean(playlistEditor?.playlist);
    const saved = wasEditing
      ? await updatePlaylist(playlistEditor.playlist.id, form)
      : await createPlaylist({ ...form, ownerId: Number(currentUser.id) });

    setPlaylists((current) => [
      saved,
      ...current.filter((playlist) => playlist.id !== saved.id),
    ]);

    if (pendingPlaylistVideo) {
      try {
        await addVideoToPlaylist(saved.id, pendingPlaylistVideo.videoId);
      } catch (error) {
        setPlaylistEditor({ playlist: saved });
        throw error;
      }
      setPlaylists((current) => current.map((playlist) =>
        playlist.id === saved.id
          ? { ...playlist, videoIds: [...(playlist.videoIds || []), pendingPlaylistVideo.videoId] }
          : playlist
      ));
      setToast(`Saved to ${saved.title}.`);
      setPendingPlaylistVideo(null);
    } else if (selectedPlaylist?.id === saved.id) {
      setSelectedPlaylist(saved);
    }
    setPlaylistEditor(null);
  }

  async function saveVideoToPlaylist(playlistId) {
    if (!playlistPickerVideo) return;
    await addVideoToPlaylist(playlistId, playlistPickerVideo.videoId);
    setPlaylists((current) => current.map((playlist) =>
      playlist.id === playlistId
        ? { ...playlist, videoIds: [...(playlist.videoIds || []), Number(playlistPickerVideo.videoId)] }
        : playlist
    ));
    setPlaylistPickerVideo(null);
    setToast("Video saved to playlist.");
  }

  async function addVideoFromPlaylist(video) {
    if (!playlistAddTarget) return;
    const playlistId = playlistAddTarget.id;
    await addVideoToPlaylist(playlistId, video.videoId);
    const videoId = Number(video.videoId);
    setPlaylists((current) => current.map((playlist) =>
      playlist.id === playlistId
        ? { ...playlist, videoIds: [...(playlist.videoIds || []), videoId] }
        : playlist
    ));
    setSelectedPlaylist((current) => current?.id === playlistId
      ? { ...current, videoIds: [...(current.videoIds || []), videoId] }
      : current
    );
    if (selectedPlaylist?.id === playlistId) {
      setPlaylistVideos((current) => [...current, video]);
    }
  }

  async function openPlaylistVideoPicker() {
    if (!selectedPlaylist) return;
    setPlaylistAddTarget(selectedPlaylist);
    setPlaylistAddLoading(true);
    setPlaylistAddError("");

    try {
      const requests = [];
      const publicQuery = buildVideoQuery({
        search: "",
        page: 0,
        sort: "createdAt,desc",
        publicOnly: true,
      });
      requests.push(apiRequest(publicQuery));

      myChannels.forEach((channel) => {
        requests.push(apiRequest(buildVideoQuery({
          search: "",
          page: 0,
          sort: "createdAt,desc",
          channelId: channel.channelId,
          publicOnly: false,
        })));
      });

      const firstPages = await Promise.all(requests);
      const pages = await Promise.all(firstPages.map(async (firstPage, index) => {
        const channel = myChannels[index - 1];
        const remaining = Array.from(
          { length: Math.max(0, firstPage.totalPages - 1) },
          (_, offset) => offset + 1
        );
        return [
          ...firstPage.content,
          ...await Promise.all(remaining.map((page) => apiRequest(buildVideoQuery({
            search: "",
            page,
            sort: "createdAt,desc",
            ...(channel ? { channelId: channel.channelId, publicOnly: false } : { publicOnly: true }),
          })))).then((results) => results.flatMap((result) => result.content)),
        ];
      }));
      const uniqueVideos = new Map();
      [...videos, ...pages.flatMap((page) => page.map((video) => mapApiVideo(video, categories)))]
        .filter((video) => video.serverVideo)
        .forEach((video) => uniqueVideos.set(Number(video.videoId), video));
      setPlaylistAddVideos([...uniqueVideos.values()]);
    } catch (error) {
      setPlaylistAddVideos([]);
      setPlaylistAddError(`Could not load videos: ${error.message}`);
    } finally {
      setPlaylistAddLoading(false);
    }
  }

  async function deleteUserPlaylist(playlist) {
    if (!window.confirm(`Delete "${playlist.title}"?`)) return;
    try {
      await deletePlaylist(playlist.id);
      setPlaylists((current) => current.filter((item) => item.id !== playlist.id));
      if (selectedPlaylist?.id === playlist.id) {
        setSelectedPlaylist(null);
        setPlaylistVideos([]);
      }
      setToast("Playlist deleted.");
    } catch (error) {
      setPlaylistError(`Could not delete playlist: ${error.message}`);
    }
  }

  async function removePlaylistVideo(video) {
    if (!selectedPlaylist) return;
    try {
      await removeVideoFromPlaylist(selectedPlaylist.id, video.videoId);
      setPlaylistVideos((current) => current.filter((item) => item.videoId !== video.videoId));
      setPlaylists((current) => current.map((playlist) =>
        playlist.id === selectedPlaylist.id
          ? { ...playlist, videoIds: (playlist.videoIds || []).filter((id) => Number(id) !== Number(video.videoId)) }
          : playlist
      ));
      setSelectedPlaylist((current) => current?.id === selectedPlaylist.id
        ? { ...current, videoIds: (current.videoIds || []).filter((id) => Number(id) !== Number(video.videoId)) }
        : current
      );
    } catch (error) {
      setPlaylistError(`Could not remove video: ${error.message}`);
    }
  }

  async function movePlaylistVideo(index, direction) {
    if (!selectedPlaylist) return;
    const next = [...playlistVideos];
    const target = index + direction;
    [next[index], next[target]] = [next[target], next[index]];
    setPlaylistVideos(next);
    setPlaylistError("");
    try {
      await reorderPlaylistVideos(selectedPlaylist.id, next.map((video) => video.videoId));
      const orderedIds = next.map((video) => Number(video.videoId));
      setSelectedPlaylist((current) => current?.id === selectedPlaylist.id
        ? { ...current, videoIds: orderedIds }
        : current
      );
      setPlaylists((current) => current.map((playlist) =>
        playlist.id === selectedPlaylist.id
          ? { ...playlist, videoIds: orderedIds }
          : playlist
      ));
    } catch (error) {
      setPlaylistVideos(playlistVideos);
      setPlaylistError(`Could not reorder playlist: ${error.message}`);
    }
  }

  function startSaveToPlaylist(video) {
    if (!currentUser?.id) {
      setAccountDialog("auth");
      setToast("Create a profile to save videos to playlists.");
      return;
    }
    setPlaylistPickerVideo(video);
  }

  function showChannel(channel) {
    setSearch("");
    setSearchInput("");
    setView("Channel");
    navigate(`/channel/${channel.channelId}`);
    setActiveChannelId(
      channel.channelId
    );
    setActiveCategory("All");
    setPageIndex(0);
    setSidebarOpen(false);
  }

  const activeChannel =
    view === "Channel"
      ? channelById.get(
          Number(activeChannelId)
        )
      : null;
  const ownChannel = myChannels[0] || null;

  const hasOwnedChannel = myChannels.length > 0;

  const feedTitle =
    view === "Trending"
      ? "Popular right now"
      : view === "Recently added"
        ? "Freshly posted"
        : view === "Your channel"
          ? "Your videos"
          : view === "Channel"
            ? `${
                activeChannel?.name ||
                "Channel"
              } videos`
            : view === "Liked videos"
              ? "Videos you liked"
              : view === "History"
                ? "Recently watched"
                : view === "Subscriptions"
                  ? "From your subscriptions"
                  : view === "Search"
                    ? search
                      ? `Results for "${search}"`
                      : "Search"
                    : activeCategory ===
                        "All"
                      ? "Videos"
                      : activeCategory;

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-brand-area">
          <button
            className="icon-button menu-trigger"
            onClick={() =>
              setSidebarOpen(
                (open) => !open
              )
            }
            aria-expanded={sidebarOpen}
            aria-label="Toggle menu"
          >
            <Icon name="menu" />
          </button>

          <a
            className="brand"
            href="/"
            onClick={(event) => {
              event.preventDefault();
              chooseView("Home");
              setActiveChannelId(null);
              setSidebarOpen(false);
              setYouPanelOpen(false);
            }}
            aria-label="MotionVille home"
          >
            <span className="brand-symbol">
              <Icon
                name="play"
                size={14}
                filled
              />
            </span>

            <span>
              Motion
              <span className="brand-red">
                Ville
              </span>
            </span>
          </a>
        </div>

        <form
          className="search-form"
          onSubmit={(event) => {
            event.preventDefault();
            setPageIndex(0);
            const query = searchInput.trim();
            setSearch(query);
            navigate(query ? `/search?q=${encodeURIComponent(query)}` : "/search");
          }}
        >
          <div className="search-input-wrap">
            <Icon
              name="search"
              size={19}
            />

            <input
              value={searchInput}
              onChange={(event) => {
                setSearchInput(event.target.value);
              }}
              placeholder="Search videos, creators, and more"
              aria-label="Search"
            />

            {searchInput && (
              <button
                type="button"
                className="search-clear"
                onClick={() => {
                  setSearchInput("");
                  setSearch("");
                  setPageIndex(0);
                  if (location.pathname === "/search") {
                    navigate("/");
                  }
                }}
              >
                <Icon
                  name="close"
                  size={16}
                />
              </button>
            )}
          </div>

          <button
            className="search-submit"
            type="submit"
            aria-label="Search"
          >
            <Icon name="search" />
          </button>
        </form>

        <div className="topbar-actions">
          {currentUser ? (
            <>
              <button
                className="create-button"
                onClick={() =>
                  setCreateDialog(
                    hasOwnedChannel
                      ? "video"
                      : "channel"
                  )
                }
              >
                <Icon
                  name="plus"
                  size={18}
                />
                <span>Create</span>
              </button>

              {isDemoAdmin && (
                <button
                  className="create-button"
                  type="button"
                  onClick={() => setReportsAdminOpen(true)}
                >
                  Manage reports
                </button>
              )}

              <div className="notification-menu" ref={notificationMenuRef}>
                <button
                  className="icon-button notification-button"
                  type="button"
                  aria-label={`Notifications${unreadNotificationCount ? `, ${unreadNotificationCount} unread` : ""}`}
                  aria-expanded={notificationsOpen}
                  onClick={() => {
                    const opening = !notificationsOpen;
                    setNotificationsOpen(opening);
                    if (opening) setAccountMenuOpen(false);
                    if (opening) refreshNotifications();
                  }}
                >
                  <Icon name="bell" />
                  {unreadNotificationCount > 0 && (
                    <span className="notification-unread-dot" aria-hidden="true" />
                  )}
                </button>

                {notificationsOpen && (
                  <section className="notification-panel" aria-label="Notifications">
                    <header className="notification-panel-header">
                      <strong>
                        Notifications
                        <span className="notification-total-count">{notificationCount}</span>
                      </strong>
                      {unreadNotificationCount > 0 && (
                        <button
                          type="button"
                          disabled={notificationActionBusy}
                          onClick={handleMarkAllNotificationsRead}
                        >
                          {notificationActionBusy ? "Marking…" : "Mark all as read"}
                        </button>
                      )}
                    </header>

                    {notificationsError && (
                      <p className="notification-error" role="alert">{notificationsError}</p>
                    )}
                    {notifications.length === 0 ? (
                      <p className="notification-empty">
                        {notificationsError ? "Notifications could not be loaded." : "No notifications yet."}
                      </p>
                    ) : (
                      <div className="notification-list">
                        {notifications.map((notification) => (
                          <button
                            className={`notification-item ${notification.readAt === null ? "notification-unread" : ""}`}
                            key={notification.id}
                            type="button"
                            onClick={() => handleNotificationClick(notification)}
                          >
                            <span className="notification-message">{notification.message}</span>
                            <time dateTime={notification.createdAt}>
                              {formatAge(notification.createdAt)}
                            </time>
                          </button>
                        ))}
                      </div>
                    )}
                  </section>
                )}
              </div>

              <div ref={accountMenuRef} style={{ position: "relative" }}>
                <button
                  type="button"
                  aria-label="Open account menu"
                  aria-expanded={accountMenuOpen}
                  onClick={() => {
                    const opening = !accountMenuOpen;
                    setAccountMenuOpen(opening);
                    if (opening) setNotificationsOpen(false);
                  }}
                  style={{
                    border: 0,
                    background: "transparent",
                    padding: 0,
                    cursor: "pointer",
                    borderRadius: "50%",
                  }}
                >
                  <Avatar
                    src={currentUser.avatarUrl}
                    name={
                      currentUser.displayName ||
                      currentUser.username ||
                      "M"
                    }
                    size="small"
                  />
                </button>

                {accountMenuOpen && (
                  <AccountMenu
                    user={currentUser}
                    myChannels={myChannels}
                    subscriptionChannels={subscriptionChannels}
                    onProfile={() => {
                      setAccountMenuOpen(false);
                      setAccountDialog("profile");
                    }}
                    onHome={() => {
                      setAccountMenuOpen(false);
                      chooseView("Your channel");
                    }}
                    onHistory={() => {
                      setAccountMenuOpen(false);
                      chooseView("History");
                    }}
                    onLiked={() => {
                      setAccountMenuOpen(false);
                      chooseView("Liked videos");
                    }}
                    onPlaylists={() => {
                      setAccountMenuOpen(false);
                      showPlaylists();
                    }}
                    onSubscriptions={() => {
                      setAccountMenuOpen(false);
                      chooseView("Subscriptions");
                    }}
                    onLogout={logoutUser}
                  />
                )}
              </div>
            </>
          ) : (
            <>
              <button
                className="create-button"
                type="button"
                onClick={() => navigate("/register")}
              >
                <Icon name="plus" size={18} />
                <span>Create account</span>
              </button>

              <button
                className="button button-primary"
                type="button"
                onClick={() => navigate("/login")}
              >
                Sign in
              </button>
            </>
          )}
        </div>
      </header>

      {currentUser && !currentUser.emailVerified && (
        <aside
          style={{
            background: "#fef3c7",
            borderBottom: "1px solid #fde68a",
            padding: "8px 24px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            fontSize: 12,
            color: "#92400e",
            zIndex: 30,
            position: "relative"
          }}
          aria-label="Email verification notice"
        >
          <span>
            Please verify your email address (<strong>{currentUser.email}</strong>) to unlock channel creation and video uploads.
          </span>
          <button
            type="button"
            className="feed-filter"
            style={{ fontSize: 11, padding: "4px 10px", cursor: "pointer", background: "#fff", borderColor: "#fcd34d" }}
            onClick={resendVerificationEmail}
          >
            Resend verification email
          </button>
        </aside>
      )}

      {sidebarOpen && (
        <button
          className="mobile-scrim"
          aria-label="Close navigation"
          onClick={() =>
            setSidebarOpen(false)
          }
        />
      )}

      <div className={`app-body ${sidebarOpen ? "sidebar-expanded" : "sidebar-collapsed"}`}>
        <aside
          className={`sidebar ${
            sidebarOpen
              ? "sidebar-open"
              : "sidebar-closed"
          }`}
          onMouseLeave={() => setYouPanelOpen(false)}
        >
          <div className="sidebar-scroll-content">
          <nav
            className="nav-group"
            aria-label="Main navigation"
          >
            <button
              className={`nav-item ${
                view === "Home"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView("Home")
              }
            >
              <Icon
                name="home"
                filled={
                  view === "Home"
                }
              />
              <span>Home</span>
            </button>

            <button
              className={`nav-item ${
                view === "Explore"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView("Explore")
              }
            >
              <Icon name="compass" />
              <span>Explore</span>
            </button>

            <button
              className={`nav-item ${
                view === "Trending"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView("Trending")
              }
            >
              <Icon name="shorts" />
              <span>Trending</span>
            </button>

            <button
              className={`nav-item ${
                view ===
                "Subscriptions"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView(
                  "Subscriptions"
                )
              }
            >
              <Icon
                name="subscriptions"
              />
              <span>
                Subscriptions
              </span>
            </button>
          </nav>

          <div className="sidebar-rule" />

          <button
            type="button"
            className={`nav-item sidebar-you-trigger ${youPanelOpen ? "nav-active" : ""}`}
            aria-expanded={youPanelOpen}
            onClick={() => {
              setYouPanelOpen((open) => !open);
            }}
          >
            <Icon name="library" />
            <span>You</span>
          </button>

          <div
            className="sidebar-rule sidebar-library-divider"
            style={{ display: youPanelOpen ? "block" : "none" }}
          />

          <nav
            className="nav-group sidebar-library-items"
            aria-label="Your library"
            style={{ display: youPanelOpen ? "grid" : "none" }}
          >
            <button
              className={`nav-item ${
                view ===
                "Your channel"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView(
                  "Your channel"
                )
              }
            >
              <Icon name="library" />
              <span>
                Your channel
              </span>
              <Icon
                name="chevron"
                size={16}
              />
            </button>

            <button
              className={`nav-item ${
                view === "History"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView("History")
              }
            >
              <Icon name="history" />
              <span>History</span>
            </button>

            <button
              className={`nav-item ${
                view ===
                "Liked videos"
                  ? "nav-active"
                  : ""
              }`}
              onClick={() =>
                chooseView(
                  "Liked videos"
                )
              }
            >
              <Icon name="like" />
              <span>
                Liked videos
              </span>
            </button>

            <button
              className={`nav-item ${view === "Playlists" ? "nav-active" : ""}`}
              onClick={showPlaylists}
            >
              <Icon name="library" />
              <span>Playlists</span>
            </button>
          </nav>

          <div className="sidebar-rule" />

          <section className="sidebar-channels">
            <div className="sidebar-section-heading">
              <span>Your channel</span>

              {myChannels.length === 0 && (
                <button
                  className="small-add"
                  onClick={() =>
                    currentUser
                      ? setCreateDialog("channel")
                      : setAccountDialog("register")
                  }
                  aria-label="Create channel"
                >
                  <Icon name="plus" size={17} />
                </button>
              )}
            </div>

            {myChannels.map((channel) => (
              <button
                className="nav-item channel-nav-item"
                key={channel.channelId}
                onClick={() => showChannel(channel)}
              >
                <Avatar
                  src={channel.avatarUrl}
                  name={channel.name}
                  size="tiny"
                />
                <span>{channel.name}</span>
              </button>
            ))}

            {myChannels.length === 0 && (
              <button
                className="nav-item add-channel-nav"
                onClick={() =>
                  currentUser
                    ? setCreateDialog("channel")
                    : setAccountDialog("register")
                }
              >
                <span className="add-channel-icon">
                  <Icon name="plus" size={16} />
                </span>
                <span>Create a channel</span>
              </button>
            )}
          </section>

          <div className="sidebar-bottom">
            <div className="sidebar-note-icon">
              <Icon
                name="sparkle"
                size={18}
              />
            </div>

            <p>
              Good videos make a good
              day.
            </p>

            <span>
              MotionVille · Create your
              corner
            </span>
          </div>
          </div>

          {youPanelOpen && !sidebarOpen && (
            <section className="you-library-flyout" aria-label="Your library">
              <button className="you-library-heading" onClick={() => { chooseView("Your channel"); setYouPanelOpen(false); }}>
                <span>You</span><Icon name="chevron" size={16} />
              </button>
              <button className={`nav-item ${view === "History" ? "nav-active" : ""}`} onClick={() => { chooseView("History"); setYouPanelOpen(false); }}><Icon name="history" /><span>History</span></button>
              <button className={`nav-item ${view === "Playlists" ? "nav-active" : ""}`} onClick={() => { showPlaylists(); setYouPanelOpen(false); }}><Icon name="library" /><span>Playlists</span></button>
              <button className={`nav-item ${view === "Liked videos" ? "nav-active" : ""}`} onClick={() => { chooseView("Liked videos"); setYouPanelOpen(false); }}><Icon name="like" /><span>Liked videos</span></button>
              <button className={`nav-item ${view === "Your channel" ? "nav-active" : ""}`} onClick={() => { chooseView("Your channel"); setYouPanelOpen(false); }}><Icon name="video" /><span>Your videos</span></button>
            </section>
          )}

        </aside>

        <main className="main-content">
          <LiveBroadcastList onWatch={(id) => navigate(`${location.pathname}?live=${id}`)} />
          {view === "Your channel" ? (
            <>
              <section className="account-profile-card" aria-label="Your profile">
                <Avatar
                  src={currentUser?.avatarUrl}
                  name={currentUser?.displayName || currentUser?.username || "M"}
                  size="profile"
                />
                <div className="account-profile-details">
                  <p className="section-eyebrow">YOUR PROFILE</p>
                  <h1>{currentUser?.displayName || currentUser?.username || "Your profile"}</h1>
                  <p>@{currentUser?.username || "username"}</p>
                  {currentUser?.email && <span>{currentUser.email}</span>}
                </div>
                {currentUser && (
                  <button
                    type="button"
                    className="feed-filter account-profile-edit"
                    onClick={() => setAccountDialog("profile")}
                  >
                    Edit profile
                  </button>
                )}
              </section>

              {ownChannel ? (
                <div className="channel-page-header">
                  <div className="channel-banner-container">
                    {ownChannel.bannerUrl ? (
                      <img
                        src={ownChannel.bannerUrl}
                        alt={`${ownChannel.name} banner`}
                      />
                    ) : (
                      <div
                        style={{
                          width: "100%",
                          height: "100%",
                          background:
                            "linear-gradient(115deg, #1e293b, #334155 58%, #475569)",
                        }}
                      />
                    )}
                  </div>

                  <div className="channel-identity-bar">
                    <div className="channel-identity-avatar">
                      <Avatar
                        src={
                          ownChannel.profileImageUrl ||
                          ownChannel.avatarUrl
                        }
                        name={ownChannel.name}
                        size="channel-header"
                      />
                    </div>

                    <div className="channel-identity-info">
                      <h2>{ownChannel.name}</h2>
                      <div className="channel-meta-text">
                        <span className="channel-handle">
                          {ownChannel.handle}
                        </span>
                        <span>•</span>
                        <span>
                          {subscriberCounts[
                            ownChannel.channelId
                          ] || 0}{" "}
                          subscriber
                          {Number(
                            subscriberCounts[
                              ownChannel.channelId
                            ] || 0
                          ) === 1
                            ? ""
                            : "s"}
                        </span>
                        <span>•</span>
                        <span>
                          {ownChannel.videoCount || 0}{" "}
                          video
                          {Number(
                            ownChannel.videoCount || 0
                          ) === 1
                            ? ""
                            : "s"}
                        </span>
                        <span>•</span>
                        <span>
                          {formatViews(
                            ownChannel.viewCount || 0
                          )}
                        </span>
                        {ownChannel.createdAt && (
                          <>
                            <span>•</span>
                            <span>
                              Joined {formatDateJoined(ownChannel.createdAt) || formatAge(ownChannel.createdAt)}
                            </span>
                          </>
                        )}
                      </div>

                      {ownChannel.description && (
                        <p className="channel-description-text">
                          {ownChannel.description}
                        </p>
                      )}

                      <div className="channel-actions-bar">
                        <button
                          type="button"
                          className="feed-filter"
                          onClick={() =>
                            setEditChannelDialog(ownChannel)
                          }
                        >
                          <Icon name="edit" size={14} />
                          Edit channel
                        </button>
                        <button
                          type="button"
                          className="feed-filter"
                          onClick={() => showChannel(ownChannel)}
                        >
                          View channel
                        </button>
                        <button
                          type="button"
                          className="button button-live"
                          onClick={() => setLiveStreamDialogOpen(true)}
                        >
                          <span
                            className="live-status-dot"
                            aria-hidden="true"
                          />
                          Start live stream
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <section className="your-channel-empty">
                  <h2>You don’t have a channel yet</h2>
                  <p>Create a channel to publish videos and share your profile with viewers.</p>
                  {currentUser && myChannels.length === 0 && (
                    <button
                      type="button"
                      className="button button-primary"
                      onClick={() => setCreateDialog("channel")}
                    >
                      <Icon name="plus" size={17} />
                      Create a channel
                    </button>
                  )}
                </section>
              )}
            </>
          ) : view ===
              "Channel" &&
            activeChannel ? (
            <div className="channel-page-header">
              <div className="channel-banner-container">
                {activeChannel.bannerUrl ? (
                  <img
                    src={activeChannel.bannerUrl}
                    alt={`${activeChannel.name} banner`}
                  />
                ) : (
                  <div
                    style={{
                      width: "100%",
                      height: "100%",
                      background:
                        "linear-gradient(115deg, #1e293b, #334155 58%, #475569)",
                    }}
                  />
                )}
              </div>

              <div className="channel-identity-bar">
                <div className="channel-identity-avatar">
                  <Avatar
                    src={
                      activeChannel.profileImageUrl ||
                      activeChannel.avatarUrl
                    }
                    name={activeChannel.name}
                    size="channel-header"
                  />
                </div>

                <div className="channel-identity-info">
                  <h1>{activeChannel.name}</h1>

                  <div className="channel-meta-text">
                    <span className="channel-handle">
                      {activeChannel.handle}
                    </span>
                    <span>•</span>
                    <span>
                      {subscriberCounts[
                        activeChannel.channelId
                      ] || 0}{" "}
                      subscriber
                      {Number(
                        subscriberCounts[
                          activeChannel.channelId
                        ] || 0
                      ) === 1
                        ? ""
                        : "s"}
                    </span>
                    <span>•</span>
                    <span>
                      {activeChannel.videoCount || 0}{" "}
                      video
                      {Number(
                        activeChannel.videoCount || 0
                      ) === 1
                        ? ""
                        : "s"}
                    </span>
                    <span>•</span>
                    <span>
                      {formatViews(
                        activeChannel.viewCount || 0
                      )}
                    </span>
                    {activeChannel.createdAt && (
                      <>
                        <span>•</span>
                        <span>
                          Joined {formatDateJoined(activeChannel.createdAt) || formatAge(activeChannel.createdAt)}
                        </span>
                      </>
                    )}
                  </div>

                  {activeChannel.description && (
                    <p className="channel-description-text">
                      {activeChannel.description}
                    </p>
                  )}

                  <div className="channel-actions-bar">
                    {currentUser &&
                      (activeChannel.ownerId ===
                        currentUser.id ||
                        currentUser.role ===
                          "ADMIN") && (
                        <button
                          type="button"
                          className="feed-filter"
                          onClick={() =>
                            setEditChannelDialog(
                              activeChannel
                            )
                          }
                        >
                          <Icon
                            name="edit"
                            size={14}
                          />
                          Edit channel
                        </button>
                      )}

                    {currentUser && (
                      <button
                        type="button"
                        className="feed-filter"
                        onClick={() =>
                          openSubscribers(
                            activeChannel
                          )
                        }
                      >
                        View subscribers
                      </button>
                    )}

                    {currentUser &&
                      activeChannel.ownerId !==
                        currentUser.id && (
                        <button
                          type="button"
                          className={`feed-filter ${
                            (
                              subscriptionChannels ||
                              []
                            ).some(
                              (c) =>
                                c.channelId ===
                                activeChannel.channelId
                            )
                              ? "subscribed"
                              : ""
                          }`}
                          onClick={() =>
                            toggleSubscription(
                              activeChannel.channelId
                            )
                          }
                        >
                          {(
                            subscriptionChannels ||
                            []
                          ).some(
                            (c) =>
                              c.channelId ===
                              activeChannel.channelId
                          )
                            ? "Subscribed"
                            : "Subscribe"}
                        </button>
                      )}
                  </div>
                </div>
              </div>
            </div>
          ) : null}

          {view === "Playlists" ? (
            <PlaylistLibrary
              playlists={playlists}
              selectedPlaylist={selectedPlaylist}
              videos={playlistVideos}
              loading={playlistLoading}
              error={playlistError}
              onCreate={() => setPlaylistEditor({ playlist: null })}
              onOpen={openPlaylist}
              onBack={() => {
                setSelectedPlaylist(null);
                setPlaylistVideos([]);
                setPlaylistError("");
              }}
              onEdit={(playlist) => setPlaylistEditor({ playlist })}
              onDelete={deleteUserPlaylist}
              onSelectVideo={selectVideo}
              onRemoveVideo={removePlaylistVideo}
              onMoveVideo={movePlaylistVideo}
              onAddVideos={openPlaylistVideoPicker}
            />
          ) : <>
          <div
            className="category-scroller"
            aria-label="Video categories"
          >
            {[
              {
                id: null,
                name: "All",
              },
              ...categories,
            ].map((category) => (
              <button
                key={
                  category.id ??
                  category.name
                }
                className={`category-chip ${
                  activeCategory ===
                  category.name
                    ? "category-selected"
                    : ""
                }`}
                onClick={() => {
                  setActiveCategory(
                    category.name
                  );
                  setPageIndex(0);
                }}
              >
                <span>
                  {category.name ===
                    "All" && (
                    <Icon
                      name="grid"
                      size={14}
                    />
                  )}
                  {category.name}
                </span>
              </button>
            ))}
          </div>

          <section className="feed-section">
            <div className="feed-heading">
              <div>
                <p className="section-eyebrow">
                  {view === "Home"
                    ? "Picked for you"
                    : view === "Search"
                      ? "Search results"
                      : view}
                </p>

                <h2>{feedTitle}</h2>
              </div>

              <div className="feed-controls">
                {view === "History" && currentUser?.id && watchHistory.length > 0 && (
                  <button
                    className="feed-filter"
                    type="button"
                    disabled={watchHistoryActionBusy || removingHistoryVideoId !== null}
                    onClick={clearAllWatchHistory}
                  >
                    {watchHistoryActionBusy ? "Clearing…" : "Clear watch history"}
                  </button>
                )}
                <select
                  className="feed-sort"
                  aria-label="Sort videos"
                  value={sortOrder}
                  onChange={(event) => {
                    setSortOrder(
                      event.target.value
                    );
                    setPageIndex(0);
                  }}
                >
                  <option value="createdAt,desc">
                    Newest
                  </option>
                  <option value="createdAt,asc">
                    Oldest
                  </option>
                  <option value="title,asc">
                    Title A-Z
                  </option>
                  <option value="title,desc">
                    Title Z-A
                  </option>
                </select>

                <button
                  className="feed-filter"
                  onClick={() =>
                    refreshBackendVideos().catch(
                      (error) =>
                        setFeedError(
                          error.message
                        )
                    )
                  }
                >
                  Refresh
                </button>
              </div>
            </div>

            {visibleFeedError && (
              <p
                className="feed-error"
                role="alert"
              >
                {visibleFeedError}
              </p>
            )}
            {view === "History" && watchHistoryActionError && (
              <p className="feed-error" role="alert">{watchHistoryActionError}</p>
            )}

            {view === "History" && currentUser?.id && watchHistoryLoading ? (
              <div className="empty-feed">
                <span><Icon name="history" size={25} /></span>
                <h2>Loading watch history</h2>
                <p>Fetching your watch history and resume positions…</p>
              </div>
            ) : view === "History" && currentUser?.id && watchHistoryError ? (
              <div className="empty-feed">
                <span><Icon name="history" size={25} /></span>
                <h2>Watch history is unavailable</h2>
                <p>{watchHistoryError}</p>
              </div>
            ) : (view !== "History" || !currentUser?.id) &&
            !backendLoaded &&
            !feedError &&
            !videosLoading ? (
              <div className="empty-feed">
                <span>
                  <Icon
                    name="video"
                    size={25}
                  />
                </span>

                <h2>
                  Loading videos
                </h2>

                <p>
                  Connecting to the video
                  service…
                </p>
              </div>
            ) : (view !== "History" || !currentUser?.id) &&
              !backendLoaded &&
              feedError ? (
              <div className="empty-feed">
                <span>
                  <Icon
                    name="video"
                    size={25}
                  />
                </span>

                <h2>
                  Videos are unavailable
                </h2>

                <p>
                  Could not connect to the
                  video service. Check the
                  backend and try again.
                </p>

                <button
                  className="button button-primary"
                  onClick={() =>
                    refreshBackendVideos().catch(
                      (error) =>
                        setFeedError(
                          error.message
                        )
                    )
                  }
                >
                  Try again
                </button>
              </div>
            ) : (visibleVideos.length || (view === "Search" && search.trim() && matchingChannels.length > 0)) ? (
              <>
                {view === "Search" && search.trim() && matchingChannels.length > 0 && (
                  <div className="search-channels-container" style={{ marginBottom: "1.75rem", display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                    <h3 style={{ fontSize: "1rem", fontWeight: 600, opacity: 0.8, margin: "0 0 0.25rem 0" }}>Channels</h3>
                    {matchingChannels.map((ch) => (
                      <div
                        key={ch.channelId}
                        className="search-channel-result"
                        onClick={() => showChannel(ch)}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "1.25rem",
                          padding: "1rem 1.25rem",
                          background: "var(--card-bg, rgba(255, 255, 255, 0.05))",
                          borderRadius: "12px",
                          border: "1px solid var(--border-color, rgba(255, 255, 255, 0.08))",
                          cursor: "pointer",
                          transition: "background 0.2s ease",
                        }}
                      >
                        <div
                          style={{
                            width: "56px",
                            height: "56px",
                            borderRadius: "50%",
                            background: "linear-gradient(135deg, #e50914, #990000)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            color: "#fff",
                            fontSize: "22px",
                            fontWeight: "bold",
                            flexShrink: 0,
                          }}
                        >
                          {ch.name ? ch.name.charAt(0).toUpperCase() : "C"}
                        </div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <h4 style={{ margin: "0 0 4px 0", fontSize: "1.15rem", fontWeight: 600, cursor: "pointer", display: "inline-block" }}>
                            {ch.name}
                          </h4>
                          <div style={{ fontSize: "0.85rem", opacity: 0.75, display: "flex", gap: "0.5rem", alignItems: "center" }}>
                            <span>{ch.handle ? (ch.handle.startsWith("@") ? ch.handle : `@${ch.handle}`) : ""}</span>
                            <span>•</span>
                            <span>Channel</span>
                          </div>
                          {ch.description && (
                            <p style={{ margin: "4px 0 0 0", fontSize: "0.85rem", opacity: 0.7, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                              {ch.description}
                            </p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {visibleVideos.length > 0 && (
                  <div className="video-grid">
                    {visibleVideos.map(
                      (video, index) => (
                        <VideoCard
                          key={
                            video.videoId
                          }
                          video={video}
                          channel={channelById.get(
                            Number(
                              video.channelId
                            )
                          )}
                          onSelect={
                            selectVideo
                          }
                          onSelectChannel={showChannel}
                          onManage={
                            setManageVideo
                          }
                          canManage={Number(channelById.get(Number(video.channelId))?.ownerId) === Number(currentUser?.id)}
                          onDelete={
                            deleteVideoFromCard
                          }
                          onSavePlaylist={startSaveToPlaylist}
                          onReport={(reportedVideo) => openReport({
                            videoId: Number(reportedVideo.videoId),
                          })}
                          onRemoveHistory={view === "History" && currentUser?.id ? removeHistoryVideo : undefined}
                          removingHistoryVideoId={removingHistoryVideoId}
                          index={index}
                        />
                      )
                    )}
                  </div>
                )}
              </>
            ) : videosLoading ? (
              <div className="empty-feed">
                <span>
                  <Icon
                    name="video"
                    size={25}
                  />
                </span>

                <h2>
                  Loading videos
                </h2>

                <p>
                  Fetching videos from
                  the backend…
                </p>
              </div>
            ) : (
              <div className="empty-feed">
                <span>
                  <Icon
                    name="video"
                    size={25}
                  />
                </span>

                <h2>
                  No videos found
                </h2>

                {view === "Channel" && activeChannel && Number(activeChannel.ownerId) !== Number(currentUser?.id) ? (
                  <p>This channel has not published any videos yet.</p>
                ) : (
                  <>
                    <p>
                      {hasOwnedChannel
                        ? "Try another category or search, or upload a video."
                        : "Create a channel to get started."}
                    </p>

                    <button
                      className="button button-primary"
                      onClick={() =>
                        currentUser
                          ? setCreateDialog(
                              hasOwnedChannel
                                ? "video"
                                : "channel"
                            )
                          : setAccountDialog(
                              "register"
                            )
                      }
                    >
                      <Icon
                        name="plus"
                        size={17}
                      />

                      {hasOwnedChannel
                        ? "Post a video"
                        : "Create a channel"}
                    </button>
                  </>
                )}
              </div>
            )}

            {backendLoaded &&
              videoPage.totalPages > 1 && (
                <nav
                  className="pagination"
                  aria-label="Video pages"
                >
                  <button
                    className="feed-filter"
                    disabled={
                      videosLoading ||
                      pageIndex === 0
                    }
                    onClick={() =>
                      setPageIndex(
                        (page) =>
                          Math.max(
                            0,
                            page - 1
                          )
                      )
                    }
                  >
                    Previous
                  </button>

                  <span>
                    Page{" "}
                    {videoPage.page + 1}{" "}
                    of{" "}
                    {
                      videoPage.totalPages
                    }{" "}
                    ·{" "}
                    {
                      videoPage.totalElements
                    }{" "}
                    videos
                  </span>

                  <button
                    className="feed-filter"
                    disabled={
                      videosLoading ||
                      pageIndex + 1 >=
                        videoPage.totalPages
                    }
                    onClick={() =>
                      setPageIndex(
                        (page) =>
                          page + 1
                      )
                    }
                  >
                    Next
                  </button>
                </nav>
              )}
          </section>
          </>}
        </main>
      </div>

      {createDialog ===
        "channel" && (
        <CreateChannelDialog
          onClose={() =>
            setCreateDialog(null)
          }
          onCreate={createChannel}
        />
      )}

      {createDialog === "video" && (
        <CreateVideoDialog
          channels={myChannels}
          categories={categories}
          onClose={() =>
            setCreateDialog(null)
          }
          onCreate={createVideo}
        />
      )}

      {liveStreamDialogOpen && (
        <LiveStudio channel={ownChannel} user={currentUser} onClose={() => setLiveStreamDialogOpen(false)} />
      )}

      {new URLSearchParams(location.search).get("live") && (
        <LiveWatchDialog id={new URLSearchParams(location.search).get("live")}
          onClose={() => navigate(location.pathname, { replace: true })} />
      )}

      {playlistEditor && (
        <PlaylistDialog
          playlist={playlistEditor.playlist}
          onClose={() => {
            setPlaylistEditor(null);
            setPendingPlaylistVideo(null);
          }}
          onSave={savePlaylist}
        />
      )}

      {playlistPickerVideo && (
        <SaveToPlaylistDialog
          video={playlistPickerVideo}
          playlists={playlists}
          onClose={() => setPlaylistPickerVideo(null)}
          onSave={saveVideoToPlaylist}
          onCreatePlaylist={() => {
            setPendingPlaylistVideo(playlistPickerVideo);
            setPlaylistPickerVideo(null);
            setPlaylistEditor({ playlist: null });
          }}
        />
      )}

      {playlistAddTarget && (
        <AddVideosToPlaylistDialog
          playlist={playlistAddTarget}
          videos={playlistAddVideos}
          loading={playlistAddLoading}
          loadError={playlistAddError}
          onClose={() => {
            setPlaylistAddTarget(null);
            setPlaylistAddVideos([]);
            setPlaylistAddError("");
          }}
          onAdd={addVideoFromPlaylist}
        />
      )}

      {manageVideo && (
        <ManageVideoDialog
          video={manageVideo}
          channels={myChannels}
          categories={categories}
          onClose={() =>
            setManageVideo(null)
          }
          onSave={saveVideo}
          onPublish={publishVideo}
          onUnpublish={
            unpublishVideo
          }
          onDelete={deleteVideo}
        />
      )}

      {selectedVideo && (
        <WatchDialog
          key={
            selectedVideo.videoId
          }
          video={selectedVideo}
          channel={channelById.get(
            Number(
              selectedVideo.channelId
            )
          )}
          recommendations={
            watchRecommendations
          }
          onSelectRecommendation={
            selectVideo
          }
          onSelectChannel={showChannel}
          onGoHome={() => {
            setSelectedVideo(null);
            setActiveChannelId(null);
            setSidebarOpen(false);
            setYouPanelOpen(false);
            setView("Home");
            navigate("/");
          }}
          onClose={() =>
            closeVideo()
          }
          onLike={toggleLike}
          onSubscribe={
            toggleSubscription
          }
          onSavePlaylist={startSaveToPlaylist}
          onManageVideo={(video) => {
            setManageVideo(video);
            closeVideo();
          }}
          onDeleteVideo={(video) => {
            if (!window.confirm(`Delete "${video.title}" permanently?`)) return;
            deleteVideo(video)
              .then(closeVideo)
              .catch((error) => setFeedError(`Could not delete video: ${error.message}`));
          }}
          onReportVideo={() => openReport({
            videoId: Number(selectedVideo.videoId),
          })}
          onReportComment={(reportedComment) => openReport({
            commentId: Number(reportedComment.id),
          })}
          onWatchProgress={updateWatchHistory}
          onNotificationsChanged={refreshNotifications}
          onSearchTag={(tagName) => {
            closeVideo();
            setSearchInput(tagName);
            setSearch(tagName);
            setPageIndex(0);
            navigate(`/search?q=${encodeURIComponent(tagName)}`);
          }}
          liked={likedVideos.includes(
            selectedVideo.videoId
          )}
          subscribed={subscriptions.includes(
            Number(
              selectedVideo.channelId
            )
          )}
          currentUser={currentUser}
        />
      )}

      {accountDialog && (
        <AccountDialog
          mode={accountDialog}
          user={currentUser}
          myChannels={myChannels}
          subscriptionChannels={
            subscriptionChannels
          }
          onClose={() => {
            setAccountDialog(null);
            if (location.pathname === "/login" || location.pathname === "/register") navigate("/");
          }}
          onLogin={loginUser}
          onSwitchLogin={() => navigate("/login")}
          onSwitchRegister={() => navigate("/register")}
          onRegister={registerUser}
          onUpdate={updateUser}
          onLogout={logoutUser}
          onCreateChannel={() => {
            setAccountDialog(null);
            setCreateDialog("channel");
          }}
          onShowSubscriptions={() => {
            setAccountDialog(null);
            chooseView(
              "Subscriptions"
            );
          }}
          onResendVerification={resendVerificationEmail}
        />
      )}

      {reportTarget && currentUser?.id && (
        <ReportDialog
          userId={currentUser.id}
          target={reportTarget}
          onClose={() => setReportTarget(null)}
          onSubmitted={handleReportSubmitted}
        />
      )}

      {subscriberDialog && (
        <SubscribersDialog
          channel={
            subscriberDialog.channel
          }
          subscribers={
            subscriberDialog.subscribers
          }
          onClose={() =>
            setSubscriberDialog(null)
          }
        />
      )}

      {editChannelDialog && (
        <EditChannelDialog
          channel={editChannelDialog}
          onClose={() => setEditChannelDialog(null)}
          onUpdate={handleUpdateChannel}
        />
      )}

      {reportsAdminOpen && isDemoAdmin && (
        <ReportsAdminPanel
          userId={Number(currentUser.id)}
          onClose={() => setReportsAdminOpen(false)}
        />
      )}

      {toast && (
        <div className="toast">
          <Icon
            name="check"
            size={17}
          />
          {toast}
        </div>
      )}
    </div>
  );
}
