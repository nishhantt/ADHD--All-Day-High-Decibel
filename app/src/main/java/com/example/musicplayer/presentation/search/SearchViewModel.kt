package com.example.musicplayer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.models.Video
import com.example.musicplayer.domain.usecase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase
) : ViewModel() {

    private val _results = MutableStateFlow<List<Video>>(emptyList())
    val results: StateFlow<List<Video>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var currentJob: Job? = null

    fun search(query: String) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _loading.value = true
            searchUseCase.execute(query).collectLatest { res ->
                res.onSuccess { list -> _results.value = list }
                res.onFailure { _results.value = emptyList() }
                _loading.value = false
            }
        }
    }
}
