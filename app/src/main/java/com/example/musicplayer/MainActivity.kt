package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.example.musicplayer.presentation.search.SearchScreen
import com.example.musicplayer.presentation.player.PlayerScreen
import com.example.musicplayer.presentation.player.PlayerViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        setContent {
            androidx.compose.material.MaterialTheme(colors = androidx.compose.material.darkColors()) {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        ModalDrawer(
            drawerState = drawerState,
            drawerContent = {
                com.example.musicplayer.ui.components.SidebarContent(
                    onSuggestedClick = { /* Navigate or filter */ },
                    onLikedClick = { /* Navigate to Liked Songs */ },
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

                composable("search") {
                    SearchScreen(
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
    }
}
