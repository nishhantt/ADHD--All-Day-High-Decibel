package com.example.musicplayer.data

import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.network.SaavnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val saavnService: SaavnService,
    private val youtubeService: com.example.musicplayer.network.YouTubeSearchService
) {
    suspend fun globalSearch(query: String): com.example.musicplayer.domain.models.SearchResult = withContext(Dispatchers.IO) {
        val saavnResults = saavnService.globalSearch(query)
        if (saavnResults.songs.isEmpty()) {
            // Fallback to YouTube
            val ytSongs = youtubeService.searchSongs(query)
            return@withContext com.example.musicplayer.domain.models.SearchResult(
                songs = ytSongs,
                topResult = ytSongs.firstOrNull()
            )
        }
        saavnResults
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val saavn = saavnService.searchSongs(query)
        if (saavn.isEmpty()) youtubeService.searchSongs(query) else saavn
    }

    suspend fun getAlbumSongs(albumId: String) = saavnService.getAlbumDetails(albumId)
    suspend fun getArtistSongs(artistId: String) = saavnService.getArtistSongs(artistId)
    suspend fun getSongDetails(songId: String) = saavnService.getSongDetails(songId)
}
