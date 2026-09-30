package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsPoint
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsSummary
import io.codepassion.doubletriangle.feature.workout.ExerciseVisualCatalog
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.feature.workout.exerciseImageUrl
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.*

internal enum class AnalyticsTimeframe(val days: Int?, val label: String, val description: String) {
    SevenDays(7, "7D", "Mostrando los últimos 7 días"),
    TwoWeeks(14, "2S", "Mostrando las últimas 2 semanas"),
    ThirtyDays(30, "30D", "Mostrando los últimos 30 días"),
    TwelveWeeks(84, "12S", "Mostrando las últimas 12 semanas"),
    AllTime(null, "TODO", "Mostrando todo el historial");
    val comparesPlan get() = this != AllTime
}

internal data class Kpi(val title: String, val value: String, val subtitle: String = "", val glyph: String)
internal data class MuscleScore(val name: String, val score: Double)
internal data class Insight(val title: String, val body: String, val glyph: String)
internal data class PersonalRecord(
    val exerciseName: String,
    val title: String,
    val value: String,
    val recordedAtMillis: Long,
    val glyph: String,
    val delta: String? = null,
)
private data class AnalyticsColors(
    val card: Color,
    val inset: Color,
    val elevated: Color,
    val border: Color,
    val accentText: Color,
    val accentSoft: Color,
    val positive: Color,
    val orange: Color,
    val red: Color,
)

@Composable
private fun analyticsColors(): AnalyticsColors = if (MaterialTheme.colors.isLight) {
    AnalyticsColors(
        card = WildforceThemeTokens.backgroundSecondary,
        inset = Color(0xFFF2F2F7), elevated = Color.White,
        border = WildforceThemeTokens.textPrimary.copy(alpha = .05f),
        accentText = WildforceThemeTokens.accent,
        accentSoft = WildforceThemeTokens.accent.copy(alpha = .14f),
        positive = Color(0xFF34C759), orange = Color(0xFFFF9500), red = Color(0xFFFF3B30),
    )
} else {
    AnalyticsColors(
        card = WildforceThemeTokens.backgroundSecondary,
        inset = Color(0xFF2C2C2E), elevated = Color(0xFF3A3A3C),
        border = WildforceThemeTokens.textPrimary.copy(alpha = .05f),
        accentText = WildforceThemeTokens.accent,
        accentSoft = WildforceThemeTokens.accent.copy(alpha = .14f),
        positive = Color(0xFF30D158), orange = Color(0xFFFF9F0A), red = Color(0xFFFF453A),
    )
}
internal data class AnalyticsData(
    val sessions: List<TrainingSessionHistoryEntry>,
    val exercises: List<ExerciseAnalyticsSummary>,
    val histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>,
)

@Composable
fun AnalyticsScreen(
    context: Context,
    state: WorkoutHubState,
    preferences: SharedPreferences,
    exerciseSummaries: List<ExerciseAnalyticsSummary> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
    gender: String = "male",
    useImperial: Boolean = false,
) {
    var timeframe by remember { mutableStateOf(AnalyticsTimeframe.ThirtyDays) }
    var selected by remember { mutableStateOf<ExerciseAnalyticsSummary?>(null) }
    val allSessions = TrainingSessionHistoryStore.load(preferences)
    val allHistories = remember(exerciseSummaries) { exerciseSummaries.associateWith { WorkoutAnalyticsStore.history(context, it.exercise) } }
    val data = remember(timeframe, allSessions, exerciseSummaries, allHistories) {
        val cutoff = timeframe.days?.let { startMillis(it - 1) }
        val histories = allHistories.mapValues { (_, points) -> points.filter { cutoff == null || it.timestampMillis >= cutoff } }
        AnalyticsData(
            allSessions.filter { cutoff == null || it.timestampMillis >= cutoff },
            exerciseSummaries.mapNotNull { summary ->
                val points = histories[summary].orEmpty()
                if (points.isEmpty()) null else summary.copy(
                    sessions = points.size,
                    personalBestKg = points.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                    totalVolumeKg = points.sumOf { it.volumeKg },
                    lastFeedback = points.firstOrNull()?.feedback,
                )
            }.sortedByDescending { it.totalVolumeKg }, histories,
        )
    }
    selected?.let { exercise ->
        ExerciseAnalyticsDetail(exercise, allHistories[exercise].orEmpty(), contentPadding, gender, useImperial) { selected = null }
        return
    }

    val completed = data.sessions.size
    val planned = timeframe.days?.let { plannedWorkoutsInRange(state.trainingDays, it) } ?: 0
    val volume = data.sessions.sumOf { it.volumeKg }.takeIf { it > 0 } ?: data.exercises.sumOf { it.totalVolumeKg }
    val averageVolume = volume.takeIf { data.sessions.isNotEmpty() }?.div(data.sessions.size)
    val minutes = data.sessions.sumOf { it.durationSeconds } / 60
    val averageMinutes = data.sessions.takeIf { it.isNotEmpty() }?.map { it.durationSeconds }?.average()?.toInt()?.div(60)
    val empty = allSessions.isEmpty() && exerciseSummaries.isEmpty()

    Column(
        Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnalyticsHeader(timeframe, { timeframe = it }, allSessions.maxByOrNull { it.timestampMillis })
        if (empty) EmptyAnalyticsCard() else {
            KpiStrip(buildList {
                add(Kpi("Entrenamientos totales", completed.toString(), if (timeframe.comparesPlan) "$completed de $planned planificados" else "", "✓"))
                add(Kpi("Volumen total", formatWeight(volume, useImperial), averageVolume?.let { "Media de ${formatWeight(it, useImperial)} por entrenamiento" }.orEmpty(), "▥"))
                add(Kpi("Racha actual", state.user.currentStreak.toString(), "Máxima ${longestStreak(allSessions)}", "♨"))
                if (timeframe.comparesPlan) add(Kpi("Consistencia", if (planned == 0) "Sin plan" else "${completed * 100 / planned}%", if (planned == 0) "No hay días programados" else "$completed de $planned planificados", "▣"))
                else if (minutes > 0) add(Kpi("Tiempo entrenando", formatMinutes(minutes), averageMinutes?.let { "Media de ${formatMinutes(it)}" }.orEmpty(), "◷"))
            })
            val buckets = volumeBuckets(data, timeframe)
            if (buckets.any { it.second > 0 }) DashboardCard {
                SectionHeader("Tendencia de volumen", averageVolume?.let { "Media de ${formatWeight(it, useImperial)} por entrenamiento" } ?: timeframe.description, "▥")
                VolumeChart(buckets, timeframe)
            }
            val records = personalRecords(data.histories, useImperial)
            if (records.isNotEmpty()) PersonalRecords(records)
            val focus = data.sessions.mapNotNull(TrainingSessionHistoryEntry::focus)
                .groupingBy { it }.eachCount().toList()
                .ifEmpty { state.workouts.filter { it.status.name == "Completed" }.groupingBy { it.focus }.eachCount().toList() }
            AnalyticsMiniCharts(data.sessions, focus)
            val balance = muscleDistribution(data.exercises)
            if (balance.isNotEmpty()) MuscleSection("Equilibrio muscular", timeframe.description, null, balance, false)
            val fatigue = muscleFatigue(data.histories)
            if (fatigue.isNotEmpty()) MuscleSection("Fatiga muscular", timeframe.description, "Estrés de entrenamiento reciente por grupo muscular, dando más peso a las sesiones más nuevas.", fatigue, true)
            if (allSessions.isNotEmpty()) ConsistencyHeatmap(allSessions)
            val insights = analyticsInsights(data, state.user.currentStreak, balance)
            if (insights.isNotEmpty()) InsightsCard(insights)
            ExerciseList(timeframe, data.exercises, gender, useImperial) { selected = it }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AnalyticsHeader(timeframe: AnalyticsTimeframe, onChange: (AnalyticsTimeframe) -> Unit, last: TrainingSessionHistoryEntry?) {
    val colors = analyticsColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Analíticas", fontFamily = Exo2FontFamily, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text(timeframe.description, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
        Row(Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(10.dp)).border(1.dp, colors.border, RoundedCornerShape(10.dp)).padding(2.dp)) {
            AnalyticsTimeframe.entries.forEach { option ->
                val selected = option == timeframe
                Text(option.label, Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent).clickable { onChange(option) }.padding(vertical = 8.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
            }
        }
        last?.let { Text("◷  Último entrenamiento ${mediumDate(it.timestampMillis)}", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun EmptyAnalyticsCard() = DashboardCard {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Badge("▥", 48)
        Text("Todavía no hay analíticas", style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 14.dp))
        Text("Completa un entrenamiento para empezar a ver aquí tus rachas, carga de entrenamiento y métricas de progresión.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable internal fun DashboardCard(content: @Composable () -> Unit) {
    val colors = analyticsColors()
    Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
}
@Composable internal fun Badge(glyph: String, size: Int = 34) {
    val colors = analyticsColors()
    Box(Modifier.size(size.dp).background(colors.accentSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(glyph, color = colors.accentText, fontWeight = FontWeight.Bold, fontSize = (size * .4f).sp) }
}
@Composable internal fun SectionHeader(title: String, subtitle: String, glyph: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Badge(glyph, 38)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.h6); Text(subtitle, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun KpiStrip(items: List<Kpi>) {
    val colors = analyticsColors()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(Modifier.width(164.dp).background(colors.card, RoundedCornerShape(22.dp)).border(1.dp, colors.border, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Badge(item.glyph); Text(item.value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(item.title, style = MaterialTheme.typography.body2); if (item.subtitle.isNotEmpty()) Text(item.subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2) }
            }
        }
    }
}

@Composable private fun VolumeChart(
    points: List<Pair<String, Double>>,
    timeframe: AnalyticsTimeframe? = null,
) {
    val max = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val grid = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
    val colors = analyticsColors()
    val accent = WildforceThemeTokens.accent
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(188.dp)) {
            val height = size.height - 12.dp.toPx()
            repeat(4) { i -> drawLine(grid, Offset(0f, height * i / 3), Offset(size.width, height * i / 3), 1.dp.toPx()) }
            val step = size.width / points.size.coerceAtLeast(1)
            points.forEachIndexed { i, point ->
                val h = height * (point.second / max).toFloat()
                val w = min(18.dp.toPx(), step * .45f)
                drawRoundRect(accent, Offset(step * i + (step - w) / 2, height - h), Size(w, h), CornerRadius(w / 2))
            }

            // Mirrors the iOS hero: the bars show the actual load and the dashed
            // orange line makes the short rolling trend readable at a glance.
            val window = if (timeframe?.days == null || (timeframe?.days ?: 0) > 30) 2 else 3
            val trend = points.indices.map { index ->
                points.subList(max(0, index - window + 1), index + 1).map { it.second }.average()
            }
            val trendPath = Path()
            trend.forEachIndexed { index, value ->
                val x = step * index + step / 2
                val y = height - height * (value / max).toFloat()
                if (index == 0) trendPath.moveTo(x, y) else trendPath.lineTo(x, y)
            }
            drawPath(
                trendPath,
                colors.orange,
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { points.forEach { Text(it.first, style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1) } }
    }
}

@Composable
private fun AnalyticsMiniCharts(
    sessions: List<TrainingSessionHistoryEntry>,
    focus: List<Pair<String, Int>>,
) {
    if (sessions.isEmpty() && focus.isEmpty()) return

    BoxWithConstraints {
        val wideLayout = maxWidth >= 424.dp
        val content: @Composable () -> Unit = {
            if (sessions.isNotEmpty()) DashboardCard {
                SectionHeader("Cuándo entrenas", "Entrenamientos completados por día de la semana", "◷")
                WeekdayChart(sessions)
            }
            if (focus.isNotEmpty()) DashboardCard {
                SectionHeader("Enfoque del entrenamiento", "Distribución de sesiones en este periodo", "◉")
                FocusChart(focus)
            }
        }

        if (wideLayout) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sessions.isNotEmpty()) Box(Modifier.weight(1f)) { DashboardCard { SectionHeader("Cuándo entrenas", "Entrenamientos completados por día de la semana", "◷"); WeekdayChart(sessions) } }
                if (focus.isNotEmpty()) Box(Modifier.weight(1f)) { DashboardCard { SectionHeader("Enfoque del entrenamiento", "Distribución de sesiones en este periodo", "◉"); FocusChart(focus) } }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

@Composable private fun PersonalRecords(records: List<PersonalRecord>) = DashboardCard {
    val colors = analyticsColors()
    SectionHeader("Récords personales", "Mejores marcas de ejercicios en este periodo", "★")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        records.forEach { record ->
            Column(Modifier.width(188.dp).height(168.dp).background(colors.inset, RoundedCornerShape(22.dp)).border(1.dp, colors.border, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Badge(record.glyph); Text(shortDate(record.recordedAtMillis), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary) }
                Column { Text(record.exerciseName, style = MaterialTheme.typography.subtitle1, maxLines = 2); Text(record.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                Text(record.value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                record.delta?.let { Text("↑ $it", style = MaterialTheme.typography.caption, color = colors.positive) }
            }
        }
    }
}

@Composable private fun WeekdayChart(sessions: List<TrainingSessionHistoryEntry>) {
    val counts = DayOfWeek.entries.associateWith { day -> sessions.count { localDate(it.timestampMillis).dayOfWeek == day } }; val max = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth().height(124.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
        DayOfWeek.entries.forEach { day -> val count = counts[day] ?: 0; Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            Text(count.toString(), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary); Box(Modifier.width(18.dp).height((12 + 70 * count / max).dp).background(WildforceThemeTokens.accent, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))); Text(day.getDisplayName(TextStyle.NARROW, esLocale).uppercase(), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
        } }
    }
}

@Composable private fun FocusChart(distribution: List<Pair<String, Int>>) {
    val colors = listOf(WildforceThemeTokens.accent, Color(0xFF007AFF), Color(0xFF34C759), Color(0xFFFF9500)); val total = distribution.sumOf { it.second }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(132.dp)) { var start = -90f; distribution.forEachIndexed { i, item -> val sweep = item.second * 360f / total; drawArc(colors[i % colors.size], start, sweep, false, style = Stroke(22.dp.toPx())); start += sweep } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { distribution.forEachIndexed { i, item -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(9.dp).background(colors[i % colors.size], CircleShape)); Text(item.first, Modifier.weight(1f), style = MaterialTheme.typography.body2, maxLines = 1); Text("${item.second}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } } }
    }
}

@Composable private fun MuscleSection(title: String, subtitle: String, description: String?, scores: List<MuscleScore>, fatigue: Boolean) {
    var radar by remember { mutableStateOf(false) }
    val colors = analyticsColors()
    DashboardCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { SectionHeader(title, subtitle, if (fatigue) "♨" else "◈") }
            Row(Modifier.background(colors.inset, RoundedCornerShape(9.dp)).border(1.dp, colors.border, RoundedCornerShape(9.dp)).padding(2.dp)) { listOf(false to "▥", true to "⬡").forEach { option -> Text(option.second, Modifier.clip(RoundedCornerShape(7.dp)).background(if (radar == option.first) colors.elevated else Color.Transparent).clickable { radar = option.first }.padding(horizontal = 12.dp, vertical = 7.dp), color = WildforceThemeTokens.textPrimary) } }
        }
        description?.let { Text(it, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
        if (radar && scores.size >= 3) RadarChart(scores, fatigue) else MuscleBars(scores, fatigue)
    }
}

@Composable private fun MuscleBars(scores: List<MuscleScore>, fatigue: Boolean) {
    val max = if (fatigue) 100.0 else scores.maxOfOrNull { it.score }?.coerceAtLeast(1.0) ?: 1.0
    val colors = analyticsColors()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { scores.sortedByDescending { it.score }.forEach { item -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.name, style = MaterialTheme.typography.subtitle2); Text(if (fatigue) "${item.score.toInt()}%" else item.score.toInt().toString(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
        Box(Modifier.fillMaxWidth().height(12.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .14f), CircleShape)) {
            val gradient = if (fatigue) listOf(colors.orange.copy(alpha = .55f), colors.red.copy(alpha = .9f)) else listOf(colors.accentText.copy(alpha = .55f), colors.accentText)
            Box(Modifier.fillMaxWidth((item.score / max).toFloat().coerceIn(.08f, 1f)).height(12.dp).background(Brush.horizontalGradient(gradient), CircleShape))
        }
    } } }
}

@Composable private fun RadarChart(scores: List<MuscleScore>, fatigue: Boolean) {
    val colors = analyticsColors()
    val max = if (fatigue) 100.0 else scores.maxOfOrNull { it.score }?.coerceAtLeast(1.0) ?: 1.0; val accent = if (fatigue) colors.orange else WildforceThemeTokens.accent
    val gridColor = WildforceThemeTokens.textSecondary
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val center = Offset(size.width / 2, size.height / 2); val radius = min(size.width, size.height) * .34f
        fun p(i: Int, scale: Float): Offset { val angle = i * 2 * Math.PI / scores.size - Math.PI / 2; return Offset(center.x + radius * scale * cos(angle).toFloat(), center.y + radius * scale * sin(angle).toFloat()) }
        listOf(.33f, .66f, 1f).forEach { ring -> val path = Path(); scores.indices.forEach { i -> val point = p(i, ring); if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }; path.close(); drawPath(path, gridColor.copy(alpha = .15f), style = Stroke(1.dp.toPx())) }
        scores.indices.forEach { drawLine(gridColor.copy(alpha = .12f), center, p(it, 1f), 1.dp.toPx()) }
        val path = Path(); scores.forEachIndexed { i, item -> val point = p(i, (item.score / max).toFloat().coerceIn(0f, 1f)); if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }; path.close(); drawPath(path, accent.copy(alpha = .28f)); drawPath(path, accent, style = Stroke(2.dp.toPx()))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { scores.take(6).forEach { Text(it.name.take(8), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary) } }
}

@Composable private fun ConsistencyHeatmap(sessions: List<TrainingSessionHistoryEntry>) = DashboardCard {
    val colors = analyticsColors()
    val days = (83 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }; val counts = sessions.groupingBy { localDate(it.timestampMillis) }.eachCount()
    SectionHeader("Consistencia", "${sessions.size} sesiones en ${counts.size} días activos", "▦")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { days.chunked(12).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { row.forEach { date -> val level = (counts[date] ?: 0).coerceAtMost(3); Box(Modifier.weight(1f).height(18.dp).background(if (level == 0) WildforceThemeTokens.textSecondary.copy(alpha = .12f) else WildforceThemeTokens.accent.copy(alpha = if (level == 1) .35f else if (level == 2) .6f else 1f), RoundedCornerShape(4.dp))) } } } }
}

@Composable private fun InsightsCard(insights: List<Insight>) = DashboardCard {
    SectionHeader("Claves", "Señales breves de tus entrenamientos recientes", "✦")
    insights.forEachIndexed { i, insight -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) { Badge(insight.glyph, 36); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(insight.title, style = MaterialTheme.typography.subtitle2); Text(insight.body, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) } }; if (i < insights.lastIndex) Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .15f)) }
}

@Composable private fun ExerciseList(timeframe: AnalyticsTimeframe, exercises: List<ExerciseAnalyticsSummary>, gender: String, useImperial: Boolean, onSelect: (ExerciseAnalyticsSummary) -> Unit) = DashboardCard {
    var query by remember { mutableStateOf("") }; val filtered = exercises.filter { it.exercise.name.contains(query, true) }
    val colors = analyticsColors()
    SectionHeader(if (timeframe == AnalyticsTimeframe.AllTime) "Todos los ejercicios completados" else "Ejercicios de este periodo", if (timeframe == AnalyticsTimeframe.AllTime) "Historial completo de ejercicios" else "Desglose de ejercicios completados recientemente", "☷")
    if (exercises.isEmpty()) Text(if (timeframe == AnalyticsTimeframe.AllTime) "Todavía no hay ejercicios completados." else "No hay ejercicios completados en este periodo.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) else {
        Row(Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("⌕", color = WildforceThemeTokens.textSecondary); BasicTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.body2.copy(color = WildforceThemeTokens.textPrimary), decorationBox = { inner -> if (query.isEmpty()) Text("Buscar ejercicios", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); inner() }); if (query.isNotEmpty()) Text("✕", Modifier.clickable { query = "" }, color = WildforceThemeTokens.textSecondary)
        }
        if (filtered.isEmpty()) Text("No hay ejercicios que coincidan con «$query»", Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        else filtered.forEachIndexed { i, summary -> ExerciseRow(summary, gender, useImperial) { onSelect(summary) }; if (i < filtered.lastIndex) Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .15f)) }
    }
}

@Composable private fun ExerciseRow(summary: ExerciseAnalyticsSummary, gender: String, useImperial: Boolean, click: () -> Unit) {
    val colors = analyticsColors()
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        RemoteTrainingImage(
            url = exerciseImageUrl(summary.exercise.imageKey, gender),
            contentDescription = summary.exercise.name,
            modifier = Modifier.size(width = 56.dp, height = 68.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(summary.exercise.name, style = MaterialTheme.typography.subtitle1, maxLines = 2); Text(muscleGroup(summary.exercise.imageKey, summary.exercise.name), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${summary.sessions} completados", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
        Column(horizontalAlignment = Alignment.End) { if (summary.personalBestKg > 0) { Text(formatWeight(summary.personalBestKg, useImperial), style = MaterialTheme.typography.subtitle1); Text("MEJOR PESO", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary) }; Text("›", fontSize = 24.sp, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun ExerciseAnalyticsDetail(summary: ExerciseAnalyticsSummary, history: List<ExerciseAnalyticsPoint>, padding: PaddingValues, gender: String, useImperial: Boolean, back: () -> Unit) {
    val colors = analyticsColors()
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(padding).verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(44.dp).background(colors.elevated, CircleShape).border(1.dp, colors.border, CircleShape).clickable(onClick = back), contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowBack, "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(
                url = exerciseImageUrl(summary.exercise.imageKey, gender),
                contentDescription = summary.exercise.name,
                modifier = Modifier.size(width = 84.dp, height = 100.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(summary.exercise.name, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold); Text(muscleGroup(summary.exercise.imageKey, summary.exercise.name), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${history.size} ejercicios completados en total", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${history.count { it.timestampMillis >= startMillis(29) }} en los últimos 30 días", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
        }
        if (history.isEmpty()) DashboardCard { Text("Completa este ejercicio en un entrenamiento para desbloquear analíticas detalladas.", color = WildforceThemeTokens.textSecondary) }
        else {
            Text("Progresión", style = MaterialTheme.typography.h6); ProgressionChart(history)
            Text("Volumen a lo largo del tiempo", style = MaterialTheme.typography.h6); VolumeChart(history.reversed().takeLast(10).map { shortDate(it.timestampMillis) to it.volumeKg })
            HighlightGrid(listOf(Kpi("Mejor marca", formatWeight(history.maxOf { it.maxWeightKg }, useImperial), glyph = "★"), Kpi("Última mejor marca", formatWeight(history.first().maxWeightKg, useImperial), glyph = "↑"), Kpi("Sesiones", history.size.toString(), glyph = "✓"), Kpi("Volumen total", formatWeight(history.sumOf { it.volumeKg }, useImperial), glyph = "▥"), Kpi("Mejores repeticiones", history.maxOf { it.reps }.toString(), glyph = "#"), Kpi("Series totales", history.sumOf { it.sets }.toString(), glyph = "☷")))
            exercisePerformanceMetrics(history, useImperial).takeIf { it.isNotEmpty() }?.let { metrics ->
                HighlightGrid(metrics, title = "Rendimiento")
            }
            ExerciseFeedbackBreakdown(history)
            Text("Sesiones", style = MaterialTheme.typography.h6)
            history.forEach { point -> Row(Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(16.dp)).border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(mediumDate(point.timestampMillis), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text("${point.sets} series · ${point.reps} repeticiones${point.feedback?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }; Text(formatWeight(point.volumeKg, useImperial), color = colors.accentText, fontWeight = FontWeight.Bold) } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun ProgressionChart(history: List<ExerciseAnalyticsPoint>) {
    val points = history.reversed().takeLast(12)
    val max = points.maxOfOrNull { it.maxWeightKg }?.coerceAtLeast(1.0) ?: 1.0
    val colors = analyticsColors()
    val grid = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
    val accent = WildforceThemeTokens.accent
    Canvas(
        Modifier.fillMaxWidth().height(200.dp).background(colors.inset, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3
            drawLine(grid, Offset.Zero.copy(y = y), Offset(size.width, y), 1.dp.toPx())
        }
        if (points.isEmpty()) return@Canvas

        fun pointAt(index: Int, point: ExerciseAnalyticsPoint): Offset {
            val x = if (points.size == 1) size.width / 2 else index * size.width / (points.size - 1)
            val y = size.height - (point.maxWeightKg / max).toFloat() * size.height
            return Offset(x, y)
        }

        val line = Path()
        points.forEachIndexed { index, point ->
            val position = pointAt(index, point)
            if (index == 0) line.moveTo(position.x, position.y) else line.lineTo(position.x, position.y)
        }
        val area = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { index, point ->
                val position = pointAt(index, point)
                lineTo(position.x, position.y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(area, accent.copy(alpha = .22f))
        drawPath(line, accent, style = Stroke(3.dp.toPx()))
        points.forEachIndexed { index, point ->
            drawCircle(accent, 4.dp.toPx(), pointAt(index, point))
        }
    }
}

@Composable private fun HighlightGrid(metrics: List<Kpi>, title: String = "Resumen") {
    val colors = analyticsColors()
    Text(title, style = MaterialTheme.typography.h6)
    metrics.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { metric -> Column(Modifier.weight(1f).background(colors.inset, RoundedCornerShape(16.dp)).border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(metric.glyph, color = colors.accentText); Text(metric.value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(metric.title, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
}

@Composable
private fun ExerciseFeedbackBreakdown(history: List<ExerciseAnalyticsPoint>) {
    val feedback = history.mapNotNull(ExerciseAnalyticsPoint::feedback)
        .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    if (feedback.isEmpty()) return

    val colors = analyticsColors()
    Column(
        Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Feedback", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
        feedback.forEach { (label, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.replace('_', ' '), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary)
                Text("$count sesión${if (count == 1) "" else "es"}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

internal fun exercisePerformanceMetrics(history: List<ExerciseAnalyticsPoint>, useImperial: Boolean = false): List<Kpi> {
    val timed = history.filter { it.durationSeconds > 0 }
    val distance = history.filter { it.distanceKm > 0 }
    return buildList {
        if (timed.isNotEmpty()) {
            add(Kpi("Tiempo total", formatDurationSeconds(timed.sumOf { it.durationSeconds }), glyph = "◷"))
            add(Kpi("Mejor duración", formatDurationSeconds(timed.maxOf { it.durationSeconds }), glyph = "⌛"))
        }
        if (distance.isNotEmpty()) {
            add(Kpi("Distancia total", formatDistance(distance.sumOf { it.distanceKm }, useImperial), glyph = "⌁"))
            add(Kpi("Mayor distancia", formatDistance(distance.maxOf { it.distanceKm }, useImperial), glyph = "↔"))
        }
    }
}

internal fun volumeBuckets(data: AnalyticsData, timeframe: AnalyticsTimeframe): List<Pair<String, Double>> {
    if (timeframe == AnalyticsTimeframe.AllTime) { val points = data.histories.values.flatten(); return points.groupBy { localDate(it.timestampMillis).withDayOfMonth(1) }.toSortedMap().entries.toList().takeLast(7).map { it.key.format(DateTimeFormatter.ofPattern("MMM", esLocale)).uppercase() to it.value.sumOf(ExerciseAnalyticsPoint::volumeKg) } }
    val days = timeframe.days ?: return emptyList(); val count = if (days <= 14) days else if (days == 30) 10 else 12; val start = LocalDate.now().minusDays((days - 1).toLong()); val size = days.toDouble() / count; val totals = MutableList(count) { 0.0 }
    val source = if (data.sessions.any { it.volumeKg > 0 }) data.sessions.map { it.timestampMillis to it.volumeKg } else data.histories.values.flatten().map { it.timestampMillis to it.volumeKg }
    source.forEach { (timestamp, volume) -> val offset = ChronoUnit.DAYS.between(start, localDate(timestamp)).toInt(); if (offset in 0 until days) totals[(offset / size).toInt().coerceIn(0, count - 1)] += volume }
    return totals.mapIndexed { index, value -> val date = start.plusDays((index * size).toLong()); (if (days <= 14) date.dayOfWeek.getDisplayName(TextStyle.NARROW, esLocale).uppercase() else date.format(DateTimeFormatter.ofPattern("d MMM", esLocale))) to value }
}
internal fun personalRecords(
    histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>,
    useImperial: Boolean = false,
): List<PersonalRecord> = histories.flatMap { (summary, history) ->
    fun <T : Comparable<T>> record(
        title: String,
        glyph: String,
        points: List<ExerciseAnalyticsPoint>,
        valueOf: (ExerciseAnalyticsPoint) -> T,
        format: (T) -> String,
    ): PersonalRecord? {
        val best = points.maxByOrNull(valueOf) ?: return null
        val previousBest = points.filterNot { it === best }.maxOfOrNull(valueOf)
        val delta = previousBest?.let { previous -> if (valueOf(best) > previous) "Nuevo récord" else null }
        return PersonalRecord(summary.exercise.name, title, format(valueOf(best)), best.timestampMillis, glyph, delta)
    }

    buildList {
        record("Mejor peso", "★", history.filter { it.maxWeightKg > 0 }, ExerciseAnalyticsPoint::maxWeightKg) { formatWeight(it, useImperial) }?.let(::add)
        record("Mejores repeticiones", "#", history.filter { it.reps > 0 }, ExerciseAnalyticsPoint::reps) { "$it reps" }?.let(::add)
        record("Mayor volumen", "▥", history.filter { it.volumeKg > 0 }, ExerciseAnalyticsPoint::volumeKg) { formatWeight(it, useImperial) }?.let(::add)
        record("Mayor duración", "⌛", history.filter { it.durationSeconds > 0 }, ExerciseAnalyticsPoint::durationSeconds) { formatDurationSeconds(it) }?.let(::add)
        record("Mayor distancia", "↔", history.filter { it.distanceKm > 0 }, ExerciseAnalyticsPoint::distanceKm) { formatDistance(it, useImperial) }?.let(::add)
    }
}.sortedByDescending(PersonalRecord::recordedAtMillis).take(8)

internal fun muscleDistribution(exercises: List<ExerciseAnalyticsSummary>): List<MuscleScore> = exercises
    .flatMap { summary ->
        val groups = exerciseMuscleGroups(summary.exercise.imageKey, summary.exercise.name)
        groups.map { group -> group to summary.totalVolumeKg / groups.size.coerceAtLeast(1) }
    }.groupBy { it.first }.map { (name, values) -> MuscleScore(name, values.sumOf { it.second }) }
    .filter { it.score > 0 }.sortedByDescending { it.score }.take(7)

internal fun muscleFatigue(histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>): List<MuscleScore> {
    val now = System.currentTimeMillis()
    return histories.flatMap { (summary, points) ->
        val groups = exerciseMuscleGroups(summary.exercise.imageKey, summary.exercise.name)
        points.flatMap { point -> groups.map { group -> group to (point to groups.size.coerceAtLeast(1)) } }
    }.groupBy { it.first }.map { (name, values) ->
        val stress = values.sumOf { (_, payload) ->
            val (point, groupCount) = payload
            point.volumeKg / groupCount * exp(-((now - point.timestampMillis).coerceAtLeast(0) / 86_400_000.0) / 7.0)
        }
        MuscleScore(name, ((1 - exp(-stress / 1500.0)) * 100).coerceIn(0.0, 100.0))
    }.filter { it.score > .5 }.sortedByDescending { it.score }.take(7)
}
internal fun analyticsInsights(data: AnalyticsData, streak: Int, balance: List<MuscleScore>) = buildList { if (streak > 1) add(Insight("Racha en marcha", "Llevas $streak días de constancia. Mantén el próximo entrenamiento para prolongarla.", "♨")); if (data.sessions.size >= 2) { val split = (data.sessions.size / 2).coerceAtLeast(1); val recent = data.sessions.take(split).sumOf { it.volumeKg }; val older = data.sessions.drop(split).sumOf { it.volumeKg }; if (older > 0) add(Insight("Carga de entrenamiento", if (recent >= older) "Tu volumen reciente está por encima del tramo anterior." else "Tu volumen reciente ha bajado; puede ser recuperación o una oportunidad para retomar ritmo.", "▥")) }; balance.firstOrNull()?.let { add(Insight("Foco principal", "${it.name} concentra la mayor parte de tu volumen en este periodo.", "◈")) } }.take(3)
internal fun longestStreak(sessions: List<TrainingSessionHistoryEntry>): Int { val days = sessions.map { localDate(it.timestampMillis) }.distinct().sorted(); var best = 0; var current = 0; var previous: LocalDate? = null; days.forEach { day -> current = if (previous?.plusDays(1) == day) current + 1 else 1; best = max(best, current); previous = day }; return best }
internal fun plannedWorkoutsInRange(trainingDays: Set<DayOfWeek>, days: Int, today: LocalDate = LocalDate.now()): Int =
    (0 until days.coerceAtLeast(0)).count { offset -> today.minusDays(offset.toLong()).dayOfWeek in trainingDays }
internal fun muscleGroup(imageKey: String?, name: String): String { val key = "${imageKey.orEmpty()} ${name.lowercase()}"; return when { listOf("squat", "lunge", "legpress", "legcurl", "quad", "sentadilla", "zancada", "pierna").any { it in key } -> "Piernas"; listOf("deadlift", "row", "pull", "lat", "back", "remo", "dominada", "espalda").any { it in key } -> "Espalda"; listOf("shoulder", "overhead", "lateral", "facepull", "hombro", "militar").any { it in key } -> "Hombros"; listOf("bench", "pushup", "chest", "pec", "pecho", "flexión").any { it in key } -> "Pecho"; listOf("curl", "biceps", "bíceps").any { it in key } -> "Bíceps"; listOf("triceps", "pushdown", "tríceps").any { it in key } -> "Tríceps"; listOf("plank", "bug", "core", "abs", "abdominal", "plancha").any { it in key } -> "Core"; else -> "General" } }
internal fun exerciseMuscleGroups(imageKey: String?, name: String): List<String> =
    ExerciseVisualCatalog.primaryMuscleLabels(imageKey).ifEmpty { listOf(muscleGroup(imageKey, name)) }
internal val esLocale: Locale = Locale.forLanguageTag("es-ES")
internal fun startMillis(daysAgo: Int) = LocalDate.now().minusDays(daysAgo.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
internal fun localDate(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
internal fun shortDate(timestamp: Long) = localDate(timestamp).format(DateTimeFormatter.ofPattern("d MMM", esLocale))
internal fun mediumDate(timestamp: Long) = DateFormat.getDateInstance(DateFormat.MEDIUM, esLocale).format(Date(timestamp))
internal fun formatWeight(valueKg: Double, useImperial: Boolean = false): String {
    val displayed = if (useImperial) valueKg * 2.2046226218 else valueKg
    val unit = if (useImperial) "lb" else "kg"
    return String.format(esLocale, if (displayed < 100) "%.1f $unit" else "%.0f $unit", displayed)
}
internal fun formatMinutes(minutes: Int) = if (minutes < 60) "$minutes min" else if (minutes % 60 == 0) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
internal fun formatDurationSeconds(seconds: Int): String = formatMinutes((seconds.coerceAtLeast(0) + 30) / 60)
internal fun formatDistance(distanceKm: Double, useImperial: Boolean = false): String {
    val displayed = if (useImperial) distanceKm * 0.6213711922 else distanceKm
    val unit = if (useImperial) "mi" else "km"
    return String.format(esLocale, if (displayed < 10) "%.2f $unit" else "%.1f $unit", displayed)
}
