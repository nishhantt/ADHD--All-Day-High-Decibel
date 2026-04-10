package com.example.musicplayer.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val loadControl by lazy {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,      // minBufferMs
                15000,     // maxBufferMs
                1500,      // bufferForPlaybackMs
                3000       // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_OFF
            }
    }

    fun asPlayer(): ExoPlayer = player
    fun play() { player.play() }
    fun pause() { player.pause() }
    fun seekTo(positionMs: Long) { player.seekTo(positionMs) }
    fun release() { player.release() }
    
    fun setRepeatMode(mode: Int) {
        player.repeatMode = mode
    }
    
    fun getRepeatMode(): Int = player.repeatMode
    
    companion object {
        const val REPEAT_MODE_OFF = ExoPlayer.REPEAT_MODE_OFF
        const val REPEAT_MODE_ALL = ExoPlayer.REPEAT_MODE_ALL
        const val REPEAT_MODE_ONE = ExoPlayer.REPEAT_MODE_ONE
    }
}
