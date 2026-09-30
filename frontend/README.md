# MotionVille React frontend

## Run

```bash
npm install
npm run dev
```

Vite prints the local URL, usually `http://localhost:5173`. Create a production build with `npm run build`.

## Demo behavior

This frontend is standalone and does not call or change the Spring backend. It starts with sample data shaped around the application's entities (`AppUser`, `Channel`, `Video`, `VideoAsset`, `Category`, `Tag`, `Comment`, `VideoReaction`, and `Subscription`). Creating channels, posting metadata with a direct MP4/WebM URL, playback, likes, comments, watch history, and subscriptions are stored in this browser's local storage.

Use **Create a channel** to add a channel with a name, handle, description, and optional banner URL. Use **Create → Post a video** to add a title, asset URL, optional thumbnail/description, category, visibility, and channel. No video files are uploaded; video playback depends on a direct URL that permits browser playback.

To reset the demo back to its sample data, clear this site's local storage in the browser developer tools.
