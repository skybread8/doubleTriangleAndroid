package io.codepassion.doubletriangle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Restrained frosted material inspired by Linear Mobile: low contrast, a fine
 * separator and almost no elevation. Content remains visually dominant.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    emphasized: Boolean = false,
): Modifier = composed {
    val light = MaterialTheme.colors.isLight
    val top = when {
        light && emphasized -> Color.White.copy(alpha = 0.84f)
        light -> Color.White.copy(alpha = 0.58f)
        emphasized -> Color.White.copy(alpha = 0.105f)
        else -> Color.White.copy(alpha = 0.060f)
    }
    val bottom = when {
        light && emphasized -> Color(0xFFF2F3F6).copy(alpha = 0.76f)
        light -> Color(0xFFE9EBEF).copy(alpha = 0.48f)
        emphasized -> Color(0xFF27272B).copy(alpha = 0.76f)
        else -> Color(0xFF202024).copy(alpha = 0.58f)
    }
    val outline = if (light) Color.Black.copy(alpha = 0.075f) else Color.White.copy(alpha = 0.105f)

    this.shadow(if (emphasized) 6.dp else 1.dp, shape, clip = false)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, outline, shape)
}

fun Modifier.liquidGlassBackground(): Modifier = composed {
    val colors = if (MaterialTheme.colors.isLight) {
        listOf(Color(0xFFF7F7F8), Color(0xFFF1F2F4), Color(0xFFF6F5F7))
    } else {
        listOf(Color(0xFF111113), Color(0xFF0E0E10), Color(0xFF151317))
    }
    this.background(Brush.linearGradient(colors))
}
