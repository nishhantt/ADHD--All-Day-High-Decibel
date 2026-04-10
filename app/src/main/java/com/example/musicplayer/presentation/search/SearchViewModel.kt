package com.example.musicplayer.presentation.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.models.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.musicplayer.data.BehaviorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val behaviorRepository: BehaviorRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow(com.example.musicplayer.domain.models.SearchResult())
    val searchResults: StateFlow<com.example.musicplayer.domain.models.SearchResult> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val recentSearches: StateFlow<List<String>> = behaviorRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var searchJob: Job? = null
    private var prefetchJob: Job? = null
    private var lastQuery = ""
    private val searchCache = mutableMapOf<String, com.example.musicplayer.domain.models.SearchResult>()
    private val urlCache = mutableMapOf<String, String?>()
    private val MIN_QUERY_LENGTH = 2
    private val DEBOUNCE_MS = 300L

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
            _isLoading.value = false
            return
        }

        val trimmedQuery = query.trim()
        
        if (trimmedQuery == lastQuery) return
        if (trimmedQuery.length < MIN_QUERY_LENGTH) return
        
        if (searchCache.containsKey(trimmedQuery)) {
            _searchResults.value = searchCache[trimmedQuery]!!
            prefetchUrls(searchCache[trimmedQuery]!!.songs)
            return
        }

        lastQuery = trimmedQuery
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            
            if (trimmedQuery != lastQuery) return@launch
            
            if (trimmedQuery != "local_files") {
                behaviorRepository.trackSearch(trimmedQuery)
            }
            
            _isLoading.value = true
            try {
                val songs = musicRepository.searchSongs(trimmedQuery)
                val result = com.example.musicplayer.domain.models.SearchResult(
                    songs = songs,
                    topResult = songs.firstOrNull()
                )
                searchCache[trimmedQuery] = result
                if (trimmedQuery == lastQuery) {
                    _searchResults.value = result
                    prefetchUrls(result.songs)
                }
            } catch (e: Exception) {
                if (trimmedQuery == lastQuery) {
                    _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
                }
            } finally {
                if (trimmedQuery == lastQuery) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun prefetchUrls(songs: List<Song>) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val toPrefetch = songs.take(3)
            toPrefetch.forEach { song ->
                val cacheKey = "${song.id}|${song.artist}|${song.title}"
                if (cacheKey !in urlCache) {
                    launch {
                        try {
                            val url = musicRepository.getStreamUrlForSong(song)
                            urlCache[cacheKey] = url
                            Log.d("SearchViewModel", "Prefetched: ${song.title}")
                        } catch (e: Exception) {
                            musicRepository.prefetchUrl(song)
                            urlCache[cacheKey] = null
                        }
                    }
                }
            }
        }
    }

    fun getCachedUrl(song: Song): String? {
        val cacheKey = "${song.id}|${song.artist}|${song.title}"
        return urlCache[cacheKey]
    }

    fun onQueryChanged(query: String) {
        search(query)
    }
    
    fun clearCache() {
        searchCache.clear()
        urlCache.clear()
    }
}
