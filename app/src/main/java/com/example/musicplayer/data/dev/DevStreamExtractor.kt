package com.example.musicplayer.data.dev

import com.example.musicplayer.data.StreamExtractor
import com.example.musicplayer.domain.models.PlayableMedia
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Development StreamExtractor that returns a stable test audio URL.
 * Replace this with a compliant extractor before production.
 */
@Singleton
class DevStreamExtractor @Inject constructor() : StreamExtractor {
    override suspend fun getPlayableMedia(videoId: String): PlayableMedia {
        // For development only: a short sample mp3 hosted publicly
        val sample = "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3"
        return PlayableMedia(uriString = sample, mimeType = "audio/mpeg")
    }
}
