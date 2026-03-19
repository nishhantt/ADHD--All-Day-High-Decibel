package com.example.musicplayer.presentation.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musicplayer.presentation.player.components.QueueBottomSheet
import com.example.musicplayer.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerScreen(
    onOpenSidebar: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState(initial = PlayerUiState.Idle)
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val duration = viewModel.getDuration()
    
    val currentSongResource = (state as? PlayerUiState.Playing)?.song
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            QueueBottomSheet(
                songs = playlist,
                currentSongId = currentSongResource?.id ?: "",
                onSongClick = { song ->
                    viewModel.playSong(song, playlist)
                    scope.launch { sheetState.hide() }
                }
            )
        },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetBackgroundColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeumorphicBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicButton(onClick = onOpenSidebar, size = 48.dp) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.Gray)
                    }
                    
                    Text(
                        text = "Now Playing",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    NeumorphicButton(onClick = onSearch, size = 48.dp) {
                        Icon(Icons.Default.Search, "Search", tint = Color.Gray)
                    }
                }

                // Full Circular Artwork
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularPlayer(
                        imageUrl = currentSongResource?.image ?: "",
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Song Info Card
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(80.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = currentSongResource?.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = currentSongResource?.title ?: "Search a song and play",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = currentSongResource?.artist ?: "",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // "Follow" button removed per user request
                    }
                }

                // Progress
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sliderValue = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    Slider(
                        value = sliderValue,
                        onValueChange = { percent ->
                            if (duration > 0) viewModel.seekTo((percent * duration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = formatTime(currentPosition), color = Color.Gray, fontSize = 12.sp)
                        Text(text = formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // 3D Control Hub
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    // Secondary Buttons (Corners)
                    NeumorphicButton(
                        onClick = { /* Menu */ },
                        size = 56.dp, 
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray)
                    }
                    
                    NeumorphicButton(
                        onClick = { scope.launch { sheetState.show() } },
                        size = 56.dp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.PlaylistPlay, null, tint = Color.Gray)
                    }

                    NeumorphicButton(
                        onClick = { /* Repeat */ },
                        size = 56.dp,
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Icon(Icons.Default.Repeat, null, tint = Color.Gray)
                    }

                    NeumorphicButton(
                        onClick = { /* Music */ },
                        size = 56.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.Gray)
                    }

                    // Central Control Circle
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .neumorphicShadow(CircleShape, elevation = 6.dp)
                            .background(NeumorphicBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Control elements in circle
                        // Top: Favorite/Heart
                        Icon(
                            Icons.Default.FavoriteBorder, 
                            null, 
                            tint = Color.Gray,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).size(24.dp).clickable { /* Liked */ }
                        )
                        
                        // Bottom: Volume/Speaker
                        Icon(
                            Icons.Default.VolumeUp, 
                            null, 
                            tint = Color.Gray,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(24.dp)
                        )

                        // Middle Row: Prev - Play/Pause - Next
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.Gray, modifier = Modifier.size(32.dp).clickable { viewModel.previous() })
                            
                            // Center Play/Pause in a smaller circle
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .neumorphicShadow(CircleShape, elevation = 2.dp)
                                    .background(NeumorphicBackground, CircleShape)
                                    .clickable { viewModel.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Icon(Icons.Default.SkipNext, null, tint = Color.Gray, modifier = Modifier.size(32.dp).clickable { viewModel.next() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularPlayer(
    imageUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "rotation_animation"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .graphicsLayer { rotationZ = if (isPlaying) rotation else 0f }
            .background(Color.Black)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
