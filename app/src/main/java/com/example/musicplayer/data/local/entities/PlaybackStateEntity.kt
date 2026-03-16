package com.example.musicplayer.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "current_track_id") val currentTrackId: String?,
    @ColumnInfo(name = "position_ms") val positionMs: Long = 0L,
    @ColumnInfo(name = "is_playing") val isPlaying: Boolean = false
)
