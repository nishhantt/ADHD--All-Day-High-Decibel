package com.example.musicplayer.data

import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.network.YouTubeSearchService
import com.example.musicplayer.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val youtubeService: YouTubeSearchService,
    private val youtubeExtractor: YouTubeExtractor,
    private val localMusicRepository: LocalMusicRepository
) {
    suspend fun globalSearch(query: String): com.example.musicplayer.domain.models.SearchResult = withContext(Dispatchers.IO) {
        if (query == "local_files") {
            val localSongs = localMusicRepository.getLocalSongs()
            return@withContext com.example.musicplayer.domain.models.SearchResult(
                songs = localSongs,
                topResult = localSongs.firstOrNull()
            )
        }

        val ytSongs = try { youtubeService.searchSongs(query) } catch (e: Exception) { emptyList() }
        
        com.example.musicplayer.domain.models.SearchResult(
            songs = ytSongs,
            topResult = ytSongs.firstOrNull()
        )
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        youtubeService.searchSongs(query)
    }

    suspend fun getAlbumSongs(albumId: String) = emptyList<Song>() // YouTube handled via search
    suspend fun getArtistSongs(artistId: String) = emptyList<Song>()
    
    suspend fun getSongDetails(songId: String): Song? {
        return when {
            songId.startsWith("local_") -> null
            songId.startsWith("yt_") -> {
                val id = songId.substringAfter("yt_")
                val streamUrl = youtubeExtractor.extractStreamUrl(id)
                Song(id = songId, title = "", artist = "", image = "", audioUrl = streamUrl)
            }
            else -> null
        }
    }
}
