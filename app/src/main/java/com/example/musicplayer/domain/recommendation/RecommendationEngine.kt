package com.example.musicplayer.domain.recommendation

import com.example.musicplayer.data.BehaviorRepository
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.models.Song
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val musicRepository: MusicRepository
) {
    suspend fun getSuggestedSongs(): List<Song> {
        return try {
            // Get top artists from listening history
            val topArtists = behaviorRepository.getTopArtists()
            
            if (topArtists.isNotEmpty()) {
                // Search for songs by top artists
                val suggestions = mutableListOf<Song>()
                for (artist in topArtists.take(3)) {
                    val results = musicRepository.searchSongs(artist.artistName)
                    suggestions.addAll(results.take(5))
                }
                suggestions.distinctBy { it.id }.take(15)
            } else {
                // No history yet, return popular searches
                val popularSearches = listOf("trending", "top hits", "new music")
                val results = mutableListOf<Song>()
                for (search in popularSearches) {
                    val songs = musicRepository.searchSongs(search)
                    results.addAll(songs.take(5))
                }
                results.distinctBy { it.id }.take(15)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNewSongs(): List<Song> {
        return try {
            // Search for new/recent songs
            val searches = listOf("new songs 2024", "latest hits", "trending music")
            val results = mutableListOf<Song>()
            
            for (search in searches) {
                val songs = musicRepository.searchSongs(search)
                results.addAll(songs.take(5))
            }
            
            results.distinctBy { it.id }.take(20)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recommendNextSong(currentSong: Song, availableSongs: List<Song>): Song {
        if (availableSongs.isEmpty()) return currentSong
        if (availableSongs.size == 1) return availableSongs[0]

        // Prefer same artist songs first
        val sameArtistSongs = availableSongs.filter { 
            it.artist == currentSong.artist && it.id != currentSong.id 
        }
        
        return if (sameArtistSongs.isNotEmpty()) {
            sameArtistSongs.random()
        } else {
            availableSongs.filter { it.id != currentSong.id }.randomOrNull() ?: currentSong
        }
    }
}
