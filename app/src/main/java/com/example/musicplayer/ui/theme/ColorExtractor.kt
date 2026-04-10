package com.example.musicplayer.ui.theme

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import kotlin.math.max
import kotlin.math.min

object ColorExtractor {
    
    data class Palette(
        val dominant: ComposeColor,
        val vibrant: ComposeColor,
        val darkVibrant: ComposeColor,
        val lightVibrant: ComposeColor,
        val muted: ComposeColor,
        val darkMuted: ComposeColor,
        val lightMuted: ComposeColor
    )
    
    fun extractPalette(bitmap: Bitmap): Palette {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val colorCounts = mutableMapOf<Int, Int>()
        val step = max(1, pixels.size / 1000)
        
        for (i in pixels.indices step step) {
            val color = pixels[i]
            if (Color.alpha(color) > 128) {
                val key = (Color.red(color) / 16) * 256 + (Color.green(color) / 16) * 16 + (Color.blue(color) / 16)
                colorCounts[key] = (colorCounts[key] ?: 0) + 1
            }
        }
        
        val sortedColors = colorCounts.entries.sortedByDescending { it.value }.take(50).map { entry ->
            val r = (entry.key / 256) * 16
            val g = ((entry.key % 256) / 16) * 16
            val b = (entry.key % 16) * 16
            Color.rgb(r, g, b)
        }.filter { color ->
            val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            luminance in 0.15..0.85
        }
        
        val dominant = if (sortedColors.isNotEmpty()) sortedColors.first() else Color.DKGRAY
        val vibrant = sortedColors.firstOrNull { isVibrant(it) } ?: dominant
        val darkVibrant = sortedColors.firstOrNull { isVibrant(it) && getLuminance(it) < 0.5 } ?: darken(dominant, 0.3f)
        val lightVibrant = sortedColors.firstOrNull { isVibrant(it) && getLuminance(it) > 0.5 } ?: lighten(dominant, 0.3f)
        val muted = sortedColors.firstOrNull { !isVibrant(it) } ?: desaturate(dominant, 0.5f)
        val darkMuted = sortedColors.firstOrNull { !isVibrant(it) && getLuminance(it) < 0.5 } ?: darken(dominant, 0.4f)
        val lightMuted = sortedColors.firstOrNull { !isVibrant(it) && getLuminance(it) > 0.5 } ?: lighten(dominant, 0.2f)
        
        return Palette(
            dominant = ComposeColor(dominant),
            vibrant = ComposeColor(vibrant),
            darkVibrant = ComposeColor(darkVibrant),
            lightVibrant = ComposeColor(lightVibrant),
            muted = ComposeColor(muted),
            darkMuted = ComposeColor(darkMuted),
            lightMuted = ComposeColor(lightMuted)
        )
    }
    
    private fun isVibrant(color: Int): Boolean {
        val maxC = maxOf(Color.red(color), Color.green(color), Color.blue(color))
        val minC = minOf(Color.red(color), Color.green(color), Color.blue(color))
        val saturation = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
        return saturation > 0.5f
    }
    
    private fun getLuminance(color: Int): Float {
        return (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
    }
    
    private fun darken(color: Int, amount: Float): Int {
        val r = (Color.red(color) * (1 - amount)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1 - amount)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1 - amount)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
    
    private fun lighten(color: Int, amount: Float): Int {
        val r = min(255, Color.red(color) + ((255 - Color.red(color)) * amount).toInt())
        val g = min(255, Color.green(color) + ((255 - Color.green(color)) * amount).toInt())
        val b = min(255, Color.blue(color) + ((255 - Color.blue(color)) * amount).toInt())
        return Color.rgb(r, g, b)
    }
    
    private fun desaturate(color: Int, amount: Float): Int {
        val gray = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)).toInt()
        val r = (Color.red(color) + (gray - Color.red(color)) * amount).toInt()
        val g = (Color.green(color) + (gray - Color.green(color)) * amount).toInt()
        val b = (Color.blue(color) + (gray - Color.blue(color)) * amount).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    
    fun generateFallbackPalette(imageUrl: String?): Palette {
        return Palette(
            dominant = ComposeColor(0xFF6366F1),
            vibrant = ComposeColor(0xFF818CF8),
            darkVibrant = ComposeColor(0xFF4338CA),
            lightVibrant = ComposeColor(0xFFA5B4FC),
            muted = ComposeColor(0xFF6B7280),
            darkMuted = ComposeColor(0xFF374151),
            lightMuted = ComposeColor(0xFF9CA3AF)
        )
    }
}
