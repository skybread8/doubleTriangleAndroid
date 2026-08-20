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

/** A lightweight, API 28-safe interpretation of a liquid-glass surface. */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    emphasized: Boolean = false,
): Modifier = composed {
    val light = MaterialTheme.colors.isLight
    val top = when {
        light && emphasized -> Color.White.copy(alpha = 0.90f)
        light -> Color.White.copy(alpha = 0.66f)
        emphasized -> Color.White.copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = 0.11f)
    }
    val bottom = when {
        light && emphasized -> Color(0xFFDCE7F2).copy(alpha = 0.72f)
        light -> Color(0xFFD7E3EE).copy(alpha = 0.46f)
        emphasized -> Color(0xFF64748B).copy(alpha = 0.18f)
        else -> Color(0xFF334155).copy(alpha = 0.13f)
    }
    val outline = if (light) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.30f)

    this.shadow(if (emphasized) 18.dp else 10.dp, shape, clip = false)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, Brush.linearGradient(listOf(outline, outline.copy(alpha = 0.18f))), shape)
}

fun Modifier.liquidGlassBackground(): Modifier = composed {
    val colors = if (MaterialTheme.colors.isLight) {
        listOf(Color(0xFFEAF4FF), Color(0xFFF7F6FB), Color(0xFFF3EAF7))
    } else {
        listOf(Color(0xFF202733), Color(0xFF111318), Color(0xFF241F2A))
    }
    this.background(Brush.linearGradient(colors))
}
