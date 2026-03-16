package com.example.musicplayer.data

import com.example.musicplayer.BuildConfig
import com.example.musicplayer.domain.models.PlayableMedia
import com.example.musicplayer.network.ExtractorBackendApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts a playable audio URL by calling your backend with the YouTube video id.
 * If EXTRACTOR_BACKEND_URL is not set or the backend fails, falls back to sample audio (DevStreamExtractor).
 */
@Singleton
class BackendStreamExtractor @Inject constructor(
    private val backendApi: ExtractorBackendApi,
    private val devExtractor: com.example.musicplayer.data.dev.DevStreamExtractor
) : StreamExtractor {

    override suspend fun getPlayableMedia(videoId: String): PlayableMedia {
        val baseUrl = BuildConfig.EXTRACTOR_BACKEND_URL
        if (baseUrl.isBlank()) {
            return devExtractor.getPlayableMedia(videoId)
        }
        return try {
            val res = backendApi.getStream(videoId)
            PlayableMedia(
                uriString = res.url,
                mimeType = res.mimeType,
                title = res.title,
                thumbnailUrl = null
            )
        } catch (t: Throwable) {
            devExtractor.getPlayableMedia(videoId)
        }
    }
}
