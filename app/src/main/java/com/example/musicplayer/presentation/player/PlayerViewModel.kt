package com.example.musicplayer.presentation.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.models.PlayableMedia
import com.example.musicplayer.player.ExoPlayerManager
import com.example.musicplayer.player.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import com.example.musicplayer.domain.usecase.PlayTrackUseCase
import com.example.musicplayer.domain.usecase.ControlPlaybackUseCases
import com.example.musicplayer.domain.usecase.GetStreamUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayerManager: ExoPlayerManager,
    private val getStreamUseCase: GetStreamUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val controlPlaybackUseCases: ControlPlaybackUseCases,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val enableObservers: Boolean = true // tests can set false to skip background loops

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val queue = mutableListOf<PlayableMedia>()
    private var currentIndex = -1
    private var repeatOne = false

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        if (enableObservers) {
            // observe ExoPlayer position periodically and update UI state
            viewModelScope.launch(Dispatchers.Main) {
                while (true) {
                    val pos = exoPlayerManager.asPlayer().currentPosition
                    val current = _uiState.value
                    if (current is PlayerUiState.Playing) {
                        _uiState.value = current.copy(positionMs = pos)
                    }
                    delay(500)
                }
            }
            // Listen to player state for end-of-track to advance queue and playing changes
            exoPlayerManager.asPlayer().addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        viewModelScope.launch { next() }
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    _isPlaying.value = isPlayingNow
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.value = PlayerUiState.Error(error)
                }
            })
        }
    }

    fun playVideo(videoId: String) = ioScope.launch {
        _uiState.value = PlayerUiState.Loading
        getStreamUseCase.execute(videoId).collect { res ->
            res.onSuccess { playable ->
                // Start service first so it can receive the play command (replay=1)
                val intent = Intent(context, MusicPlayerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                playTrackUseCase.execute(playable)
                queue.add(playable)
                currentIndex = queue.lastIndex
                _uiState.value = PlayerUiState.Playing(playable, 0L)
            }
            res.onFailure { t ->
                _uiState.value = PlayerUiState.Error(t)
            }
        }
    }

    fun togglePlayPause() {
        ioScope.launch {
            val player = exoPlayerManager.asPlayer()
            if (player.isPlaying) controlPlaybackUseCases.pause() else controlPlaybackUseCases.play()
        }
    }

    fun seekTo(ms: Long) { exoPlayerManager.seekTo(ms) }

    fun previous() {
        ioScope.launch { controlPlaybackUseCases.previous() }
    }

    fun next() {
        if (repeatOne && currentIndex >= 0) {
            val media = queue[currentIndex]
            exoPlayerManager.prepareAndPlay(Uri.parse(media.uriString))
            _uiState.value = PlayerUiState.Playing(media, 0L)
            return
        }
        ioScope.launch { controlPlaybackUseCases.next() }
    }

    fun toggleRepeat() {
        repeatOne = !repeatOne
    }

    fun exoPlayerPositionMaxMs(): Long {
        val d = exoPlayerManager.asPlayer().duration
        return if (d >= 0) d else 0L
    }
}

sealed interface PlayerUiState {
    object Idle : PlayerUiState
    object Loading : PlayerUiState
    data class Playing(val media: PlayableMedia, val positionMs: Long = 0L) : PlayerUiState
    data class Error(val throwable: Throwable) : PlayerUiState
}
