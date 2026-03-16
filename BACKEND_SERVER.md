# Extractor backend (for real YouTube audio)

The app **searches YouTube** (via YouTube Data API) and shows results. When you **tap a result**, it asks **your backend** for a direct audio stream URL for that video id. Your backend does the actual “extract audio from YouTube” step (e.g. with yt-dlp). The app then plays that URL with ExoPlayer.

## 1. App configuration

In the project root, create or edit **`local.properties`** and add:

```properties
YOUTUBE_API_KEY=your_youtube_data_api_key
EXTRACTOR_BACKEND_URL=https://YOUR_SERVER/ 
```

- **YOUTUBE_API_KEY** – from Google Cloud Console (YouTube Data API v3). Needed for search.
- **EXTRACTOR_BACKEND_URL** – base URL of your extractor backend **with a trailing slash**, e.g. `https://192.168.1.10:8080/` or `https://mybackend.com/`.

Rebuild the APK after changing `local.properties`.

If **EXTRACTOR_BACKEND_URL** is missing or empty, the app still works but uses a **sample MP3** for every track (no real YouTube audio).

---

## 2. Backend API contract

Your server must expose one endpoint:

**GET** `{EXTRACTOR_BACKEND_URL}stream?videoId=VIDEO_ID`

Example: `GET https://YOUR_SERVER/stream?videoId=dQw4w9WgXcQ`

**Response:** JSON with the direct audio stream URL.

Required:

- **url** (string) – direct URL that ExoPlayer can stream (e.g. an audio-only stream). Must be **https** and playable by the app (no auth cookies in the URL if the backend does the extraction).

Optional:

- **title** (string) – track title (app may use it if YouTube metadata is missing).
- **mimeType** (string) – e.g. `audio/mpeg`, `audio/webm`.

Example response:

```json
{
  "url": "https://rr3---sn-xxx.googlevideo.com/...",
  "title": "Song name",
  "mimeType": "audio/webm"
}
```

- Status **200** and valid JSON with **url** → app plays that URL.
- Any error (4xx, 5xx, network error, invalid JSON) → app falls back to the sample MP3 for that tap.

---

## 3. Example backend (Python + yt-dlp)

Run this on your PC or a server. The phone and the server must be on the same network (or the server reachable from the internet). Use the server’s URL (e.g. `http://192.168.1.10:8080/`) as **EXTRACTOR_BACKEND_URL** in `local.properties`.

**Requirements:** Python 3, `flask`, `yt-dlp`:

```bash
pip install flask yt-dlp
```

**`extractor_server.py`** (in project root or anywhere):

```python
from flask import Flask, request, jsonify
import yt_dlp

app = Flask(__name__)

@app.route("/stream")
def stream():
    video_id = request.args.get("videoId")
    if not video_id:
        return jsonify({"error": "missing videoId"}), 400
    ydl_opts = {
        "format": "bestaudio/best",
        "quiet": True,
        "no_warnings": True,
        "extract_flat": False,
    }
    url = f"https://www.youtube.com/watch?v={video_id}"
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return jsonify({"error": "no info"}), 404
            # Prefer a direct URL (some formats are direct)
            format_url = None
            for f in info.get("formats") or []:
                if f.get("vcodec") == "none" and f.get("url"):
                    format_url = f["url"]
                    break
            if not format_url and info.get("url"):
                format_url = info["url"]
            if not format_url:
                return jsonify({"error": "no audio url"}), 404
            return jsonify({
                "url": format_url,
                "title": info.get("title"),
                "mimeType": "audio/webm",
            })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
```

Run:

```bash
python extractor_server.py
```

Then set in **local.properties**:

```properties
EXTRACTOR_BACKEND_URL=http://YOUR_PC_IP:8080/
```

(e.g. `http://192.168.1.10:8080/`). Rebuild the app and test: search a song, tap a result → app should play audio from that video via your backend.

---

## 4. Summary

| Step | Where |
|------|--------|
| Search YouTube | App → YouTube Data API (YOUTUBE_API_KEY) |
| Get stream URL for a video id | App → Your backend `GET /stream?videoId=xxx` |
| Extract audio | Your server (e.g. yt-dlp) |
| Play audio | App (ExoPlayer) using URL returned by backend |

The app does **not** extract YouTube audio itself; it only calls your backend. You are responsible for running the backend and complying with YouTube’s Terms of Service.
