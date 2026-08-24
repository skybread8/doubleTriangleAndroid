package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsSummary
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsPoint
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import java.text.DateFormat
import java.util.Date
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(
    context: Context,
    state: WorkoutHubState,
    preferences: SharedPreferences,
    exerciseSummaries: List<ExerciseAnalyticsSummary> = emptyList(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
) {
    var selectedExercise by remember { mutableStateOf<ExerciseAnalyticsSummary?>(null) }
    var rangeDays by remember { mutableStateOf(7) }
    selectedExercise?.let { summary ->
        ExerciseAnalyticsDetailScreen(summary, WorkoutAnalyticsStore.history(context, summary.exercise), contentPadding) { selectedExercise = null }
        return
    }
    val completed = state.workouts.count { it.status.name == "Completed" }
    val total = state.workouts.size.coerceAtLeast(1)
    val lastDuration = preferences.getInt("last_workout_duration", 0)
    val lastSets = preferences.getInt("last_workout_sets", 0)
    val lastVolume = java.lang.Double.longBitsToDouble(preferences.getLong("last_workout_volume", 0L))
    val sessionHistory = remember { TrainingSessionHistoryStore.load(preferences) }
    val averageDuration = sessionHistory.takeIf { it.isNotEmpty() }?.map { it.durationSeconds }?.average()?.toInt() ?: lastDuration
    val averageSets = sessionHistory.takeIf { it.isNotEmpty() }?.map { it.sets }?.average()?.toInt() ?: lastSets
    val averageVolume = sessionHistory.takeIf { it.isNotEmpty() }?.map { it.volumeKg }?.average() ?: lastVolume
    val trendVolumes = remember(exerciseSummaries, rangeDays) {
        volumeBuckets(context, exerciseSummaries, rangeDays)
    }
    val sessionsInRange = remember(exerciseSummaries, rangeDays) {
        val startDate = LocalDate.now().minusDays((rangeDays - 1).toLong())
        exerciseSummaries.flatMap { WorkoutAnalyticsStore.history(context, it.exercise) }
            .count { point ->
                val date = java.time.Instant.ofEpochMilli(point.timestampMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(startDate)
            }
    }
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ANALÍTICAS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
        Text("Tu evolución y consistencia en un vistazo.", color = WildforceThemeTokens.textSecondary)
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), emphasized = true).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("PROGRESO SEMANAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("$completed / ${state.workouts.size} sesiones", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary) }
                Text("${(completed * 100 / total)}%", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.accentGold)
            }
            LinearProgressIndicator(completed.toFloat() / total, Modifier.fillMaxWidth().height(10.dp), WildforceThemeTokens.accentGold, WildforceThemeTokens.textSecondary.copy(alpha = .15f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("RACHA", state.user.currentStreak.toString(), "días", Modifier.weight(1f))
            MetricCard("SESIONES", completed.toString(), "completadas", Modifier.weight(1f))
            MetricCard("FRECUENCIA", sessionsInRange.toString(), "últimos $rangeDays días", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ÚLTIMA SESIÓN", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
            if (lastDuration == 0 && lastSets == 0) {
                Text("Completa tu primer entrenamiento para desbloquear métricas detalladas.", color = WildforceThemeTokens.textSecondary)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricLine("Tiempo medio", formatDuration(averageDuration)); MetricLine("Series medias", averageSets.toString()); MetricLine("Volumen medio", String.format("%.0f kg", averageVolume))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { days ->
                Text("$days DÍAS", Modifier.liquidGlass(RoundedCornerShape(12.dp), emphasized = rangeDays == days).clickable { rangeDays = days }.padding(horizontal = 12.dp, vertical = 9.dp), color = if (rangeDays == days) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
            }
        }
        VolumeTrend(trendVolumes, rangeDays)
        Text("EJERCICIOS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
        if (exerciseSummaries.isEmpty()) {
            Text("Completa ejercicios para ver progresión, récords y volumen por movimiento.", color = WildforceThemeTokens.textSecondary)
        } else {
            exerciseSummaries.take(6).forEach { summary -> ExerciseAnalyticsCard(summary) { selectedExercise = summary } }
            MuscleVolumeBreakdown(exerciseSummaries)
        }
        Spacer(Modifier.height(4.dp))
        Text("Toca un ejercicio para ver sus sesiones, volumen y mejor marca.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun MuscleVolumeBreakdown(summaries: List<ExerciseAnalyticsSummary>) {
    val grouped = summaries.groupBy { muscleGroupFor(it.exercise.imageKey, it.exercise.name) }
        .mapValues { (_, items) -> items.sumOf { it.totalVolumeKg } }
        .toList().sortedByDescending { it.second }.take(6)
    val max = grouped.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("VOLUMEN POR GRUPO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
        if (grouped.isEmpty() || grouped.all { it.second <= 0 }) {
            Text("Aún no hay volumen suficiente para comparar grupos musculares.", color = WildforceThemeTokens.textSecondary)
        } else grouped.forEach { (group, volume) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(group, Modifier.width(82.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
                val fraction = (volume / max).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(fraction, Modifier.weight(1f).height(8.dp), WildforceThemeTokens.accentGold, WildforceThemeTokens.textSecondary.copy(alpha = .14f))
                Text(String.format("%.0f", volume), Modifier.width(38.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
            }
        }
    }
}

private fun volumeBuckets(context: Context, summaries: List<ExerciseAnalyticsSummary>, rangeDays: Int): List<Double> {
    val startDate = LocalDate.now().minusDays((rangeDays - 1).toLong())
    val bucketSize = rangeDays.toDouble() / 7.0
    val totals = MutableList(7) { 0.0 }
    summaries.flatMap { WorkoutAnalyticsStore.history(context, it.exercise) }.forEach { point ->
        val date = java.time.Instant.ofEpochMilli(point.timestampMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val offset = java.time.temporal.ChronoUnit.DAYS.between(startDate, date).toInt()
        if (offset in 0 until rangeDays) totals[(offset / bucketSize).toInt().coerceIn(0, 6)] += point.volumeKg
    }
    return totals
}

private fun muscleGroupFor(imageKey: String?, name: String): String {
    val key = "${imageKey.orEmpty()} ${name.lowercase()}"
    return when {
        listOf("squat", "lunge", "legpress", "legcurl", "quad", "sentadilla", "zancada", "pierna").any { it in key } -> "Piernas"
        listOf("deadlift", "row", "pull", "lat", "back", "remo", "dominada", "espalda").any { it in key } -> "Espalda"
        listOf("shoulder", "overhead", "lateral", "facepull", "hombro", "militar").any { it in key } -> "Hombros"
        listOf("bench", "pushup", "chest", "pec", "pecho", "flexión").any { it in key } -> "Pecho"
        listOf("curl", "biceps", "bíceps").any { it in key } -> "Bíceps"
        listOf("triceps", "pushdown", "tríceps").any { it in key } -> "Tríceps"
        listOf("plank", "bug", "core", "abs", "abdominal", "plancha").any { it in key } -> "Core"
        else -> "General"
    }
}

@Composable
private fun VolumeTrend(volumes: List<Double>, rangeDays: Int) {
    val labels = if (rangeDays == 7) (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("es-ES")).uppercase() } else (1..7).map { "S$it" }
    val max = volumes.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("VOLUMEN · ÚLTIMOS $rangeDays DÍAS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
        if (volumes.all { it <= 0.0 }) {
            Text("Todavía no hay sesiones registradas en este periodo.", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 20.dp))
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            volumes.forEachIndexed { index, volume ->
                val animatedHeight by animateDpAsState((12 + 56 * (volume / max)).dp, animationSpec = tween(650), label = "analytics-volume")
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(Modifier.width(20.dp).height(animatedHeight).background(if (volume > 0) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = .14f), RoundedCornerShape(8.dp)))
                    Text(labels[index], Modifier.padding(top = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
        Text("${String.format("%.0f", volumes.sum())} kg acumulados", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun ExerciseAnalyticsCard(summary: ExerciseAnalyticsSummary, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(summary.exercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text("${summary.sessions} sesiones · ${String.format("%.0f kg", summary.totalVolumeKg)} volumen", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (summary.personalBestKg > 0) String.format("%.1f kg", summary.personalBestKg) else "—", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
            Text("MEJOR MARCA", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Composable
private fun ExerciseAnalyticsDetailScreen(summary: ExerciseAnalyticsSummary, history: List<ExerciseAnalyticsPoint>, contentPadding: androidx.compose.foundation.layout.PaddingValues, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("‹ VOLVER", Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
        Text(summary.exercise.name, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
        Text("${summary.sessions} sesiones registradas", color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("MEJOR MARCA", if (summary.personalBestKg > 0) String.format("%.1f kg", summary.personalBestKg) else "—", "peso", Modifier.weight(1f))
            MetricCard("VOLUMEN", String.format("%.0f", summary.totalVolumeKg), "kg total", Modifier.weight(1f))
        }
        ExerciseWeightTrend(history)
        Text("SESIONES", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
        if (history.isEmpty()) Text("Todavía no hay sesiones para este ejercicio.", color = WildforceThemeTokens.textSecondary)
        history.take(12).forEach { point ->
            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(14.dp)).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(point.timestampMillis)), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text("${point.sets} series · ${point.reps} reps${point.feedback?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                Text(String.format("%.0f kg", point.volumeKg), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExerciseWeightTrend(history: List<ExerciseAnalyticsPoint>) {
    val maxWeight = history.maxOfOrNull { it.maxWeightKg }?.takeIf { it > 0 } ?: 1.0
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("EVOLUCIÓN DE CARGA", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
        if (history.none { it.maxWeightKg > 0 }) {
            Text("Completa series con peso para ver la progresión.", color = WildforceThemeTokens.textSecondary)
        } else {
            Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                history.take(12).reversed().forEach { point ->
                    val target = (12 + 70 * (point.maxWeightKg / maxWeight)).dp
                    val height by animateDpAsState(target, animationSpec = tween(550), label = "exercise-weight")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(height).background(WildforceThemeTokens.accentGold, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                    }
                }
            }
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, suffix: String, modifier: Modifier) { Column(modifier.liquidGlass(RoundedCornerShape(16.dp)).padding(14.dp)) { Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text(value, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary); Text(suffix, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
@Composable private fun MetricLine(title: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
private fun formatDuration(seconds: Int): String = if (seconds >= 3600) "%d h %02d".format(seconds / 3600, seconds / 60 % 60) else "%d min".format(seconds / 60)
