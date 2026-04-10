package com.example.musicplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.musicplayer.ui.theme.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DefaultDarkVibrant = Color(0xFF1A1A2E)
private val DefaultVibrant = Color(0xFF4A4A6A)
private val DefaultLightVibrant = Color(0xFF6A6A8A)
private val DefaultMuted = Color(0xFF2A2A3E)

@Composable
fun DynamicBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var palette by remember { mutableStateOf<ColorExtractor.Palette?>(null) }
    var hasLoadedPalette by remember { mutableStateOf(false) }
    
    val darkVibrant by animateColorAsState(
        targetValue = if (hasLoadedPalette && palette != null) palette!!.darkVibrant else DefaultDarkVibrant,
        animationSpec = tween(800),
        label = "darkVibrant"
    )
    
    val vibrant by animateColorAsState(
        targetValue = if (hasLoadedPalette && palette != null) palette!!.vibrant else DefaultVibrant,
        animationSpec = tween(800),
        label = "vibrant"
    )
    
    val lightVibrant by animateColorAsState(
        targetValue = if (hasLoadedPalette && palette != null) palette!!.lightVibrant else DefaultLightVibrant,
        animationSpec = tween(800),
        label = "lightVibrant"
    )
    
    val muted by animateColorAsState(
        targetValue = if (hasLoadedPalette && palette != null) palette!!.muted else DefaultMuted,
        animationSpec = tween(800),
        label = "muted"
    )
    
    LaunchedEffect(imageUrl) {
        if (!imageUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .size(50, 50)
                        .build()
                    
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        bitmap?.let {
                            palette = ColorExtractor.extractPalette(it)
                            hasLoadedPalette = true
                        }
                    }
                } catch (e: Exception) {
                    hasLoadedPalette = true
                }
            }
        } else {
            hasLoadedPalette = true
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkVibrant.copy(alpha = 1f),
                        muted.copy(alpha = 0.8f),
                        darkVibrant.copy(alpha = 0.9f),
                        Color(0xFF0A0A15).copy(alpha = 1f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            vibrant.copy(alpha = 0.3f),
                            lightVibrant.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 1500f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            muted.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        startY = 300f,
                        endY = 800f
                    )
                )
        )
        content()
    }
}
