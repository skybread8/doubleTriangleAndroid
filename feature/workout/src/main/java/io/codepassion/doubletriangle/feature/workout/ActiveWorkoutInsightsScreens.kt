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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import java.text.DateFormat
import java.util.Date

private const val KM_TO_MILES = 0.621371

@Composable
internal fun WorkoutPathScreen(
    workout: WorkoutDaySummary,
    gender: String,
    currentExerciseIndex: Int,
    completedByExercise: Map<Int, Int>,
    addedSetsByExercise: Map<Int, Int>,
    elapsedSeconds: Int,
    useImperial: Boolean = false,
    onAddExerciseToBlock: (Int) -> Unit = {},
    onBack: () -> Unit,
) {
    val pathBlocks = remember(workout) { workout.pathBlocks() }
    val pathExercises = remember(pathBlocks) { pathBlocks.flatMap { it.exercises } }
    val matchedPathIndex = pathExercises.indexOfFirst { currentExerciseIndex in it.executionIndices }
    val lastExecutionIndex = pathExercises.flatMap { it.executionIndices }.maxOrNull() ?: -1
    // Match iOS's clamped "current / total" indicator: a finished session
    // remains at N/N instead of falling back to 1/N when there is no active
    // execution index anymore.
    val currentPathPosition = when {
        pathExercises.isEmpty() -> 0
        matchedPathIndex >= 0 -> matchedPathIndex + 1
        currentExerciseIndex > lastExecutionIndex -> pathExercises.size
        else -> 1
    }
    val currentRowRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(currentExerciseIndex, pathBlocks) {
        if (matchedPathIndex >= 0) currentRowRequester.bringIntoView()
    }
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        InsightHeader("RUTA DEL ENTRENAMIENTO", onBack)
        Text(workout.title, fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(formatInsightClock(elapsedSeconds), style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Spacer(Modifier.weight(1f))
            Text("$currentPathPosition de ${pathExercises.size}", color = WildforceThemeTokens.textSecondary)
        }
        LinearProgressIndicator(
            progress = if (pathExercises.isEmpty()) 0f else currentPathPosition.toFloat() / pathExercises.size,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(5.dp),
            color = WildforceThemeTokens.textPrimary,
            backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.18f),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            pathBlocks.forEachIndexed { blockIndex, block ->
                Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 10.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        block.type.label.uppercase(),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = when (block.type) {
                            WorkoutBlockType.Warmup -> WildforceThemeTokens.warmupAccent
                            WorkoutBlockType.Cooldown -> WildforceThemeTokens.cooldownAccent
                            // iOS gives Superset no section color, so it falls back to secondary.
                            WorkoutBlockType.Superset -> WildforceThemeTokens.textSecondary
                            WorkoutBlockType.Standard -> WildforceThemeTokens.textSecondary
                        },
                    )
                    if (block.rounds > 1) Text(" · ${block.rounds} RONDAS", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(48.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = "Añadir ejercicio" }
                            .clickable { onAddExerciseToBlock(blockIndex) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = WildforceThemeTokens.textPrimary)
                    }
                }
                block.notes?.let { Text(it, Modifier.fillMaxWidth().padding(start = 28.dp, bottom = 4.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                block.exercises.forEach { pathExercise ->
                    val exercise = pathExercise.exercise
                    val completed = pathExercise.isCompleted(workout, completedByExercise, addedSetsByExercise)
                    val current = currentExerciseIndex in pathExercise.executionIndices
                    val rowModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .then(if (current) Modifier.bringIntoViewRequester(currentRowRequester) else Modifier)
                    Row(
                        rowModifier
                            .background(if (current) WildforceThemeTokens.accent.copy(alpha = 0.13f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (completed) Icons.Filled.CheckCircle else if (current) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.width(22.dp).size(16.dp),
                            tint = if (current) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary,
                        )
                        RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exerciseDisplayName(exercise.imageKey, exercise.name), Modifier.size(width = 48.dp, height = 58.dp).clip(RoundedCornerShape(11.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(exerciseDisplayName(exercise.imageKey, exercise.name), fontWeight = if (current) FontWeight.Bold else FontWeight.Normal, color = if (completed) WildforceThemeTokens.textSecondary else WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pathExercisePrescription(pathExercise, useImperial), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

internal fun pathExercisePrescription(pathExercise: WorkoutPathExercise, useImperial: Boolean = false): String {
    val exercise = pathExercise.exercise
    val base = when {
        exercise.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown) && exercise.targetDurationMinutes != null -> "${exercise.targetDurationMinutes} min"
        exercise.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown) && exercise.targetDurationSeconds != null -> "${exercise.targetDurationSeconds}s"
        pathExercise.rounds > 1 -> "${pathExercise.rounds} rondas × ${exercise.repRange?.displayText ?: exercise.targetRepsPerSet?.firstOrNull()?.toString() ?: exercise.reps}"
        exercise.trackingMode == io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration -> exercise.targetDurationMinutes?.let { "$it min" } ?: "${exercise.targetDurationSeconds ?: 0}s"
        exercise.trackingMode == io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> listOfNotNull(
            exercise.targetDurationMinutes?.let { "$it min" } ?: exercise.targetDurationSeconds?.let { "${it}s" },
            exercise.targetDistanceKm?.takeIf { it > 0 }?.let { "${it} km" },
        ).joinToString(" · ")
        else -> "${exercise.sets} × ${exercise.repRange?.displayText ?: exercise.targetRepsPerSet?.firstOrNull()?.toString() ?: exercise.reps}"
    }
    val weight = exercise.targetWeightsKg?.firstOrNull() ?: exercise.targetWeightKg
    val weightText = weight?.takeIf { it > 0 }?.let { " · ${formatTrainingWeight(it, useImperial)}" }
    val restText = exercise.restSeconds.takeIf { it > 0 }?.let { " · ${it}s" }
    return base + (weightText ?: "") + (restText ?: "")
}

@Composable
internal fun ExerciseHistoryScreen(exercise: ExerciseSummary, gender: String, history: List<ExerciseHistoryEntry>, useImperial: Boolean = false, onBack: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val best = history.maxByOrNull { it.maxWeightKg }
    val maxVolume = (history.maxOfOrNull { it.volumeKg } ?: 1.0).coerceAtLeast(1.0)
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        InsightHeader("HISTORIAL DEL EJERCICIO", onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exerciseDisplayName(exercise.imageKey, exercise.name), Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.padding(start = 14.dp)) {
                Text(exerciseDisplayName(exercise.imageKey, exercise.name), fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Progreso y ejecuciones anteriores", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (history.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(56.dp), tint = WildforceThemeTokens.textSecondary)
                Text("SIN EJECUCIONES ANTERIORES", fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("Completa este ejercicio para desbloquear su historial.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            return
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryMetric("PR", formatTrainingWeight(best?.maxWeightKg ?: 0.0, useImperial), "MEJOR PESO", Modifier.weight(1f))
            HistoryMetric("Último", formatTrainingWeight(history.first().maxWeightKg, useImperial), "MEJOR PESO", Modifier.weight(1f))
        }
        Text("VOLUMEN POR SESIÓN", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Row(
            Modifier.fillMaxWidth().height(138.dp).padding(vertical = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom,
        ) {
            history.take(12).reversed().forEach { entry ->
                val targetHeight = (22 + 78 * entry.volumeKg / maxVolume).dp
                val animatedHeight by animateDpAsState(targetHeight, animationSpec = tween(650), label = "history-volume")
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(String.format(locale, "%.0f", if (useImperial) entry.volumeKg * KG_TO_LB else entry.volumeKg), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Box(Modifier.width(24.dp).height(animatedHeight).background(WildforceThemeTokens.accent, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                }
            }
        }
        Text("EJECUCIONES ANTERIORES", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            history.forEach { entry -> HistoryEntryRow(entry, useImperial) }
        }
    }
}

@Composable
private fun InsightHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp)
                .clip(CircleShape)
                .background(WildforceThemeTokens.surfaceElevated)
                .semantics { contentDescription = "Volver" }
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HistoryMetric(prefix: String, value: String, subtitle: String, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(15.dp)).padding(12.dp)) {
        Text(prefix, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
        Text(value, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun HistoryEntryRow(entry: ExerciseHistoryEntry, useImperial: Boolean) {
    val locale = LocalLocale.current.platformLocale
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestampMillis)), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(historyEntrySummary(entry, useImperial), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            if (entry.setDetails.isNotEmpty()) {
                Text(
                    entry.setDetails.joinToString("  ·  ") { set ->
                        val values = listOfNotNull(
                            set.reps.takeIf { it > 0 }?.let { "$it reps" },
                            set.durationSeconds.takeIf { it > 0 }?.let(::formatHistoryDuration),
                            set.distanceKm.takeIf { it > 0 }?.let { formatHistoryDistance(it, useImperial) },
                            set.weightKg.takeIf { it > 0 }?.let { formatTrainingWeight(it, useImperial) },
                        )
                        "${set.setStyle.glyph} S${set.setNumber}  ${values.joinToString(" · ").ifBlank { "registrada" }}"
                    },
                    modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
            entry.feedback?.let {
                Text("● $it", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            }
            entry.note?.let {
                Text("“$it”", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTrainingWeight(entry.maxWeightKg, useImperial), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text(String.format(locale, "%.0f %s vol.", if (useImperial) entry.volumeKg * KG_TO_LB else entry.volumeKg, if (useImperial) "lb" else "kg"), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}

internal fun historyEntrySummary(entry: ExerciseHistoryEntry, useImperial: Boolean): String = listOfNotNull(
    entry.sets.takeIf { it > 0 }?.let { "$it series" },
    entry.totalReps.takeIf { it > 0 }?.let { "$it repeticiones" },
    entry.durationSeconds.takeIf { it > 0 }?.let(::formatHistoryDuration),
    entry.distanceKm.takeIf { it > 0 }?.let { formatHistoryDistance(it, useImperial) },
).joinToString(" · ").ifBlank { "Sesión registrada" }

private fun formatHistoryDuration(seconds: Int): String = if (seconds >= 60) {
    "${seconds / 60} min${if (seconds % 60 == 0) "" else " ${seconds % 60}s"}"
} else "${seconds}s"

private fun formatHistoryDistance(kilometres: Double, useImperial: Boolean): String = if (useImperial) {
    "${String.format(java.util.Locale.getDefault(), "%.2f", kilometres * KM_TO_MILES)} mi"
} else {
    "${String.format(java.util.Locale.getDefault(), "%.2f", kilometres)} km"
}

private fun formatInsightClock(seconds: Int): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
