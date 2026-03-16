package com.example.musicplayer.data

import com.example.musicplayer.domain.models.PlayableMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PlayerCommand {
    data class Play(val media: PlayableMedia) : PlayerCommand
    object Pause : PlayerCommand
    object Resume : PlayerCommand
    object Next : PlayerCommand
    object Previous : PlayerCommand
    data class Seek(val positionMs: Long) : PlayerCommand
    data class SetQueue(val queue: List<PlayableMedia>, val startIndex: Int = 0) : PlayerCommand
}

interface PlayerRepository {
    fun observeQueue(): StateFlow<List<PlayableMedia>>
    fun observeCommands(): SharedFlow<PlayerCommand>

    suspend fun enqueueAndPlay(media: PlayableMedia)
    suspend fun setQueue(queue: List<PlayableMedia>, startIndex: Int = 0)
    suspend fun play()
    suspend fun pause()
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(ms: Long)
}

@Singleton
class PlayerRepositoryImpl @Inject constructor(): PlayerRepository {
    private val _queue = MutableStateFlow<List<PlayableMedia>>(emptyList())
    private val _commands = MutableSharedFlow<PlayerCommand>(replay = 1)

    override fun observeQueue(): StateFlow<List<PlayableMedia>> = _queue.asStateFlow()
    override fun observeCommands(): SharedFlow<PlayerCommand> = _commands.asSharedFlow()

    override suspend fun enqueueAndPlay(media: PlayableMedia) {
        val list = _queue.value.toMutableList()
        list.add(media)
        _queue.value = list
        _commands.emit(PlayerCommand.Play(media))
    }

    override suspend fun setQueue(queue: List<PlayableMedia>, startIndex: Int) {
        _queue.value = queue
        _commands.emit(PlayerCommand.SetQueue(queue, startIndex))
    }

    override suspend fun play() { _commands.emit(PlayerCommand.Resume) }
    override suspend fun pause() { _commands.emit(PlayerCommand.Pause) }
    override suspend fun next() { _commands.emit(PlayerCommand.Next) }
    override suspend fun previous() { _commands.emit(PlayerCommand.Previous) }
    override suspend fun seekTo(ms: Long) { _commands.emit(PlayerCommand.Seek(ms)) }
}
