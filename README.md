# ADHD - All Day High Decibel 🎵

So I built this app because I got tired of paying for Spotify every month. Why should I pay $10/month when I can just stream music from YouTube for free?

This is my personal music player - I use it daily on my Samsung A21s. It's not perfect, but it works exactly how I want it to.

---

## What it does

- Search any song (iTunes gives me the metadata, YouTube gives me the audio)
- Play music straight from YouTube streams
- Queue up songs and have them play automatically
- Skip forward/backward 10 seconds with buttons
- Next/Previous buttons that actually work
- Lock screen controls - play/pause/skip from notification
- Repeat mode - off, repeat all, or repeat one

---

## How I made it fast

I spent a lot of time optimizing because I hate waiting for songs to buffer:

- **Reuse connections** - HTTP session stays open so I don't reconnect every time
- **Cache URLs** - Once I get a YouTube stream URL, I keep it for 48 hours
- **Prefetch** - When a song starts playing, I fetch the next 5 songs in the background
- **Tuned ExoPlayer** - Adjusted the buffer sizes so songs start faster on mobile data

---

## The tech I used

| What | Why |
|------|-----|
| Kotlin + Jetpack Compose | Modern Android UI, fast development |
| Media3 ExoPlayer | Best media player for Android, handles streaming great |
| Python FastAPI | Lightweight backend, easy to run on phone |
| iTunes Search API | Free, no API key needed, good metadata |
| YouTube (youtubei) | The actual audio source - works surprisingly well |
| Termux | Running the Python server directly on my Android phone |

---

## How I run it

I don't use any cloud hosting - my backend runs on my phone itself using Termux:

1. Install Termux from Play Store
2. `pkg install python git`
3. Clone this repo
4. `pip install -r server/requirements.txt`
5. `python server/app.py`

Done. Server runs on port 7860. My Android app connects to it and streams music.

---

## Is it legal?

For me, this is personal use only. I don't distribute anything. YouTube might not like it, but I'm not commercializing this. Use your own judgment.

---

That's pretty much it. Feel free to use it however you want.