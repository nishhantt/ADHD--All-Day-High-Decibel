package com.example.musicplayer.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Backend that returns a direct audio stream URL for a YouTube video id.
 * Your server should extract the stream (e.g. with yt-dlp) and return the URL.
 */
interface ExtractorBackendApi {
    /**
     * GET /stream?videoId=xxx
     * Response: { "url": "https://...", "title": "optional", "mimeType": "optional" }
     */
    @GET("stream")
    suspend fun getStream(@Query("videoId") videoId: String): StreamResponse
}

data class StreamResponse(
    val url: String,
    val title: String? = null,
    val mimeType: String? = null
)
