package io.codepassion.doubletriangle.feature.workout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import kotlinx.coroutines.delay

private enum class CompletionPhase {
    DurationWarning,
    Records,
    Summary,
    Score,
    Streak,
    WorkoutXp,
    WorkoutLevelUp,
    PlanComplete,
    MesocycleComplete,
    PlanXp,
    PlanLevelUp,
    MesocycleXp,
    MesocycleLevelUp,
    NotificationRequest,
    RateApp,
    PlanGenerator,
}

private data class CompletionMetric(
    val title: String,
    val value: String,
    val icon: ImageVector,
)

internal fun shouldOfferNextPlan(isPlanCompleted: Boolean, canGenerateNextPlan: Boolean): Boolean =
    isPlanCompleted && canGenerateNextPlan

@Composable
internal fun WorkoutCompletionFlowScreen(
    workout: WorkoutDaySummary,
    durationSeconds: Int,
    stats: Map<Int, ExerciseSessionStats>,
    feedback: Map<Int, String>,
    records: List<ExerciseRecordEvent>,
    progress: CompletionProgress,
    isPlanCompleted: Boolean = false,
    isMesocycleCompleted: Boolean = false,
    planName: String = "",
    completedPlanWorkouts: Int = 0,
    totalPlanExercises: Int = 0,
    mesocycleIndex: Int = 0,
    cycleLength: Int = 0,
    skippedExerciseIndices: Set<Int> = emptySet(),
    useImperial: Boolean = false,
    userAgeYears: Int = 30,
    canGenerateNextPlan: Boolean = true,
    onCancelWorkout: () -> Unit,
    onCompletionConfirmed: () -> Unit,
    onDone: () -> Unit,
    onGenerateNextPlan: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val firstRegularPhase = if (records.isEmpty()) CompletionPhase.Summary else CompletionPhase.Records
    var phase by remember(workout.id) {
        mutableStateOf(
            if (durationSeconds < workout.estimatedMinutes * 60 / 4) CompletionPhase.DurationWarning else firstRegularPhase,
        )
    }
    var completionConfirmed by remember(workout.id) { mutableStateOf(false) }
    fun confirmCompletion() {
        if (!completionConfirmed) {
            completionConfirmed = true
            onCompletionConfirmed()
        }
    }
    LaunchedEffect(workout.id) {
        if (durationSeconds >= workout.estimatedMinutes * 60 / 4) confirmCompletion()
    }
    LaunchedEffect(phase) {
        if (phase in setOf(
                CompletionPhase.Records,
                CompletionPhase.Summary,
                CompletionPhase.WorkoutLevelUp,
                CompletionPhase.PlanComplete,
                CompletionPhase.MesocycleComplete,
                CompletionPhase.PlanLevelUp,
                CompletionPhase.MesocycleLevelUp,
            )) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val score = remember(workout.id, stats, feedback, userAgeYears) { WorkoutCompletionCalculator.score(workout, stats, feedback, userAgeYears) }
    val mainExercises = remember(workout) { workout.pathBlocks().mainExercises() }
    val completedSets = stats.mapValues { it.value.sets }
    val completedMainExercises = mainExercises.count { it.isCompleted(workout, completedSets) }
    fun finishOrGenerate() {
        when {
            shouldShowNotificationEducation(context) -> phase = CompletionPhase.NotificationRequest
            progress.streakAfter > 0 && progress.streakAfter % 2 == 0 && !hasRatedApp(context) -> phase = CompletionPhase.RateApp
            shouldOfferNextPlan(isPlanCompleted, canGenerateNextPlan) -> phase = CompletionPhase.PlanGenerator
            else -> onDone()
        }
    }
    fun continueAfterNotificationEducation() {
        phase = if (progress.streakAfter > 0 && progress.streakAfter % 2 == 0 && !hasRatedApp(context)) {
            CompletionPhase.RateApp
        } else if (shouldOfferNextPlan(isPlanCompleted, canGenerateNextPlan)) {
            CompletionPhase.PlanGenerator
        } else {
            onDone()
            return
        }
    }
    fun continueAfterWorkout() {
        if (isPlanCompleted) phase = CompletionPhase.PlanComplete else onDone()
    }
    fun continueAfterPlanXp() {
        when {
            progress.levelAfterPlan > progress.levelAfterWorkout -> phase = CompletionPhase.PlanLevelUp
            isMesocycleCompleted -> phase = CompletionPhase.MesocycleXp
            else -> finishOrGenerate()
        }
    }
    fun continueAfterMesocycleXp() {
        if (progress.levelAfter > progress.levelAfterPlan) phase = CompletionPhase.MesocycleLevelUp else finishOrGenerate()
    }
    // iOS presents the finish flow as a sequence of soft, progressive transitions.
    // Keep the phase state machine intact, but animate each screen change so the
    // completion experience does not snap from one celebration to the next.
    Crossfade(targetState = phase, animationSpec = tween(durationMillis = 420), label = "completion-phase") { currentPhase ->
        when (currentPhase) {
            CompletionPhase.DurationWarning -> DurationWarning(onCancelWorkout) { confirmCompletion(); phase = firstRegularPhase }
            CompletionPhase.Records -> RecordsCelebration(records) { phase = CompletionPhase.Summary }
            CompletionPhase.Summary -> CompletionSummary(workout, durationSeconds, stats, completedMainExercises, skippedExerciseIndices, useImperial) { phase = CompletionPhase.Score }
            CompletionPhase.Score -> ScoreCelebration(
                score = score,
                completedExercises = completedMainExercises,
                totalExercises = mainExercises.size,
                effort = dominantFeedback(feedback),
                averageHeartRate = workout.averageHeartRate,
                durationSeconds = durationSeconds,
            ) { phase = if (progress.streakIncreased) CompletionPhase.Streak else CompletionPhase.WorkoutXp }
            CompletionPhase.Streak -> StreakCelebration(progress.streakAfter) { phase = CompletionPhase.WorkoutXp }
            CompletionPhase.WorkoutXp -> XpCelebration(progress.xpBefore, progress.xpAfterWorkout, progress.levelBefore, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.workout_complete)) {
                if (progress.levelAfterWorkout > progress.levelBefore) phase = CompletionPhase.WorkoutLevelUp else continueAfterWorkout()
            }
            CompletionPhase.WorkoutLevelUp -> LevelUpCelebration(progress.levelAfterWorkout) { continueAfterWorkout() }
            CompletionPhase.PlanComplete -> PlanCompletedCelebration(planName.ifBlank { workout.title }, completedPlanWorkouts, totalPlanExercises) {
                phase = if (isMesocycleCompleted) CompletionPhase.MesocycleComplete else CompletionPhase.PlanXp
            }
            CompletionPhase.MesocycleComplete -> MesocycleCompletedCelebration(mesocycleIndex, cycleLength) { phase = CompletionPhase.PlanXp }
            CompletionPhase.PlanXp -> XpCelebration(progress.xpAfterWorkout, progress.xpAfterPlan, progress.levelAfterWorkout, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.plan_completed_)) { continueAfterPlanXp() }
            CompletionPhase.PlanLevelUp -> LevelUpCelebration(progress.levelAfterPlan) {
                if (isMesocycleCompleted) phase = CompletionPhase.MesocycleXp else finishOrGenerate()
            }
            CompletionPhase.MesocycleXp -> XpCelebration(progress.xpAfterPlan, progress.xpAfter, progress.levelAfterPlan, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle_complete)) { continueAfterMesocycleXp() }
            CompletionPhase.MesocycleLevelUp -> LevelUpCelebration(progress.levelAfter) { finishOrGenerate() }
            CompletionPhase.NotificationRequest -> NotificationEducationCelebration(context) { continueAfterNotificationEducation() }
            CompletionPhase.RateApp -> RateAppCelebration(context) {
                if (shouldOfferNextPlan(isPlanCompleted, canGenerateNextPlan)) phase = CompletionPhase.PlanGenerator else onDone()
            }
            CompletionPhase.PlanGenerator -> PlanGeneratorCelebration(onDone, onGenerateNextPlan)
        }
    }
}

@Composable
private fun DurationWarning(onCancel: () -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().liquidGlassBackground().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, modifier = Modifier.size(64.dp), tint = WildforceThemeTokens.accentGold)
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.the_duration_doesn_t_seem_right).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_5441cb400ad25e80), Modifier.padding(top = 10.dp, bottom = 28.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828), contentColor = Color.White)) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancel_workout).uppercase(), fontWeight = FontWeight.Bold)
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_key_continue).uppercase(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecordsCelebration(records: List<ExerciseRecordEvent>, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CelebrationFrame(onContinue = onContinue) {
    val locale = LocalLocale.current.platformLocale
    var visibleRecords by remember { mutableStateOf(0) }
    LaunchedEffect(records) {
        visibleRecords = 0
        records.indices.forEach { index ->
            delay(if (index == 0) 180 else 260)
            visibleRecords = index + 1
        }
    }
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = WildforceThemeTokens.accent)
            Text(if (records.size == 1) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.new_record).uppercase() else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_e34b6abaeaebfa07), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEachIndexed { index, record ->
                    AnimatedVisibility(
                        visible = index < visibleRecords,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 },
                    ) {
                        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp)) {
                            Text(record.exerciseName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(androidx.compose.ui.res.stringResource(if (record.label.contains("VOLUMEN")) io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_best_volume else io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_best_weight), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accent)
                            Text(String.format(locale, "%.1f %s", record.newValue, record.unit), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                            Text(String.format(locale, "%.1f → %.1f · +%.1f %s", record.previousValue, record.newValue, record.newValue - record.previousValue, record.unit), color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        ConfettiOverlay(intensity = if (records.size == 1) .7f else 1.2f)
    }
}

@Composable
private fun CompletionSummary(
    workout: WorkoutDaySummary,
    durationSeconds: Int,
    stats: Map<Int, ExerciseSessionStats>,
    completedExercises: Int,
    skippedExerciseIndices: Set<Int>,
    useImperial: Boolean,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val shareId = remember(workout.id) { completedWorkoutShareId(context, workout.id) }
    val locale = LocalLocale.current.platformLocale
    val mainExercises = workout.pathBlocks().mainExercises()
    val mainIndices = mainExercises.flatMap { it.executionIndices }.toSet()
    val mainStats = stats.filterKeys { it in mainIndices }.values
    val sets = mainStats.sumOf { it.sets }
    val volume = mainStats.sumOf { it.volumeKg }
    val totalExercises = mainExercises.size.coerceAtLeast(completedExercises)
    val skipped = mainExercises.skippedExerciseCount(skippedExerciseIndices)
    val exercisesLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.exercises)
    val skippedLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_skipped)
    val setsLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_sets)
    val volumeLabel = stringResource(if (useImperial) io.codepassion.doubletriangle.core.designsystem.R.string.volume__lb_ else io.codepassion.doubletriangle.core.designsystem.R.string.volume__kg_)
    val caloriesLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.kcal)
    val minutesLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.minutes)
    val averageLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.avg_bpm)
    val maximumLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.max_bpm)
    // iOS builds this grid from available data: repetitions are not a summary
    // card, while skipped exercises, calories and heart-rate cards are added
    // only when the corresponding completion data exists.
    val metrics = buildList {
        add(CompletionMetric(exercisesLabel, "$completedExercises / $totalExercises", Icons.Filled.FitnessCenter))
        if (skipped > 0) add(CompletionMetric(skippedLabel, skipped.toString(), Icons.Filled.FastForward))
        if (sets > 0) add(CompletionMetric(setsLabel, sets.toString(), Icons.Filled.Layers))
        if (volume > 0) add(CompletionMetric(volumeLabel, String.format(locale, "%.0f", if (useImperial) volume * KG_TO_LB else volume), Icons.Filled.Scale))
        workout.activeCaloriesBurned?.takeIf { it > 0 }?.let { calories ->
            add(CompletionMetric(caloriesLabel, String.format(locale, "%.0f", calories), Icons.Filled.LocalFireDepartment))
        }
        if (durationSeconds > 0) add(CompletionMetric(minutesLabel, (durationSeconds / 60).toString(), Icons.Filled.Timer))
        workout.averageHeartRate?.takeIf { it > 0 }?.let { average ->
            add(CompletionMetric(averageLabel, String.format(locale, "%.0f", average), Icons.Filled.Favorite))
        }
        workout.maximumHeartRate?.takeIf { it > 0 }?.let { maximum ->
            add(CompletionMetric(maximumLabel, String.format(locale, "%.0f", maximum), Icons.Filled.BarChart))
        }
    }
    val continueLabel = stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_key_continue)
    CelebrationFrame(buttonLabel = continueLabel, onContinue = onContinue, scrollableBody = true, summaryActionDelay = 300 + metrics.size * 100, summaryAction = if (shareId != null) {
        {
            OutlinedButton(
                onClick = {
                    val baseUrl = io.codepassion.doubletriangle.core.model.WildforceApiEnvironment.apiBaseUrl.removeSuffix("/api")
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$baseUrl/workout-days/$shareId/share")
                    }, context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.share_workout)))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.share_workout), fontWeight = FontWeight.Bold) }
        }
    } else null) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.summaryEntrance(100).size(80.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.1f), CircleShape).padding(20.dp), tint = WildforceThemeTokens.accent)
        Column(Modifier.padding(top = 16.dp).summaryEntrance(200), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.great_job_).uppercase(), fontFamily = Exo2FontFamily, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
            Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_ve_completed_your_workout_), Modifier.padding(top = 4.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(top = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            metrics.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEachIndexed { columnIndex, metric ->
                        val entranceDelay = 300 + (rowIndex * 2 + columnIndex) * 100
                        CompletionStat(metric.title, metric.value, Modifier.weight(1f).summaryEntrance(entranceDelay), metric.icon, summaryStyle = true, iconBounceDelay = entranceDelay)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreCelebration(
    score: Double,
    completedExercises: Int,
    totalExercises: Int,
    effort: String,
    averageHeartRate: Double?,
    durationSeconds: Int,
    onContinue: () -> Unit,
) = CelebrationFrame(onContinue = onContinue) {
    var revealScore by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealScore = true }
    val animated by animateFloatAsState(if (revealScore) score.toFloat() / 100f else 0f, animationSpec = tween(1_000, delayMillis = 160))
    val displayedScore by animateIntAsState(if (revealScore) score.toInt() else 0, animationSpec = tween(900, delayMillis = 170))
    val tier = when {
        score >= 95 -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.elite_session) to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_matched_the_workout_beautifully_and_kept_the_effort_high_)
        score >= 80 -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.strong_session) to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_were_close_to_the_prescription_and_brought_strong_effort_today_)
        score >= 65 -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.solid_session) to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.nice_consistency__a_little_more_precision_and_this_score_climbs_fast_)
        else -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.good_work) to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_got_it_done__keep_stacking_sessions_and_the_score_will_follow_)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.workout_score).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    Text(tier.first.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(tier.second, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Box(Modifier.padding(24.dp).size(230.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(animated, Modifier.fillMaxSize(), WildforceThemeTokens.textPrimary, strokeWidth = 18.dp)
        Text("$displayedScore%", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h2, color = WildforceThemeTokens.textPrimary)
    }
    val exerciseLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.exercises).uppercase()
    val effortLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.effort).uppercase()
    val heartRateLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.avg_hr).uppercase()
    val minutesLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.minutes).uppercase()
    val localizedEffort = feedbackTitleResource(effort)?.let { androidx.compose.ui.res.stringResource(it) } ?: if (effort == "SIN DATOS") androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.no_data) else effort
    val highlights = buildList {
        add(CompletionMetric(exerciseLabel, "$completedExercises/$totalExercises", Icons.Filled.FitnessCenter))
        add(CompletionMetric(effortLabel, localizedEffort, Icons.AutoMirrored.Filled.DirectionsRun))
        averageHeartRate?.takeIf { it > 0 }?.let { average ->
            add(CompletionMetric(heartRateLabel, String.format(LocalLocale.current.platformLocale, "%.0f", average), Icons.Filled.Favorite))
        } ?: run {
            if (durationSeconds > 0) add(CompletionMetric(minutesLabel, (durationSeconds / 60).toString(), Icons.Filled.Timer))
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        highlights.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { metric ->
                    CompletionStat(metric.title, metric.value, Modifier.weight(1f), metric.icon)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StreakCelebration(streak: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealStreak by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealStreak = true }
    val displayedStreak by animateIntAsState(if (revealStreak) streak else (streak - 1).coerceAtLeast(0), animationSpec = tween(700, delayMillis = 120))
    Spacer(Modifier.weight(1f))
    val transition = rememberInfiniteTransition(label = "streak-flame")
    val flameScale by transition.animateFloat(1f, 1.10f, infiniteRepeatable(tween(100), RepeatMode.Reverse), label = "streak-flame-scale")
    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(76.dp).graphicsLayer { scaleX = flameScale; scaleY = flameScale }, tint = Color(0xFFF07818))
    Text(displayedStreak.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    Text(if (streak == 1) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_4a9232af2bc6e80d) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_e1855ede78c165c8), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color(0xFFF07818))
    val streakMessage = when {
        streak == 1 -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.the_first_step_is_the_most_important_)
        streak % 7 == 0 -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.great_start__keep_the_momentum_going_)
        else -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_re_on_fire_)
    }
    Text(streakMessage, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun XpCelebration(xpBefore: Int, xpAfter: Int, levelBefore: Int, gainedLabel: String, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealXp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealXp = true }
    Spacer(Modifier.weight(1f))
    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(76.dp), tint = Color(0xFFFFC107))
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.total_xp).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    Text(gainedLabel, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
    val displayedXp by animateIntAsState(if (revealXp) xpAfter else xpBefore, animationSpec = tween(850, delayMillis = 140))
    Text(displayedXp.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_11a14b6a6f7a431f, levelBefore), color = WildforceThemeTokens.textSecondary)
    val levelStart = WorkoutCompletionCalculator.minimumXp(levelBefore)
    val levelEnd = WorkoutCompletionCalculator.minimumXp(levelBefore + 1)
    val xpBeforeProgress = ((xpBefore - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val xpAfterProgress = ((xpAfter - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedXpProgress by animateFloatAsState(if (revealXp) xpAfterProgress else xpBeforeProgress, animationSpec = tween(950, delayMillis = 150))
    RoundedCompletionProgress(animatedXpProgress, Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(14.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_452c2b03d5053d8a, xpBefore), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f9af631634ddf30d, xpAfter - xpBefore), color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun LevelUpCelebration(level: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealLevel by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealLevel = true }
    val displayedLevel by animateIntAsState(if (revealLevel) level else (level - 1).coerceAtLeast(1), animationSpec = tween(750, delayMillis = 150))
    Spacer(Modifier.weight(1f))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.level).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Text(displayedLevel.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.level_up_).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_reached_level__d_, level), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun PlanCompletedCelebration(planName: String, completedWorkouts: Int, totalExercises: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    Spacer(Modifier.weight(1f))
    Box(
        Modifier.size(120.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(60.dp), tint = WildforceThemeTokens.accent)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.plan_completed_).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(planName, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompletionStat(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.workouts).uppercase(), completedWorkouts.toString(), Modifier.weight(1f), Icons.Filled.CalendarMonth)
        CompletionStat(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.exercises).uppercase(), totalExercises.toString(), Modifier.weight(1f), Icons.Filled.FitnessCenter)
    }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun MesocycleCompletedCelebration(mesocycleIndex: Int, cycleLength: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(54.dp), tint = WildforceThemeTokens.textPrimary)
        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(38.dp), tint = Color.Yellow)
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(54.dp), tint = WildforceThemeTokens.textPrimary)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle_complete).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(
        if (mesocycleIndex > 0) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle__d_complete, mesocycleIndex) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle_complete),
        color = WildforceThemeTokens.textSecondary,
        textAlign = TextAlign.Center,
    )
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompletionStat(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle).uppercase(), if (mesocycleIndex > 0) mesocycleIndex.toString() else "—", Modifier.weight(1f), Icons.Filled.Repeat)
        CompletionStat(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.plans).uppercase(), if (cycleLength > 0) cycleLength.toString() else "—", Modifier.weight(1f), Icons.Filled.CalendarMonth)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_4514362231b4ebda), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun PlanGeneratorCelebration(onDone: () -> Unit, onGenerateNextPlan: () -> Unit) = CelebrationFrame(
    buttonLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.generate_next_plan).uppercase(),
    onContinue = onGenerateNextPlan,
    secondaryLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.done).uppercase(),
    onSecondary = onDone,
) {
    Spacer(Modifier.weight(1f))
    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(76.dp), tint = WildforceThemeTokens.accent)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_e43262c17207504d), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d67126ae53afa3cf), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun NotificationEducationCelebration(context: Context, onContinue: () -> Unit) = CelebrationFrame(
    buttonLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.enable_reminders).uppercase(),
    onContinue = {
        WorkoutNotificationPreferences.markCompletionEducationSeen(context)
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            context.findActivity()?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7204)
        }
        onContinue()
    },
    secondaryLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.not_now).uppercase(),
    onSecondary = {
        WorkoutNotificationPreferences.markCompletionEducationSeen(context)
        onContinue()
    },
) {
    Spacer(Modifier.weight(1f))
    NotificationStackMockup()
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stay_consistent).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_006a2c0894258f2d), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun NotificationStackMockup() {
    val transition = rememberInfiniteTransition(label = "notification-stack")
    val cycle by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2_000, easing = LinearEasing)), label = "notification-stack-cycle")
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        repeat(3) { index ->
            val position = index - 1
            Box(
                Modifier.fillMaxWidth(.78f).height(62.dp)
                    .graphicsLayer {
                        val drift = if (position == 0) -cycle * 18f else if (position == 2) cycle * 18f else 0f
                        translationY = position * 36f + drift
                        scaleX = if (position == 0) .90f else 1f
                        scaleY = scaleX
                        alpha = if (position == 0) .60f else if (position == 2) .72f else 1f
                    }
                    .clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary.copy(alpha = .94f)).padding(12.dp),
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(24.dp), tint = WildforceThemeTokens.accent)
                Column(Modifier.padding(start = 30.dp)) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_697e0b6bbef69f5c), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption)
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_your_future_self_will_thank_you_), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ConfettiOverlay(intensity: Float) {
    val transition = rememberInfiniteTransition(label = "completion-confetti")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4_400, easing = LinearEasing)), label = "completion-confetti-time")
    val colors = listOf(WildforceThemeTokens.accent, Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF43A047), Color(0xFFE53935))
    Canvas(Modifier.fillMaxSize()) {
        repeat((18 * intensity).toInt()) { index ->
            val seed = (index + 1).toFloat()
            val duration = .62f + kotlin.math.abs(kotlin.math.sin(seed * 1.3f)) * .38f
            val start = kotlin.math.abs(kotlin.math.cos(seed * 2.7f)) * .20f
            val progress = ((time - start + 1f) % 1f) / duration
            if (progress <= 1f) {
                val width = 8f + kotlin.math.abs(kotlin.math.cos(seed * 1.9f)) * 12f
                val x = ((kotlin.math.sin(seed * 17f) + 1f) / 2f) * size.width + kotlin.math.sin(progress * 6.283185f) * kotlin.math.sin(seed * .8f) * 28f
                val y = -width + progress * (size.height + width * 2f)
                val alpha = when { progress < .1f -> progress / .1f; progress > .9f -> (1f - progress) / .1f; else -> 1f }
                rotate(degrees = (time * 360f + seed * 37f), pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                    drawRoundRect(colors[index % colors.size].copy(alpha = alpha * .9f), topLeft = androidx.compose.ui.geometry.Offset(x - width / 2f, y - width * .27f), size = androidx.compose.ui.geometry.Size(width, width * .55f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * .2f))
                }
            }
        }
    }
}

@Composable
private fun RateAppCelebration(context: Context, onContinue: () -> Unit) = CelebrationFrame(
    buttonLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.rate_us_).uppercase(),
    onContinue = {
        markAppRated(context)
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        runCatching { context.startActivity(marketIntent) }.recoverCatching { context.startActivity(webIntent) }
        onContinue()
    },
    secondaryLabel = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.not_now).uppercase(),
    onSecondary = onContinue,
) {
    Spacer(Modifier.weight(1f))
    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE53935))
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_03812c2ff805ac31), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_17c4d9381ad5aa89), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun CelebrationFrame(buttonLabel: String? = null, onContinue: () -> Unit, secondaryLabel: String? = null, onSecondary: () -> Unit = {}, scrollableBody: Boolean = false, summaryActionDelay: Int = 0, summaryAction: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(
        (if (scrollableBody) Modifier.background(WildforceThemeTokens.background) else Modifier.liquidGlassBackground().background(Brush.radialGradient(listOf(WildforceThemeTokens.accent.copy(alpha = 0.10f), Color.Transparent)))).fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The iOS finish flow reveals the celebration content progressively.
        // Animate the body independently from the persistent action button.
        AnimatedVisibility(
            visible = entered,
            modifier = Modifier.weight(1f),
            enter = if (scrollableBody) androidx.compose.animation.EnterTransition.None else fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 12 },
        ) {
            val bodyModifier = if (scrollableBody) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }
            Column(bodyModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                content()
                if (!scrollableBody) Spacer(Modifier.weight(1f))
            }
        }
        Column((if (scrollableBody) Modifier.summaryEntrance(summaryActionDelay).padding(top = 16.dp) else Modifier).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (scrollableBody) 16.dp else 8.dp)) {
            summaryAction?.invoke()
            secondaryLabel?.let { label ->
                OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(label, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
                Text(buttonLabel ?: androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_key_continue).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletionStat(title: String, value: String, modifier: Modifier, icon: ImageVector? = null, summaryStyle: Boolean = false, iconBounceDelay: Int = 0) {
    val surface = if (summaryStyle) modifier.clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary) else modifier.liquidGlass(RoundedCornerShape(15.dp))
    Column(surface.padding(if (summaryStyle) 12.dp else 13.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(if (summaryStyle) 8.dp else 0.dp)) {
        icon?.let {
            val bounce = remember { Animatable(1f) }
            LaunchedEffect(summaryStyle, iconBounceDelay, value) {
                if (summaryStyle) {
                    delay(iconBounceDelay.toLong())
                    bounce.snapTo(1f)
                    bounce.animateTo(1.2f, spring(dampingRatio = 0.6f, stiffness = 600f))
                    bounce.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f))
                }
            }
            Icon(it, contentDescription = null, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }, tint = WildforceThemeTokens.accent)
        }
        Text(value, fontSize = if (summaryStyle) 22.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun Modifier.summaryEntrance(delayMillis: Int): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(delayMillis) { delay(delayMillis.toLong()); visible = true }
    val progress by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 250f), label = "summary-entrance")
    val offset = with(LocalDensity.current) { 30.dp.toPx() }
    return blur((10f * (1f - progress.coerceIn(0f, 1f))).dp).graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * offset
        scaleX = 0.95f + 0.05f * progress
        scaleY = scaleX
    }
}

@Composable
private fun RoundedCompletionProgress(progress: Float, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(WildforceThemeTokens.accent))
    }
}

private fun dominantFeedback(feedback: Map<Int, String>): String = feedback.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "SIN DATOS"
private fun shouldShowNotificationEducation(context: Context): Boolean =
    !WorkoutNotificationPreferences.completionEducationSeen(context) &&
        Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private fun hasRatedApp(context: Context): Boolean =
    context.getSharedPreferences("wildforce_completion_prompts", 0).getBoolean("has_rated_app", false)

private fun markAppRated(context: Context) {
    context.getSharedPreferences("wildforce_completion_prompts", 0).edit().putBoolean("has_rated_app", true).apply()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
