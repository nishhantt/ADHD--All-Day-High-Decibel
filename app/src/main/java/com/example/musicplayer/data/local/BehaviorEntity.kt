package com.example.musicplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_behavior")
data class BehaviorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: String = "",
    val action: String, // "PLAY", "SKIP", "SEARCH"
    val query: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val artistName: String = "",
    val albumName: String = ""
)

@Dao
interface BehaviorDao {
    @Insert
    suspend fun insertBehavior(behavior: BehaviorEntity)

    @Query("SELECT * FROM user_behavior ORDER BY timestamp DESC LIMIT 1000")
    fun getAllBehaviors(): Flow<List<BehaviorEntity>>

    @Query("SELECT songId, COUNT(*) as playCount FROM user_behavior WHERE action = 'PLAY' GROUP BY songId")
    suspend fun getMostPlayedSongs(): List<SongPlayCount>

    @Query("SELECT artistName, COUNT(*) as playCount FROM user_behavior WHERE action = 'PLAY' GROUP BY artistName ORDER BY playCount DESC LIMIT 5")
    suspend fun getTopArtists(): List<ArtistPlayCount>
}

data class SongPlayCount(val songId: String, val playCount: Int)
data class ArtistPlayCount(val artistName: String, val playCount: Int)
