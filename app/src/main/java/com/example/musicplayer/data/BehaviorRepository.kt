package com.example.musicplayer.data

import com.example.musicplayer.data.local.BehaviorDao
import com.example.musicplayer.data.local.BehaviorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorRepository @Inject constructor(
    private val behaviorDao: BehaviorDao
) {
    suspend fun trackPlay(songId: String, artist: String, album: String) {
        behaviorDao.insertBehavior(BehaviorEntity(songId = songId, action = "PLAY", artistName = artist, albumName = album))
    }

    suspend fun trackSkip(songId: String) {
        behaviorDao.insertBehavior(BehaviorEntity(songId = songId, action = "SKIP"))
    }

    suspend fun trackSearch(query: String) {
        behaviorDao.insertBehavior(
            BehaviorEntity(
                action = "SEARCH",
                query = query
            )
        )
    }

    fun getRecentSearches(): Flow<List<String>> =
        behaviorDao.getAllBehaviors().map { list ->
            list.filter { it.action == "SEARCH" }
                .map { it.query }
                .distinct()
                .take(10)
        }

    fun getAllBehaviors() = behaviorDao.getAllBehaviors()

    suspend fun getTopArtists() = behaviorDao.getTopArtists()
    suspend fun getMostPlayedSongs() = behaviorDao.getMostPlayedSongs()
}
