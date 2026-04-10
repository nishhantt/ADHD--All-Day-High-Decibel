package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.musicplayer.domain.models.Song
import com.example.musicplayer.presentation.search.SearchScreen
import com.example.musicplayer.presentation.player.PlayerScreen
import com.example.musicplayer.presentation.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        setContent {
            MaterialTheme(colors = darkColors()) {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var suggestedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var newSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var isLoadingNewSongs by remember { mutableStateOf(false) }

    val suggestedSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val newSongsSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    fun loadSuggestedSongs() {
        isLoadingSuggestions = true
        scope.launch {
            val lastPlayedId = playerViewModel.currentSong.value?.id
            suggestedSongs = if (lastPlayedId != null) {
                playerViewModel.musicRepository.getRecommendations(lastPlayedId)
            } else {
                playerViewModel.musicRepository.getChart().take(10)
            }
            isLoadingSuggestions = false
        }
    }
    
    fun loadNewSongs() {
        isLoadingNewSongs = true
        scope.launch {
            newSongs = playerViewModel.musicRepository.getChart()
            isLoadingNewSongs = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalDrawer(
            drawerState = drawerState,
            drawerContent = {
                MainSidebarContent(
                    onSuggestedClick = {
                        scope.launch { drawerState.close() }
                        loadSuggestedSongs()
                        scope.launch { suggestedSheetState.show() }
                    },
                    onNewSongsClick = {
                        scope.launch { drawerState.close() }
                        loadNewSongs()
                        scope.launch { newSongsSheetState.show() }
                    },
                    onLikedClick = { scope.launch { drawerState.close() } },
                    onLocalFilesClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("search?query=local_files")
                    },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            NavHost(navController = navController, startDestination = "player") {
                composable("player") {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onOpenSidebar = { scope.launch { drawerState.open() } },
                        onSearch = { navController.navigate("search") }
                    )
                }

                composable(
                    "search?query={query}",
                    arguments = listOf(
                        androidx.navigation.navArgument("query") { 
                            type = androidx.navigation.NavType.StringType
                            nullable = true 
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query")
                    SearchScreen(
                        initialQuery = query,
                        onSongSelected = { song, playlist ->
                            playerViewModel.playSong(song, playlist)
                            navController.navigate("player") {
                                popUpTo("player") { inclusive = true }
                            }
                        },
                        onArtistSelected = { artist ->
                            navController.navigate("artist/${artist.id}?name=${artist.name}&img=${android.net.Uri.encode(artist.image)}")
                        },
                        onAlbumSelected = { album ->
                            navController.navigate("album/${album.id}?title=${album.title}&img=${android.net.Uri.encode(album.image)}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    "artist/{id}?name={name}&img={img}",
                    arguments = listOf(
                        androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("img") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val img = backStackEntry.arguments?.getString("img") ?: ""
                    com.example.musicplayer.presentation.detail.DetailScreen(
                        title = name,
                        imageUrl = img,
                        type = "ARTIST",
                        id = id,
                        onBack = { navController.popBackStack() },
                        onSongClick = { song, playlist ->
                            playerViewModel.playSong(song, playlist)
                            navController.navigate("player") { popUpTo("player") { inclusive = true } }
                        }
                    )
                }

                composable(
                    "album/{id}?title={title}&img={img}",
                    arguments = listOf(
                        androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("img") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    val img = backStackEntry.arguments?.getString("img") ?: ""
                    com.example.musicplayer.presentation.detail.DetailScreen(
                        title = title,
                        imageUrl = img,
                        type = "ALBUM",
                        id = id,
                        onBack = { navController.popBackStack() },
                        onSongClick = { song, playlist ->
                            playerViewModel.playSong(song, playlist)
                            navController.navigate("player") { popUpTo("player") { inclusive = true } }
                        }
                    )
                }
            }
        }

        ModalBottomSheetLayout(
            sheetState = suggestedSheetState,
            sheetContent = {
                SuggestedSongsContent(
                    songs = suggestedSongs,
                    isLoading = isLoadingSuggestions,
                    onRefresh = { loadSuggestedSongs() },
                    onSongClick = { song ->
                        playerViewModel.playSong(song, suggestedSongs)
                        scope.launch { suggestedSheetState.hide() }
                    },
                    onDismiss = { scope.launch { suggestedSheetState.hide() } }
                )
            },
            sheetBackgroundColor = Color(0xFF1A1A1A),
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {}

        ModalBottomSheetLayout(
            sheetState = newSongsSheetState,
            sheetContent = {
                NewSongsContent(
                    songs = newSongs,
                    isLoading = isLoadingNewSongs,
                    onRefresh = { loadNewSongs() },
                    onSongClick = { song ->
                        playerViewModel.playSong(song, newSongs)
                        scope.launch { newSongsSheetState.hide() }
                    },
                    onDismiss = { scope.launch { newSongsSheetState.hide() } }
                )
            },
            sheetBackgroundColor = Color(0xFF1A1A1A),
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {}
    }
}

@Composable
fun MainSidebarContent(
    onSuggestedClick: () -> Unit,
    onNewSongsClick: () -> Unit,
    onLikedClick: () -> Unit,
    onLocalFilesClick: () -> Unit,
    onClose: () -> Unit
) {
    val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentTime) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        Text(
            text = greeting,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "ADHD",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "All Day High Decibel",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SidebarMenuItem("Suggested For You", Icons.Default.AutoAwesome, onSuggestedClick)
        SidebarMenuItem("New Songs", Icons.Default.NewReleases, onNewSongsClick)
        SidebarMenuItem("Liked Songs", Icons.Default.Favorite, onLikedClick)
        SidebarMenuItem("Local Files", Icons.Default.Folder, onLocalFilesClick)

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onClose) {
            Text("Close", color = Color.Gray)
        }
    }
}

@Composable
private fun SidebarMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text, 
            color = Color.White, 
            fontSize = 16.sp
        )
    }
}

@Composable
fun SuggestedSongsContent(
    songs: List<Song>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSongClick: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Suggested For You",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No suggestions yet", color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Play some songs first!", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(songs) { song ->
                    SongRow(song = song, onClick = { onSongClick(song) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NewSongsContent(
    songs: List<Song>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSongClick: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "New Songs",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NewReleases, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unable to load charts", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(songs) { song ->
                    SongRow(song = song, onClick = { onSongClick(song) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.image,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            "Play",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
