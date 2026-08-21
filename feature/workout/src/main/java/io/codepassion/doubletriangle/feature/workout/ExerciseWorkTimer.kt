package io.codepassion.doubletriangle.feature.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens

@Composable
internal fun ExerciseWorkTimer(seconds: Int, initialSeconds: Int, running: Boolean, onToggle: () -> Unit, onAddTime: () -> Unit) {
    val progress by animateFloatAsState((seconds.toFloat() / initialSeconds.coerceAtLeast(1)).coerceIn(0f, 1f), animationSpec = tween(1_000, easing = LinearEasing))
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        TimerButton("+", "+15s", false, onAddTime)
        Box(Modifier.padding(horizontal = 16.dp).size(132.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress, Modifier.fillMaxSize(), WildforceThemeTokens.accentGold, 8.dp, WildforceThemeTokens.textSecondary.copy(alpha = 0.12f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(seconds.toString().padStart(2, '0'), style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("RESTANTE", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        TimerButton(if (running) "Ⅱ" else "▶", if (running) "PAUSA" else "INICIAR", true, onToggle)
    }
}

@Composable
private fun TimerButton(glyph: String, label: String, primary: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.size(62.dp).background(if (primary) WildforceThemeTokens.accentGold.copy(alpha = 0.14f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f), if (primary) CircleShape else RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(glyph, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
    }
}

internal fun targetDurationSeconds(value: String?): Int? {
    val normalized = value.orEmpty().trim().lowercase()
    val amount = Regex("\\d+").find(normalized)?.value?.toIntOrNull() ?: return null
    return when {
        "min" in normalized -> amount * 60
        Regex("(^|\\s)${amount}\\s*s(ec)?(ondos?)?($|\\s)").containsMatchIn(normalized) || normalized.endsWith("s") -> amount
        else -> null
    }
}
