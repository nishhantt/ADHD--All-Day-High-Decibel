package com.example.musicplayer.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    videoId: String,
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState(initial = PlayerUiState.Idle)
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(videoId) {
        if (videoId.isNotBlank()) viewModel.playVideo(videoId)
    }

    // Vinyl rotation
    val rotation = remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            rotation.value = (rotation.value + 0.5f) % 360f
            delay(16L)
        }
    }

    val currentTitle = if (state is PlayerUiState.Playing) {
        (state as PlayerUiState.Playing).media.title ?: "Unknown"
    } else "No Track"
    val thumb = if (state is PlayerUiState.Playing) {
        (state as PlayerUiState.Playing).media.thumbnailUrl
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: glassy search bar (no back, no menu)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x33FFFFFF))
                .clickable { onSearch() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search songs…",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        // Middle: rotating vinyl with cover art filling the label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(rotationZ = rotation.value)
                    .background(Color.DarkGray, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = currentTitle,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                    )
                }
            }
        }

        // Song info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (state is PlayerUiState.Loading) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Loading…", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            if (state is PlayerUiState.Error) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Can’t play right now. Tap Play to retry.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Progress bar + times
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            val position = when (state) {
                is PlayerUiState.Playing -> (state as PlayerUiState.Playing).positionMs.toFloat()
                else -> 0f
            }
            val duration = viewModel.exoPlayerPositionMaxMs().toFloat().coerceAtLeast(1f)
            Slider(
                value = position.coerceIn(0f, duration),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMs(position.toLong()), color = Color.White, fontSize = 12.sp)
                Text(formatMs(duration.toLong()), color = Color.White, fontSize = 12.sp)
            }
        }

        // Bottom controls: prev, play/pause, next, repeat (at far right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previous() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }
            IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
                if (isPlaying) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = Color.White)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000) % 60
    val m = (ms / 1000) / 60
    return String.format("%d:%02d", m, s)
}
