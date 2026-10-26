package io.codepassion.doubletriangle

import android.content.Context
import android.content.res.Configuration
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.wildforce.android.R
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsPoint
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsSummary
import io.codepassion.doubletriangle.feature.workout.ExerciseVisualCatalog
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.feature.workout.exerciseImageUrl
import io.codepassion.doubletriangle.feature.workout.exerciseDisplayName
import io.codepassion.doubletriangle.feature.workout.localizedWorkoutLabel
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import kotlin.math.*

internal enum class AnalyticsTimeframe(
    val days: Int?,
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int,
) {
    SevenDays(7, "7D", "Mostrando los últimos 7 días", R.string.ios_catalog_d50aade27375357f, R.string.ios_catalog_5ebd2b999268b810),
    TwoWeeks(14, "2W", "Mostrando las últimas 2 semanas", R.string.ios_catalog_4676a110a97fbc15, R.string.ios_catalog_86e836d797f5267c),
    ThirtyDays(30, "30D", "Mostrando los últimos 30 días", R.string.ios_catalog_0ef5849949bcf7b6, R.string.ios_catalog_03e400240a8afc6c),
    TwelveWeeks(84, "12W", "Mostrando las últimas 12 semanas", R.string.ios_catalog_a4a1c512469926e7, R.string.ios_catalog_9bee5fa62d5b224c),
    AllTime(null, "All", "Mostrando todo el historial", R.string.ios_catalog_a52ace420f2175d0, R.string.ios_catalog_22360a5e0faf7473);
    val comparesPlan get() = this != AllTime
}

private enum class AnalyticsExerciseCategory(val label: String) {
    Strength("Fuerza"),
    Bodyweight("Peso corporal"),
    Cardio("Cardio"),
    Mobility("Movilidad"),
    Hiit("HIIT"),
}

private val analyticsBodyweightExerciseKeys = setOf(
    "airSquat", "walkingLunge", "reverseLunge", "stepUp", "gluteBridge", "boxJump",
    "pushUp", "declinePushUp", "tricepsDip", "chestDip", "ringDip", "pullUp", "chinUp",
    "plank", "sidePlank", "deadBug", "birdDog", "russianTwist", "bicycleCrunch",
    "hangingLegRaise", "lyingLegRaise", "pikePushUp", "superman", "handReleasePushUp",
)

private val analyticsCardioExerciseKeys = setOf(
    "walking", "outdoorRunning", "treadmillWalking", "treadmillRunning", "stationaryBike",
    "elliptical", "rowing", "jumpRope", "stairClimber", "skiErgWork", "boxingBagWork",
)

private val analyticsHiitExerciseKeys = setOf(
    "mountainClimber", "burpee", "jumpingJacks", "highKnees", "squatJump", "butterflySitUp",
    "sprawl", "climber",
)

private val analyticsMobilityExerciseKeys = setOf(
    "catCow", "hipFlexorStretch", "hamstringStretch", "thoracicRotation", "shoulderMobilityFlow",
    "worldGreatestStretch", "pigeonPose", "cobraStretch", "childsPose", "inchworm", "armCircles",
    "legSwings", "wristCircles", "ankleCircles", "deepSquatHold", "scapularCircles", "quadStretch",
    "doorwayChestStretch", "scorpions", "threadTheNeedle", "neckCircles",
)

private fun analyticsExerciseCategoryType(exercise: ExerciseSummary): AnalyticsExerciseCategory {
    val key = exercise.imageKey.orEmpty().trim()
    return when {
        key in analyticsBodyweightExerciseKeys -> AnalyticsExerciseCategory.Bodyweight
        key in analyticsCardioExerciseKeys -> AnalyticsExerciseCategory.Cardio
        key in analyticsHiitExerciseKeys -> AnalyticsExerciseCategory.Hiit
        key in analyticsMobilityExerciseKeys -> AnalyticsExerciseCategory.Mobility
        exercise.trackingMode == ExerciseTrackingMode.DurationAndDistance -> AnalyticsExerciseCategory.Cardio
        exercise.trackingMode == ExerciseTrackingMode.Duration -> AnalyticsExerciseCategory.Bodyweight
        else -> AnalyticsExerciseCategory.Strength
    }
}

internal fun analyticsExerciseCategory(exercise: ExerciseSummary): String = analyticsExerciseCategoryType(exercise).label

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
        // iOS UISegmentedControl uses the system fill for the unselected
        // track, which is visibly darker than the page background.
        inset = Color(0xFFE5E5EA), elevated = Color.White,
        border = WildforceThemeTokens.textPrimary.copy(alpha = .05f),
        accentText = WildforceThemeTokens.accent,
        accentSoft = WildforceThemeTokens.accent.copy(alpha = .14f),
        positive = Color(0xFF34C759), orange = Color(0xFFFF9500), red = Color(0xFFFF3B30),
    )
} else {
    AnalyticsColors(
        card = WildforceThemeTokens.backgroundSecondary,
        inset = Color(0xFF3A3A3C), elevated = Color(0xFF48484A),
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

internal fun exerciseHistoryForDetail(
    histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>,
    exercise: ExerciseSummary,
): List<ExerciseAnalyticsPoint> = histories.entries
    .firstOrNull { it.key.exercise == exercise }
    ?.value
    .orEmpty()

internal fun analyticsDataIsEmpty(
    data: AnalyticsData,
    allTimeCompletedWorkoutCount: Int = data.sessions.size,
    allTimeUniqueExercisesCompleted: Int = data.exercises.size,
): Boolean =
    // Mirrors iOS GlobalAnalyticsSummary.isEmpty: selecting a quiet recent
    // period does not turn an account with older history into a first-use
    // empty state.
    allTimeCompletedWorkoutCount == 0 && allTimeUniqueExercisesCompleted == 0

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
        val cutoff = timeframe.days?.let { analyticsStartMillis(timeframe) }
        val histories = allHistories.mapValues { (_, points) ->
            filteredAnalyticsHistory(points, timeframe)
        }
        val summaries = exerciseSummaries.mapNotNull { summary ->
            val points = histories[summary].orEmpty()
            if (points.isEmpty()) null else {
                summary.copy(
                    sessions = points.size,
                    personalBestKg = points.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                    totalVolumeKg = points.sumOf { it.volumeKg },
                    lastFeedback = points.firstOrNull()?.feedback,
                ) to (points.maxOfOrNull { it.timestampMillis } ?: 0L)
            }
        }.sortedWith(
            compareByDescending<Pair<ExerciseAnalyticsSummary, Long>> { it.second }
                .thenByDescending { it.first.sessions }
                .thenBy { exerciseDisplayName(it.first.exercise.imageKey, it.first.exercise.name) },
        ).map { it.first }
        AnalyticsData(
            allSessions.filter { cutoff == null || it.timestampMillis >= cutoff },
            summaries,
            histories,
        )
    }
    selected?.let { exercise ->
        // The dashboard/list is timeframe-filtered, but iOS's
        // ExerciseAnalyticsView loads the selected exercise's complete
        // history after navigation.
        val selectedHistory = exerciseHistoryForDetail(allHistories, exercise.exercise)
        ExerciseAnalyticsDetail(exercise, selectedHistory, contentPadding, gender, useImperial) { selected = null }
        return
    }

    val completed = data.sessions.size
    val planned = timeframe.days?.let { plannedWorkoutsInRange(state.trainingDays, it) } ?: 0
    val volume = data.sessions.sumOf { it.volumeKg }.takeIf { it > 0 } ?: data.exercises.sumOf { it.totalVolumeKg }
    val averageVolume = averageVolumePerWorkout(data.sessions)
    val minutes = totalActiveMinutes(data.sessions)
    val averageMinutes = averageActiveMinutes(data.sessions)
    val longest = longestStreakForAnalytics(allSessions, preferences.getInt("longest_streak", 0))
    val appLanguage = preferences.getString("app_language", "Español") ?: "Español"
    // iOS bases its empty state on the account's all-time history, not on the
    // currently selected timeframe. Older workouts therefore keep the
    // dashboard populated when a recent range happens to be quiet.
    val empty = analyticsDataIsEmpty(
        data = data,
        allTimeCompletedWorkoutCount = allSessions.size,
        allTimeUniqueExercisesCompleted = exerciseSummaries.size,
    )

    Column(
        Modifier.fillMaxSize()
            .liquidGlassBackground()
            .padding(contentPadding)
            // The root navigation and Resume accessory are floating overlays.
            // Keep the background full-height; the trailing scroll padding
            // below lets the final card clear the controls when scrolled.
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding().coerceAtLeast(160.dp),
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnalyticsHeader(
            context = context,
            appLanguage = appLanguage,
            timeframe = timeframe,
            onChange = { timeframe = it },
            last = allSessions.maxByOrNull { it.timestampMillis },
        )
        if (empty) EmptyAnalyticsCard() else {
            KpiStrip(buildList {
                add(Kpi("Entrenamientos totales", completed.toString(), if (timeframe.comparesPlan) "$completed de $planned planificados" else "", "✓"))
                add(Kpi("Volumen total", formatWeight(volume, useImperial), averageVolume?.let { "Media de ${formatWeight(it, useImperial)} por entrenamiento" }.orEmpty(), "▥"))
                add(Kpi("Racha actual", state.user.currentStreak.toString(), "Máxima $longest", "♨"))
                if (timeframe.comparesPlan) add(Kpi("Consistencia", if (planned == 0) "Sin plan" else "${analyticsCompletionPercentage(completed, planned)}%", if (planned == 0) "No hay días programados" else "$completed de $planned planificados", "▣"))
                else if (minutes > 0) add(Kpi("Tiempo entrenando", formatMinutes(minutes), averageMinutes?.let { "Media de ${formatMinutes(it)}" }.orEmpty(), "◷"))
            })
            val buckets = volumeBuckets(data, timeframe)
            if (buckets.any { it.second > 0 }) DashboardCard {
                SectionHeader("Tendencia de volumen", timeframe.description, "▥")
                VolumeTrendSummary(
                    totalVolumeKg = volume,
                    previousVolumeKg = previousAnalyticsVolume(allSessions, timeframe),
                    useImperial = useImperial,
                )
                VolumeChart(
                    points = buckets,
                    timeframe = timeframe,
                    useImperial = useImperial,
                    averageVolumeKg = averageActiveVolume(buckets.map { it.second }),
                )
            }
            val records = personalRecords(data.histories, useImperial)
            if (records.isNotEmpty()) PersonalRecords(records)
            val focus = analyticsFocusDistribution(data.sessions)
            AnalyticsMiniCharts(data.sessions, focus, appLanguage)
            val balance = muscleDistribution(data.histories)
            if (balance.isNotEmpty()) MuscleSection("Equilibrio muscular", timeframe.description, null, balance, false)
            val fatigue = muscleFatigue(data.histories)
            if (fatigue.isNotEmpty()) MuscleSection("Fatiga muscular", timeframe.description, "Estrés de entrenamiento reciente por grupo muscular, dando más peso a las sesiones más nuevas.", fatigue, true)
            if (data.sessions.isNotEmpty()) ConsistencyHeatmap(data.sessions, timeframe)
            val insights = analyticsInsights(
                data = data,
                currentStreak = state.user.currentStreak,
                longestStreak = longest,
                timeframe = timeframe,
                plannedWorkoutCount = planned,
                previousVolumeKg = previousAnalyticsVolume(allSessions, timeframe),
            )
            if (insights.isNotEmpty()) InsightsCard(insights)
            ExerciseList(timeframe, data.exercises, gender, useImperial) { selected = it }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AnalyticsHeader(
    context: Context,
    appLanguage: String,
    timeframe: AnalyticsTimeframe,
    onChange: (AnalyticsTimeframe) -> Unit,
    last: TrainingSessionHistoryEntry?,
) {
    val colors = analyticsColors()
    val localizedResources = remember(appLanguage) {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(AppLocale.languageTag(appLanguage)))
        }
        context.createConfigurationContext(configuration).resources
    }
    val fontScale = LocalDensity.current.fontScale
    val timeframeSelectorHeight = maxOf(32f, 38f * fontScale).dp
    val timeframeLabelMaxLines = if (fontScale > 1.15f) 2 else 1
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(localizedResources.getString(R.string.ios_catalog_94c116ee118a7299), fontFamily = Exo2FontFamily, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text(localizedResources.getString(timeframe.descriptionRes), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
        // Match SwiftUI's compact .pickerStyle(.segmented) geometry.
        Row(
            Modifier
                .fillMaxWidth()
                .requiredHeight(timeframeSelectorHeight)
                .background(colors.inset, RoundedCornerShape(10.dp))
                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(2.dp),
        ) {
            AnalyticsTimeframe.entries.forEach { option ->
                val selected = option == timeframe
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent)
                        .semantics {
                            contentDescription = localizedResources.getString(option.labelRes)
                            stateDescription = if (selected) "Seleccionado" else "No seleccionado"
                            this.selected = selected
                        }
                        .clickable { onChange(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        // iOS feeds AnalyticsTimeframe.shortLabel through the
                        // shared catalog, so Spanish and other locales keep
                        // their translated compact/long variants.
                        localizedResources.getString(option.labelRes),
                        fontFamily = FontFamily.Default,
                        // Match the system label size used by iOS's
                        // UISegmentedControl instead of shrinking the
                        // localized labels into the segment.
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = timeframeLabelMaxLines,
                        softWrap = timeframeLabelMaxLines > 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
        last?.let {
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = "Último entrenamiento ${mediumDate(it.timestampMillis)}" },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = WildforceThemeTokens.textSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    "Último entrenamiento ${mediumDate(it.timestampMillis)}",
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        }
    }
}

@Composable private fun EmptyAnalyticsCard() = DashboardCard {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Badge("▥", 48)
        Text(stringResource(R.string.ios_catalog_43830f6a6aa83027), style = MaterialTheme.typography.h6, modifier = Modifier.padding(top = 14.dp))
        Text(stringResource(R.string.ios_catalog_0d773880d977fca0), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable internal fun DashboardCard(content: @Composable () -> Unit) {
    val colors = analyticsColors()
    Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
}
@Composable internal fun Badge(glyph: String, size: Int = 34) {
    val colors = analyticsColors()
    var appeared by remember(glyph) { mutableStateOf(false) }
    LaunchedEffect(glyph) { appeared = true }
    // Equivalent to the SwiftUI symbol bounce used by analytics cards: each
    // contextual badge gets one restrained spring entrance rather than a
    // permanently animated icon.
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else .72f,
        animationSpec = spring(dampingRatio = .58f, stiffness = 520f),
        label = "analytics-badge-$glyph",
    )
    Box(
        Modifier.size(size.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .background(colors.accentSoft, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(analyticsIcon(glyph), contentDescription = null, tint = colors.accentText, modifier = Modifier.size((size * .46f).dp)) }
}

/** Maps the legacy analytics glyph keys to the SF-Symbol-like Material icon used on Android. */
private fun analyticsIcon(glyph: String): ImageVector = when (glyph) {
    "✓" -> Icons.Filled.CheckCircle
    "▥" -> Icons.Filled.BarChart
    "♨", "🔥" -> Icons.Filled.LocalFireDepartment
    "▣", "▦" -> Icons.Filled.CalendarToday
    "◷", "⌛" -> Icons.Filled.AccessTime
    // iOS uses `figure.mixed.cardio` for muscle balance; this is the closest
    // filled Material counterpart and avoids the crossed-dumbbell glyph.
    "◈" -> Icons.Filled.SportsGymnastics
    "◉" -> Icons.Filled.PieChart
    "☷" -> Icons.Filled.ViewWeek
    "#" -> Icons.Filled.Tag
    "⌁" -> Icons.Filled.Straighten
    "↔" -> Icons.Filled.SwapHoriz
    "✦" -> Icons.Filled.AutoAwesome
    "★" -> Icons.Filled.Star
    "↑" -> Icons.Filled.TrendingUp
    else -> Icons.Filled.Info
}
@Composable internal fun SectionHeader(title: String, subtitle: String, glyph: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Badge(glyph, 38)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.h6); Text(subtitle, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun KpiStrip(items: List<Kpi>) {
    val colors = analyticsColors()
    // iOS keeps the compact 164pt card at the default size, but Dynamic Type
    // grows the content instead of allowing Compose to split a word in the
    // middle. Give the horizontal strip a little more room at large Android
    // font scales while preserving the normal iOS-equivalent geometry.
    val cardWidth = if (LocalDensity.current.fontScale > 1.15f) 204.dp else 164.dp
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(Modifier.width(cardWidth).background(colors.card, RoundedCornerShape(22.dp)).border(1.dp, colors.border, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Badge(item.glyph); AnimatedKpiValue(item.value)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(item.title, style = MaterialTheme.typography.body2); if (item.subtitle.isNotEmpty()) Text(item.subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2) }
            }
        }
    }
}

@Composable
private fun AnimatedKpiValue(value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (fadeIn(tween(150)) + scaleIn(tween(180), initialScale = .88f)) togetherWith
                (fadeOut(tween(110)) + scaleOut(tween(120), targetScale = 1.08f))
        },
        label = "analytics-kpi-value",
    ) { displayed ->
        Text(displayed, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VolumeTrendSummary(totalVolumeKg: Double, previousVolumeKg: Double?, useImperial: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(formatWeight(totalVolumeKg, useImperial), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Spacer(Modifier.weight(1f))
        previousVolumeKg?.takeIf { it > 0.0 }?.let { previous ->
            val change = (totalVolumeKg - previous) / previous
            val positive = change > 0
            val color = when {
                positive -> Color(0xFF2E9F63)
                change < 0 -> Color(0xFFE38A2E)
                else -> WildforceThemeTokens.textSecondary
            }
            Text(
                "${if (positive) "↗" else if (change < 0) "↘" else "="} ${if (change > 0) "+" else ""}${(change * 100).roundToInt()}% vs anterior",
                modifier = Modifier.background(color.copy(alpha = .12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable private fun VolumeChart(
    points: List<Pair<String, Double>>,
    timeframe: AnalyticsTimeframe? = null,
    useImperial: Boolean = false,
    averageVolumeKg: Double? = null,
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val max = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val grid = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
    val colors = analyticsColors()
    val accent = WildforceThemeTokens.accent
    val averageReference = WildforceThemeTokens.textSecondary.copy(alpha = .7f)
    val labelIndices = remember(points, timeframe) {
        volumeChartLabelIndices(points.map { it.first }, timeframe)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(188.dp)) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val step = size.width / points.size.toFloat().coerceAtLeast(1f)
                                selectedIndex = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                            }
                        }
                    },
            ) {
                val height = size.height - 12.dp.toPx()
                repeat(4) { i -> drawLine(grid, Offset(0f, height * i / 3), Offset(size.width, height * i / 3), 1.dp.toPx()) }
                val step = size.width / points.size.coerceAtLeast(1)
                points.forEachIndexed { i, point ->
                    val h = height * (point.second / max).toFloat()
                    val w = min(18.dp.toPx(), step * .45f)
                    drawRoundRect(accent, Offset(step * i + (step - w) / 2, height - h), Size(w, h), CornerRadius(w / 2))
                }

                // iOS uses a reference line for the average of active buckets;
                // rest days do not lower this value and there is no rolling trend.
                averageVolumeKg?.takeIf { it > 0.0 }?.let { average ->
                    val y = height - height * (average / max).toFloat()
                    drawLine(
                        averageReference,
                        Offset(0f, y),
                        Offset(size.width, y),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                }

                selectedIndex?.let { index ->
                    points.getOrNull(index)?.let { point ->
                        val x = step * index + step / 2
                        val y = height - height * (point.second / max).toFloat()
                        drawLine(
                            accent.copy(alpha = .3f),
                            Offset(x, 0f),
                            Offset(x, height),
                            strokeWidth = 2.dp.toPx(),
                        )
                        drawCircle(colors.card, 6.dp.toPx(), Offset(x, y))
                        drawCircle(accent, 5.dp.toPx(), Offset(x, y))
                    }
                }
            }
            selectedIndex?.let { index ->
                points.getOrNull(index)?.let { point ->
                    val tooltipWidth = 132.dp
                    val x = (maxWidth * ((index + .5f) / points.size.coerceAtLeast(1)))
                        .minus(tooltipWidth / 2)
                        .coerceIn(0.dp, (maxWidth - tooltipWidth).coerceAtLeast(0.dp))
                    val plotHeight = 176.dp
                    val y = (plotHeight * (1f - (point.second / max).toFloat()) - 40.dp)
                        .coerceIn(20.dp, 148.dp)
                    Text(
                        "${formatWeight(point.second, useImperial)}\n${point.first}",
                        style = MaterialTheme.typography.caption,
                        color = WildforceThemeTokens.textPrimary,
                        modifier = Modifier
                            .offset(x = x, y = y)
                            .background(colors.inset, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        averageVolumeKg?.takeIf { it > 0.0 }?.let { average ->
            Text("Media ${formatWeight(average, useImperial)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            points.forEachIndexed { index, point ->
                Text(
                    if (index in labelIndices) point.first else "",
                    style = MaterialTheme.typography.overline,
                    color = WildforceThemeTokens.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Matches the iOS chart axis density without dropping the underlying daily
 * bars: short ranges use narrow weekday labels, 30 days use weekly marks and
 * longer ranges use month changes.
 */
internal fun volumeChartLabelIndices(labels: List<String>, timeframe: AnalyticsTimeframe?): Set<Int> {
    if (labels.isEmpty()) return emptySet()
    val last = labels.lastIndex
    return when (timeframe) {
        AnalyticsTimeframe.SevenDays, AnalyticsTimeframe.TwoWeeks -> labels.indices.toSet()
        AnalyticsTimeframe.ThirtyDays -> buildSet {
            add(0)
            labels.indices.filter { it % 7 == 0 }.forEach(::add)
        }
        AnalyticsTimeframe.TwelveWeeks, AnalyticsTimeframe.AllTime -> buildSet {
            add(0)
            labels.indices.zipWithNext().forEach { (index, next) ->
                val month = labels[index].substringAfterLast(' ')
                val nextMonth = labels[next].substringAfterLast(' ')
                if (month != nextMonth) add(next)
            }
            if (last > 0 && labels[last].substringAfterLast(' ') != labels[last - 1].substringAfterLast(' ')) {
                add(last)
            }
        }
        null -> buildSet {
            add(0)
            val stride = (last / 4).coerceAtLeast(1)
            var index = stride
            while (index < last) {
                add(index)
                index += stride
            }
            add(last)
        }
    }
}

@Composable
private fun AnalyticsMiniCharts(
    sessions: List<TrainingSessionHistoryEntry>,
    focus: List<Pair<String, Int>>,
    appLanguage: String,
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
                FocusChart(focus, appLanguage)
            }
        }

        if (wideLayout) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sessions.isNotEmpty()) Box(Modifier.weight(1f)) { DashboardCard { SectionHeader("Cuándo entrenas", "Entrenamientos completados por día de la semana", "◷"); WeekdayChart(sessions) } }
                if (focus.isNotEmpty()) Box(Modifier.weight(1f)) { DashboardCard { SectionHeader("Enfoque del entrenamiento", "Distribución de sesiones en este periodo", "◉"); FocusChart(focus, appLanguage) } }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

/** Matches iOS focusDistribution: only completed sessions in the active range,
 * sorted by count and then by the stable English focus name. */
internal fun analyticsFocusDistribution(sessions: List<TrainingSessionHistoryEntry>): List<Pair<String, Int>> {
    return sessions
        .mapNotNull { it.focus?.trim()?.takeIf(String::isNotBlank) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { analyticsFocusEnglishName(it.key) })
        .map { it.key to it.value }
}

private fun analyticsFocusEnglishName(raw: String): String = when (raw.lowercase(Locale.ROOT).replace("_", "")) {
    "fullbody" -> "Full Body"
    "upperbody" -> "Upper Body"
    "lowerbody" -> "Lower Body"
    "push" -> "Push"
    "pull" -> "Pull"
    "legs" -> "Legs"
    "core" -> "Core"
    "cardio" -> "Cardio"
    "mobility" -> "Mobility"
    "recovery" -> "Recovery"
    else -> raw
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
                record.delta?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.positive)
                        Text(it, style = MaterialTheme.typography.caption, color = colors.positive)
                    }
                }
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

@Composable private fun FocusChart(distribution: List<Pair<String, Int>>, appLanguage: String) {
    val colors = listOf(WildforceThemeTokens.accent, Color(0xFF007AFF), Color(0xFF34C759), Color(0xFFFF9500))
    val total = distribution.sumOf { it.second }.coerceAtLeast(1)
    // iOS uses SectorMark with a bottom legend. Keep the ring centered and put
    // the focus names in one compact horizontal legend instead of the Android-
    // only vertical list with counts.
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.size(180.dp)) {
            var start = -90f
            distribution.forEachIndexed { i, item ->
                val sweep = item.second * 360f / total
                drawArc(colors[i % colors.size], start, sweep, false, style = Stroke(28.dp.toPx()))
                start += sweep
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            distribution.forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(colors[i % colors.size], CircleShape))
                    Text(localizedWorkoutLabel(item.first, appLanguage), style = MaterialTheme.typography.caption, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun MuscleSection(title: String, subtitle: String, description: String?, scores: List<MuscleScore>, fatigue: Boolean) {
    var radar by remember { mutableStateOf(false) }
    val colors = analyticsColors()
    DashboardCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { SectionHeader(title, subtitle, if (fatigue) "♨" else "◈") }
            Row(Modifier.background(colors.inset, RoundedCornerShape(9.dp)).border(1.dp, colors.border, RoundedCornerShape(9.dp)).padding(2.dp)) {
                listOf(false to Icons.Filled.BarChart, true to Icons.Filled.Hexagon).forEach { option ->
                    Box(
                        Modifier.size(40.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (radar == option.first) colors.elevated else Color.Transparent)
                            .clickable { radar = option.first },
                        contentAlignment = Alignment.Center,
                    ) { Icon(option.second, contentDescription = if (option.first) "Gráfico radial" else "Gráfico de barras", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(19.dp)) }
                }
            }
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

@Composable private fun ConsistencyHeatmap(sessions: List<TrainingSessionHistoryEntry>, timeframe: AnalyticsTimeframe) = DashboardCard {
    val colors = analyticsColors()
    val today = LocalDate.now()
    val firstDay = if (timeframe == AnalyticsTimeframe.AllTime) {
        sessions.minOfOrNull { localDate(it.timestampMillis) } ?: today
    } else {
        analyticsStartDate(timeframe, today)
    }
    val days = generateSequence(firstDay) { day -> day.plusDays(1).takeIf { it <= today } }.toList()
    val counts = sessions.groupingBy { localDate(it.timestampMillis) }.eachCount()
    val activeDays = counts.keys.count { it in days }
    SectionHeader("Consistencia", "${sessions.size} sesiones en $activeDays días activos", "▦")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { days.chunked(12).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { row.forEach { date -> val level = (counts[date] ?: 0).coerceAtMost(3); Box(Modifier.weight(1f).height(18.dp).background(if (level == 0) WildforceThemeTokens.textSecondary.copy(alpha = .12f) else WildforceThemeTokens.accent.copy(alpha = if (level == 1) .35f else if (level == 2) .6f else 1f), RoundedCornerShape(4.dp))) } } } }
}

@Composable private fun InsightsCard(insights: List<Insight>) = DashboardCard {
    SectionHeader("Claves", "Señales breves de tus entrenamientos recientes", "✦")
    insights.forEachIndexed { i, insight -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) { Badge(insight.glyph, 36); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(insight.title, style = MaterialTheme.typography.subtitle2); Text(insight.body, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) } }; if (i < insights.lastIndex) Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .15f)) }
}

@Composable private fun ExerciseList(timeframe: AnalyticsTimeframe, exercises: List<ExerciseAnalyticsSummary>, gender: String, useImperial: Boolean, onSelect: (ExerciseAnalyticsSummary) -> Unit) = DashboardCard {
    var query by remember { mutableStateOf("") }; val filtered = exercises.filter { exerciseDisplayName(it.exercise.imageKey, it.exercise.name).contains(query, true) }
    val colors = analyticsColors()
    SectionHeader(if (timeframe == AnalyticsTimeframe.AllTime) "Todos los ejercicios completados" else "Ejercicios de este periodo", if (timeframe == AnalyticsTimeframe.AllTime) "Historial completo de ejercicios" else "Desglose de ejercicios completados recientemente", "☷")
    if (exercises.isEmpty()) Text(if (timeframe == AnalyticsTimeframe.AllTime) "Todavía no hay ejercicios completados." else "No hay ejercicios completados en este periodo.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) else {
        Row(Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = WildforceThemeTokens.textSecondary); BasicTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.body2.copy(color = WildforceThemeTokens.textPrimary), decorationBox = { inner -> if (query.isEmpty()) Text("Buscar ejercicios", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); inner() }); if (query.isNotEmpty()) Box(Modifier.size(48.dp).clickable { query = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda", modifier = Modifier.size(24.dp), tint = WildforceThemeTokens.textSecondary) }
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
            contentDescription = exerciseDisplayName(summary.exercise.imageKey, summary.exercise.name),
            modifier = Modifier.size(width = 56.dp, height = 68.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(exerciseDisplayName(summary.exercise.imageKey, summary.exercise.name), style = MaterialTheme.typography.subtitle1, maxLines = 2); Text(analyticsExerciseCategory(summary.exercise), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${summary.sessions} completados", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
        Column(horizontalAlignment = Alignment.End) { if (summary.personalBestKg > 0) { Text(formatWeight(summary.personalBestKg, useImperial), style = MaterialTheme.typography.subtitle1); Text("MEJOR PESO", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary) }; Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp), tint = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun ExerciseAnalyticsDetail(summary: ExerciseAnalyticsSummary, history: List<ExerciseAnalyticsPoint>, padding: PaddingValues, gender: String, useImperial: Boolean, back: () -> Unit) {
    val colors = analyticsColors()
    Column(
        Modifier.fillMaxSize()
            .liquidGlassBackground()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding().coerceAtLeast(160.dp),
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.size(48.dp).background(colors.elevated, CircleShape).border(1.dp, colors.border, CircleShape).clickable(onClick = back), contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowBack, "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(
                url = exerciseImageUrl(summary.exercise.imageKey, gender),
                contentDescription = exerciseDisplayName(summary.exercise.imageKey, summary.exercise.name),
                modifier = Modifier.size(width = 84.dp, height = 100.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(exerciseDisplayName(summary.exercise.imageKey, summary.exercise.name), fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold); Text(analyticsExerciseCategory(summary.exercise), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${history.size} ejercicios completados en total", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary); Text("${history.count { it.timestampMillis >= startMillis(29) }} en los últimos 30 días", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
        }
        if (history.isEmpty()) DashboardCard { Text("Completa este ejercicio en un entrenamiento para desbloquear analíticas detalladas.", color = WildforceThemeTokens.textSecondary) }
        else {
            Text("Progresión", style = MaterialTheme.typography.h6); ProgressionChart(history, summary.exercise, useImperial)
            Text("Volumen a lo largo del tiempo", style = MaterialTheme.typography.h6); VolumeChart(exerciseDailyVolumeBuckets(history), useImperial = useImperial)
            exerciseRecordMetrics(history, summary.exercise, useImperial).takeIf { it.isNotEmpty() }?.let { metrics ->
                HighlightGrid(metrics, title = "Récords")
            }
            HighlightGrid(exerciseOverviewMetrics(history), title = "Resumen")
            exerciseCategoryMetrics(history, summary.exercise, useImperial).takeIf { it.isNotEmpty() }?.let { metrics ->
                HighlightGrid(metrics, title = "Rendimiento")
            }
            Text("Sesiones", style = MaterialTheme.typography.h6)
            history.forEach { point ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.inset, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(mediumDate(point.timestampMillis), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                        Text(
                            analyticsSessionSummary(point, summary.exercise, useImperial),
                            style = MaterialTheme.typography.caption,
                            color = WildforceThemeTokens.textSecondary,
                        )
                        point.feedback?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                    if (point.volumeKg > 0) {
                        Text(formatWeight(point.volumeKg, useImperial), color = colors.accentText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun ProgressionChart(history: List<ExerciseAnalyticsPoint>, exercise: ExerciseSummary, useImperial: Boolean) {
    val points = history.reversed().mapNotNull { point -> progressionMetric(point, exercise)?.let { point to it } }
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val max = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val colors = analyticsColors()
    val grid = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
    val accent = WildforceThemeTokens.accent
    Canvas(
        Modifier.fillMaxWidth().height(200.dp).background(colors.inset, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    selectedIndex = ((offset.x / size.width.toFloat().coerceAtLeast(1f)) * points.size)
                        .toInt().coerceIn(0, points.lastIndex)
                }
            },
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3
            drawLine(grid, Offset.Zero.copy(y = y), Offset(size.width, y), 1.dp.toPx())
        }
        if (points.isEmpty()) return@Canvas

        fun pointAt(index: Int, point: Pair<ExerciseAnalyticsPoint, Double>): Offset {
            val x = if (points.size == 1) size.width / 2 else index * size.width / (points.size - 1)
            val y = size.height - (point.second / max).toFloat() * size.height
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
            val position = pointAt(index, point)
            drawCircle(accent, 4.dp.toPx(), position)
            if (selectedIndex == index) {
                drawCircle(colors.inset, 8.dp.toPx(), position)
                drawCircle(accent, 5.dp.toPx(), position)
            }
        }
    }
    selectedIndex?.let { index ->
        points.getOrNull(index)?.let { point ->
            Text("${shortDate(point.first.timestampMillis)} · ${formatProgressionMetric(point.second, point.first, exercise, useImperial)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}

private fun progressionMetric(point: ExerciseAnalyticsPoint, exercise: ExerciseSummary): Double? = when (exercise.trackingMode) {
    ExerciseTrackingMode.Duration -> point.durationSeconds.toDouble().takeIf { it > 0 }
    ExerciseTrackingMode.DurationAndDistance -> point.distanceKm.takeIf { it > 0 } ?: point.durationSeconds.toDouble().takeIf { it > 0 }
    ExerciseTrackingMode.Repetitions -> when {
        point.maxWeightKg > 0 && point.reps > 0 -> point.maxWeightKg / (1.0278 - 0.0278 * point.reps)
        point.maxWeightKg > 0 -> point.maxWeightKg
        point.reps > 0 -> point.reps.toDouble()
        else -> null
    }
}

private fun formatProgressionMetric(value: Double, point: ExerciseAnalyticsPoint, exercise: ExerciseSummary, useImperial: Boolean): String = when (exercise.trackingMode) {
    ExerciseTrackingMode.Duration -> formatDurationSeconds(value.toInt())
    ExerciseTrackingMode.DurationAndDistance -> if (point.distanceKm > 0) formatDistance(value, useImperial) else formatDurationSeconds(value.toInt())
    ExerciseTrackingMode.Repetitions -> if (point.maxWeightKg > 0) formatWeight(value, useImperial) else "${value.roundToInt()} reps"
}

/** Mirrors iOS ExerciseAnalyticsView's four common overview highlights. */
internal fun exerciseOverviewMetrics(history: List<ExerciseAnalyticsPoint>): List<Kpi> {
    if (history.isEmpty()) return emptyList()
    val first = history.minOf { it.timestampMillis }
    val last = history.maxOf { it.timestampMillis }
    return listOf(
        Kpi("Completados", history.size.toString(), glyph = "✓"),
        Kpi("Primera vez", mediumDate(first), glyph = "▣"),
        Kpi("Última vez", mediumDate(last), glyph = "▣"),
        Kpi("Últimos 30 días", history.count { it.timestampMillis >= startMillis(29) }.toString(), glyph = "▦"),
    )
}

/** Mirrors iOS ExerciseAnalyticsViewModel.recordHighlights. */
internal fun exerciseRecordMetrics(
    history: List<ExerciseAnalyticsPoint>,
    exercise: ExerciseSummary,
    useImperial: Boolean,
): List<Kpi> {
    if (history.isEmpty()) return emptyList()

    fun value(points: List<ExerciseAnalyticsPoint>): String? = when (exercise.trackingMode) {
        ExerciseTrackingMode.Repetitions -> points.maxOfOrNull { it.maxWeightKg }
            ?.takeIf { it > 0 }
            ?.let { formatWeight(it, useImperial) }
            ?: points.maxOfOrNull { it.reps }
                ?.takeIf { it > 0 }
                ?.let { "$it reps" }
        ExerciseTrackingMode.Duration -> points.maxOfOrNull { it.durationSeconds }
            ?.takeIf { it > 0 }
            ?.let(::formatDurationSeconds)
        ExerciseTrackingMode.DurationAndDistance -> points.maxOfOrNull { it.distanceKm }
            ?.takeIf { it > 0 }
            ?.let { formatDistance(it, useImperial) }
            ?: points.maxOfOrNull { it.durationSeconds }
                ?.takeIf { it > 0 }
                ?.let(::formatDurationSeconds)
    }

    val latestTimestamp = history.maxOfOrNull { it.timestampMillis } ?: return emptyList()
    val latestSession = history.filter { it.timestampMillis == latestTimestamp }
    return buildList {
        value(history)?.let { add(Kpi("Récord personal", it, glyph = "★")) }
        value(latestSession)?.let { add(Kpi("Último mejor", it, glyph = "↗")) }
    }
}

/** Category-specific highlights corresponding to iOS strength/cardio/mobility metrics. */
private fun exerciseCategoryMetrics(
    history: List<ExerciseAnalyticsPoint>,
    exercise: ExerciseSummary,
    useImperial: Boolean,
): List<Kpi> = buildList {
    when (analyticsExerciseCategoryType(exercise)) {
        AnalyticsExerciseCategory.Strength, AnalyticsExerciseCategory.Bodyweight, AnalyticsExerciseCategory.Hiit -> {
            if (analyticsExerciseCategoryType(exercise) == AnalyticsExerciseCategory.Hiit) {
                history.sumOf { it.durationSeconds }.takeIf { it > 0 }?.let {
                    add(Kpi("Duración total", "$it sec", glyph = "◷"))
                }
            }
            history.maxOfOrNull { it.maxWeightKg }?.takeIf { it > 0 }?.let { add(Kpi("Mejor peso", formatWeight(it, useImperial), glyph = "★")) }
            history.maxOfOrNull { it.reps }?.takeIf { it > 0 }?.let { add(Kpi("Mejores repeticiones", it.toString(), glyph = "#")) }
            history.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 }?.let { add(Kpi("Mejor volumen", formatWeight(it, useImperial), glyph = "▥")) }
        }
        AnalyticsExerciseCategory.Cardio -> {
            history.sumOf { it.distanceKm }.takeIf { it > 0 }?.let { add(Kpi("Distancia total", formatDistance(it, useImperial), glyph = "⌁")) }
            history.sumOf { it.durationSeconds }.takeIf { it > 0 }?.let { add(Kpi("Tiempo total", "${it / 60} min", glyph = "◷")) }
            history.maxOfOrNull { it.distanceKm }?.takeIf { it > 0 }?.let { add(Kpi("Mayor distancia", formatDistance(it, useImperial), glyph = "↔")) }
            history.maxOfOrNull { it.durationSeconds }?.takeIf { it > 0 }?.let { add(Kpi("Mayor duración", "${it / 60} min", glyph = "⌛")) }
            history.mapNotNull { point ->
                if (point.distanceKm > 0 && point.durationSeconds > 0) point.durationSeconds / point.distanceKm else null
            }.minOrNull()?.let { add(Kpi("Ritmo más rápido", formatPaceSecondsPerKm(it, useImperial), glyph = "◉")) }
        }
        AnalyticsExerciseCategory.Mobility -> {
            add(Kpi("Sesiones totales", history.size.toString(), glyph = "✓"))
            history.sumOf { it.durationSeconds }.takeIf { it > 0 }?.let { add(Kpi("Tiempo total", "${it / 60} min", glyph = "◷")) }
            history.maxOfOrNull { it.durationSeconds }?.takeIf { it > 0 }?.let { add(Kpi("Aguante más largo", "${it / 60} min", glyph = "⌛")) }
        }
    }
}

private fun formatPaceSecondsPerKm(secondsPerKm: Double, useImperial: Boolean): String {
    val secondsPerUnit = if (useImperial) secondsPerKm / 0.621371 else secondsPerKm
    val totalSeconds = secondsPerUnit.roundToInt().coerceAtLeast(0)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')} /${if (useImperial) "mi" else "km"}"
}

/** iOS aggregates exercise volume by calendar day instead of rendering one bar per session. */
internal fun exerciseDailyVolumeBuckets(history: List<ExerciseAnalyticsPoint>): List<Pair<String, Double>> = history
    .filter { it.volumeKg > 0 }
    .groupBy { localDate(it.timestampMillis) }
    .toSortedMap()
    .map { (date, volume) -> date.format(DateTimeFormatter.ofPattern("d MMM", esLocale)) to volume.sumOf { it.volumeKg } }

@Composable private fun HighlightGrid(metrics: List<Kpi>, title: String = "Resumen") {
    val colors = analyticsColors()
    Text(title, style = MaterialTheme.typography.h6)
    metrics.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { metric -> Column(Modifier.weight(1f).background(colors.inset, RoundedCornerShape(16.dp)).border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Badge(metric.glyph, 28); AnimatedKpiValue(metric.value); Text(metric.title, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
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

internal fun volumeBuckets(
    data: AnalyticsData,
    timeframe: AnalyticsTimeframe,
    today: LocalDate = LocalDate.now(),
): List<Pair<String, Double>> {
    val source = if (data.sessions.any { it.volumeKg > 0 }) data.sessions.map { it.timestampMillis to it.volumeKg } else data.histories.values.flatten().map { it.timestampMillis to it.volumeKg }
    if (timeframe == AnalyticsTimeframe.AllTime) {
        val currentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val datedSource = source.filter { localDate(it.first) <= today }
        val firstWeek = datedSource.minOfOrNull { (timestamp, _) ->
            val date = localDate(timestamp)
            date.minusDays((date.dayOfWeek.value - 1).toLong())
        } ?: return emptyList()
        val weekCount = ChronoUnit.WEEKS.between(firstWeek, currentWeek).toInt().coerceAtLeast(0) + 1
        return (0 until weekCount).map { index ->
            val week = firstWeek.plusWeeks(index.toLong())
            val total = datedSource
                .filter { (timestamp, _) ->
                    val date = localDate(timestamp)
                    date.minusDays((date.dayOfWeek.value - 1).toLong()) == week
                }
                .sumOf { it.second }
            week.format(DateTimeFormatter.ofPattern("d MMM", esLocale)).uppercase() to total
        }
    }
    val days = timeframe.days ?: return emptyList(); val count = if (days <= 30) days else 12; val start = analyticsStartDate(timeframe, today); val size = days.toDouble() / count; val totals = MutableList(count) { 0.0 }
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
        return PersonalRecord(exerciseDisplayName(summary.exercise.imageKey, summary.exercise.name), title, format(valueOf(best)), best.timestampMillis, glyph, delta)
    }

    buildList {
        record("Mejor peso", "★", history.filter { it.maxWeightKg > 0 }, ExerciseAnalyticsPoint::maxWeightKg) { formatWeight(it, useImperial) }?.let(::add)
        record("Mejores repeticiones", "#", history.filter { it.reps > 0 }, ExerciseAnalyticsPoint::reps) { "$it reps" }?.let(::add)
        record("Mayor volumen", "▥", history.filter { it.volumeKg > 0 }, ExerciseAnalyticsPoint::volumeKg) { formatWeight(it, useImperial) }?.let(::add)
        record("Mayor duración", "⌛", history.filter { it.durationSeconds > 0 }, ExerciseAnalyticsPoint::durationSeconds) { formatDurationSeconds(it) }?.let(::add)
        record("Mayor distancia", "↔", history.filter { it.distanceKm > 0 }, ExerciseAnalyticsPoint::distanceKm) { formatDistance(it, useImperial) }?.let(::add)
    }
}.sortedByDescending(PersonalRecord::recordedAtMillis).take(8)

internal fun muscleDistribution(histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>): List<MuscleScore> = histories
    .flatMap { (summary, points) ->
        val completedDays = points.map { localDate(it.timestampMillis) }.distinct().count()
        if (completedDays == 0) return@flatMap emptyList()
        val (primary, secondary) = ExerciseVisualCatalog.majorMuscleGroups(summary.exercise.imageKey)
        val fallback = if (primary.isEmpty()) listOf(muscleGroup(summary.exercise.imageKey, summary.exercise.name)) else primary
        fallback.map { it to completedDays.toDouble() } + secondary.map { it to completedDays * 0.30 }
    }
    .groupBy { it.first }
    .map { (name, values) -> MuscleScore(name, values.sumOf { it.second }) }
    .filter { it.score > 0 }
    .sortedWith(compareByDescending<MuscleScore> { it.score }.thenBy { it.name })
    .take(7)

internal fun muscleFatigue(histories: Map<ExerciseAnalyticsSummary, List<ExerciseAnalyticsPoint>>): List<MuscleScore> {
    val now = System.currentTimeMillis()
    return histories.flatMap { (summary, points) ->
        val (primary, secondary) = ExerciseVisualCatalog.fatigueMajorMuscleGroups(summary.exercise.imageKey)
        val primaryGroups = if (primary.isEmpty()) listOf(muscleGroup(summary.exercise.imageKey, summary.exercise.name)) else primary
        val groups = primaryGroups.map { it to 1.0 } + secondary.map { it to 0.30 }
        points.mapNotNull { point ->
            val stress = fatigueBaseStress(point, summary.exercise)
            if (stress <= 0.0) return@mapNotNull null
            val elapsedDays = ChronoUnit.DAYS.between(localDate(point.timestampMillis), localDate(now)).coerceAtLeast(0)
            val adjustedStress = stress * fatigueFeedbackMultiplier(point.feedback) * 0.8.pow(elapsedDays.toDouble())
            groups.map { (group, weight) -> group to adjustedStress * weight }
        }.flatten()
    }.groupBy { it.first }.map { (name, values) ->
        val stress = values.sumOf { it.second }
        MuscleScore(name, ((1 - exp(-stress / 1500.0)) * 100).coerceIn(0.0, 100.0))
    }.filter { it.score > 0.0 }
        .sortedWith(compareByDescending<MuscleScore> { it.score }.thenBy { it.name })
        .take(7)
}

private fun fatigueBaseStress(point: ExerciseAnalyticsPoint, exercise: ExerciseSummary): Double = when (exercise.trackingMode) {
    ExerciseTrackingMode.Duration, ExerciseTrackingMode.DurationAndDistance ->
        point.durationSeconds.takeIf { it > 0 }?.div(60.0) ?: 0.0
    ExerciseTrackingMode.Repetitions -> when {
        point.volumeKg > 0.0 -> point.volumeKg
        point.durationSeconds > 0 -> point.durationSeconds / 60.0
        point.reps > 0 -> point.reps.toDouble()
        else -> 0.0
    }
}

private fun fatigueFeedbackMultiplier(feedback: String?): Double = when (feedback.orEmpty().trim().uppercase()) {
    "MUY FÁCIL", "VERY EASY" -> 0.70
    "FÁCIL", "EASY" -> 0.85
    "DIFÍCIL", "HARD" -> 1.15
    "MUY DIFÍCIL", "VERY HARD" -> 1.30
    else -> 1.0
}
internal fun previousAnalyticsVolume(sessions: List<TrainingSessionHistoryEntry>, timeframe: AnalyticsTimeframe, today: LocalDate = LocalDate.now()): Double? {
    val days = timeframe.days ?: return null
    val currentStart = analyticsStartDate(timeframe, today)
    val previousStart = currentStart.minusDays(days.toLong())
    val currentStartMillis = currentStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val previousStartMillis = previousStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return sessions.filter { it.timestampMillis in previousStartMillis until currentStartMillis }.sumOf { it.volumeKg }
        .takeIf { it > 0.0 }
}

internal fun analyticsInsights(
    data: AnalyticsData,
    currentStreak: Int,
    longestStreak: Int,
    timeframe: AnalyticsTimeframe,
    plannedWorkoutCount: Int,
    previousVolumeKg: Double?,
): List<Insight> {
    if (data.sessions.isEmpty()) return emptyList()
    val prioritized = buildList {
        if (currentStreak >= 3 && currentStreak == longestStreak) {
            add(100 to Insight("Has igualado tu mejor racha", "$currentStreak sesiones seguidas igualan tu racha más larga.", "♨"))
        }
        if (timeframe.comparesPlan && plannedWorkoutCount > 0) {
            val completionRate = data.sessions.size.toDouble() / plannedWorkoutCount
            val (title, priority) = when {
                completionRate >= 1.0 -> "Consistencia perfecta" to 95
                completionRate >= .8 -> "Entrenamiento en buen camino" to 88
                completionRate < .5 -> "La consistencia ha bajado" to 80
                else -> "El plan sigue avanzando" to 75
            }
            add(priority to Insight(title, "Has completado ${data.sessions.size} de $plannedWorkoutCount sesiones planificadas en este periodo.", "▣"))
        }
        previousVolumeKg?.takeIf { it > 0.0 }?.let { previous ->
            val current = data.sessions.sumOf { it.volumeKg }
            val change = (current - previous) / previous
            if (abs(change) >= .15) {
                val percent = (abs(change) * 100).roundToInt()
                val direction = if (change > 0) "subido" else "bajado"
                add(84 to Insight("La carga ha $direction un $percent%", "Has registrado ${formatWeight(current)} este periodo frente a ${formatWeight(previous)} en el anterior.", if (change > 0) "↗" else "↘"))
            }
        }
        val topFocus = data.sessions.mapNotNull { it.focus }.groupingBy { it }.eachCount().maxByOrNull { it.value }
        if (topFocus != null && data.sessions.size >= 3 && topFocus.value.toDouble() / data.sessions.size >= .45) {
            add(68 to Insight("Te estás centrando en ${topFocus.key}", "${topFocus.value} de ${data.sessions.size} sesiones se centraron en ${topFocus.key.lowercase()} este periodo.", "◈"))
        }
        val relevantMuscles = muscleDistribution(data.histories)
            .associate { it.name to it.score }
            .filterKeys { it != "General" }
        if (relevantMuscles.size >= 3) {
            val strongest = relevantMuscles.maxByOrNull { it.value }
            val weakest = relevantMuscles.minByOrNull { it.value }
            if (strongest != null && weakest != null && weakest.value <= strongest.value * .45) {
                add(64 to Insight("${weakest.key} está poco representado", "${weakest.key} puntúa bastante menos que tus grupos más entrenados en este periodo.", "◈"))
            }
        }
    }
    return if (prioritized.isEmpty()) {
        listOf(Insight("Has creado un buen impulso", "Has completado ${data.sessions.size} entrenamientos y registrado ${formatWeight(data.sessions.sumOf { it.volumeKg })} en este periodo.", "✦"))
    } else {
        prioritized.sortedWith(compareByDescending<Pair<Int, Insight>> { it.first }.thenBy { it.second.title }).take(3).map { it.second }
    }
}
internal fun averageVolumePerWorkout(sessions: List<TrainingSessionHistoryEntry>): Double? {
    val volumeBearingSessions = sessions.filter { it.volumeKg > 0.0 }
    return volumeBearingSessions.takeIf { it.isNotEmpty() }?.sumOf { it.volumeKg }?.div(volumeBearingSessions.size)
}
/** Mirrors iOS's chart reference: average only buckets that contain training. */
internal fun averageActiveVolume(volumes: List<Double>): Double? {
    val active = volumes.filter { it > 0.0 }
    return active.takeIf { it.isNotEmpty() }?.average()
}
internal fun roundedDurationMinutes(durationSeconds: Int): Int = (durationSeconds.coerceAtLeast(0) / 60.0).roundToInt()
internal fun totalActiveMinutes(sessions: List<TrainingSessionHistoryEntry>): Int = sessions.sumOf { roundedDurationMinutes(it.durationSeconds) }
internal fun averageActiveMinutes(sessions: List<TrainingSessionHistoryEntry>): Int? = sessions.takeIf { it.isNotEmpty() }?.map { roundedDurationMinutes(it.durationSeconds) }?.average()?.roundToInt()
internal fun longestStreak(sessions: List<TrainingSessionHistoryEntry>): Int { val days = sessions.map { localDate(it.timestampMillis) }.distinct().sorted(); var best = 0; var current = 0; var previous: LocalDate? = null; days.forEach { day -> current = if (previous?.plusDays(1) == day) current + 1 else 1; best = max(best, current); previous = day }; return best }
internal fun longestStreakForAnalytics(sessions: List<TrainingSessionHistoryEntry>, persistedLongestStreak: Int): Int = maxOf(longestStreak(sessions), persistedLongestStreak.coerceAtLeast(0))
internal fun plannedWorkoutsInRange(trainingDays: Set<DayOfWeek>, days: Int, today: LocalDate = LocalDate.now()): Int =
    (0 until days.coerceAtLeast(0)).count { offset -> today.minusDays(offset.toLong()).dayOfWeek in trainingDays }
internal fun analyticsCompletionPercentage(completed: Int, planned: Int): Int =
    if (planned <= 0) 0 else (completed.toDouble() / planned.toDouble() * 100.0).roundToInt()
internal fun analyticsStartDate(timeframe: AnalyticsTimeframe, today: LocalDate = LocalDate.now()): LocalDate = when (timeframe) {
    AnalyticsTimeframe.TwelveWeeks -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11)
    AnalyticsTimeframe.AllTime -> today
    else -> today.minusDays(((timeframe.days ?: 1) - 1).toLong())
}
internal fun analyticsStartMillis(timeframe: AnalyticsTimeframe, today: LocalDate = LocalDate.now()) =
    analyticsStartDate(timeframe, today).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
internal fun filteredAnalyticsHistory(
    points: List<ExerciseAnalyticsPoint>,
    timeframe: AnalyticsTimeframe,
    today: LocalDate = LocalDate.now(),
): List<ExerciseAnalyticsPoint> {
    val cutoff = timeframe.days?.let { analyticsStartMillis(timeframe, today) }
    return points.filter { cutoff == null || it.timestampMillis >= cutoff }
}
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
internal fun formatDurationSeconds(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    if (safeSeconds < 60) return "$safeSeconds sec"
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return if (remainingSeconds == 0) "$minutes min" else "%d:%02d".format(esLocale, minutes, remainingSeconds)
}
internal fun analyticsSessionSummary(
    point: ExerciseAnalyticsPoint,
    exercise: ExerciseSummary,
    useImperial: Boolean,
): String = buildList {
    when (exercise.trackingMode) {
        ExerciseTrackingMode.Repetitions -> {
            if (point.reps > 0) add("${point.reps} reps")
            if (point.maxWeightKg > 0) add(formatWeight(point.maxWeightKg, useImperial))
            if (point.volumeKg > 0) add("Volumen ${formatWeight(point.volumeKg, useImperial)}")
        }
        ExerciseTrackingMode.Duration -> if (point.durationSeconds > 0) {
            add(formatDurationSeconds(point.durationSeconds))
        }
        ExerciseTrackingMode.DurationAndDistance -> {
            if (point.durationSeconds > 0) add(formatDurationSeconds(point.durationSeconds))
            if (point.distanceKm > 0) add(formatDistance(point.distanceKm, useImperial))
        }
    }
}.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Completado"
internal fun formatDistance(distanceKm: Double, useImperial: Boolean = false): String {
    val displayed = if (useImperial) distanceKm * 0.6213711922 else distanceKm
    val unit = if (useImperial) "mi" else "km"
    return String.format(esLocale, if (displayed < 10) "%.2f $unit" else "%.1f $unit", displayed)
}
