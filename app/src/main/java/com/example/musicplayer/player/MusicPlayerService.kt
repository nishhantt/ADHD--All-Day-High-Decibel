package com.example.musicplayer.player

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.domain.models.PlayableMedia
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaSession
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import javax.inject.Inject
import android.media.AudioManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat

/**
 * Foreground playback service skeleton. Connects UI (via binding or MediaController) to ExoPlayerManager.
 */
@AndroidEntryPoint
class MusicPlayerService : LifecycleService() {

    companion object {
        const val ACTION_TOGGLE_PLAY = "com.example.musicplayer.action.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.musicplayer.action.NEXT"
        const val ACTION_PREV = "com.example.musicplayer.action.PREV"
        const val ACTION_STOP = "com.example.musicplayer.action.STOP"
    }

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager
    @Inject
    lateinit var playerRepository: com.example.musicplayer.data.PlayerRepository

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>(replay = 1)
    val playbackEvents = _playbackEvents.asSharedFlow()

    private val binder = LocalBinder()
    private var mediaSession: MediaSession? = null
    private var notification: Notification? = null
    private var noisyReceiverRegistered = false
    private var currentMedia: PlayableMedia? = null
    private var listenerAndCollectorStarted = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val player = exoPlayerManager.asPlayer()
        if (mediaSession == null) {
            val mainActivityIntent = Intent(this, com.example.musicplayer.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .build()
        }

        val nm = MediaNotificationManager(this)
        notification = nm.createNotification("Music Player")
        
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        
        ServiceCompat.startForeground(this, 1, notification!!, type)

        player.setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)

        intent?.action?.let { action ->
            when (action) {
                ACTION_TOGGLE_PLAY -> {
                    if (player.isPlaying) pause() else playCurrentFromQueueIfAny()
                }
                ACTION_NEXT -> next()
                ACTION_PREV -> previous()
                ACTION_STOP -> {
                    player.stop()
                    stopForeground(true)
                    stopSelf()
                }
            }
        }

        if (!listenerAndCollectorStarted) {
            listenerAndCollectorStarted = true
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    lifecycleScope.launch { _playbackEvents.emit(PlaybackEvent.Error(error)) }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    lifecycleScope.launch {
                        if (isPlaying) _playbackEvents.emit(PlaybackEvent.PlayingCurrent)
                        else _playbackEvents.emit(PlaybackEvent.Paused)
                    }
                }
            })

            lifecycleScope.launch {
                playerRepository.observeCommands().collect { cmd ->
                    when (cmd) {
                        is com.example.musicplayer.data.PlayerCommand.Play -> play(cmd.media)
                        is com.example.musicplayer.data.PlayerCommand.Pause -> pause()
                        is com.example.musicplayer.data.PlayerCommand.Resume -> playCurrentFromQueueIfAny()
                        is com.example.musicplayer.data.PlayerCommand.Next -> next()
                        is com.example.musicplayer.data.PlayerCommand.Previous -> previous()
                        is com.example.musicplayer.data.PlayerCommand.Seek -> seekTo(cmd.positionMs)
                        is com.example.musicplayer.data.PlayerCommand.SetQueue -> {
                            val list = cmd.queue
                            val idx = cmd.startIndex
                            val item = list.getOrNull(idx)
                            if (item != null) play(item)
                        }
                    }
                }
            }

            if (!noisyReceiverRegistered) {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                registerReceiver(becomingNoisyReceiver, filter)
                noisyReceiverRegistered = true
            }
        }

        return Service.START_STICKY
    }

    fun play(media: PlayableMedia) {
        currentMedia = media
        lifecycleScope.launch {
            exoPlayerManager.prepareAndPlay(android.net.Uri.parse(media.uriString), title = media.title, artworkUri = media.thumbnailUrl)
            _playbackEvents.emit(PlaybackEvent.Playing(media))
            MediaNotificationManager(this@MusicPlayerService).notify(media.title ?: "Playing")

            media.thumbnailUrl?.let { thumb ->
                try {
                    val loader = ImageLoader(this@MusicPlayerService)
                    val req = ImageRequest.Builder(this@MusicPlayerService).data(thumb).allowHardware(false).build()
                    val result = loader.execute(req)
                    if (result is SuccessResult) {
                        val bmp = result.drawable.toBitmap()
                        MediaNotificationManager(this@MusicPlayerService).notify(media.title ?: "Playing", bmp)
                    }
                } catch (_: Exception) {}
            }
            try { audioFocusRequester?.requestFocus() } catch (_: Exception) { }
        }
    }

    private val audioFocusRequester by lazy { AudioFocusRequester(this) }

    private fun playCurrentFromQueueIfAny() {
        val player = exoPlayerManager.asPlayer()
        if (!player.isPlaying) player.play()
    }

    fun pause() {
        exoPlayerManager.pause()
        lifecycleScope.launch { _playbackEvents.emit(PlaybackEvent.Paused) }
        try { audioFocusRequester.abandonFocus() } catch (_: Exception) {}
    }

    fun seekTo(ms: Long) { exoPlayerManager.seekTo(ms) }

    private fun next() {
        val queue = playerRepository.observeQueue().value
        if (queue.isEmpty()) return
        val cur = currentMedia
        val idx = if (cur != null) queue.indexOfFirst { it.uriString == cur.uriString } else -1
        val nextIdx = if (idx >= 0) (idx + 1) % queue.size else 0
        play(queue[nextIdx])
    }

    private fun previous() {
        val queue = playerRepository.observeQueue().value
        if (queue.isEmpty()) return
        val cur = currentMedia
        val idx = if (cur != null) queue.indexOfFirst { it.uriString == cur.uriString } else -1
        val prevIdx = if (idx > 0) idx - 1 else queue.size - 1
        play(queue[prevIdx])
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        exoPlayerManager.release()
        MediaNotificationManager(this).cancel()
        if (noisyReceiverRegistered) {
            try { unregisterReceiver(becomingNoisyReceiver) } catch (_: Exception) {}
            noisyReceiverRegistered = false
        }
    }
}

sealed interface PlaybackEvent {
    data class Playing(val media: PlayableMedia) : PlaybackEvent
    object PlayingCurrent : PlaybackEvent
    object Paused : PlaybackEvent
    data class Error(val throwable: Throwable) : PlaybackEvent
}
