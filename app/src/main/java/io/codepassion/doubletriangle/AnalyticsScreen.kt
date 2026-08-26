package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
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
import java.time.format.DateTimeFormatter
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
    var rangeDays by remember { mutableStateOf(30) }
    val rangeOptions = listOf(7 to "7D", 14 to "2S", 30 to "30D", 84 to "12S", ALL_HISTORY_DAYS to "TODO")
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
    val totalRecordedSessions = sessionHistory.size
    val totalRecordedVolume = sessionHistory.sumOf { it.volumeKg }
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
        Text("ESTADÍSTICAS", fontFamily = Exo2FontFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("Se muestran tus resultados del periodo seleccionado.", color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            rangeOptions.forEach { (days, label) ->
                val selected = rangeDays == days
                Text(label, Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent).clickable { rangeDays = days }.padding(vertical = 10.dp), color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        sessionHistory.firstOrNull()?.let { latest ->
            Text("◴  Último entrenamiento: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(latest.timestampMillis))}", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
        }
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), emphasized = true).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("PROGRESO SEMANAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("$completed / ${state.workouts.size} sesiones", fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary) }
                Text("${(completed * 100 / total)}%", fontFamily = Exo2FontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
            }
            LinearProgressIndicator(completed.toFloat() / total, Modifier.fillMaxWidth().height(10.dp), WildforceThemeTokens.accentGold, WildforceThemeTokens.textSecondary.copy(alpha = .15f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("ENTRENAMIENTOS", totalRecordedSessions.toString(), "registrados", Modifier.weight(1f))
            MetricCard("VOLUMEN TOTAL", String.format("%.0f kg", totalRecordedVolume), "acumulado", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("RACHA", state.user.currentStreak.toString(), "días", Modifier.weight(1f))
            MetricCard("SESIONES", completed.toString(), "completadas", Modifier.weight(1f))
            MetricCard("FRECUENCIA", sessionsInRange.toString(), "últimos $rangeDays días", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ÚLTIMA SESIÓN", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
            if (lastDuration == 0 && lastSets == 0) {
                Text("Completa tu primer entrenamiento para desbloquear métricas detalladas.", color = WildforceThemeTokens.textSecondary)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricLine("Tiempo medio", formatDuration(averageDuration)); MetricLine("Series medias", averageSets.toString()); MetricLine("Volumen medio", String.format("%.0f kg", averageVolume))
                }
            }
        }
        VolumeTrend(trendVolumes, rangeDays)
        Text("EJERCICIOS", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
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
        Text("VOLUMEN POR GRUPO", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
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

private const val ALL_HISTORY_DAYS = 3650

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
    val rangeLabel = when (rangeDays) {
        7 -> "7 DÍAS"
        14 -> "2 SEMANAS"
        30 -> "30 DÍAS"
        84 -> "12 SEMANAS"
        else -> "TODO EL HISTORIAL"
    }
    val labels = if (rangeDays == 7) {
        (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("es-ES")).uppercase() }
    } else if (rangeDays <= 84) {
        val startDate = LocalDate.now().minusDays((rangeDays - 1).toLong())
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))
        (0..6).map { bucket -> startDate.plusDays((bucket * rangeDays / 7.0).toLong()).format(formatter) }
    } else {
        (1..7).map { "·" }
    }
    val max = volumes.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val barColor = WildforceThemeTokens.textPrimary.copy(alpha = 0.86f)
    val gridColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)
    val trendColor = WildforceThemeTokens.accentGold
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("VOLUMEN · $rangeLabel", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
        if (volumes.all { it <= 0.0 }) {
            Text("Todavía no hay sesiones registradas en este periodo.", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 20.dp))
        }
        Box(Modifier.fillMaxWidth().height(142.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val chartHeight = size.height - 12.dp.toPx()
                val step = size.width / volumes.size.coerceAtLeast(1)
                val barWidth = 13.dp.toPx()
                repeat(3) { index ->
                    val y = chartHeight * index / 2f
                    drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val trend = Path()
                volumes.forEachIndexed { index, volume ->
                    val fraction = (volume / max).toFloat().coerceIn(0f, 1f)
                    val x = step * (index + 0.5f)
                    val y = chartHeight - chartHeight * fraction
                    val barHeight = (chartHeight * fraction).coerceAtLeast(if (volume > 0) 7.dp.toPx() else 0f)
                    if (barHeight > 0f) drawRoundRect(barColor, topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2f, chartHeight - barHeight), size = androidx.compose.ui.geometry.Size(barWidth, barHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f))
                    if (index == 0) trend.moveTo(x, y) else trend.lineTo(x, y)
                }
                drawPath(trend, trendColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { label -> Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
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
            Text(if (summary.personalBestKg > 0) String.format("%.1f kg", summary.personalBestKg) else "—", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
            Text("MEJOR MARCA", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Composable
private fun ExerciseAnalyticsDetailScreen(summary: ExerciseAnalyticsSummary, history: List<ExerciseAnalyticsPoint>, contentPadding: androidx.compose.foundation.layout.PaddingValues, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(24.dp))
        }
        Text(summary.exercise.name, fontFamily = Exo2FontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 2)
        Text("${summary.sessions} sesiones registradas", color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("MEJOR MARCA", if (summary.personalBestKg > 0) String.format("%.1f kg", summary.personalBestKg) else "—", "peso", Modifier.weight(1f))
            MetricCard("VOLUMEN", String.format("%.0f", summary.totalVolumeKg), "kg total", Modifier.weight(1f))
        }
        ExerciseWeightTrend(history)
        Text("SESIONES", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
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
        Text("EVOLUCIÓN DE CARGA", fontFamily = Exo2FontFamily, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
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

@Composable
private fun MetricCard(title: String, value: String, suffix: String, modifier: Modifier) {
    val glyph = when (title) {
        "ENTRENAMIENTOS", "SESIONES" -> "✓"
        "VOLUMEN TOTAL", "VOLUMEN" -> "▥"
        "RACHA" -> "♨"
        "FRECUENCIA" -> "◷"
        "MEJOR MARCA" -> "★"
        else -> "•"
    }
    Column(modifier.liquidGlass(RoundedCornerShape(16.dp)).padding(14.dp)) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
        }
        Text(title, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(value, fontFamily = Exo2FontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(suffix, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
@Composable private fun MetricLine(title: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
private fun formatDuration(seconds: Int): String = if (seconds >= 3600) "%d h %02d".format(seconds / 3600, seconds / 60 % 60) else "%d min".format(seconds / 60)
