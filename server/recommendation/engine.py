from .behavior import get_behavior_recommendations
from .content import get_similar_songs


def get_recommendations(song_id, limit=5):
    behavior = get_behavior_recommendations(song_id, limit=limit)
    content = get_similar_songs(song_id, limit=limit)
    seen = set(behavior)
    for cid in content:
        if cid not in seen:
            behavior.append(cid)
            seen.add(cid)
    return {"behavior_based": behavior[:limit], "content_based": content[:limit]}


def get_up_next(song_id, limit=10):
    behavior = get_behavior_recommendations(song_id, limit=limit)
    content = get_similar_songs(song_id, limit=limit)
    result = []
    seen = set()
    for bid in behavior:
        if bid not in seen:
            result.append({"song_id": bid, "reason": "Because you played this"})
            seen.add(bid)
    for cid in content:
        if cid not in seen:
            result.append({"song_id": cid, "reason": "Similar to what you played"})
            seen.add(cid)
    return result[:limit]
