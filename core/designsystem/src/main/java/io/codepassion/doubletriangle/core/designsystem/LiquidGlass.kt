package io.codepassion.doubletriangle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Borderless frosted material inspired by Linear Mobile. Separation comes from
 * translucency and tonal contrast, avoiding halos or doubled outlines.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    emphasized: Boolean = false,
): Modifier = composed {
    val light = MaterialTheme.colors.isLight
    val top = when {
        light && emphasized -> Color.White.copy(alpha = 0.88f)
        light -> Color.White.copy(alpha = 0.62f)
        emphasized -> Color.White.copy(alpha = 0.11f)
        else -> Color.White.copy(alpha = 0.065f)
    }
    val bottom = when {
        light && emphasized -> Color(0xFFE9EAED).copy(alpha = 0.72f)
        light -> Color(0xFFE4E6E9).copy(alpha = 0.42f)
        emphasized -> Color(0xFF242428).copy(alpha = 0.70f)
        else -> Color(0xFF1D1D20).copy(alpha = 0.52f)
    }

    this.clip(shape).background(Brush.verticalGradient(listOf(top, bottom)))
}

fun Modifier.liquidGlassBackground(): Modifier = composed {
    val colors = if (MaterialTheme.colors.isLight) {
        listOf(Color(0xFFF7F7F8), Color(0xFFF1F2F4), Color(0xFFF6F5F7))
    } else {
        listOf(Color(0xFF111113), Color(0xFF0E0E10), Color(0xFF151317))
    }
    this.background(Brush.linearGradient(colors))
}
