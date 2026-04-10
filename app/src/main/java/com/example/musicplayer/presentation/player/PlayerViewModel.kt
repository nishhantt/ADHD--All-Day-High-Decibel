package com.example.musicplayer.presentation.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.player.ExoPlayerManager
import com.example.musicplayer.player.MusicPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val musicRepository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val player = exoPlayerManager.player
    
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false
    private var artworkLoadJob: Job? = null
    private var prefetchJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(ExoPlayerManager.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentIndex = 0
    private var currentPlayJob: Job? = null
    private var lastPlayedSongId: String? = null
    private var cachedArtwork: Bitmap? = null
    private var positionUpdateJob: Job? = null

    init {
        bindService()
        setupPlayerListener()
        startPositionUpdates()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                Log.d("PlayerViewModel", "Media item transition: index=$index, reason=$reason")
                
                if (index >= 0 && index < _playlist.value.size) {
                    val newSong = _playlist.value[index]
                    
                    if (_currentSong.value?.id != newSong.id) {
                        if (newSong.id.isNotBlank()) {
                            viewModelScope.launch {
                                musicRepository.updateTransition(lastPlayedSongId, newSong.id)
                            }
                            lastPlayedSongId = newSong.id
                        }
                        _currentSong.value = newSong
                        loadArtworkForNotification(newSong)
                    }
                    
                    currentIndex = index
                    prefetchNextSongs()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("PlayerViewModel", "Playback state: $playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        _duration.value = player.duration.coerceAtLeast(0)
                        _isLoading.value = false
                    }
                    Player.STATE_ENDED -> {
                        Log.d("PlayerViewModel", "Playback ended")
                        _isLoading.value = false
                    }
                    Player.STATE_BUFFERING -> {
                        _isLoading.value = true
                    }
                    Player.STATE_IDLE -> {
                        _isLoading.value = false
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("PlayerViewModel", "Player error: ${error.message}")
                _isLoading.value = false
            }
        })
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _playbackPosition.value = player.currentPosition
                    _duration.value = player.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    private fun bindService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startService(intent)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MusicPlayerService.LocalBinder
                musicService = binder.getService()
                serviceBound = true
                Log.d("PlayerViewModel", "Service connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
                serviceBound = false
            }
        }, Context.BIND_AUTO_CREATE)
    }

    private fun loadArtworkForNotification(song: Song) {
        artworkLoadJob?.cancel()
        if (song.image.isBlank()) {
            cachedArtwork = null
            return
        }
        artworkLoadJob = viewModelScope.launch {
            cachedArtwork = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = URL(song.image)
                    val connection = url.openConnection()
                    connection.doInput = true
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.inputStream.use { input ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(input, null, options)
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Artwork load failed: ${e.message}")
                    null
                }
            }
            updateNotification()
        }
    }

    private fun updateNotification() {
        if (!serviceBound) return
        val song = _currentSong.value ?: return
        musicService?.updateNotification(
            title = song.title,
            artist = song.artist,
            isPlaying = player.isPlaying,
            artwork = cachedArtwork
        )
    }

    fun playSong(song: Song, existingPlaylist: List<Song> = emptyList()) {
        currentPlayJob?.cancel()
        prefetchJob?.cancel()
        
        _isLoading.value = true
        
        val playlistToUse: List<Song>
        val startIndex: Int
        
        if (existingPlaylist.isNotEmpty()) {
            val songIndex = existingPlaylist.indexOfFirst { it.id == song.id }
            if (songIndex >= 0) {
                playlistToUse = existingPlaylist.toList()
                startIndex = songIndex
            } else {
                playlistToUse = listOf(song) + existingPlaylist
                startIndex = 0
            }
        } else {
            playlistToUse = listOf(song)
            startIndex = 0
        }
        
        _playlist.value = playlistToUse
        currentIndex = startIndex
        
        currentPlayJob = viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "Playing: ${song.title}")
                
                val songWithUrl = if (song.audioUrl.isNotBlank()) {
                    song
                } else {
                    Log.d("PlayerViewModel", "Fetching URL for: ${song.title}")
                    val streamUrl = musicRepository.getStreamUrlForSong(song)
                    if (streamUrl.isNullOrBlank()) {
                        Log.e("PlayerViewModel", "Failed to get URL")
                        _isLoading.value = false
                        return@launch
                    }
                    Log.d("PlayerViewModel", "Got URL")
                    song.copy(audioUrl = streamUrl)
                }

                _currentSong.value = songWithUrl
                
                val updatedPlaylist = playlistToUse.mapIndexed { i, s -> 
                    if (i == startIndex) songWithUrl else s 
                }
                _playlist.value = updatedPlaylist

                val mediaItems = updatedPlaylist.mapNotNull { s -> 
                    if (s.audioUrl.isNotBlank()) createMediaItem(s) else null 
                }
                
                if (mediaItems.isEmpty()) {
                    Log.e("PlayerViewModel", "No valid media items")
                    _isLoading.value = false
                    return@launch
                }
                
                player.stop()
                player.clearMediaItems()
                player.setMediaItems(mediaItems, startIndex, 0L)
                player.repeatMode = _repeatMode.value
                player.prepare()
                player.play()
                
                Log.d("PlayerViewModel", "Started playing at index $startIndex")
                
                loadArtworkForNotification(songWithUrl)
                updateNotification()
                prefetchNextSongs()
                
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch {
            val songWithUrl = if (song.audioUrl.isBlank()) {
                val streamUrl = musicRepository.getStreamUrlForSong(song)
                if (streamUrl.isNullOrBlank()) {
                    Log.e("PlayerViewModel", "Failed to get URL for playNext")
                    return@launch
                }
                song.copy(audioUrl = streamUrl)
            } else song

            val currentList = _playlist.value.toMutableList()
            val nextIndex = (currentIndex + 1).coerceAtMost(currentList.size)
            
            currentList.add(nextIndex, songWithUrl)
            _playlist.value = currentList
            
            player.addMediaItem(nextIndex, createMediaItem(songWithUrl))
            Log.d("PlayerViewModel", "Added to play next: ${songWithUrl.title}")
        }
    }

    private fun prefetchNextSongs() {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val currentList = _playlist.value
            if (currentList.isEmpty()) return@launch
            
            val startIndex = currentIndex + 1
            if (startIndex >= currentList.size) return@launch
            
            val songsToFetch = currentList.drop(startIndex).take(5).filter { it.audioUrl.isBlank() }
            if (songsToFetch.isEmpty()) return@launch
            
            musicRepository.prefetchUrls(songsToFetch)
            
            val updatedList = _playlist.value.toMutableList()
            var changed = false
            
            updatedList.forEachIndexed { i, song ->
                if (i >= startIndex && song.audioUrl.isBlank()) {
                    val streamUrl = musicRepository.getCachedUrl(song.id, song.artist, song.title)
                    if (!streamUrl.isNullOrBlank()) {
                        updatedList[i] = song.copy(audioUrl = streamUrl)
                        changed = true
                    }
                }
            }
            
            if (changed) {
                _playlist.value = updatedList
                Log.d("PlayerViewModel", "Prefetch complete")
            }
        }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch {
            val songWithUrl = if (song.audioUrl.isBlank()) {
                val streamUrl = musicRepository.getStreamUrlForSong(song)
                if (streamUrl.isNullOrBlank()) {
                    Log.e("PlayerViewModel", "Failed to get URL for queue")
                    return@launch
                }
                song.copy(audioUrl = streamUrl)
            } else song

            val currentList = _playlist.value.toMutableList()
            currentList.add(songWithUrl)
            _playlist.value = currentList
            
            player.addMediaItem(createMediaItem(songWithUrl))
            Log.d("PlayerViewModel", "Added to queue: ${songWithUrl.title}")
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

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {
        Log.d("PlayerViewModel", "next() called, hasNext=${player.hasNextMediaItem()}, count=${player.mediaItemCount}")
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_repeatMode.value == ExoPlayerManager.REPEAT_MODE_ALL && player.mediaItemCount > 0) {
            player.seekTo(0, 0)
        }
    }

    fun previous() {
        Log.d("PlayerViewModel", "previous() called, position=${player.currentPosition}, hasPrev=${player.hasPreviousMediaItem()}")
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (_repeatMode.value == ExoPlayerManager.REPEAT_MODE_ALL && player.mediaItemCount > 0) {
            player.seekTo(player.mediaItemCount - 1, 0)
        } else {
            player.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun seekForward(ms: Long = 10000L) {
        val newPosition = (player.currentPosition + ms).coerceAtMost(player.duration)
        player.seekTo(newPosition)
    }

    fun seekBackward(ms: Long = 10000L) {
        val newPosition = (player.currentPosition - ms).coerceAtLeast(0)
        player.seekTo(newPosition)
    }

    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            ExoPlayerManager.REPEAT_MODE_OFF -> ExoPlayerManager.REPEAT_MODE_ALL
            ExoPlayerManager.REPEAT_MODE_ALL -> ExoPlayerManager.REPEAT_MODE_ONE
            else -> ExoPlayerManager.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        player.repeatMode = nextMode
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        artworkLoadJob?.cancel()
        prefetchJob?.cancel()
        currentPlayJob?.cancel()
    }
}
