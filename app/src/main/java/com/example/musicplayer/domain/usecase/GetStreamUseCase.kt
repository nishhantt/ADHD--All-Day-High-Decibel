package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.YouTubeRepository
import com.example.musicplayer.domain.models.PlayableMedia
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStreamUseCase @Inject constructor(
    private val repository: YouTubeRepository
) {
    fun execute(videoId: String): Flow<Result<PlayableMedia>> {
        return repository.getPlayableStream(videoId)
    }
}
