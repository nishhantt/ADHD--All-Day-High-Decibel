package com.example.musicplayer.presentation.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicplayer.player.ExoPlayerManager
import com.example.musicplayer.presentation.player.components.QueueBottomSheet
import com.example.musicplayer.ui.components.DynamicBackground
import com.example.musicplayer.ui.components.NeumorphicButton
import com.example.musicplayer.ui.components.neumorphicShadow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onOpenSidebar: () -> Unit,
    onSearch: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            QueueBottomSheet(
                songs = playlist,
                currentSongId = currentSong?.id ?: "",
                onSongClick = { viewModel.playSong(it, playlist) }
            )
        },
        sheetPeekHeight = 0.dp,
        sheetBackgroundColor = Color(0xFF1A1A1A),
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        DynamicBackground(
            imageUrl = currentSong?.image,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenSidebar) {
                        Icon(Icons.Default.Menu, null, tint = Color.White)
                    }
                    
                    Text(
                        text = "NOW PLAYING",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation = rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(15000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .neumorphicShadow(CircleShape, elevation = 16.dp)
                    )

                    AsyncImage(
                        model = currentSong?.image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .graphicsLayer {
                                if (isPlaying) {
                                    rotationZ = rotation.value
                                }
                            }
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong?.title ?: "No Song Selected",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playbackPosition.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(playbackPosition), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    NeumorphicButton(
                        onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                        size = 56.dp,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Default.QueueMusic, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                    
                    NeumorphicButton(
                        onClick = { viewModel.seekBackward() },
                        size = 40.dp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 100.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }

                    NeumorphicButton(
                        onClick = { viewModel.seekForward() },
                        size = 40.dp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 48.dp)
                    ) {
                        Icon(Icons.Default.FastForward, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }

                    NeumorphicButton(
                        onClick = { viewModel.toggleRepeatMode() },
                        size = 40.dp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            when (repeatMode) {
                                ExoPlayerManager.REPEAT_MODE_OFF -> Icons.Default.Repeat
                                ExoPlayerManager.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            }, 
                            null, 
                            tint = if (repeatMode == ExoPlayerManager.REPEAT_MODE_ONE) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .neumorphicShadow(CircleShape, elevation = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { },
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.FavoriteBorder, null, tint = Color.White.copy(alpha = 0.7f))
                        }

                        IconButton(
                            onClick = { },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, null, tint = Color.White.copy(alpha = 0.7f))
                        }

                        IconButton(
                            onClick = { viewModel.previous() },
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = { viewModel.next() },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                                .clickable(enabled = !isLoading) { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF1A1A1A),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = Color(0xFF1A1A1A),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
