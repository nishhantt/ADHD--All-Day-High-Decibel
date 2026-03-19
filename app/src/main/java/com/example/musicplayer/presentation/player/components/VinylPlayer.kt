package com.example.musicplayer.presentation.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.R

@Composable
fun VinylPlayer(
    imageUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var currentRotation by remember { mutableStateOf(0f) }
    val rotation = rememberInfiniteTransition(label = "vinyl_rotation")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = durationBasedAnimation(isPlaying),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f),
        contentAlignment = Alignment.Center
    ) {
        // Main Vinyl Disc
        Image(
            painter = painterResource(id = R.drawable.vinyl_disc),
            contentDescription = "Vinyl Record",
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isPlaying) angle else currentRotation),
            contentScale = ContentScale.Fit
        )

        // Center Album Art Label
        Box(
            modifier = Modifier
                .fillMaxSize(0.38f)
                .clip(CircleShape)
                .rotate(if (isPlaying) angle else currentRotation)
        ) {
            val placeholder = rememberVectorPainter(Icons.Default.MusicNote)
            AsyncImage(
                model = imageUrl,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder
            )
        }
        
        // Small Center Hole
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .align(Alignment.Center)
        )
    }
    
    // Save rotation when paused
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            currentRotation = angle
        }
    }
}

private fun durationBasedAnimation(isPlaying: Boolean): TweenSpec<Float> {
    return tween(
        durationMillis = if (isPlaying) 3000 else Int.MAX_VALUE,
        easing = LinearEasing
    )
}
