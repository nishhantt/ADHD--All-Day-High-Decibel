"""
Example extractor backend for the Music Player app.
Run: pip install flask yt-dlp   then   python extractor_server.py
Set EXTRACTOR_BACKEND_URL=http://YOUR_IP:8080/ in local.properties and rebuild the app.
"""
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
            # Pick best audio-only format that has a direct URL (ExoPlayer can stream)
            format_url = None
            formats = info.get("formats") or []
            # Prefer audio-only formats with direct url, then any with url
            for f in formats:
                if f.get("vcodec") == "none" and f.get("url"):
                    format_url = f["url"]
                    break
            if not format_url:
                for f in formats:
                    if f.get("url"):
                        format_url = f["url"]
                        break
            if not format_url and info.get("url"):
                format_url = info["url"]
            if not format_url:
                return jsonify({"error": "no audio url"}), 404
            # Prefer https URLs for Android clients
            if format_url.startswith("http://"):
                format_url = "https://" + format_url[len("http://"):]
            return jsonify({
                "url": format_url,
                "title": info.get("title"),
                "mimeType": "audio/webm",
            })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    import os
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port)
