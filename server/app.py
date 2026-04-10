import json
import os
import time
from collections import OrderedDict
from concurrent.futures import ThreadPoolExecutor
from functools import lru_cache
from pathlib import Path
from urllib.parse import quote

import requests
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, PlainTextResponse, StreamingResponse
from starlette.middleware.gzip import GZipMiddleware

session = requests.Session()
session.headers.update({
    "User-Agent": "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36",
})

from recommendation import (
    get_recommendations as get_song_recommendations,
    get_song_by_id,
    get_songs_by_ids,
    get_up_next,
    update_transition,
    upsert_song_records,
)


app = FastAPI()

app.add_middleware(GZipMiddleware, minimum_size=500)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Content-Type", "Authorization"],
)

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
CACHE_DIR = BASE_DIR / "song_cache"
URL_CACHE_FILE = BASE_DIR / "url_cache.json"
CACHE_LIMIT_BYTES = 600 * 1024 * 1024

DATA_DIR.mkdir(parents=True, exist_ok=True)
CACHE_DIR.mkdir(parents=True, exist_ok=True)

executor = ThreadPoolExecutor(max_workers=3)


class LRUCache:
    def __init__(self, maxsize: int = 128):
        self.cache = OrderedDict()
        self.maxsize = maxsize
        self.hits = 0
        self.misses = 0
    
    def get(self, key: str) -> any:
        if key in self.cache:
            self.hits += 1
            self.cache.move_to_end(key)
            return self.cache[key]
        self.misses += 1
        return None
    
    def set(self, key: str, value: any):
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.maxsize:
            self.cache.popitem(last=False)
    
    def clear(self):
        self.cache.clear()
        self.hits = 0
        self.misses = 0
    
    def stats(self) -> dict:
        total = self.hits + self.misses
        return {
            "hits": self.hits,
            "misses": self.misses,
            "hit_rate": self.hits / total if total > 0 else 0,
            "size": len(self.cache)
        }


search_cache = LRUCache(maxsize=256)
url_cache_memory = LRUCache(maxsize=512)

url_cache = {}
if URL_CACHE_FILE.exists():
    try:
        with open(URL_CACHE_FILE, "r") as f:
            url_cache = json.load(f)
    except Exception:
        url_cache = {}

soundcloud_token = None
soundcloud_token_expires_at = 0


def save_url_cache():
    try:
        with open(URL_CACHE_FILE, "w") as f:
            json.dump(url_cache, f)
    except Exception:
        pass


def get_cached_url(song_id, artist, title):
    key = f"{song_id}|{artist}|{title}"
    
    cached = url_cache_memory.get(key)
    if cached:
        return cached
    
    if key in url_cache:
        entry = url_cache[key]
        if time.time() - entry.get("time", 0) < 172800:
            result = (entry.get("source"), entry.get("url"), entry.get("video_id"))
            url_cache_memory.set(key, result)
            return result
    return None, None, None


def cache_url(song_id, artist, title, source, url, video_id=None):
    key = f"{song_id}|{artist}|{title}"
    url_cache[key] = {"source": source, "url": url, "video_id": video_id, "time": time.time()}
    url_cache_memory.set(key, (source, url, video_id))
    save_url_cache()


@app.middleware("http")
async def security_headers(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "SAMEORIGIN"
    response.headers["Accept-Ranges"] = "bytes"
    return response


def is_song_cached(song_id):
    return (CACHE_DIR / f"{song_id}.m4a").exists()


def get_cache_size_bytes():
    return sum(entry.stat().st_size for entry in CACHE_DIR.iterdir() if entry.is_file())


def clear_audio_cache():
    for entry in CACHE_DIR.iterdir():
        if entry.is_file():
            try:
                entry.unlink()
            except OSError:
                pass


def clear_cache_if_needed():
    if get_cache_size_bytes() > CACHE_LIMIT_BYTES:
        clear_audio_cache()


def inject_cache_status(songs):
    for song in songs:
        song["cached"] = is_song_cached(song["id"])
    return songs


def _itunes_to_song(item):
    art_url = item.get("artworkUrl100", "")
    cover = art_url.replace("100x100bb", "200x200bb") if art_url else ""
    cover_xl = art_url.replace("100x100bb", "600x600bb") if art_url else ""
    return {
        "id": str(item.get("trackId", item.get("collectionId", 0))),
        "title": item.get("trackName", "Unknown"),
        "artist": item.get("artistName", "Unknown"),
        "artist_id": item.get("artistId", 0),
        "album": item.get("collectionName", "Single"),
        "cover": cover,
        "cover_xl": cover_xl,
        "duration": item.get("trackTimeMillis", 0) // 1000,
        "genre": item.get("primaryGenreName", "Music"),
    }


def search_songs(query):
    if not query:
        return []
    
    cache_key = f"search:{query.lower().strip()}"
    cached = search_cache.get(cache_key)
    if cached:
        return cached
    
    try:
        response = session.get(
            "https://itunes.apple.com/search",
            params={"term": query, "media": "music", "limit": 50},
            timeout=8,
        )
        data = response.json()
        items = data.get("results", [])
        
        query_lower = query.lower().strip()
        songs = []
        primary_genres = {"pop", "rock", "hip-hop", "rap", "r&b", "electronic", "dance", "indie", "bollywood", "hip hop", "punjabi", "hindi", "k-pop", "korean", "j-pop", "japanese", "c-pop", "chinese"}
        
        for item in items:
            if not item.get("trackName"):
                continue
            
            song = _itunes_to_song(item)
            title_lower = song["title"].lower()
            artist_lower = song["artist"].lower()
            genre_lower = song.get("genre", "").lower()
            
            score = 0
            if query_lower in title_lower or query_lower in artist_lower:
                score += 10
            if query_lower in artist_lower:
                score += 5
            
            has_latin = any(c.isalpha() and ord(c) < 128 for c in query_lower)
            has_non_latin = any(ord(c) > 127 for c in title_lower) or any(ord(c) > 127 for c in artist_lower)
            
            if has_latin and has_non_latin:
                score -= 3
            
            is_primary_genre = any(g in genre_lower for g in primary_genres)
            if is_primary_genre:
                score += 1
            
            song["_score"] = score
            songs.append(song)
        
        songs.sort(key=lambda x: x.get("_score", 0), reverse=True)
        
        for song in songs:
            song.pop("_score", None)
        
        upsert_song_records(songs[:25])
        result = inject_cache_status(songs[:25])
        search_cache.set(cache_key, result)
        return result
    except Exception as e:
        print(f"Search error: {e}")
        return []


def get_chart():
    try:
        response = requests.get("https://itunes.apple.com/in/rss/topsongs/limit=25/json", timeout=10).json()
        entries = response.get("feed", {}).get("entry", [])
        songs = []
        for entry in entries:
            try:
                art_url = ""
                for img in entry.get("im:image", []):
                    art_url = img.get("label", "")
                cover = art_url.replace("170x170bb", "200x200bb") if art_url else ""
                cover_xl = art_url.replace("170x170bb", "600x600bb") if art_url else ""
                artist_id = 0
                artist_link = entry.get("im:artist", {}).get("attributes", {}).get("href", "")
                if "/id" in artist_link:
                    try:
                        artist_id = int(artist_link.split("/id")[-1].split("?")[0])
                    except Exception:
                        pass
                track_id = str(entry.get("id", {}).get("attributes", {}).get("im:id", "0") or "0")
                genre = entry.get("category", {}).get("attributes", {}).get("label", "Music")
                songs.append(
                    {
                        "id": track_id,
                        "title": entry.get("im:name", {}).get("label", "Unknown"),
                        "artist": entry.get("im:artist", {}).get("label", "Unknown"),
                        "artist_id": artist_id,
                        "album": entry.get("im:collection", {}).get("im:name", {}).get("label", "Single"),
                        "cover": cover,
                        "cover_xl": cover_xl,
                        "duration": 0,
                        "genre": genre,
                    }
                )
            except Exception:
                continue
        upsert_song_records(songs)
        return inject_cache_status(songs)
    except Exception as e:
        print(f"Chart error: {e}")
        return []


def fetch_lyrics(artist, title):
    try:
        resp = requests.get(
            "https://lrclib.net/api/search",
            params={"artist_name": artist, "track_name": title},
            headers={"User-Agent": "Bitsongs/1.0"},
            timeout=5,
        )
        data = resp.json()
        if isinstance(data, list) and data:
            for item in data:
                if item.get("syncedLyrics"):
                    return {"type": "synced", "text": item["syncedLyrics"]}
            for item in data:
                if item.get("plainLyrics"):
                    return {"type": "plain", "text": item["plainLyrics"]}
        return {"type": "error", "text": "No lyrics found."}
    except Exception:
        return {"type": "error", "text": "Lyrics unavailable."}


def fetch_artist_tracks(artist_id, limit=20):
    try:
        artist_id = int(artist_id or 0)
        if artist_id <= 0:
            return []
        response = requests.get(
            "https://itunes.apple.com/lookup",
            params={"id": artist_id, "entity": "song", "limit": limit},
            timeout=10,
        )
        data = response.json()
        songs = [_itunes_to_song(item) for item in data.get("results", []) if item.get("wrapperType") == "track" and item.get("trackName")]
        if songs:
            upsert_song_records(songs)
        return songs
    except Exception:
        return []


def fetch_artist_search_results(artist_name, limit=25):
    try:
        artist_name = (artist_name or "").strip()
        if not artist_name:
            return []
        response = requests.get(
            "https://itunes.apple.com/search",
            params={"term": artist_name, "media": "music", "entity": "song", "limit": limit},
            timeout=10,
        )
        data = response.json()
        songs = []
        normalized_artist = artist_name.casefold()
        for item in data.get("results", []):
            item_artist = str(item.get("artistName", "")).strip()
            if not item.get("trackName"):
                continue
            if normalized_artist not in item_artist.casefold() and item_artist.casefold() not in normalized_artist:
                continue
            songs.append(_itunes_to_song(item))
        if songs:
            upsert_song_records(songs)
        return songs
    except Exception:
        return []


def enrich_catalog_for_song(song_id):
    song = get_song_by_id(song_id)
    if not song:
        return
    fetched_songs = fetch_artist_tracks(song.get("artist_id"))
    if not fetched_songs:
        fetch_artist_search_results(song.get("artist"))


def hydrate_song_ids(song_ids):
    songs = get_songs_by_ids(song_ids)
    missing_ids = set(str(sid) for sid in song_ids) - {str(s.get("id")) for s in songs if s}
    if missing_ids:
        for sid in missing_ids:
            song = get_song_by_id(sid)
            if song:
                songs.append(song)
    return songs


def build_recommendation_response(song_id):
    enrich_catalog_for_song(song_id)
    grouped_ids = get_song_recommendations(song_id)
    recommendations = grouped_ids.get("behavior_based", []) + grouped_ids.get("content_based", [])
    
    if len(recommendations) < 5:
        chart = get_chart()
        seen = set(recommendations)
        for song in chart:
            if song["id"] not in seen:
                recommendations.append(song["id"])
                seen.add(song["id"])
            if len(recommendations) >= 10:
                break
    
    return {
        "behavior_based": hydrate_song_ids(recommendations[:5]),
        "content_based": hydrate_song_ids(recommendations[5:10]) if len(recommendations) > 5 else [],
    }


def build_up_next_response(song_id, limit=10):
    enrich_catalog_for_song(song_id)
    entries = get_up_next(song_id, limit=limit)
    songs_by_id = {song["id"]: song for song in hydrate_song_ids([entry["song_id"] for entry in entries])}
    result = []
    for entry in entries:
        song = songs_by_id.get(entry["song_id"])
        if song:
            item = dict(song)
            item["reason"] = entry["reason"]
            result.append(item)
    
    if len(result) < limit:
        chart = get_chart()
        seen = {entry["song_id"] for entry in entries}
        for song in chart:
            if song["id"] not in seen:
                item = dict(song)
                item["reason"] = "Popular song"
                result.append(item)
                seen.add(song["id"])
            if len(result) >= limit:
                break
    
    return result


def download_task(song_id, artist, title):
    pass


def build_proxy_response(url, incoming_headers, headers_json):
    try:
        try:
            yt_headers = json.loads(headers_json or "{}")
        except Exception:
            yt_headers = {}

        req_headers = {
            "User-Agent": yt_headers.get("User-Agent", "Mozilla/5.0 (Linux; Android 13)"),
            "Accept": "*/*",
            "Accept-Language": "en-us,en;q=0.5",
            "Referer": "https://music.youtube.com/",
            "Accept-Encoding": "identity",
        }
        if "range" in incoming_headers:
            req_headers["Range"] = incoming_headers["range"]

        req = requests.get(url, stream=True, headers=req_headers, timeout=15)
        excluded_headers = {"content-encoding", "transfer-encoding", "connection", "keep-alive"}
        response_headers = {name: value for name, value in req.headers.items() if name.lower() not in excluded_headers}
        response_headers["Accept-Ranges"] = "bytes"
        response_headers["Cache-Control"] = "no-cache"
        return StreamingResponse(
            req.iter_content(chunk_size=32 * 1024),
            status_code=req.status_code,
            media_type=req.headers.get("content-type", "audio/mp4"),
            headers=response_headers,
        )
    except Exception as exc:
        return PlainTextResponse(f"Stream error: {exc}", status_code=500)


def get_soundcloud_token():
    global soundcloud_token, soundcloud_token_expires_at
    
    if soundcloud_token and time.time() < soundcloud_token_expires_at - 60:
        return soundcloud_token
    
    try:
        client_id = os.environ.get("SOUNDCLOUD_CLIENT_ID", "")
        client_secret = os.environ.get("SOUNDCLOUD_CLIENT_SECRET", "")
        
        if not client_id or not client_secret:
            print("SoundCloud credentials not configured")
            return None
        
        resp = requests.post(
            "https://api.soundcloud.com/oauth2/token",
            data={
                "client_id": client_id,
                "client_secret": client_secret,
                "grant_type": "client_credentials"
            },
            timeout=10
        )
        
        if resp.status_code == 200:
            data = resp.json()
            soundcloud_token = data.get("access_token")
            expires_in = data.get("expires_in", 3600)
            soundcloud_token_expires_at = time.time() + expires_in
            print(f"Got SoundCloud token")
            return soundcloud_token
        else:
            print(f"SoundCloud auth failed: {resp.status_code}")
    except Exception as e:
        print(f"SoundCloud token error: {e}")
    
    return None


def search_soundcloud(query, artist="", title=""):
    token = get_soundcloud_token()
    if not token:
        return None
    
    try:
        params = {
            "q": query,
            "limit": 10,
            "client_id": os.environ.get("SOUNDCLOUD_CLIENT_ID", "")
        }
        
        headers = {"Authorization": f"OAuth {token}"}
        resp = requests.get(
            "https://api.soundcloud.com/tracks",
            params=params,
            headers=headers,
            timeout=5
        )
        
        if resp.status_code == 200:
            tracks = resp.json()
            
            for track in tracks:
                track_title = track.get("title", "").lower()
                artist_name = track.get("user", {}).get("username", "").lower()
                
                if artist.lower() in track_title or artist.lower() in artist_name:
                    if track.get("id"):
                        return track
            
            for track in tracks:
                if title.lower() in track.get("title", "").lower():
                    if track.get("id"):
                        return track
                        
            if tracks and tracks[0].get("id"):
                return tracks[0]
                        
        elif resp.status_code == 401:
            soundcloud_token = None
            soundcloud_token_expires_at = 0
            
    except Exception as e:
        print(f"SoundCloud search error: {e}")
    
    return None


def get_soundcloud_stream_url(track_id):
    token = get_soundcloud_token()
    if not token:
        return None, None
    
    try:
        headers = {"Authorization": f"OAuth {token}"}
        client_id = os.environ.get("SOUNDCLOUD_CLIENT_ID", "")
        
        resp = requests.get(
            f"https://api.soundcloud.com/tracks/{track_id}/streams",
            headers=headers,
            timeout=5
        )
        
        if resp.status_code == 200:
            data = resp.json()
            
            if data.get("hls_aac_160_url"):
                return data["hls_aac_160_url"], "audio/aac"
            elif data.get("http_mp3_128_url"):
                return data["http_mp3_128_url"], "audio/mpeg"
            elif data.get("preview_url"):
                return data["preview_url"], "audio/mpeg"
                
        elif resp.status_code == 401:
            soundcloud_token = None
            soundcloud_token_expires_at = 0
            
    except Exception as e:
        print(f"SoundCloud stream error: {e}")
    
    return None, None


YOUTUBEI_CLIENTS = [
    {"clientName": "ANDROID", "clientVersion": "19.37.35", "clientScreen": "SMAP"},
    {"clientName": "ANDROID_MUSIC", "clientVersion": "6.22.51", "clientScreen": "NORMAL"},
]


def search_youtube_api(query, artist="", title=""):
    client = YOUTUBEI_CLIENTS[0]
    try:
        resp = session.post(
            "https://music.youtube.com/youtubei/v1/search",
            params={"key": "AIzaSyC9XL3QzW49E9b7P4L-pzNcQo0MxzF_Pz0"},
            json={
                "query": query,
                "context": {"client": client},
                "params": "EgWKAQIIAUABAgQKAggB"
            },
            headers={"Content-Type": "application/json"},
            timeout=4
        )
        if resp.status_code == 200:
            data = resp.json()
            contents = data.get("contents", {}).get("tabbedSearchResultsRenderer", {}).get("tabs", [])
            for tab in contents:
                if "tabRenderer" in tab:
                    results = tab["tabRenderer"].get("content", {}).get("sectionListRenderer", {}).get("contents", [])
                    for section in results:
                        for key in ["musicShelfRenderer", "itemSectionRenderer"]:
                            if key in section:
                                items = section[key].get("contents", [])
                                for item in items:
                                    if "musicResponsiveListItemRenderer" in item:
                                        renderer = item["musicResponsiveListItemRenderer"]
                                        for flex in renderer.get("flexColumns", []):
                                            for t in flex.get("musicResponsiveListItemFlexColumnRenderer", {}).get("text", {}).get("runs", []):
                                                if t.get("navigationEndpoint", {}).get("watchEndpoint"):
                                                    return {"videoId": t["navigationEndpoint"]["watchEndpoint"].get("videoId")}
            return None
    except Exception as e:
        print(f"YouTube API search failed: {e}")
    return None


def get_stream_youtube_api(video_id):
    client = YOUTUBEI_CLIENTS[0]
    try:
        resp = session.post(
            "https://music.youtube.com/youtubei/v1/player",
            params={"key": "AIzaSyC9XL3QzW49E9b7P4L-pzNcQo0MxzF_Pz0"},
            json={
                "videoId": video_id,
                "context": {"client": client},
            },
            headers={"Content-Type": "application/json"},
            timeout=4
        )
        if resp.status_code == 200:
            data = resp.json()
            streaming_data = data.get("streamingData", {})
            for fmt in streaming_data.get("adaptiveFormats", []):
                mime_type = fmt.get("mimeType", "")
                if "audio" in mime_type:
                    url = fmt.get("url", "")
                    if url:
                        return url, mime_type
            for fmt in streaming_data.get("formats", []):
                mime_type = fmt.get("mimeType", "")
                if "audio" in mime_type:
                    url = fmt.get("url", "")
                    if url:
                        return url, mime_type
    except Exception as e:
        print(f"YouTube API stream failed: {e}")
    return None, None


INVIDIOUS_INSTANCES = [
    "https://yewtu.be",
    "https://inv.nadeko.net",
]


def search_invidious(query, artist="", title=""):
    for instance in INVIDIOUS_INSTANCES:
        try:
            resp = session.get(
                f"{instance}/api/v1/search",
                params={"q": query, "type": "video"},
                timeout=3
            )
            if resp.status_code == 200:
                results = resp.json()
                if results:
                    for video in results[:3]:
                        video_title = video.get("title", "").lower()
                        if artist.lower() in video_title and title.lower() in video_title:
                            return video
                        if title.lower() in video_title and ("lyrics" not in video_title or "official" in video_title):
                            return video
                    return results[0]
        except Exception:
            continue
    return None


def get_invidious_streams(video_id):
    for instance in INVIDIOUS_INSTANCES:
        try:
            resp = session.get(
                f"{instance}/api/v1/videos/{video_id}",
                timeout=3
            )
            if resp.status_code == 200:
                data = resp.json()
                formats = data.get("adaptiveFormats", [])
                for fmt in formats:
                    if "audio" in fmt.get("type", "").lower():
                        return fmt.get("url", ""), fmt.get("type", "audio/webm")
        except Exception:
            continue
    return None, None


def render_play_response(request: Request, song_id: str, artist: str, title: str):
    cached_source, cached_url, cached_video_id = get_cached_url(song_id, artist, title)
    
    if cached_url:
        return JSONResponse({
            "source": cached_source,
            "url": cached_url,
            "direct_url": cached_url,
            "video_id": cached_video_id
        })
    
    query = f"{artist} {title}"
    print(f"Searching: {query}")
    
    sc_track = search_soundcloud(query, artist, title)
    if sc_track:
        track_id = sc_track.get("id")
        stream_url, mime = get_soundcloud_stream_url(track_id)
        if stream_url:
            cache_url(song_id, artist, title, "soundcloud", stream_url, str(track_id))
            return JSONResponse({
                "source": "soundcloud",
                "url": stream_url,
                "direct_url": stream_url,
                "video_id": str(track_id)
            })
    
    video = search_youtube_api(query, artist, title)
    
    if video:
        video_id = video.get("videoId")
        print(f"Found YouTube: {video_id}")
        stream_url, mime = get_stream_youtube_api(video_id)
        if stream_url:
            cache_url(song_id, artist, title, "youtube", stream_url, video_id)
            return JSONResponse({
                "source": "youtube",
                "url": stream_url,
                "direct_url": stream_url,
                "video_id": video_id
            })
    
    inv_video = search_invidious(query, artist, title)
    if inv_video:
        video_id = inv_video.get("videoId", "")
        stream_url, _ = get_invidious_streams(video_id)
        if stream_url:
            cache_url(song_id, artist, title, "invidious", stream_url, video_id)
            return JSONResponse({
                "source": "invidious",
                "url": stream_url,
                "direct_url": stream_url,
                "video_id": video_id
            })
    
    return JSONResponse({"error": "Could not find stream. Try a different search."}, status_code=404)


@app.get("/")
def root():
    return JSONResponse({
        "status": "ok",
        "message": "ADHD Music Server",
        "soundcloud_configured": bool(os.environ.get("SOUNDCLOUD_CLIENT_ID"))
    })


@app.get("/api/mobile/search")
def mobile_search(q: str = ""):
    result = search_songs(q)
    return JSONResponse(
        content=result,
        headers={
            "Cache-Control": "public, max-age=300",
            "X-Cache-Status": "hit" if search_cache.get(f"search:{q.lower().strip()}") else "miss"
        }
    )


@app.get("/api/mobile/chart")
def mobile_chart():
    cache_key = "chart:top25"
    cached = search_cache.get(cache_key)
    if cached:
        return JSONResponse(
            content=cached,
            headers={"Cache-Control": "public, max-age=600"}
        )
    result = get_chart()
    search_cache.set(cache_key, result)
    return JSONResponse(
        content=result,
        headers={"Cache-Control": "public, max-age=600"}
    )


@app.get("/api/mobile/recommend")
def mobile_recommend(song_id: str = ""):
    return JSONResponse(build_recommendation_response(song_id))


@app.get("/api/mobile/up_next")
def mobile_up_next(song_id: str = "", limit: int = 10):
    return JSONResponse(build_up_next_response(song_id, limit=limit or 10))


@app.get("/api/mobile/lyrics")
def mobile_lyrics(artist: str = "", title: str = ""):
    return JSONResponse(fetch_lyrics(artist, title))


@app.get("/api/mobile/play")
def mobile_play(request: Request, id: str = "", artist: str = "", title: str = "", previous_song_id: str | None = None):
    update_transition(previous_song_id, id)
    return render_play_response(request, id, artist, title)


@app.get("/api/mobile/play_fast")
def mobile_play_fast(request: Request, id: str = "", artist: str = "", title: str = ""):
    return render_play_response(request, id, artist, title)


@app.get("/api/mobile/prefetch")
async def mobile_prefetch(id: str = "", artist: str = "", title: str = ""):
    def do_prefetch():
        cached_source, cached_url, _ = get_cached_url(id, artist, title)
        if cached_url:
            print(f"Already cached: {artist} - {title}")
            return
        
        query = f"{artist} {title}"
        
        sc_track = search_soundcloud(query, artist, title)
        if sc_track:
            track_id = sc_track.get("id")
            stream_url, _ = get_soundcloud_stream_url(track_id)
            if stream_url:
                cache_url(id, artist, title, "soundcloud", stream_url, str(track_id))
                print(f"Prefetched (SoundCloud): {artist} - {title}")
                return
        
        video = search_youtube_api(query, artist, title)
        if video and video.get("videoId"):
            video_id = video["videoId"]
            stream_url, _ = get_stream_youtube_api(video_id)
            if stream_url:
                cache_url(id, artist, title, "youtube", stream_url, video_id)
                print(f"Prefetched (YouTube): {artist} - {title}")
    
    executor.submit(do_prefetch)
    return JSONResponse({"status": "prefetching"})


@app.get("/api/mobile/stream_cache/{filename:path}")
def mobile_stream_cache(filename: str):
    filepath = CACHE_DIR / filename
    if not filepath.exists():
        return PlainTextResponse("Not Found", status_code=404)
    return FileResponse(filepath)


@app.get("/api/mobile/stream_proxy")
def mobile_stream_proxy(request: Request, url: str = "", headers: str = "{}"):
    if not url:
        return PlainTextResponse("No URL", status_code=400)
    return build_proxy_response(url, request.headers, headers)


@app.post("/api/mobile/cache_song")
async def mobile_cache_song(request: Request):
    data = await request.json()
    if not data:
        return JSONResponse({"error": "No data"}, status_code=400)
    executor.submit(download_task, str(data.get("id")), data.get("artist"), data.get("title"))
    return JSONResponse({"status": "queued"})


@app.get("/api/mobile/health")
def mobile_health():
    sc_token = get_soundcloud_token()
    return JSONResponse({
        "status": "ok",
        "server": "ADHD Music",
        "version": "2.1",
        "soundcloud": "connected" if sc_token else "not_configured",
        "timestamp": int(time.time()),
        "cache_stats": {
            "search": search_cache.stats(),
            "url": url_cache_memory.stats()
        }
    })


@app.get("/api/mobile/clear_cache")
def mobile_clear_cache():
    global url_cache
    url_cache = {}
    search_cache.clear()
    url_cache_memory.clear()
    try:
        if URL_CACHE_FILE.exists():
            URL_CACHE_FILE.unlink()
    except Exception:
        pass
    return JSONResponse({"status": "cache cleared"})


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app:app",
        host="0.0.0.0",
        port=7860,
        reload=False,
        limit_concurrency=100,
        limit_max_requests=1000,
        timeout_keep_alive=30,
        workers=1
    )
