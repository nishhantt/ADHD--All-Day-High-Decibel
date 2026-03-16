package com.example.musicplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val player: ExoPlayer by lazy { ExoPlayer.Builder(context).build() }

    fun asPlayer(): ExoPlayer = player

    fun prepareAndPlay(uri: Uri, title: String? = null, artworkUri: String? = null, startPositionMs: Long = 0L) {
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        if (!title.isNullOrBlank() || !artworkUri.isNullOrBlank()) {
            val metaBuilder = MediaMetadata.Builder()
            title?.let { metaBuilder.setTitle(it) }
            artworkUri?.let { metaBuilder.setArtworkUri(android.net.Uri.parse(it)) }
            mediaItemBuilder.setMediaMetadata(metaBuilder.build())
        }
        val mediaItem = mediaItemBuilder.build()
        player.setMediaItem(mediaItem)
        player.seekTo(startPositionMs)
        player.prepare()
        player.play()
    }

    fun play() { player.play() }
    fun pause() { player.pause() }
    fun seekTo(positionMs: Long) { player.seekTo(positionMs) }
    fun release() { player.release() }
}
