package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.PlayerRepository
import javax.inject.Inject

class ControlPlaybackUseCases @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend fun play() = playerRepository.play()
    suspend fun pause() = playerRepository.pause()
    suspend fun next() = playerRepository.next()
    suspend fun previous() = playerRepository.previous()
    suspend fun seekTo(ms: Long) = playerRepository.seekTo(ms)
}
