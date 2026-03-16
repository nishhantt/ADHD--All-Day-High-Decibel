package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.YouTubeRepository
import com.example.musicplayer.domain.models.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val repository: YouTubeRepository
) {
    fun execute(query: String): Flow<Result<List<Video>>> {
        return repository.searchVideos(query)
    }
}
