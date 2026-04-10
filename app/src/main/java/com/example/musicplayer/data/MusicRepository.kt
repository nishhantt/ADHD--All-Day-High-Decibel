package com.example.musicplayer.data

import com.example.musicplayer.domain.models.Album
import com.example.musicplayer.domain.models.Artist
import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.network.PhoneBackendService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val phoneBackendService: PhoneBackendService,
    private val localMusicRepository: LocalMusicRepository
) {
    suspend fun searchSongs(query: String): List<Song> {
        return phoneBackendService.searchSongs(query)
    }

    suspend fun getSongDetails(songId: String): Song? {
        return null
    }

    suspend fun getArtistDetails(artistId: String): Artist? {
        return null
    }

    suspend fun getAlbumDetails(albumId: String): Album? {
        return null
    }

    suspend fun getTrendingSongs(): List<Song> {
        return phoneBackendService.getChart()
    }

    suspend fun getNewReleases(): List<Song> {
        return phoneBackendService.getChart()
    }

    suspend fun getRecommendedSongs(): List<Song> {
        return phoneBackendService.getChart()
    }

    suspend fun getRecentSearches(): List<String> {
        return emptyList()
    }

    suspend fun getStreamUrl(songId: String, artist: String, title: String): String? {
        return phoneBackendService.getStreamUrl(songId, artist, title)
    }

    suspend fun getStreamUrlForSong(song: Song): String? {
        return phoneBackendService.getStreamUrl(song.id, song.artist, song.title)
    }
    
    suspend fun getCachedUrl(songId: String, artist: String, title: String): String? {
        return phoneBackendService.getStreamUrl(songId, artist, title)
    }

    suspend fun prefetchUrls(songs: List<Song>) {
        phoneBackendService.prefetchUrls(songs)
    }

    suspend fun prefetchUrl(song: Song) {
        phoneBackendService.prefetchUrls(listOf(song))
    }

    suspend fun getRecommendations(songId: String): List<Song> {
        return phoneBackendService.getRecommendations(songId)
    }

    suspend fun getUpNext(songId: String, limit: Int = 10): List<Song> {
        return phoneBackendService.getUpNext(songId, limit)
    }
    
    suspend fun updateTransition(previousSongId: String?, currentSongId: String) {
        phoneBackendService.updateTransition(previousSongId, currentSongId)
    }

    suspend fun getChart(): List<Song> {
        return phoneBackendService.getChart()
    }

    suspend fun getAlbumSongs(albumId: String): List<Song> {
        return emptyList()
    }

    suspend fun getArtistSongs(artistId: String): List<Song> {
        return emptyList()
    }

    fun getLocalSongs(): List<Song> {
        return localMusicRepository.getLocalSongs()
    }
    
    fun clearUrlCache() {
        phoneBackendService.clearUrlCache()
    }
}
