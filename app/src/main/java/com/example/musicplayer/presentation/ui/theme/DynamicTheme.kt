package com.example.musicplayer.presentation.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

@Stable
class DynamicThemeState(
    initialPrimary: Color = Color(0xFF1C1C1E),
    initialSecondary: Color = Color(0xFF2C2C2E)
) {
    var primaryColor by mutableStateOf(initialPrimary)
    var secondaryColor by mutableStateOf(initialSecondary)

    fun updateFromBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        val palette = Palette.from(bitmap).generate()
        
        // Extract dominant colors, fallback to dark Apple Music grey
        val dominantColor = palette.getDominantColor(0xFF1C1C1E.toInt())
        val mutedColor = palette.getMutedColor(0xFF2C2C2E.toInt())
        
        primaryColor = Color(dominantColor)
        secondaryColor = Color(mutedColor)
    }
}

@Composable
fun rememberDynamicThemeState(): DynamicThemeState {
    return remember { DynamicThemeState() }
}
