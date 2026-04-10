package com.example.musicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.ServiceCompat
import com.example.musicplayer.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayerService : Service() {

    companion object {
        const val ACTION_TOGGLE_PLAY = "com.example.musicplayer.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.musicplayer.NEXT"
        const val ACTION_PREV = "com.example.musicplayer.PREV"
        const val ACTION_STOP = "com.example.musicplayer.STOP"
    }

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private val binder = LocalBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: MediaNotificationManager
    
    private var currentTitle = "Not Playing"
    private var currentArtist = ""
    private var currentArtwork: Bitmap? = null

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = MediaNotificationManager(this)
        setupMediaSession()
    }

    private fun setupMediaSession() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        mediaSession = MediaSessionCompat(this, "MusicPlayer").apply {
            setSessionActivity(pendingIntent)
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.support.v4.media.session.MediaButtonReceiver.handleIntent(mediaSession, intent)
        
        when (intent?.action) {
            ACTION_TOGGLE_PLAY -> togglePlayPause()
            ACTION_NEXT -> seekToNext()
            ACTION_PREV -> seekToPrevious()
            ACTION_STOP -> {
                stopPlayback()
                return START_NOT_STICKY
            }
        }
        
        return START_STICKY
    }

    private fun getPlayer() = exoPlayerManager.player

    private fun togglePlayPause() {
        val player = getPlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        updatePlaybackState()
        buildAndShowNotification()
    }

    private fun seekToNext() {
        val player = getPlayer()
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            player.seekTo(0, 0)
        }
        updatePlaybackState()
        buildAndShowNotification()
    }

    private fun seekToPrevious() {
        val player = getPlayer()
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
        updatePlaybackState()
        buildAndShowNotification()
    }

    private fun stopPlayback() {
        val player = getPlayer()
        player.stop()
        player.clearMediaItems()
        mediaSession.isActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updatePlaybackState() {
        val player = getPlayer()
        val state = if (player.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, player.currentPosition, 1f)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun buildAndShowNotification() {
        val player = getPlayer()
        val title = player.mediaMetadata.title?.toString() ?: currentTitle
        val artist = player.mediaMetadata.artist?.toString() ?: currentArtist
        val playing = player.isPlaying

        val mediaMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .build()
        mediaSession.setMetadata(mediaMetadata)

        val notification = notificationManager.createNotification(
            title, artist, playing, currentArtwork
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        ServiceCompat.startForeground(this, 1, notification, type)
    }

    fun updateNotification(title: String, artist: String, isPlaying: Boolean, artwork: Bitmap?) {
        currentTitle = title
        currentArtist = artist
        currentArtwork = artwork
        
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, notificationManager.createNotification(title, artist, isPlaying, artwork))
    }

    fun getMediaSession(): MediaSessionCompat = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
