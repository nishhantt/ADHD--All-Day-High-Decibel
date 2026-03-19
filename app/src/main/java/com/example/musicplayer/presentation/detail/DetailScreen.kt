package com.example.musicplayer.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musicplayer.domain.models.Song

@Composable
fun DetailScreen(
    title: String,
    imageUrl: String,
    type: String, // "ARTIST" or "ALBUM"
    id: String,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(id) {
        if (type == "ALBUM") viewModel.loadAlbum(id) else viewModel.loadArtist(id)
    }

    Scaffold(
        backgroundColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Section
            item {
                DetailHeader(title, imageUrl, type)
            }

            // Controls
            item {
                ActionButtons(
                    onPlay = { if (songs.isNotEmpty()) onSongClick(songs[0], songs) },
                    onShuffle = { if (songs.isNotEmpty()) onSongClick(songs.shuffled()[0], songs.shuffled()) }
                )
            }

            // Tracklist
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFA2D48))
                    }
                }
            } else {
                items(songs) { song ->
                    DetailSongItem(song = song, onClick = { onSongClick(song, songs) })
                }
            }
        }
    }
}

@Composable
fun DetailHeader(title: String, imageUrl: String, type: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .clip(if (type == "ARTIST") CircleShape else RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = if (type == "ARTIST") "Artist" else "Album",
            color = Color(0xFFFA2D48),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ActionButtons(onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onPlay,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFFA2D48))
            Spacer(Modifier.width(8.dp))
            Text("Play", color = Color.White)
        }
        
        Button(
            onClick = onShuffle,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Shuffle, null, tint = Color(0xFFFA2D48))
            Spacer(Modifier.width(8.dp))
            Text("Shuffle", color = Color.White)
        }
    }
}

@Composable
fun DetailSongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.image,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(song.artist, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
    }
}
