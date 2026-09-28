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
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.ExerciseSummary

@Composable
internal fun ExerciseWorkTimer(seconds: Int, initialSeconds: Int, running: Boolean, onToggle: () -> Unit, onAddTime: () -> Unit) {
    val progress by animateFloatAsState((seconds.toFloat() / initialSeconds.coerceAtLeast(1)).coerceIn(0f, 1f), animationSpec = tween(1_000, easing = LinearEasing))
    val timerDescription = stringResource(R.string.workout_work_timer_description, formatWorkDuration(seconds))
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        TimerButton("+", "+15s", false, onAddTime)
        Box(
            Modifier.padding(horizontal = 16.dp).size(132.dp).semantics {
                contentDescription = timerDescription
                stateDescription = if (running) "En marcha" else "Pausado"
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
            },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(progress, Modifier.fillMaxSize(), WildforceThemeTokens.accent, 8.dp, WildforceThemeTokens.textSecondary.copy(alpha = 0.12f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatWorkDuration(seconds), fontFamily = Exo2FontFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text(stringResource(R.string.workout_remaining), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        TimerButton(if (running) "Ⅱ" else "▶", if (running) "PAUSA" else "INICIAR", true, onToggle)
    }
}

@Composable
private fun TimerButton(glyph: String, label: String, primary: Boolean, onClick: () -> Unit) {
    val size = if (primary) 52.dp else 58.dp
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.size(size)
            .background(if (primary) WildforceThemeTokens.accent.copy(alpha = 0.12f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f), if (primary) CircleShape else RoundedCornerShape(18.dp))
            .semantics { contentDescription = label }
            .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        // iOS uses an icon-only 52pt circle for play/pause; the +15s control
        // remains a labelled 58pt rounded rectangle.
        Icon(
            imageVector = when (glyph) {
                "Ⅱ" -> Icons.Filled.Pause
                "▶" -> Icons.Filled.PlayArrow
                else -> Icons.Filled.Add
            },
            contentDescription = null,
            tint = WildforceThemeTokens.textPrimary,
            modifier = Modifier.size(if (primary) 22.dp else 18.dp),
        )
        if (!primary) {
            Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        }
    }
}

internal fun formatWorkDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3_600
    val minutes = (safeSeconds % 3_600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
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

internal fun ExerciseSummary.resolvedTargetDurationSeconds(): Int? =
    when (trackingMode) {
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> null
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration,
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance
        -> targetDurationSeconds ?: targetDurationMinutes?.times(60)
    }
