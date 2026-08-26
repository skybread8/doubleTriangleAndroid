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
        light && emphasized -> Color(0xFFFFFFFF)
        light -> Color(0xFFFFFEFF).copy(alpha = 0.94f)
        emphasized -> Color.White.copy(alpha = 0.11f)
        else -> Color.White.copy(alpha = 0.065f)
    }
    val bottom = when {
        light && emphasized -> Color(0xFFFDFBFF)
        light -> Color(0xFFF8F6FA).copy(alpha = 0.92f)
        emphasized -> Color(0xFF242428).copy(alpha = 0.70f)
        else -> Color(0xFF1D1D20).copy(alpha = 0.52f)
    }

    this.clip(shape).background(Brush.verticalGradient(listOf(top, bottom)))
}

fun Modifier.liquidGlassBackground(): Modifier = composed {
    val colors = if (MaterialTheme.colors.isLight) {
        listOf(Color(0xFFFAF8FF), Color(0xFFF7F5FC), Color(0xFFFAF8FF))
    } else {
        listOf(Color(0xFF111113), Color(0xFF0E0E10), Color(0xFF151317))
    }
    this.background(Brush.linearGradient(colors))
}
