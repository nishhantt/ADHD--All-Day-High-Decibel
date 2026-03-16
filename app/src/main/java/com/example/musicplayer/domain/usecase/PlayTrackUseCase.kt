package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.PlayerRepository
import com.example.musicplayer.domain.models.PlayableMedia
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlayTrackUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend fun execute(media: PlayableMedia) {
        playerRepository.enqueueAndPlay(media)
    }
}
