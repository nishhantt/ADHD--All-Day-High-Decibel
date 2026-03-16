package com.example.musicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.musicplayer.player.MusicPlayerService

class MediaNotificationManager(private val context: Context) {
    private val channelId = "music_playback_channel"
    private val notificationId = 1

    fun createNotification(mediaTitle: String, largeIcon: Bitmap? = null): Notification {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Playback", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(chan)
        }

        val playIntent = Intent(context, MusicPlayerService::class.java).apply { action = MusicPlayerService.ACTION_TOGGLE_PLAY }
        val nextIntent = Intent(context, MusicPlayerService::class.java).apply { action = MusicPlayerService.ACTION_NEXT }
        val prevIntent = Intent(context, MusicPlayerService::class.java).apply { action = MusicPlayerService.ACTION_PREV }
        val stopIntent = Intent(context, MusicPlayerService::class.java).apply { action = MusicPlayerService.ACTION_STOP }

        val playPending = PendingIntent.getService(context, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(context, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPending = PendingIntent.getService(context, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopPending = PendingIntent.getService(context, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(mediaTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(android.R.drawable.ic_media_play, "Play/Pause", playPending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)

        largeIcon?.let { builder.setLargeIcon(it) }
        return builder.build()
    }

    fun notify(mediaTitle: String, largeIcon: Bitmap? = null) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, createNotification(mediaTitle, largeIcon))
    }

    fun cancel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}
