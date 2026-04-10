package com.example.musicplayer.data

import com.example.musicplayer.domain.models.PlayableMedia

interface StreamExtractor {
    suspend fun getPlayableMedia(videoId: String): PlayableMedia
}
