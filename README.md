# MotionVille

MotionVille is a Java 17 / Spring Boot video-sharing application built with Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, and PostgreSQL. It follows a layered MVC design: controllers handle web requests, services enforce business rules, repositories own persistence, and templates render server-side views.

## First release

- Email/password registration and session-based sign-in with BCrypt password hashing
- One creator channel per account
- Creator channel and video metadata editing with owner-only soft deletion
- Direct-to-R2 upload of MP4 or WebM videos up to 500 MB
- Public/private video visibility, byte-range video playback, and view counts
- Lazy video-frame thumbnail previews on feed and channel cards, with a branded fallback
- Case-insensitive video search across titles and descriptions, with pagination, sorting, and category/channel filters
- Creator subscription feed and a trending list ranked by local video views
- Comments, video likes, and channel subscriptions
- Soft deletion of videos and comments at the service/domain layer
- Private visibility metadata (authorization is not implemented in the no-sign-in development setup)
- Hibernate-managed local schema and PostgreSQL-ready production configuration

This release intentionally starts with the reliable upload-to-playback path. It does not claim to implement every feature in the full project proposal.

## Run locally

Requirements: Java 17+ and network access the first time Maven resolves dependencies.

The default local profile uses a file-backed H2 database at `./data/motionville`. Set your R2 credentials and enable the development channel bootstrap before starting the backend:

```bash
DEV_CHANNEL_BOOTSTRAP_ENABLED=true \
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com \
R2_BUCKET=<bucket-name> \
R2_ACCESS_KEY=<access-key-id> \
R2_SECRET_KEY=<secret-access-key> \
./mvnw spring-boot:run
```

Open the frontend at `http://localhost:5173`, create a channel, then upload a supported video. Browsing demo data remains local to the browser.

Configuration can be overridden with environment variables. Use the shared **MotionVille Local** run configuration or run `./mvnw spring-boot:run`.

| Variable | Default / purpose |
|---|---|
| `DATABASE_URL` | `jdbc:h2:file:./data/motionville;DB_CLOSE_ON_EXIT=FALSE` |
| `DATABASE_USERNAME` | `sa` |
| `DATABASE_PASSWORD` | Empty for local H2; set a secret for PostgreSQL |
| `R2_ENDPOINT` | Cloudflare R2 S3 API endpoint |
| `R2_BUCKET` | R2 bucket name |
| `R2_ACCESS_KEY` | R2 access key ID |
| `R2_SECRET_KEY` | R2 secret access key |
| `FRONTEND_ORIGIN` | `http://localhost:5173` |
| `DDL_AUTO` | `update` locally; production uses `validate` and requires a pre-created schema |

Video uploads require the R2 settings above. The browser receives a 15-minute presigned PUT URL, uploads directly to R2, and notifies the backend to verify object size and content type. The backend reports `READY` after generating and storing playable assets. Playback URLs are freshly signed for one hour. Configure bucket CORS to allow your frontend origin, `PUT`, `GET`, and `HEAD`; allow the `Content-Type` and `Range` headers, and expose `Content-Length` and `Content-Range` for browser playback.

For a no-sign-in local demo, start the backend with `DEV_CHANNEL_BOOTSTRAP_ENABLED=true`. The **Create channel** UI then creates a backend-backed development channel, ready for upload. Video upload and playback endpoints are unauthenticated; do not expose them to the public internet or use private visibility as access control until authentication and authorization are implemented. The bootstrap defaults off and should remain off in deployed environments.

For PostgreSQL, set the database URL and credentials, then run with the `prod` profile:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:postgresql://localhost:5432/motionville \
DATABASE_USERNAME=motionville \
DATABASE_PASSWORD='set-through-your-secret-manager' \
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com \
R2_BUCKET=<bucket-name> \
R2_ACCESS_KEY=<access-key-id> \
R2_SECRET_KEY=<secret-access-key> \
DEV_CHANNEL_BOOTSTRAP_ENABLED=false \
./mvnw spring-boot:run
```

Do not commit database passwords or production secrets. R2 credentials must be supplied through environment variables or a secret manager.

## Architecture

### Video discovery API

`GET /api/videos` returns a page object containing `page`, `size`,
`totalElements`, `totalPages`, and `content`. Search checks titles and
descriptions; optional category and channel IDs filter the results.
Use `publicOnly=true` for feed queries to filter to published, ready public
videos before pagination, so private or processing videos do not consume page
slots. The frontend uses this for public feed views and requests the broader
result set in channel-management views.
`GET /api/categories` returns the available category IDs and names used by
video creation and category filtering. Categories also support CRUD through
`POST /api/categories`, `GET /api/categories/{id}`, `PUT /api/categories/{id}`,
and `DELETE /api/categories/{id}`. Create and update requests accept `name`
and a lowercase hyphenated `slug`; both are unique without regard to case.
Deleting a category assigned to videos returns `409 Conflict`.
For example, create a category with `{"name":"Technology","slug":"technology"}`.

### Tags and video assets

Tags can be managed through `POST`, `GET`, `PUT`, and `DELETE /api/tags`
(single-tag routes use `/api/tags/{tagId}`). Requests use `{"name":"Java"}`.
A tag name is unique without regard to case; deleting a tag that is assigned
to a video returns `409 Conflict`.

Associate a tag with a video using
`POST /api/videos/{videoId}/tags/{tagId}`; remove it with `DELETE` on that
route, and list a video's tags with `GET /api/videos/{videoId}/tags`.
`GET /api/tags/{tagId}/videos` lists the videos associated with a tag.
Adding a duplicate association returns `409 Conflict`.

Video-asset endpoints are `POST /api/videos/{videoId}/assets`,
`GET /api/videos/{videoId}/assets`, and
`DELETE /api/videos/{videoId}/assets/{assetId}`. The create request contains
`assetUrl`, `quality`, `mimeType`, and `sizeBytes`. These endpoints persist
asset metadata and the external URL only; media bytes remain in object storage.
Removing an asset record does not delete its external object. Variants are
unique per video, quality, and MIME type.

```text
GET /api/videos?search=java
GET /api/videos?page=0&size=20
GET /api/videos?sort=createdAt,desc
GET /api/videos?categoryId=1
GET /api/videos?channelId=2
GET /api/videos?publicOnly=true&page=0&size=20
```

The default page is `0`, the default size is `20`, and page sizes are limited
to `1..100`. Sort uses `field,asc|desc`; supported fields are `createdAt`,
`updatedAt`, `publishedAt`, `title`, `durationSeconds`, and `id`. Results use
video ID as a stable tie-breaker where needed.

Feature packages keep MVC layers close to their domain:

```text
account/       account entity, role, repository, authentication service and MVC pages
channel/       channel entity, repository, service and channel controller
video/         video entity, repository, upload/storage service and MVC/media controllers
engagement/    comments, likes, subscriptions and engagement service
config/        Spring Security, MVC error handling
resources/
  templates/   Thymeleaf server-rendered pages
  static/      CSS
```

Controllers accept validated form input and delegate. Services enforce ownership, visibility, upload, and engagement rules. JPA entities are not returned from JSON APIs; templates only receive entities loaded with the relations they render. Constructor injection is used throughout.

## Delivery roadmap

1. **Foundation and first release (implemented):** account/channel creation, upload, playback, discovery/search, comments, likes, and subscriptions.
2. **Processing and streaming:** background job state and retries, FFmpeg thumbnail/transcoding, HLS renditions, and resumable/range-friendly media delivery.
3. **Creator and discovery polish:** watch history, playlists, trending, notifications, and a creator dashboard with metrics backed by recorded view events.
4. **Production hardening:** object storage/CDN, content validation and moderation, rate limits, observability, deployment automation, and integration/load testing.

Advanced ML recommendations, DASH, live streaming, payments, and direct messaging remain out of scope as specified in the project plan.

## Tests

```bash
./mvnw test
```

Tests use an isolated in-memory H2 database and cover view rendering, password hashing, and the account/channel/upload/engagement flow.


### Video conversion (common input formats)

New uploads accept common containers including MP4/M4V/MOV, WebM/MKV, AVI,
WMV/ASF, FLV, MPEG/MPG, TS/MTS/M2TS, 3GP and Ogg video. Actual codec support
comes from the installed FFmpeg build; corrupt, encrypted or unsupported files
fail processing. MIME types/extensions alone do not prove a file is playable.

Install FFmpeg (including ffprobe) on the backend host. If IntelliJ cannot find
Homebrew executables, add `FFMPEG_PATH=/opt/homebrew/bin/ffmpeg` and
`FFPROBE_PATH=/opt/homebrew/bin/ffprobe` to the motionVille run configuration.
Restart the backend after changing code or environment variables.

The browser uploads the original to R2, then POSTs `/api/videos/{id}/complete`.
HTTP 202 queues conversion. `/api/videos/{id}/status` returns `PROCESSING`,
`UPLOADED` (the existing enum's ready state), or `FAILED`. The player polls this
endpoint. It requests `/playback` only when ready, receiving a signed URL for
H.264/AAC MP4. The original and converted assets share the same video ID.
A full-resolution playback asset is always generated, plus 360p/480p/720p/1080p
variants only where the source height permits. Existing uploads are not migrated.

This development worker uses a bounded in-memory queue (one conversion at a time).
Keep the backend running while processing. A restart loses queued jobs; affected
videos currently need re-uploading. A production deployment should use a durable
job queue with retry/recovery. Originals stay in R2; temporary local files are cleaned
up and partial converted objects are removed on failure.

Conversion tests require ffmpeg and ffprobe on PATH:
`mvn -Dtest=VideoProcessingTest test`. They generate small synthetic MKV/AVI files
and mock R2/database access, so they do not upload anything to your bucket.
