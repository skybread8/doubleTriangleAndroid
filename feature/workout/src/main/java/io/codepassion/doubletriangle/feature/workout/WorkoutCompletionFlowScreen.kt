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
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
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
    totalPlanWorkouts: Int = 0,
    totalPlanExercises: Int = 0,
    useImperial: Boolean = false,
    onCancelWorkout: () -> Unit,
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
    LaunchedEffect(phase) {
        if (phase in setOf(CompletionPhase.WorkoutLevelUp, CompletionPhase.PlanComplete, CompletionPhase.MesocycleComplete, CompletionPhase.PlanLevelUp, CompletionPhase.MesocycleLevelUp)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val score = remember(workout.id, stats, feedback) { WorkoutCompletionCalculator.score(workout, stats, feedback) }
    val mainExercises = remember(workout) { workout.pathBlocks().mainExercises() }
    val completedSets = stats.mapValues { it.value.sets }
    val completedMainExercises = mainExercises.count { it.isCompleted(workout, completedSets) }
    fun finishOrGenerate() {
        when {
            shouldShowNotificationEducation(context) -> phase = CompletionPhase.NotificationRequest
            progress.streakAfter > 0 && progress.streakAfter % 2 == 0 && !hasRatedApp(context) -> phase = CompletionPhase.RateApp
            isPlanCompleted -> phase = CompletionPhase.PlanGenerator
            else -> onDone()
        }
    }
    fun continueAfterNotificationEducation() {
        phase = if (progress.streakAfter > 0 && progress.streakAfter % 2 == 0 && !hasRatedApp(context)) {
            CompletionPhase.RateApp
        } else if (isPlanCompleted) {
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
            CompletionPhase.DurationWarning -> DurationWarning(onCancelWorkout) { phase = firstRegularPhase }
            CompletionPhase.Records -> RecordsCelebration(records) { phase = CompletionPhase.Summary }
            CompletionPhase.Summary -> CompletionSummary(workout, durationSeconds, stats, completedMainExercises, useImperial) { phase = CompletionPhase.Score }
            CompletionPhase.Score -> ScoreCelebration(score, completedMainExercises, mainExercises.size, dominantFeedback(feedback)) { phase = if (progress.streakIncreased) CompletionPhase.Streak else CompletionPhase.WorkoutXp }
            CompletionPhase.Streak -> StreakCelebration(progress.streakAfter) { phase = CompletionPhase.WorkoutXp }
            CompletionPhase.WorkoutXp -> XpCelebration(progress.xpBefore, progress.xpAfterWorkout, progress.levelBefore, "ENTRENAMIENTO COMPLETADO") {
                if (progress.levelAfterWorkout > progress.levelBefore) phase = CompletionPhase.WorkoutLevelUp else continueAfterWorkout()
            }
            CompletionPhase.WorkoutLevelUp -> LevelUpCelebration(progress.levelAfterWorkout) { continueAfterWorkout() }
            CompletionPhase.PlanComplete -> PlanCompletedCelebration(planName.ifBlank { workout.title }, completedPlanWorkouts, totalPlanWorkouts, totalPlanExercises) {
                phase = if (isMesocycleCompleted) CompletionPhase.MesocycleComplete else CompletionPhase.PlanXp
            }
            CompletionPhase.MesocycleComplete -> MesocycleCompletedCelebration { phase = CompletionPhase.PlanXp }
            CompletionPhase.PlanXp -> XpCelebration(progress.xpAfterWorkout, progress.xpAfterPlan, progress.levelAfterWorkout, "PLAN COMPLETADO") { continueAfterPlanXp() }
            CompletionPhase.PlanLevelUp -> LevelUpCelebration(progress.levelAfterPlan) {
                if (isMesocycleCompleted) phase = CompletionPhase.MesocycleXp else finishOrGenerate()
            }
            CompletionPhase.MesocycleXp -> XpCelebration(progress.xpAfterPlan, progress.xpAfter, progress.levelAfterPlan, "MESOCICLO COMPLETADO") { continueAfterMesocycleXp() }
            CompletionPhase.MesocycleLevelUp -> LevelUpCelebration(progress.levelAfter) { finishOrGenerate() }
            CompletionPhase.NotificationRequest -> NotificationEducationCelebration(context) { continueAfterNotificationEducation() }
            CompletionPhase.RateApp -> RateAppCelebration(context) {
                if (isPlanCompleted) phase = CompletionPhase.PlanGenerator else onDone()
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
        Text("⚠", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
        Text("LA DURACIÓN NO PARECE CORRECTA", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text("Has terminado mucho antes de lo previsto. ¿Quieres cancelar el entrenamiento?", Modifier.padding(top = 10.dp, bottom = 28.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828), contentColor = Color.White)) {
            Text("CANCELAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text("CONTINUAR", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecordsCelebration(records: List<ExerciseRecordEvent>, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    val locale = LocalLocale.current.platformLocale
    var visibleRecords by remember { mutableStateOf(0) }
    LaunchedEffect(records) {
        visibleRecords = 0
        records.indices.forEach { index ->
            delay(if (index == 0) 180 else 260)
            visibleRecords = index + 1
        }
    }
    Text("🏅", style = MaterialTheme.typography.h2)
    Text(if (records.size == 1) "NUEVO RÉCORD" else "NUEVOS RÉCORDS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        records.forEachIndexed { index, record ->
            AnimatedVisibility(
                visible = index < visibleRecords,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 },
            ) {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Text(record.exerciseName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(record.label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold)
                    Text(String.format(locale, "%.1f %s", record.newValue, record.unit), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text(String.format(locale, "Antes %.1f · +%.1f %s", record.previousValue, record.newValue - record.previousValue, record.unit), color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CompletionSummary(workout: WorkoutDaySummary, durationSeconds: Int, stats: Map<Int, ExerciseSessionStats>, completedExercises: Int, useImperial: Boolean, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    val locale = LocalLocale.current.platformLocale
    var visibleRows by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(180)
        visibleRows = 1
        delay(180)
        visibleRows = 2
        delay(180)
        visibleRows = 3
    }
    Text("✓", Modifier.size(76.dp).background(WildforceThemeTokens.accentGold.copy(alpha = 0.12f), CircleShape).padding(12.dp), style = MaterialTheme.typography.h3, color = WildforceThemeTokens.accentGold, textAlign = TextAlign.Center)
    Text("¡GRAN TRABAJO!", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Has completado ${workout.title}.", color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    val mainIndices = workout.pathBlocks().mainExercises().flatMap { it.executionIndices }.toSet()
    val mainStats = stats.filterKeys { it in mainIndices }.values
    val sets = mainStats.sumOf { it.sets }
    val volume = mainStats.sumOf { it.volumeKg }
    val repetitions = mainStats.sumOf { it.totalReps }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(visibleRows >= 1, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompletionStat("EJERCICIOS", completedExercises.toString(), Modifier.weight(1f))
                CompletionStat("SERIES", sets.toString(), Modifier.weight(1f))
            }
        }
        AnimatedVisibility(visibleRows >= 2, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompletionStat("REPETICIONES", repetitions.toString(), Modifier.weight(1f))
                CompletionStat("VOLUMEN", String.format(locale, "%.0f %s", if (useImperial) volume * KG_TO_LB else volume, if (useImperial) "lb" else "kg"), Modifier.weight(1f))
            }
        }
        AnimatedVisibility(visibleRows >= 3, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) {
            CompletionStat("TIEMPO", formatCompletionClock(durationSeconds), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScoreCelebration(score: Double, completedExercises: Int, totalExercises: Int, effort: String, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealScore by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealScore = true }
    val animated by animateFloatAsState(if (revealScore) score.toFloat() / 100f else 0f, animationSpec = tween(1_000, delayMillis = 160))
    val displayedScore by animateIntAsState(if (revealScore) score.toInt() else 0, animationSpec = tween(900, delayMillis = 170))
    val tier = when {
        score >= 95 -> "SESIÓN DE ÉLITE" to "Has clavado el entrenamiento y mantenido un esfuerzo excelente."
        score >= 80 -> "SESIÓN POTENTE" to "Muy cerca de la prescripción y con un esfuerzo sólido."
        score >= 65 -> "SESIÓN SÓLIDA" to "Buena constancia. Un poco más de precisión elevará la puntuación."
        else -> "BUEN TRABAJO" to "Lo has completado. Sigue acumulando sesiones."
    }
    Text("PUNTUACIÓN DEL ENTRENAMIENTO", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    Text(tier.first, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(tier.second, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Box(Modifier.padding(24.dp).size(230.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(animated, Modifier.fillMaxSize(), WildforceThemeTokens.textPrimary, strokeWidth = 18.dp)
        Text("$displayedScore%", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h2, color = WildforceThemeTokens.textPrimary)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompletionStat("EJERCICIOS", "$completedExercises/$totalExercises", Modifier.weight(1f))
        CompletionStat("ESFUERZO", effort, Modifier.weight(1f))
    }
}

@Composable
private fun StreakCelebration(streak: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealStreak by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealStreak = true }
    val displayedStreak by animateIntAsState(if (revealStreak) streak else (streak - 1).coerceAtLeast(0), animationSpec = tween(700, delayMillis = 120))
    Spacer(Modifier.weight(1f))
    Text("🔥", style = MaterialTheme.typography.h1)
    Text(displayedStreak.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    Text(if (streak == 1) "DÍA DE RACHA" else "DÍAS DE RACHA", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color(0xFFF07818))
    val streakMessage = when {
        streak == 1 -> "¡El primer paso es el más importante!"
        streak % 7 == 0 -> "¡Una semana completa! Tu constancia marca la diferencia."
        else -> "¡Estás en llamas! Sigue manteniendo el ritmo."
    }
    Text(streakMessage, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun XpCelebration(xpBefore: Int, xpAfter: Int, levelBefore: Int, gainedLabel: String, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealXp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealXp = true }
    Spacer(Modifier.weight(1f))
    Text("⚡", style = MaterialTheme.typography.h1)
    Text("XP TOTAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    Text(gainedLabel, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
    val displayedXp by animateIntAsState(if (revealXp) xpAfter else xpBefore, animationSpec = tween(850, delayMillis = 140))
    Text(displayedXp.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    Text("Nivel $levelBefore", color = WildforceThemeTokens.textSecondary)
    val levelStart = WorkoutCompletionCalculator.minimumXp(levelBefore)
    val levelEnd = WorkoutCompletionCalculator.minimumXp(levelBefore + 1)
    val xpBeforeProgress = ((xpBefore - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val xpAfterProgress = ((xpAfter - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedXpProgress by animateFloatAsState(if (revealXp) xpAfterProgress else xpBeforeProgress, animationSpec = tween(950, delayMillis = 150))
    RoundedCompletionProgress(animatedXpProgress, Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(14.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("$xpBefore XP", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.weight(1f))
        Text("+${xpAfter - xpBefore} XP", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun LevelUpCelebration(level: Int, onContinue: () -> Unit) = CelebrationFrame("CONTINUAR", onContinue) {
    var revealLevel by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealLevel = true }
    val displayedLevel by animateIntAsState(if (revealLevel) level else (level - 1).coerceAtLeast(1), animationSpec = tween(750, delayMillis = 150))
    Spacer(Modifier.weight(1f))
    Text("★", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
    Text("NUEVO NIVEL", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(displayedLevel.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    Text("Tu constancia sigue dando resultados.", color = WildforceThemeTokens.textSecondary)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun PlanCompletedCelebration(planName: String, completedWorkouts: Int, totalWorkouts: Int, totalExercises: Int, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    Spacer(Modifier.weight(1f))
    Text("🏆", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
    Text("PLAN COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text(planName, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompletionStat("SESIONES", "$completedWorkouts/$totalWorkouts", Modifier.weight(1f))
        CompletionStat("EJERCICIOS", totalExercises.toString(), Modifier.weight(1f))
    }
    val completionRatio = if (totalWorkouts > 0) (completedWorkouts.toFloat() / totalWorkouts).coerceIn(0f, 1f) else 1f
    val animatedRatio by animateFloatAsState(completionRatio, animationSpec = tween(800, delayMillis = 180))
    RoundedCompletionProgress(animatedRatio, Modifier.fillMaxWidth().padding(top = 18.dp).height(10.dp))
    Text("Ya no quedan sesiones pendientes en este plan.", Modifier.padding(top = 18.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun MesocycleCompletedCelebration(onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    Spacer(Modifier.weight(1f))
    Text("◆", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
    Text("MESOCICLO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Has completado todas las semanas del ciclo. Tu progresión está lista para el siguiente bloque.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun PlanGeneratorCelebration(onDone: () -> Unit, onGenerateNextPlan: () -> Unit) = CelebrationFrame(
    buttonLabel = "GENERAR SIGUIENTE PLAN",
    onContinue = onGenerateNextPlan,
    secondaryLabel = "TERMINAR",
    onSecondary = onDone,
) {
    Spacer(Modifier.weight(1f))
    Text("＋", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
    Text("SIGUIENTE PLAN", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Genera la siguiente semana manteniendo tu progresión, historial y feedback.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun NotificationEducationCelebration(context: Context, onContinue: () -> Unit) = CelebrationFrame(
    buttonLabel = "ACTIVAR RECORDATORIOS",
    onContinue = {
        WorkoutNotificationPreferences.markCompletionEducationSeen(context)
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            context.findActivity()?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7204)
        }
        onContinue()
    },
    secondaryLabel = "QUIZÁ MÁS TARDE",
    onSecondary = {
        WorkoutNotificationPreferences.markCompletionEducationSeen(context)
        onContinue()
    },
) {
    Spacer(Modifier.weight(1f))
    Text("🔔", style = MaterialTheme.typography.h1)
    Text("MANTÉN LA CONSTANCIA", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Activa recordatorios antes de tus entrenamientos y avisos cuando termine un descanso.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun RateAppCelebration(context: Context, onContinue: () -> Unit) = CelebrationFrame(
    buttonLabel = "VALORAR LA APP",
    onContinue = {
        markAppRated(context)
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        runCatching { context.startActivity(marketIntent) }.recoverCatching { context.startActivity(webIntent) }
        onContinue()
    },
    secondaryLabel = "QUIZÁ MÁS TARDE",
    onSecondary = onContinue,
) {
    Spacer(Modifier.weight(1f))
    Text("♥", style = MaterialTheme.typography.h1, color = Color(0xFFE53935))
    Text("¿TE GUSTA TU PROGRESO?", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Si disfrutas Double Triangle, dedica un momento a valorarla en Google Play.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun CelebrationFrame(buttonLabel: String = "CONTINUAR", onContinue: () -> Unit, secondaryLabel: String? = null, onSecondary: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(
        Modifier.fillMaxSize().liquidGlassBackground().background(Brush.radialGradient(listOf(WildforceThemeTokens.accentGold.copy(alpha = 0.10f), Color.Transparent))).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The iOS finish flow reveals the celebration content progressively.
        // Animate the body independently from the persistent action button.
        AnimatedVisibility(
            visible = entered,
            modifier = Modifier.weight(1f),
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 12 },
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                content()
                Spacer(Modifier.weight(1f))
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            secondaryLabel?.let { label ->
                OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(label, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
                Text(buttonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletionStat(title: String, value: String, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(15.dp)).padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun RoundedCompletionProgress(progress: Float, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(WildforceThemeTokens.accentGold))
    }
}

private fun dominantFeedback(feedback: Map<Int, String>): String = feedback.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "SIN DATOS"
private fun formatCompletionClock(seconds: Int): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)

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
