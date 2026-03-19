package com.example.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BehaviorEntity::class,
        com.example.musicplayer.data.local.entities.PlaybackStateEntity::class
    ],
    version = 2, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun behaviorDao(): BehaviorDao
    abstract fun playbackStateDao(): com.example.musicplayer.data.local.dao.PlaybackStateDao
}
