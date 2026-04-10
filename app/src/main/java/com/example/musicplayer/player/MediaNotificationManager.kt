package com.example.musicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.musicplayer.MainActivity

class MediaNotificationManager(private val context: Context) {
    private val channelId = "music_playback_channel"
    private val notificationId = 1

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        largeIcon: Bitmap?
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPending = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePlayIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_TOGGLE_PLAY
        }
        val togglePlayPending = PendingIntent.getService(
            context, 1, togglePlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_NEXT
        }
        val nextPending = PendingIntent.getService(
            context, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PREV
        }
        val prevPending = PendingIntent.getService(
            context, 3, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context, 4, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseText = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setDeleteIntent(stopPending)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(
                        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                            ?.let { null }
                    )
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseText, togglePlayPending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)

        largeIcon?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    fun cancel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}
