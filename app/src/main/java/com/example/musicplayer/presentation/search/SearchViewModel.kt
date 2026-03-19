package com.example.musicplayer.presentation.search

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

    private val _searchResults = MutableStateFlow<com.example.musicplayer.domain.models.SearchResult>(com.example.musicplayer.domain.models.SearchResult())
    val searchResults: StateFlow<com.example.musicplayer.domain.models.SearchResult> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val recentSearches: StateFlow<List<String>> = behaviorRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
            return
        }

        viewModelScope.launch {
            behaviorRepository.trackSearch(query)
            _isLoading.value = true
            try {
                _searchResults.value = musicRepository.globalSearch(query)
            } catch (e: Exception) {
                _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
            _isLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _isLoading.value = true
            try {
                _searchResults.value = musicRepository.globalSearch(query)
            } catch (e: Exception) {
                _searchResults.value = com.example.musicplayer.domain.models.SearchResult()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
