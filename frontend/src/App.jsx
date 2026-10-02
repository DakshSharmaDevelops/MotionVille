import { useEffect, useMemo, useState } from "react";
import {
  apiRequest,
  buildVideoQuery,
  createVideoThumbnail,
  mapApiCategory,
  mapApiChannel,
  mapApiVideo,
  uploadFile,
} from "./api/videoApi.js";

import { Avatar, Icon, VideoCard } from "./components/ui.jsx";

import {
  AccountDialog,
  SubscribersDialog,
} from "./components/account.jsx";

import {
  CreateChannelDialog,
  CreateVideoDialog,
  ManageVideoDialog,
  WatchDialog,
} from "./components/dialogs.jsx";

function loadStoredIds(key) {
  try {
    const saved = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

export default function App() {
  const [videos, setVideos] = useState([]);
  const [channels, setChannels] = useState([]);
  const [categories, setCategories] = useState([]);

  const [selectedVideo, setSelectedVideo] = useState(null);
  const [manageVideo, setManageVideo] = useState(null);

  const [backendLoaded, setBackendLoaded] = useState(false);

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

  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [view, setView] = useState("Home");

  const [createDialog, setCreateDialog] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [toast, setToast] = useState("");

  const [likedVideos, setLikedVideos] = useState(() =>
    loadStoredIds("motionville.likes")
  );

  const [history, setHistory] = useState(() =>
    loadStoredIds("motionville.history")
  );

  const [subscriptions, setSubscriptions] = useState(() =>
    loadStoredIds("motionville.subscriptions")
  );

  const [activeChannelId, setActiveChannelId] = useState(null);

  const [currentUser, setCurrentUser] = useState(null);
  const [accountDialog, setAccountDialog] = useState(null);

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
    const storedId = localStorage.getItem(
      "motionville.currentUserId"
    );

    if (!storedId) return;

    apiRequest(`/users/${storedId}`)
      .then((user) => {
        setCurrentUser(user);
      })
      .catch(() => {
        localStorage.removeItem("motionville.currentUserId");
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
      items = items.filter((video) =>
        history.includes(video.videoId)
      );
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

    if (view === "History") {
      items = [...items].sort(
        (a, b) =>
          history.indexOf(a.videoId) -
          history.indexOf(b.videoId)
      );
    }

    return items;
  }, [
    activeChannelId,
    history,
    likedVideos,
    myChannels,
    subscriptions,
    videos,
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
      setAccountDialog("register");
      setToast("Create a profile first.");
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
  }

  async function registerUser(form) {
    const user = await apiRequest(
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

    localStorage.setItem(
      "motionville.currentUserId",
      String(user.id)
    );

    setCurrentUser(user);
    setAccountDialog("profile");
    setToast(
      "Profile created successfully."
    );
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

  function logoutUser() {
    localStorage.removeItem(
      "motionville.currentUserId"
    );

    setCurrentUser(null);
    setMyChannels([]);
    setSubscriptionChannels([]);
    setSubscriptions([]);
    setAccountDialog(null);

    setToast("Signed out.");
  }

  async function subscribeToChannel(channelId) {
    if (!currentUser?.id) {
      setAccountDialog("register");
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
    } else {
      setActiveChannelId(null);
      setView("Home");
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

    setHistory((current) => [
      video.videoId,
      ...current.filter(
        (id) =>
          id !== video.videoId
      ),
    ]);
  }

  function chooseView(nextView) {
    setView(nextView);
    setActiveChannelId(null);
    setActiveCategory("All");
    setPageIndex(0);
    setSidebarOpen(false);
  }

  function showChannel(channel) {
    setView("Channel");
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

  const hasBackendChannels =
    channels.length > 0;

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
            aria-label="Toggle menu"
          >
            <Icon name="menu" />
          </button>

          <a
            className="brand"
            href="#"
            onClick={(event) => {
              event.preventDefault();
              chooseView("Home");
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
          }}
        >
          <div className="search-input-wrap">
            <Icon
              name="search"
              size={19}
            />

            <input
              value={search}
              onChange={(event) => {
                setSearch(
                  event.target.value
                );
                setPageIndex(0);
              }}
              placeholder="Search videos, creators, and more"
              aria-label="Search"
            />

            {search && (
              <button
                type="button"
                className="search-clear"
                onClick={() => {
                  setSearch("");
                  setPageIndex(0);
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
          <button
            className="create-button"
            onClick={() => {
              if (!currentUser) {
                setAccountDialog(
                  "register"
                );
                setToast(
                  "Create a profile first."
                );
                return;
              }

              setCreateDialog(
                hasBackendChannels
                  ? "video"
                  : "channel"
              );
            }}
          >
            <Icon
              name="plus"
              size={18}
            />
            <span>Create</span>
          </button>

          <button
            className="icon-button notification-button"
            aria-label="Notifications"
          >
            <Icon name="bell" />
          </button>

          <button
            type="button"
            aria-label={
              currentUser
                ? "Open profile"
                : "Create profile"
            }
            onClick={() =>
              setAccountDialog(
                currentUser
                  ? "profile"
                  : "register"
              )
            }
            style={{
              border: 0,
              background:
                "transparent",
              padding: 0,
              cursor: "pointer",
              borderRadius:
                "50%",
            }}
          >
            <Avatar
              src={
                currentUser?.avatarUrl
              }
              name={
                currentUser?.displayName ||
                currentUser?.username ||
                "M"
              }
              size="small"
            />
          </button>
        </div>
      </header>

      {sidebarOpen && (
        <button
          className="mobile-scrim"
          aria-label="Close navigation"
          onClick={() =>
            setSidebarOpen(false)
          }
        />
      )}

      <div className="app-body">
        <aside
          className={`sidebar ${
            sidebarOpen
              ? "sidebar-open"
              : ""
          }`}
        >
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

          <nav
            className="nav-group"
            aria-label="Your library"
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
          </nav>

          <div className="sidebar-rule" />

          <section className="sidebar-channels">
            <div className="sidebar-section-heading">
              <span>Channels</span>

              <button
                className="small-add"
                onClick={() =>
                  currentUser
                    ? setCreateDialog(
                        "channel"
                      )
                    : setAccountDialog(
                        "register"
                      )
                }
                aria-label="Create channel"
              >
                <Icon
                  name="plus"
                  size={17}
                />
              </button>
            </div>

            {channels
              .slice(0, 5)
              .map((channel) => (
                <button
                  className="nav-item channel-nav-item"
                  key={
                    channel.channelId
                  }
                  onClick={() =>
                    showChannel(
                      channel
                    )
                  }
                >
                  <Avatar
                    src={
                      channel.avatarUrl
                    }
                    name={
                      channel.name
                    }
                    size="tiny"
                  />
                  <span>
                    {channel.name}
                  </span>
                </button>
              ))}

            <button
              className="nav-item add-channel-nav"
              onClick={() =>
                currentUser
                  ? setCreateDialog(
                      "channel"
                    )
                  : setAccountDialog(
                      "register"
                    )
              }
            >
              <span className="add-channel-icon">
                <Icon
                  name="plus"
                  size={16}
                />
              </span>

              <span>
                Create a channel
              </span>
            </button>
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
        </aside>

        <main className="main-content">
          {view === "Home" ||
          view === "Explore" ? (
            <div className="welcome-strip">
              <div className="welcome-copy">
                <span className="welcome-kicker">
                  <Icon
                    name="sparkle"
                    size={14}
                  />
                  YOUR SPACE TO WATCH &
                  SHARE
                </span>

                <h1>
                  Find your next{" "}
                  <em>favorite.</em>
                </h1>

                <p>
                  Stories, ideas, and
                  little moments from
                  creators worth following.
                </p>
              </div>

              <div
                className="welcome-art"
                aria-hidden="true"
              >
                <div className="art-sun" />

                <div className="art-arch">
                  <div className="art-land art-land-one" />
                  <div className="art-land art-land-two" />
                  <div className="art-water" />
                </div>

                <span className="art-spark art-spark-one">
                  ✳
                </span>

                <span className="art-spark art-spark-two">
                  ✦
                </span>
              </div>
            </div>
          ) : view ===
              "Channel" &&
            activeChannel ? (
            <section className="channel-banner">
              {activeChannel.bannerUrl && (
                <img
                  src={
                    activeChannel.bannerUrl
                  }
                  alt=""
                />
              )}

              <div className="channel-banner-content">
                <Avatar
                  src={
                    activeChannel.avatarUrl
                  }
                  name={
                    activeChannel.name
                  }
                  size="banner"
                />

                <div>
                  <h1>
                    {activeChannel.name}
                  </h1>

                  <p>
                    {activeChannel.handle}
                  </p>

                  <span>
                    {
                      activeChannel.description
                    }
                  </span>

                  <div
                    style={{
                      marginTop: 8,
                      display: "flex",
                      gap: 8,
                      alignItems:
                        "center",
                      flexWrap:
                        "wrap",
                    }}
                  >
                    <span>
                      {
                        subscriberCounts[
                          activeChannel
                            .channelId
                        ] || 0
                      }{" "}
                      subscriber
                      {Number(
                        subscriberCounts[
                          activeChannel
                            .channelId
                        ] || 0
                      ) === 1
                        ? ""
                        : "s"}
                    </span>

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
                  </div>
                </div>
              </div>
            </section>
          ) : null}

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
                    : view}
                </p>

                <h2>{feedTitle}</h2>
              </div>

              <div className="feed-controls">
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

            {!backendLoaded &&
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
            ) : !backendLoaded &&
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
            ) : visibleVideos.length ? (
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
                      onManage={
                        setManageVideo
                      }
                      onDelete={
                        deleteVideoFromCard
                      }
                      index={index}
                    />
                  )
                )}
              </div>
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

                <p>
                  {hasBackendChannels
                    ? "Try another category or search, or upload a video."
                    : "Create a channel to get started."}
                </p>

                <button
                  className="button button-primary"
                  onClick={() =>
                    currentUser
                      ? setCreateDialog(
                          hasBackendChannels
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

                  {hasBackendChannels
                    ? "Post a video"
                    : "Create a channel"}
                </button>
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
          channels={myChannels.length
            ? myChannels
            : channels}
          categories={categories}
          onClose={() =>
            setCreateDialog(null)
          }
          onCreate={createVideo}
        />
      )}

      {manageVideo && (
        <ManageVideoDialog
          video={manageVideo}
          channels={
            myChannels.length
              ? myChannels
              : channels
          }
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
          onClose={() =>
            setSelectedVideo(null)
          }
          onLike={toggleLike}
          onSubscribe={
            toggleSubscription
          }
          liked={likedVideos.includes(
            selectedVideo.videoId
          )}
          subscribed={subscriptions.includes(
            Number(
              selectedVideo.channelId
            )
          )}
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
          onClose={() =>
            setAccountDialog(null)
          }
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