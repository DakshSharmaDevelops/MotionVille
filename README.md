# MotionVille

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

MotionVille is a high-performance, full-stack video-sharing and live streaming platform built with Java 17, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Cloudflare R2, MediaMTX, and React (Vite).

---

## 🌐 Live Deployment

The application is deployed and live in production on AWS EC2 with full HTTPS/TLS:

- **Live Application URL**: **[https://52-66-239-42.sslip.io](https://52-66-239-42.sslip.io)**
- **SSL/TLS**: Automated Let's Encrypt certificates with HTTP-to-HTTPS redirection
- **Object Storage**: Cloudflare R2 (high-speed S3-compatible storage with zero egress fees)
- **Streaming Engine**: MediaMTX with WebRTC (WHIP/WHEP) and Low-Latency HLS (LL-HLS)

---

## Key Features

### 🎬 Video Upload & Playback
- **Direct-to-R2 Streaming Uploads**: Zero-CORS same-origin upload proxy (`/r2-upload/`) streams files directly from the browser to Cloudflare R2 with no disk buffering on the server.
- **Ultra-Fast Processing Pipeline**:
  - **Smart Format Detection & Stream Copy**: MP4 files with H.264 video and AAC/MP3 audio bypass expensive software re-encoding entirely. Fast stream copy (`-c copy -movflags +faststart`) finishes in **~0.04 seconds**.
  - **Immediate `READY` Status**: Videos become playable and live for viewers **within seconds** as soon as the `playback.mp4` asset is uploaded, without waiting for multi-resolution or HLS generation.
  - **4K+ Resource Protection**: Automatically downscales $2160 \times 3840$ (portrait) or $3840 \times 2160$ (landscape) inputs to 1080p, reducing encoding pixel load by 4x and preventing memory exhaustion and swap thrashing on cloud instances.
  - **Intermediate Playback Normalization**: Multi-resolution variants (360p, 720p, 1080p) and HLS playlists are generated from the lightweight 8-bit H.264 `playback.mp4` file rather than repeatedly decoding heavy original containers (such as 10-bit HEVC).
  - **Concurrent Bounded HLS Uploads**: HLS `.ts` segments and `.m3u8` playlists upload concurrently using a bounded 4-thread pool, cutting HLS upload time from ~25s down to ~3s.
  - **Instant Browser Playback**: Player configured with `preload="auto"` and `playsInline` for sub-200ms click-to-play startup.

### 🔴 WebRTC Live Broadcasting & LL-HLS
- **In-Browser Studio**: Broadcast camera, microphone, or system screen directly from the browser over WebRTC (WHIP protocol) — no external software or OBS required.
- **Fast Startup Latency**: Sub-second connection establishment using an 800ms ICE gathering debounce and Google STUN NAT traversal.
- **Graceful Media Fallback**: Automatic microphone fallback allows video-only screen sharing and streaming even without an audio input device.
- **Low-Latency HLS (LL-HLS)**: Viewers receive 200ms fMP4 parts with adaptive playback latency (1s–2.5s).

### 🔐 Authentication & Security
- **JWT & Refresh Tokens**: Secure stateless authentication with short-lived access tokens and database-backed refresh tokens with automated expiration cleanup.
- **Dual Verification Flow**: User registration sends both a 6-digit OTP code and a secure, time-bounded verification link via Gmail SMTP.
- **Password Reset**: Secure forgot-password flow with one-time BCrypt-validated tokens.
- **Granular Ownership Control**: Spring Security SpEL authorization ensures only channel owners can update or soft-delete their videos and comments.
- **CORS & CSRF Hardening**: Strict origin whitelisting, HTTP-only SameSite cookies, and security headers.

### 💬 Community & Engagement
- **Channels**: Personalized creator channels, custom avatars, banners, and handle URLs.
- **Engagements**: Comments, nested comment reactions, video likes/dislikes, and channel subscriptions.
- **Watch History & Playlists**: View progress persistence and custom playlist curation.
- **Content Moderation**: User report submissions with administrative notifications.

---

## Architecture Overview

```
                      ┌──────────────────────────────────────┐
                      │          React Frontend SPA          │
                      │       (Vite / Responsive UI)         │
                      └──────────────────┬───────────────────┘
                                         │
                   HTTPS / Port 443      │
                                         ▼
                      ┌──────────────────────────────────────┐
                      │             Nginx Reverse            │
                      │               Proxy / TLS            │
                      └────┬─────────────┬──────────────┬────┘
                           │             │              │
             /api/*        │    /live-*  │   /r2-upload │
                           ▼             ▼              ▼
       ┌─────────────────────┐   ┌────────────┐   ┌───────────────┐
       │     Spring Boot     │   │  MediaMTX  │   │ Cloudflare R2 │
       │     Application     │   │   Server   │   │ Object Storage│
       │     (Port 8080)     │   │ (Port 8889)│   └───────────────┘
       └──────────┬──────────┘   └─────┬──────┘
                  │                    │
                  ▼                    ▼
       ┌─────────────────────┐   ┌────────────┐
       │ PostgreSQL Database │   │ WebRTC ICE │
       │     (Port 5432)     │   │ (Port 8189)│
       └─────────────────────┘   └────────────┘
```

---

## Configuration & Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:h2:file:./data/motionville` | Database connection URL (PostgreSQL in production) |
| `DATABASE_USERNAME` | `sa` | Database username |
| `DATABASE_PASSWORD` | `""` | Database password |
| `R2_ENDPOINT` | — | Cloudflare R2 S3-compatible API endpoint |
| `R2_BUCKET` | — | Cloudflare R2 storage bucket name |
| `R2_ACCESS_KEY` | — | Cloudflare R2 Access Key ID |
| `R2_SECRET_KEY` | — | Cloudflare R2 Secret Access Key |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Allowed CORS origin |
| `FRONTEND_PUBLIC_URL` | `https://52-66-239-42.sslip.io` | Public URL for email links and live sharing |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `SPRING_MAIL_PORT` | `587` | SMTP port |
| `SPRING_MAIL_USERNAME` | — | SMTP username / sender email |
| `SPRING_MAIL_PASSWORD` | — | SMTP password or 16-character Google App Password |
| `MOTIONVILLE_LIVE_API_URL`| `http://127.0.0.1:9997` | Internal MediaMTX API endpoint |

---

## Running Locally

### Prerequisites
- **Java 17+**
- **Node.js 18+** & npm
- **FFmpeg & FFprobe** installed and available on your system `PATH`

### 1. Start the Backend
```bash
./mvnw clean spring-boot:run
```
The backend initializes the local H2 database at `./data/motionville` and automatically starts an embedded MediaMTX process using `streaming/mediamtx.yml`.

### 2. Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

---

## Running Tests

Execute the full suite of automated unit, integration, and security tests:

```bash
# Run all backend tests
./mvnw test

# Run video processing performance tests
./mvnw test -Dtest=VideoProcessingTest

# Run security and authentication tests
./mvnw test -Dtest=Developer3SecurityTest
```

---

## Production Deployment

Production builds are automated via the included Maven and Vite pipelines:

```bash
# Frontend build
cd frontend && npm run build

# Backend production packaging
./mvnw clean package -DskipTests

# Start with production environment
java -Xms128m -Xmx384m -jar target/MotionVille-0.0.1-SNAPSHOT.jar
```

Systemd service configuration and Nginx virtual host configurations are available under the server deployment directories.
