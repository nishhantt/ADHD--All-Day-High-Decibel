package com.example.musicplayer.network

import retrofit2.http.GET
import retrofit2.http.Query

data class YouTubeSearchResponse(val items: List<YouTubeSearchItem>?)
data class YouTubeSearchItem(val id: VideoId?, val snippet: YouTubeSnippet?)
data class VideoId(val videoId: String?)
data class YouTubeSnippet(val title: String?, val thumbnails: ThumbnailContainer?)
data class ThumbnailContainer(val default: Thumbnail?)
data class Thumbnail(val url: String?)

data class YouTubeVideosResponse(val items: List<YouTubeVideoItem>?)
data class YouTubeVideoItem(val id: String?, val snippet: YouTubeSnippet?)

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String? = null,
        @Query("videoDuration") videoDuration: String? = null,
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") key: String
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideos(
        @Query("part") part: String = "snippet",
        @Query("id") id: String,
        @Query("key") key: String
    ): YouTubeVideosResponse
}
