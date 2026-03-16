package com.example.musicplayer.data

import com.example.musicplayer.domain.models.PlayableMedia
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakePlayerRepository : PlayerRepository {
    private val _queue = MutableStateFlow<List<PlayableMedia>>(emptyList())
    private val _commands = MutableSharedFlow<PlayerCommand>(replay = 0)

    val emittedCommands = mutableListOf<PlayerCommand>()

    override fun observeQueue(): StateFlow<List<PlayableMedia>> = _queue
    override fun observeCommands(): SharedFlow<PlayerCommand> = _commands

    override suspend fun enqueueAndPlay(media: PlayableMedia) {
        _queue.value = _queue.value + media
        val cmd = PlayerCommand.Play(media)
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun setQueue(queue: List<PlayableMedia>, startIndex: Int) {
        _queue.value = queue
        val cmd = PlayerCommand.SetQueue(queue, startIndex)
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun play() {
        val cmd = PlayerCommand.Resume
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun pause() {
        val cmd = PlayerCommand.Pause
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun next() {
        val cmd = PlayerCommand.Next
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun previous() {
        val cmd = PlayerCommand.Previous
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }

    override suspend fun seekTo(ms: Long) {
        val cmd = PlayerCommand.Seek(ms)
        emittedCommands.add(cmd)
        _commands.emit(cmd)
    }
}
