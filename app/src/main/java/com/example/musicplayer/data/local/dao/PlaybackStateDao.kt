package com.example.musicplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.musicplayer.data.local.entities.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 0 LIMIT 1")
    fun observeState(): Flow<PlaybackStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlaybackStateEntity)
}
