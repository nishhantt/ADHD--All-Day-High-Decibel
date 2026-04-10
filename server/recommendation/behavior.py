import json
import math
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
TALLY_FILE = DATA_DIR / "tally_counter.json"


def _load_tally():
    if not TALLY_FILE.exists():
        return {"transitions": {}}
    try:
        with open(TALLY_FILE, "r") as f:
            return json.load(f)
    except Exception:
        return {"transitions": {}}


def _save_tally(data):
    try:
        with open(TALLY_FILE, "w") as f:
            json.dump(data, f)
    except Exception:
        pass


def update_transition(previous_song_id, current_song_id):
    if not previous_song_id or not current_song_id:
        return
    try:
        data = _load_tally()
        transitions = data.get("transitions", {})
        if previous_song_id not in transitions:
            transitions[previous_song_id] = {}
        if current_song_id not in transitions[previous_song_id]:
            transitions[previous_song_id][current_song_id] = 0
        transitions[previous_song_id][current_song_id] += 1
        data["transitions"] = transitions
        _save_tally(data)
    except Exception:
        pass


def get_behavior_recommendations(song_id, limit=5):
    try:
        data = _load_tally()
        transitions = data.get("transitions", {})
        if song_id not in transitions:
            return []
        scored = []
        for next_id, count in transitions[song_id].items():
            scored.append((next_id, count))
        scored.sort(key=lambda x: x[1], reverse=True)
        return [song_id for song_id, _ in scored[:limit]]
    except Exception:
        return []
