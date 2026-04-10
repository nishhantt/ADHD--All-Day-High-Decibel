package com.example.musicplayer.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.domain.models.SearchResult

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.example.musicplayer.ui.components.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: com.example.musicplayer.presentation.player.PlayerViewModel = hiltViewModel(),
    initialQuery: String? = null,
    onSongSelected: (Song, List<Song>) -> Unit,
    onArtistSelected: (com.example.musicplayer.domain.models.Artist) -> Unit = {},
    onAlbumSelected: (com.example.musicplayer.domain.models.Album) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var query by remember { mutableStateOf(initialQuery ?: "") }
    
    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.search(initialQuery)
        }
    }
    
    val results by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeumorphicButton(onClick = onBack, size = 48.dp) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            NeumorphicCard(
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { newQuery ->
                            query = newQuery
                            viewModel.search(newQuery)
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text("Search...", color = Color.DarkGray)
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { 
                            viewModel.search(query)
                            focusManager.clearFocus() 
                        })
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close, 
                            null, 
                            tint = Color.Gray, 
                            modifier = Modifier.clickable { 
                                query = ""
                                viewModel.search("")
                            }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.White,
                backgroundColor = Color.Transparent
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (query.isEmpty() && recentSearches.isNotEmpty()) {
                item { SectionHeader("Recent Searches") }
                items(recentSearches) { search ->
                    RecentSearchItem(text = search) {
                        query = search
                        viewModel.search(search)
                    }
                }
            }

            results.let { res ->
                res.topResult?.let { song ->
                    item { SectionHeader("Top Result") }
                    item {
                        TopResultItem(
                            song = song, 
                            onClick = { onSongSelected(song, listOf(song)) },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) }
                        )
                    }
                }

                if (res.songs.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    items(res.songs) { song ->
                        SongItem(
                            song = song, 
                            onClick = { onSongSelected(song, res.songs) },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) }
                        )
                    }
                }

                if (res.artists.isNotEmpty()) {
                    item { SectionHeader("Artists") }
                    items(res.artists) { artist ->
                        ArtistItem(artist = artist, onClick = { onArtistSelected(artist) })
                    }
                }

                if (res.albums.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    items(res.albums) { album ->
                        AlbumItem(album = album, onClick = { onAlbumSelected(album) })
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Close, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp)) // History icon style
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun TopResultItem(
    song: Song, 
    onClick: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.image,
                contentDescription = null,
                modifier = Modifier.size(92.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(song.artist, color = Color.Gray, fontSize = 16.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("SONG", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NeumorphicBackground)
                ) {
                    DropdownMenuItem(onClick = { onPlayNext(song); showMenu = false }) {
                        Text("Play Next", color = Color.White)
                    }
                    DropdownMenuItem(onClick = { onAddToQueue(song); showMenu = false }) {
                        Text("Add to Queue", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song, 
    onClick: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.image,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(song.artist, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
        
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(NeumorphicBackground)
            ) {
                DropdownMenuItem(onClick = { 
                    onPlayNext(song)
                    showMenu = false
                }) {
                    Text("Play Next", color = Color.White)
                }
                DropdownMenuItem(onClick = { 
                    onAddToQueue(song)
                    showMenu = false
                }) {
                    Text("Add to Queue", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ArtistItem(artist: com.example.musicplayer.domain.models.Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.image,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(artist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AlbumItem(album: com.example.musicplayer.domain.models.Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.image,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(album.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${album.artist} • Album", color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
    }
}
