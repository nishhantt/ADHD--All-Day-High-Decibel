package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Colors for the 3D effect
val NeumorphicBackground = Color(0xFF121212) // Darker background
val NeumorphicButtonBackground = Color(0xFF1C1C1C) // Lighter button surface
val NeumorphicDarkShadow = Color(0xFF080808) // Deep shadow
val NeumorphicGlow = Color(0xFF252525) // Subtle glow for all-around effect

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .neumorphicShadow(shape = CircleShape, elevation = 6.dp)
            .clip(CircleShape)
            .background(NeumorphicButtonBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neumorphicShadow(shape = RoundedCornerShape(cornerRadius), elevation = 4.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(NeumorphicButtonBackground),
        content = content
    )
}

fun Modifier.neumorphicShadow(
    shape: androidx.compose.ui.graphics.Shape,
    elevation: Dp = 4.dp
): Modifier = this.drawBehind {
    val shadowColorDark = NeumorphicDarkShadow.toArgb()
    val glowColor = NeumorphicGlow.toArgb()
    
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        val outline = shape.createOutline(size, layoutDirection, this)
        
        // 1. Centered Glow (All-around)
        frameworkPaint.color = glowColor
        frameworkPaint.setShadowLayer(
            elevation.toPx() * 1.5f,
            0f,
            0f,
            glowColor
        )
        canvas.drawOutline(outline, paint)

        // 2. Primary Dark Shadow (Bottom Right)
        frameworkPaint.color = shadowColorDark
        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            elevation.toPx(),
            elevation.toPx(),
            shadowColorDark
        )
        canvas.drawOutline(outline, paint)
    }
}
