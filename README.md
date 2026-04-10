# ADHD - All Day High Decibel 🎵

Experimental Android music streaming application with Python backend. Built as a proof-of-concept to explore YouTube as an alternative audio CDN, bypassing traditional music streaming services.

---

## Core Functionality

- **Search**: iTunes Search API for metadata (artist, title, artwork), YouTube for stream URL resolution
- **Playback**: Media3 ExoPlayer with adaptive streaming
- **Queue Management**: In-memory playlist with auto-advance on track completion
- **Media Controls**: Previous/Next media items, seek forward/backward (10s intervals)
- **Background Playback**: Foreground service with MediaStyle notification
- **Repeat Modes**: OFF / ALL / ONE

---

## Performance Optimizations

Implemented various optimizations to minimize latency:

| Optimization | Implementation |
|--------------|----------------|
| HTTP Keep-Alive | `requests.Session()` with persistent connections |
| Stream URL Cache | LRU cache with 48-hour TTL in `url_cache.json` |
| Prefetching | Async fetch next 5 queue items on media transition |
| Buffer Tuning | ExoPlayer LoadControl: 5s min, 15s max buffer |
| Gzip Compression | Starlette GZipMiddleware for responses >500B |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android Client                        │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Compose UI  │→ │ ViewModel    │→ │ ExoPlayer     │  │
│  │ (Kotlin)    │  │ (StateFlow)  │  │ (Media3)      │  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                    localhost:7860
                           │
┌─────────────────────────────────────────────────────────┐
│                   Python Backend (FastAPI)              │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ iTunes API  │  │ youtubei API │  │ LRU Cache     │  │
│  │ (metadata)  │  │ (streaming)  │  │ (URL + search)│  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Client | Kotlin 1.8, Jetpack Compose 1.5, Hilt DI |
| Media | Media3 ExoPlayer 1.2, MediaSession |
| Backend | Python 3.11, FastAPI 0.118, Uvicorn |
| Search | iTunes Search API (no auth required) |
| Streaming | YouTube Internal API (youtubei) |
| Host | Termux on Android (Samsung A21s) |

---

## Deployment

**Runtime Environment**: Android device via Termux

```bash
# Install dependencies
pip install -r server/requirements.txt

# Start FastAPI server
python server/app.py
# → Uvicorn running on http://0.0.0.0:7860
```

Backend exposes REST endpoints:
- `/api/mobile/search?q=<query>` - iTunes search
- `/api/mobile/play?id=<>&artist=<>&title=<>` - Get stream URL
- `/api/mobile/prefetch` - Background URL resolution
- `/api/mobile/health` - Server health check

---

## Notes

- Personal use only - no commercial distribution
- YouTube ToS may prohibit streaming outside their client
- Backend can be deployed to any Python-hosting service (Render, Railway, etc.)
- Uses Android `adb reverse` for localhost development

---

Built with Kotlin + Python. No subscriptions required.