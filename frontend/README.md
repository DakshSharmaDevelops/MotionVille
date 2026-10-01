# MotionVille React frontend

## Run

```bash
npm install
npm run dev
```

Vite prints the local URL, usually `http://localhost:5173`. Create a production build with `npm run build`.

## Video upload and playback

The React app calls the Spring API at `http://localhost:8080/api` by default. Override the API root with `VITE_API_BASE_URL` when running or building the frontend. The feed and channel list load from the backend on startup; sample videos remain as demo entries. In **Create → Post a video**, choose a video file (up to 500 MB); the browser requests a presigned URL, uploads directly to R2, then asks the backend to verify and process the asset. If no thumbnail URL is entered, the frontend captures a frame (or creates a branded title card for browser-undecodable formats) and uploads the JPEG to R2. Playback and thumbnail URLs are refreshed when the video is opened/displayed.

The feed requests paginated results from `GET /api/videos` and supports title/description search, API-backed category/channel filtering, sorting, and previous/next page navigation. Public feed views send `publicOnly=true`, so private, unpublished, or not-ready videos are filtered by the backend before pagination and do not occupy page slots. Channel-management views omit this filter so creators can still see their unpublished videos. Categories load from `GET /api/categories` and are assigned by ID on upload/edit. The video API also supports `categoryId`, `channelId`, `page`, `size`, and `sort=field,asc|desc`; page size is capped at 100. Each saved video card shows explicit **Edit** and **Delete** actions. Edit updates metadata (`PUT /api/videos/{id}`) and visibility, then hides that card from the current feed session; delete requires confirmation and calls `DELETE /api/videos/{id}`.

Opening a video uses a full-width responsive player view. Other public, ready videos from the loaded feed are shown beneath it as recommendations and can be opened without leaving the player view.

Before uploading, configure the backend R2 environment variables and start the backend with `DEV_CHANNEL_BOOTSTRAP_ENABLED=true`. Use **Create channel** in the app to create a backend-backed development channel. Configure R2 bucket CORS to allow the frontend origin to send `PUT`, `GET`, and `HEAD` requests with `Content-Type` and `Range` headers.

This sign-in-free API is for local development only. Do not expose it publicly or rely on its private visibility settings for access control until authentication/authorization is added. Likes, comments, watch history, subscriptions, and sample demo entries remain stored in browser local storage; clear this site's local storage to reset them.
