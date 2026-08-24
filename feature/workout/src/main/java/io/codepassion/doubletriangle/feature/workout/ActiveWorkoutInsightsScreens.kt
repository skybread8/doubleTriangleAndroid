package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun WorkoutPathScreen(
    workout: WorkoutDaySummary,
    gender: String,
    currentExerciseIndex: Int,
    completedByExercise: Map<Int, Int>,
    elapsedSeconds: Int,
    onBack: () -> Unit,
) {
    val pathBlocks = remember(workout) { workout.pathBlocks() }
    val pathExercises = remember(pathBlocks) { pathBlocks.flatMap { it.exercises } }
    val currentPathIndex = pathExercises.indexOfFirst { currentExerciseIndex in it.executionIndices }.coerceAtLeast(0)
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        InsightHeader("RUTA DEL ENTRENAMIENTO", onBack)
        Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(formatInsightClock(elapsedSeconds), style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Spacer(Modifier.weight(1f))
            Text("${currentPathIndex + 1} de ${pathExercises.size}", color = WildforceThemeTokens.textSecondary)
        }
        LinearProgressIndicator(
            progress = if (pathExercises.isEmpty()) 0f else (currentPathIndex + 1f) / pathExercises.size,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(5.dp),
            color = WildforceThemeTokens.textPrimary,
            backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.18f),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            pathBlocks.forEach { block ->
                Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 10.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        block.type.label.uppercase(),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = when (block.type) {
                            WorkoutBlockType.Warmup -> Color(0xFFF08A24)
                            WorkoutBlockType.Cooldown -> Color(0xFF4A8FE7)
                            WorkoutBlockType.Superset -> WildforceThemeTokens.accentGold
                            WorkoutBlockType.Standard -> WildforceThemeTokens.textSecondary
                        },
                    )
                    if (block.rounds > 1) Text(" · ${block.rounds} RONDAS", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                block.exercises.forEach { pathExercise ->
                    val exercise = pathExercise.exercise
                    val completed = pathExercise.isCompleted(workout, completedByExercise)
                    val current = currentExerciseIndex in pathExercise.executionIndices
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(if (current) WildforceThemeTokens.accentGold.copy(alpha = 0.13f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (completed) "✓" else if (current) "●" else "·", Modifier.width(22.dp), color = if (current) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
                        pathExercise.label?.let { Text(it, Modifier.width(28.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption) }
                        RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.size(width = 48.dp, height = 58.dp).clip(RoundedCornerShape(11.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(exercise.name, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal, color = if (completed) WildforceThemeTokens.textSecondary else WildforceThemeTokens.textPrimary, maxLines = 1)
                            val prescription = if (pathExercise.rounds > 1) "${pathExercise.rounds} rondas × ${exercise.reps}" else "${exercise.sets} × ${exercise.reps}"
                            Text(prescription + if (exercise.restSeconds > 0) " · ${exercise.restSeconds}s" else "", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        if (current) Text("AHORA", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExerciseHistoryScreen(exercise: ExerciseSummary, gender: String, history: List<ExerciseHistoryEntry>, onBack: () -> Unit) {
    val best = history.maxByOrNull { it.maxWeightKg }
    val maxVolume = (history.maxOfOrNull { it.volumeKg } ?: 1.0).coerceAtLeast(1.0)
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        InsightHeader("HISTORIAL DEL EJERCICIO", onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.padding(start = 14.dp)) {
                Text(exercise.name.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("Progreso y ejecuciones anteriores", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
            }
        }
        if (history.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("▥", style = MaterialTheme.typography.h2, color = WildforceThemeTokens.textSecondary)
                Text("SIN EJECUCIONES ANTERIORES", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("Completa este ejercicio para desbloquear su historial.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
            }
            return
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryMetric("PR", formatWeight(best?.maxWeightKg ?: 0.0), "MEJOR PESO", Modifier.weight(1f))
            HistoryMetric("ÚLTIMO", formatWeight(history.first().maxWeightKg), "MEJOR PESO", Modifier.weight(1f))
        }
        Text("VOLUMEN POR SESIÓN", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Row(
            Modifier.fillMaxWidth().height(138.dp).padding(vertical = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom,
        ) {
            history.take(12).reversed().forEach { entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(String.format(Locale.getDefault(), "%.0f", entry.volumeKg), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Box(Modifier.width(24.dp).height((22 + 78 * entry.volumeKg / maxVolume).dp).background(WildforceThemeTokens.accentGold, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                }
            }
        }
        Text("EJECUCIONES ANTERIORES", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            history.forEach { entry -> HistoryEntryRow(entry) }
        }
    }
}

@Composable
private fun InsightHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹ VOLVER", Modifier.clickable(onClick = onBack).padding(top = 8.dp, end = 12.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun HistoryMetric(prefix: String, value: String, subtitle: String, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(15.dp)).padding(12.dp)) {
        Text(prefix, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
        Text(value, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun HistoryEntryRow(entry: ExerciseHistoryEntry) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestampMillis)), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text("${entry.sets} series · ${entry.totalReps} repeticiones", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            if (entry.setDetails.isNotEmpty()) {
                Text(
                    entry.setDetails.joinToString("  ·  ") { set -> "${set.setStyle.glyph} S${set.setNumber}  ${set.reps}×${formatWeight(set.weightKg)}" },
                    modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatWeight(entry.maxWeightKg), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text(String.format(Locale.getDefault(), "%.0f kg vol.", entry.volumeKg), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}

private fun formatWeight(value: Double): String = String.format(Locale.getDefault(), "%.1f kg", value)
private fun formatInsightClock(seconds: Int): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
