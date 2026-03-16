package com.example.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicplayer.data.local.dao.PlaylistDao
import com.example.musicplayer.data.local.dao.PlaybackStateDao
import com.example.musicplayer.data.local.entities.PlaylistEntity
import com.example.musicplayer.data.local.entities.PlaybackStateEntity
import com.example.musicplayer.data.local.entities.TrackEntity

@Database(entities = [TrackEntity::class, PlaylistEntity::class, PlaybackStateEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStateDao(): PlaybackStateDao
}
