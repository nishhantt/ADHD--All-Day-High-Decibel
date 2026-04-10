# ADHD - All Day High Decibel 🎵

A free Android music streaming app that uses YouTube as its music source. Think of it as Spotify, but free and running straight from your phone!

## ✨ Features

- 🔍 **Smart Search** - Finds any song using iTunes metadata + YouTube streaming
- ▶️ **Instant Play** - Stream music directly from YouTube
- 🔄 **Auto-Queue** - Plays the next song automatically when current ends
- ⏮️⏭️ **Full Controls** - Next/Previous, Seek forward/backward buttons
- 🔔 **Lock Screen Controls** - Full playback controls in notification
- 📱 **Queue System** - Add songs to queue, reorder as you like
- 🔁 **Repeat Modes** - Off / Repeat All / Repeat One

## 🎯 How It Works

```
┌─────────────┐      ┌─────────────────┐      ┌──────────────┐
│  Android    │ ───> │  Your Phone     │ ───> │   YouTube    │
│    App      │      │  (Backend)      │      │   (Audio)    │
└─────────────┘      └─────────────────┘      └──────────────┘
                            ▲
                            │
                     ┌──────┴──────┐
                     │   iTunes    │
                     │  (Search)   │
                     └─────────────┘
```

**Simple Flow:**
1. You search for a song
2. App hits iTunes API to get song details (title, artist, artwork)
3. Backend searches YouTube for the audio and returns the stream URL
4. ExoPlayer plays the audio directly from YouTube

---

## 🚀 Setup Guide

### Part 1: The Backend (Python Server)

You need to run the backend on your phone. Here's how:

**Step 1: Install Termux on Android**
```
→ Play Store → Search "Termux" → Install
```

**Step 2: Clone and Setup**
```bash
# Open Termux and run:
pkg update && pkg install python git

# Clone the project
cd ~/ && git clone https://github.com/nishhantt/ADHD--All-Day-High-Decibel.git

# Go to server folder
cd ADHD--All-Day-High-Decibel/server

# Install Python packages
pip install -r requirements.txt
```

**Step 3: Start the Server**
```bash
python app.py
```

You'll see something like:
```
Uvicorn running on http://0.0.0.0:7860
```

**Step 4: Get Your Phone's IP**
- Go to Settings → WiFi → Your Network
- Note the IP address (like `192.168.1.xxx`)

---

### Part 2: The Android App

**Option A: Connect via USB (Easiest)**

```bash
# Connect phone via USB, enable USB debugging
adb reverse tcp:7860 tcp:7860

# Build the app
cd app && ./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Connect over WiFi**

In the app settings, change the backend URL to:
```
http://YOUR_PHONE_IP:7860
```
(Replace with your actual phone IP)

---

## 🔧 Troubleshooting

### "Connection Failed" Error
- Make sure Termux server is running (`python app.py`)
- Check your phone IP is correct
- Try: `curl http://localhost:7860` in Termux to test

### Music Not Playing
- Check your internet connection
- Make sure you port-forwarded with ADB (for USB)
- Try restarting the server: Ctrl+C, then `python app.py` again

### App Crash
- Check if backend is running first
- Look at logcat: `adb logcat | grep -i music`

---

## 🎓 How to Deploy Backend on Render (Cloud)

Want to run the backend in the cloud instead of on your phone? Here's how:

### Step 1: Push to GitHub
```bash
# In the project root
git add .
git commit -m "My ADHD Music App"
git push origin main
```

### Step 2: Setup on Render
1. Go to [render.com](https://render.com) and sign up
2. Click **New** → **Web Service**
3. Connect your GitHub and select the repo
4. Configure:
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `python app.py`
5. Click **Create Web Service**

### Step 3: Update App
After deploy, you'll get a URL like `https://your-app.onrender.com`
Update this in your app's settings!

---

## 📁 Project Structure

```
ADHD--All-Day-High-Decibel/
├── app/                    # Android app (Kotlin + Jetpack Compose)
│   └── src/main/java/
│       ├── player/         # ExoPlayer, Service, Notifications
│       ├── network/        # Backend API calls (PhoneBackendService)
│       └── presentation/   # UI screens (Search, Player)
│
├── server/                 # Python FastAPI backend
│   ├── app.py              # Main server with YouTube API
│   ├── recommendation/     # ML-based song recommendations
│   ├── requirements.txt    # Python dependencies
│   └── README.md           # Server-specific docs
│
└── README.md               # This file
```

---

## 🛠️ Tech Stack

| What | Technology |
|------|------------|
| **Mobile UI** | Jetpack Compose + Kotlin |
| **Media Player** | Media3 ExoPlayer |
| **Backend** | FastAPI (Python) |
| **Search** | iTunes Search API (free, no key needed) |
| **Audio** | YouTube (via youtubei internal API) |
| **Hosting** | Termux (phone) OR Render (cloud) |

---

## ⚠️ Legal & Disclaimer

- This app is for **personal use only**
- Don't redistribute copyrighted music
- YouTube streaming may violate their Terms of Service - use responsibly

---

## 🤝 Credits

- **iTunes Search API** - Free music metadata
- **YouTube** - Audio content source
- **Media3 ExoPlayer** - Rock-solid media player
- **FastAPI** - Fast Python web framework

---

## 📝 Build Commands

```bash
# Build Android app
cd app && ./gradlew assembleDebug

# Install on phone (with USB debugging on)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Test server locally
cd server && python app.py
```

---

**Enjoy your free music! 🎧**

Built with ❤️ using Python + Kotlin