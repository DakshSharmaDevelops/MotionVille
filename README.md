# MotionVille

MotionVille is a Java 17 / Spring Boot video-sharing application built with Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, and PostgreSQL. It follows a layered MVC design: controllers handle web requests, services enforce business rules, repositories own persistence, and templates render server-side views.

## First release

- Email/password registration and session-based sign-in with BCrypt password hashing
- One creator channel per account
- Creator channel and video metadata editing with owner-only soft deletion
- Local upload of MP4, WebM, or MOV videos up to 500 MB
- Public/private video visibility, byte-range video playback, and view counts
- Lazy video-frame thumbnail previews on feed and channel cards, with a branded fallback
- Public video search across titles, descriptions, and tags with pagination
- Category filtering
- Creator subscription feed and a trending list ranked by local video views
- Comments, video likes, and channel subscriptions
- Soft deletion of videos and comments at the service/domain layer
- Private video playback restricted to the channel owner
- Hibernate-managed local schema and PostgreSQL-ready production configuration

This release intentionally starts with the reliable upload-to-playback path. It does not claim to implement every feature in the full project proposal.

## Run locally

Requirements: Java 17+ and network access the first time Maven resolves dependencies.

```bash
./mvnw spring-boot:run
```

The default local profile uses a file-backed H2 database at `./data/motionville` and stores video files outside the web root at `./data/media`. Open `http://localhost:8080`, create an account, create a channel, and upload a supported video. Browsing, search, playback, and trending are local and require no external API keys.

Configuration can be overridden with environment variables. Use the shared **MotionVille Local** run configuration or run `./mvnw spring-boot:run`.

| Variable | Default / purpose |
|---|---|
| `DATABASE_URL` | `jdbc:h2:file:./data/motionville;DB_CLOSE_ON_EXIT=FALSE` |
| `DATABASE_USERNAME` | `sa` |
| `DATABASE_PASSWORD` | Empty for local H2; set a secret for PostgreSQL |
| `MEDIA_DIRECTORY` | `./data/media` |
| `DDL_AUTO` | `update` locally; production uses `validate` and requires a pre-created schema |

For PostgreSQL, set the database URL and credentials, then run with the `prod` profile:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:postgresql://localhost:5432/motionville \
DATABASE_USERNAME=motionville \
DATABASE_PASSWORD='set-through-your-secret-manager' \
MEDIA_DIRECTORY=/var/lib/motionville/media \
./mvnw spring-boot:run
```

Do not commit database passwords or production secrets. The filesystem backend is intended for local/demo deployments; use object storage and a CDN before running multiple application instances.

## Architecture

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
