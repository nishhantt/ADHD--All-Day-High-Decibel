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
    private val youtubeService: com.example.musicplayer.network.YouTubeSearchService,
    private val soundCloudService: com.example.musicplayer.network.SoundCloudService,
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
        val scSongs = try { soundCloudService.search(query) } catch (e: Exception) { emptyList() }

        // Merge and de-duplicate (prefer Saavn -> SC -> YT)
        val mergedSongs = (saavnResults.songs + scSongs + ytSongs).distinctBy { "${it.title}-${it.artist}".lowercase() }
        
        saavnResults.copy(
            songs = mergedSongs,
            topResult = saavnResults.topResult ?: mergedSongs.firstOrNull()
        )
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val saavn = saavnService.searchSongs(query)
        if (saavn.isEmpty()) youtubeService.searchSongs(query) else saavn
    }

    suspend fun getAlbumSongs(albumId: String) = saavnService.getAlbumDetails(albumId)
    suspend fun getArtistSongs(artistId: String) = saavnService.getArtistSongs(artistId)
    suspend fun getSongDetails(songId: String): Song? {
        return when {
            songId.startsWith("local_") -> null // Local songs already have path in audioUrl
            songId.startsWith("sc_") -> {
                val id = songId.substringAfter("sc_")
                val streamUrl = soundCloudService.resolveStreamUrl(id)
                // We'd ideally need the metadata too, but if it's already in the Song object from search, we're good.
                // For simplicity, we return a Song with just the streamUrl.
                Song(id = songId, title = "Resolving...", artist = "", image = "", audioUrl = streamUrl)
            }
            songId.startsWith("yt_") -> {
                // YT URLs are already set to the proxy in YouTubeSearchService
                null
            }
            else -> saavnService.getSongDetails(songId)
        }
    }
}
