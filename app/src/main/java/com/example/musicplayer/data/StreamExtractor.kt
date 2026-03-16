package com.example.musicplayer.data

import com.example.musicplayer.domain.models.PlayableMedia

/**
 * Abstract extractor that returns a playable media resource for a given YouTube video id.
 * Implementations must comply with YouTube Terms of Service. Do NOT implement TOS-violating logic.
 */
interface StreamExtractor {
    suspend fun getPlayableMedia(videoId: String): PlayableMedia
}
