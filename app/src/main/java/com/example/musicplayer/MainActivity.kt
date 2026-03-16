package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.example.musicplayer.presentation.search.SearchScreen
import com.example.musicplayer.presentation.player.PlayerScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notification permission on Android 13+
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
        NavHost(navController = navController, startDestination = "player") {
            // Home player screen (no specific video yet)
            composable("player") {
                PlayerScreen(
                    videoId = "",
                    onBack = { /* no-op, root */ },
                    onSearch = { navController.navigate("search") }
                )
            }

            composable("search") {
                SearchScreen(
                    onPlayVideo = { videoId ->
                        navController.navigate("player/$videoId") {
                            popUpTo("player") { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("player/{videoId}") { backStack ->
                val vid = backStack.arguments?.getString("videoId") ?: ""
                PlayerScreen(
                    videoId = vid,
                    onBack = { navController.popBackStack() },
                    onSearch = { navController.navigate("search") }
                )
            }
        }
    }
}
