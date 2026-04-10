import json
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
SONGS_FILE = DATA_DIR / "songs.json"


def _load_songs():
    if not SONGS_FILE.exists():
        return {}
    try:
        with open(SONGS_FILE, "r") as f:
            return json.load(f)
    except Exception:
        return {}


def _save_songs(data):
    try:
        with open(SONGS_FILE, "w") as f:
            json.dump(data, f)
    except Exception:
        pass


def upsert_song_records(songs):
    if not songs:
        return
    catalog = _load_songs()
    for song in songs:
        song_id = song.get("id")
        if song_id:
            if song_id not in catalog:
                catalog[song_id] = song
            else:
                catalog[song_id].update(song)
    _save_songs(catalog)


def get_song_by_id(song_id):
    catalog = _load_songs()
    return catalog.get(str(song_id))


def get_songs_by_ids(song_ids):
    catalog = _load_songs()
    return [catalog.get(str(sid)) for sid in song_ids if str(sid) in catalog]


def get_similar_songs(song_id, limit=5):
    catalog = _load_songs()
    song = catalog.get(str(song_id))
    if not song:
        return []
    
    artist_id = song.get("artist_id")
    genre = song.get("genre", "").lower()
    artist = song.get("artist", "").lower()
    title = song.get("title", "").lower()
    
    candidates = []
    
    for sid, s in catalog.items():
        if sid == str(song_id):
            continue
        score = 0
        s_artist_id = s.get("artist_id", 0)
        s_artist = s.get("artist", "").lower()
        s_genre = s.get("genre", "").lower()
        s_title = s.get("title", "").lower()
        
        if artist_id and s_artist_id == artist_id and artist_id > 0:
            score += 10
        
        if artist in s_artist or s_artist in artist:
            score += 8
        
        if genre and genre in s_genre:
            score += 3
        
        if title in s_title or s_title in title:
            score += 5
        
        if score > 0:
            candidates.append((score, sid))
    
    candidates.sort(key=lambda x: x[0], reverse=True)
    return [sid for _, sid in candidates[:limit]] if candidates else []
