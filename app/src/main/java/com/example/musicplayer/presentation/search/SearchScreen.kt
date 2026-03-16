package com.example.musicplayer.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musicplayer.domain.models.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlayVideo: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val queryState = remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        TextField(
            value = queryState.value,
            onValueChange = {
                queryState.value = it
                scope.launch {
                    delay(400)
                    if (it == queryState.value && it.isNotBlank()) viewModel.search(it)
                }
            },
            placeholder = { Text("Search songs…", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color(0xFF1E1E1E),
                textColor = Color.White,
                placeholderColor = Color.Gray,
                cursorColor = Color.White,
                focusedIndicatorColor = Color(0xFF1DB954),
                unfocusedIndicatorColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            results.forEach { video ->
                SearchItem(video = video, onClick = { onPlayVideo(video.id) })
            }
        }
    }
}

@Composable
fun SearchItem(video: Video, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clickable { onClick() }) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(video.title, color = Color.White)
        }
    }
}
