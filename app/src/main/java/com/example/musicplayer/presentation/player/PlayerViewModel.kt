package com.example.musicplayer.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.player.ExoPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val exoPlayerManager: ExoPlayerManager,
    private val musicRepository: com.example.musicplayer.data.MusicRepository,
    private val behaviorRepository: com.example.musicplayer.data.BehaviorRepository,
    private val playbackStateDao: com.example.musicplayer.data.local.dao.PlaybackStateDao,
    private val recommendationEngine: com.example.musicplayer.domain.recommendation.RecommendationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private var currentIndex = -1

    private val player: Player = exoPlayerManager.asPlayer()

    init {
        // Load last played song
        viewModelScope.launch {
            playbackStateDao.observeState().collect { state ->
                if (state != null && _uiState.value is PlayerUiState.Idle) {
                    val song = Song(
                        id = state.currentTrackId ?: "",
                        title = state.title ?: "",
                        artist = state.artist ?: "",
                        image = state.image ?: "",
                        audioUrl = state.audioUrl ?: ""
                    )
                    _uiState.value = PlayerUiState.Playing(song)
                    _currentPosition.value = state.positionMs
                }
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
                if (isPlayingNow) {
                    _duration.value = player.duration.coerceAtLeast(0)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                } else if (state == Player.STATE_ENDED) {
                    smartNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Update current song state when transitioning (e.g. via Next/Prev)
                val currentSong = _playlist.value.getOrNull(player.currentMediaItemIndex)
                if (currentSong != null) {
                    currentIndex = player.currentMediaItemIndex
                    _uiState.value = PlayerUiState.Playing(currentSong)
                    
                    // Track play behavior
                    viewModelScope.launch {
                        behaviorRepository.trackPlay(currentSong.id, currentSong.artist, currentSong.title)
                    }
                }
            }
        })

        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                    // Occasionally duration is -1 until some data is buffered
                    if (_duration.value <= 0) {
                        _duration.value = player.duration.coerceAtLeast(0)
                    }
                }
                delay(1000)
            }
        }
    }

    private var playJob: kotlinx.coroutines.Job? = null

    fun playSong(song: Song, songs: List<Song> = emptyList()) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            
            try {
                // Ensure we have a stream URL (this might take a second, so we allow cancellation)
                val songWithUrl = if (song.audioUrl.isBlank()) {
                    Log.d("Player", "Fetching stream URL for: ${song.title}")
                    musicRepository.getSongDetails(song.id) ?: song
                } else {
                    song
                }

                if (!kotlinx.coroutines.isActive) return@launch

                if (songWithUrl.audioUrl.isBlank()) {
                    _uiState.value = PlayerUiState.Error("Invalid audio URL")
                    return@launch
                }

                // If a list was provided (e.g. from search results), use it as the playlist
                val newList = if (songs.isNotEmpty()) songs.toMutableList() else mutableListOf(songWithUrl)
                
                // Ensure the explicit song with the URL is updated in the list
                val idx = newList.indexOfFirst { it.id == songWithUrl.id }
                if (idx != -1) {
                    newList[idx] = songWithUrl
                } else {
                    newList.add(0, songWithUrl)
                }

                _playlist.value = newList
                currentIndex = newList.indexOfFirst { it.id == songWithUrl.id }.coerceAtLeast(0)

                Log.d("PlayerViewModel", "Final Playlist Size: ${newList.size}, Index: $currentIndex")
                
                val mediaItems = newList.map { createMediaItem(it) }
                
                // Clear state before new session
                player.stop()
                player.clearMediaItems()
                
                player.setMediaItems(mediaItems, currentIndex, 0L)
                player.prepare()
                player.play()
                
                _uiState.value = PlayerUiState.Playing(songWithUrl)
                saveState(songWithUrl)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Playback error", e)
                _uiState.value = PlayerUiState.Error("Playback failed")
            }
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setUri(song.audioUrl)
            .setMediaId(song.id)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(android.net.Uri.parse(song.image))
                    .build()
            )
            .build()
    }

    private fun saveState(song: Song) {
        viewModelScope.launch {
            playbackStateDao.upsert(
                com.example.musicplayer.data.local.entities.PlaybackStateEntity(
                    id = 0,
                    currentTrackId = song.id,
                    title = song.title,
                    artist = song.artist,
                    image = song.image,
                    audioUrl = song.audioUrl,
                    positionMs = 0L,
                    isPlaying = true
                )
            )
        }
    }

    fun addToQueue(song: Song) {
        val currentList = _playlist.value.toMutableList()
        if (!currentList.any { it.id == song.id }) {
            currentList.add(song)
            _playlist.value = currentList
            player.addMediaItem(createMediaItem(song))
        }
    }

    fun playNext(song: Song) {
        val currentList = _playlist.value.toMutableList()
        val nextIndex = (currentIndex + 1).coerceAtMost(currentList.size)
        currentList.add(nextIndex, song)
        _playlist.value = currentList
        player.addMediaItem(nextIndex, createMediaItem(song))
    }

    fun togglePlayPause() {
        if (player.isPlaying) exoPlayerManager.pause() else exoPlayerManager.play()
    }

    fun next() {
        val currentSong = (_uiState.value as? PlayerUiState.Playing)?.song
        if (currentSong != null) {
            viewModelScope.launch { behaviorRepository.trackSkip(currentSong.id) }
        }
        fadeOutAndNext()
    }

    private fun fadeOutAndNext() {
        viewModelScope.launch {
            // Smooth fade out
            for (i in 10 downTo 0) {
                player.volume = i / 10f
                delay(50)
            }
            smartNext()
            // Reset volume for next track
            player.volume = 1.0f
        }
    }

    private fun smartNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            // AI logic: Recommend a fresh song if we reach the end of the playlist
            val currentSong = (_uiState.value as? PlayerUiState.Playing)?.song
            if (currentSong != null && _playlist.value.isNotEmpty()) {
                viewModelScope.launch {
                    val recommended = recommendationEngine.recommendNextSong(currentSong, _playlist.value)
                    // In a real app, we might fetch NEW songs from API here.
                    // For now, we jump to the recommended one in the current list.
                    val index = _playlist.value.indexOf(recommended)
                    if (index != -1) {
                        player.seekTo(index, 0L)
                    } else {
                        player.seekTo(0, 0)
                    }
                    player.play()
                }
            } else if (_playlist.value.isNotEmpty()) {
                player.seekTo(0, 0)
                player.play()
            }
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        _currentPosition.value = ms
    }

    fun getDuration(): Long = _duration.value
}

sealed interface PlayerUiState {
    object Idle : PlayerUiState
    object Loading : PlayerUiState
    data class Playing(val song: Song) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
