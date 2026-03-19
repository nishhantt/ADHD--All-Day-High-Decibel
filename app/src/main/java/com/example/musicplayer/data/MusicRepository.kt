package com.example.musicplayer.data

import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.network.SaavnService
import com.example.musicplayer.network.YouTubeSearchService
import com.example.musicplayer.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val saavnService: SaavnService,
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

        val saavnResults = saavnService.globalSearch(query)
        val ytSongs = try { youtubeService.searchSongs(query) } catch (e: Exception) { emptyList() }

        // Merge and de-duplicate
        val mergedSongs = (saavnResults.songs + ytSongs).distinctBy { "${it.title}-${it.artist}".lowercase() }
        
        saavnResults.copy(
            songs = mergedSongs,
            topResult = saavnResults.topResult ?: mergedSongs.firstOrNull()
        )
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        saavnService.searchSongs(query)
    }

    suspend fun getAlbumSongs(albumId: String) = saavnService.getAlbumDetails(albumId)
    suspend fun getArtistSongs(artistId: String) = saavnService.getArtistSongs(artistId)
    
    suspend fun getSongDetails(songId: String): Song? {
        return when {
            songId.startsWith("local_") -> null
            songId.startsWith("yt_") -> {
                val id = songId.substringAfter("yt_")
                val streamUrl = youtubeExtractor.extractStreamUrl(id)
                // Returning a song shell with the stream URL
                Song(id = songId, title = "", artist = "", image = "", audioUrl = streamUrl)
            }
            else -> saavnService.getSongDetails(songId)
        }
    }
}
