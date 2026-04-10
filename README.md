# ADHD - All Day High Decibel 🎵

A free, personal-use Android music streaming app that uses YouTube as its audio source.

---

## Why This App?

Built this because:
- Spotify/Premium apps cost money
- Want full control over my music without subscriptions
- Can run everything from my phone - no cloud bills
- Learned a lot building it

**For personal use only** - Don't redistribute copyrighted content.

---

## Features

- 🔍 Smart search via iTunes API
- ▶️ Stream audio directly from YouTube
- 🔄 Auto-queue with next song auto-advance
- ⏮️⏭️ Previous/Next buttons
- ⏪⏩ Seek forward/backward (10s)
- 🔔 Lock screen & notification controls
- 📱 Queue management
- 🔁 Repeat modes: Off / All / One

---

## Performance

Built with speed in mind:
- **Session reuse** - Same HTTP connection for multiple requests
- **URL caching** - Stream URLs cached for instant replay
- **Smart prefetching** - Pre-fetches next 5 songs in queue
- **Optimized ExoPlayer** - Tuned buffers for faster playback start

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Mobile UI | Kotlin + Jetpack Compose |
| Media Player | Media3 ExoPlayer |
| Backend | Python FastAPI |
| Search | iTunes Search API |
| Audio Source | YouTube (youtubei API) |
| Hosting | Termux on Android |

---

## Deployment

Backend runs directly on Android phone via Termux:
- Clone repo in Termux
- `pip install -r requirements.txt`
- `python app.py`
- Server starts on port 7860

Android app connects to this local backend for streaming.