package com.example.musicplayer.data

import com.example.musicplayer.BuildConfig
import com.example.musicplayer.domain.models.PlayableMedia
import com.example.musicplayer.domain.models.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface YouTubeRepository {
    fun searchVideos(query: String): Flow<Result<List<Video>>>
    fun getPlayableStream(videoId: String): Flow<Result<PlayableMedia>>
}

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val api: com.example.musicplayer.network.YouTubeApiService,
    private val extractor: StreamExtractor
) : YouTubeRepository {

    override fun searchVideos(query: String): Flow<Result<List<Video>>> = flow {
        val res = api.searchVideos(
            query = query,
            videoCategoryId = "10",           // Music category
            videoDuration = "medium",         // avoid shorts and very long videos
            key = BuildConfig.YOUTUBE_API_KEY
        )
        val items = res.items.orEmpty().mapNotNull { item ->
            val id = item.id?.videoId ?: return@mapNotNull null
            val title = item.snippet?.title ?: ""
            val thumb = item.snippet?.thumbnails?.default?.url ?: ""
            Video(id = id, title = title, thumbnailUrl = thumb)
        }
        emit(Result.success(items))
    }.catch { e -> emit(Result.failure(e)) }.flowOn(Dispatchers.IO)

    override fun getPlayableStream(videoId: String): Flow<Result<PlayableMedia>> = flow {
        val playable = extractor.getPlayableMedia(videoId)

        // try to fetch metadata (title, thumbnail) from YouTube videos endpoint
        try {
            val vidResp = api.getVideos(id = videoId, key = BuildConfig.YOUTUBE_API_KEY)
            val item = vidResp.items.orEmpty().firstOrNull()
            val title = item?.snippet?.title
            val thumb = item?.snippet?.thumbnails?.default?.url
            val enriched = PlayableMedia(
                uriString = playable.uriString,
                mimeType = playable.mimeType,
                title = title,
                thumbnailUrl = thumb
            )
            emit(Result.success(enriched))
        } catch (t: Throwable) {
            // if metadata fetch fails, still return playable
            emit(Result.success(playable))
        }
    }.catch { e -> emit(Result.failure(e)) }.flowOn(Dispatchers.IO)
}
