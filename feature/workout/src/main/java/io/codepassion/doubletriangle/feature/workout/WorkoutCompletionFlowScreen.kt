package io.codepassion.doubletriangle.feature.workout

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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import java.util.Locale

private enum class CompletionPhase { DurationWarning, Records, Summary, Score, Streak, Xp, LevelUp, PlanComplete }

@Composable
internal fun WorkoutCompletionFlowScreen(
    workout: WorkoutDaySummary,
    durationSeconds: Int,
    stats: Map<Int, ExerciseSessionStats>,
    feedback: Map<Int, String>,
    records: List<ExerciseRecordEvent>,
    progress: CompletionProgress,
    isPlanCompleted: Boolean = false,
    useImperial: Boolean = false,
    onCancelWorkout: () -> Unit,
    onDone: () -> Unit,
) {
    val firstRegularPhase = if (records.isEmpty()) CompletionPhase.Summary else CompletionPhase.Records
    var phase by remember(workout.id) {
        mutableStateOf(
            if (durationSeconds < workout.estimatedMinutes * 60 / 4) CompletionPhase.DurationWarning else firstRegularPhase,
        )
    }
    val score = remember(workout.id, stats, feedback) { WorkoutCompletionCalculator.score(workout, stats, feedback) }
    val mainExercises = remember(workout) { workout.pathBlocks().mainExercises() }
    val completedSets = stats.mapValues { it.value.sets }
    val completedMainExercises = mainExercises.count { it.isCompleted(workout, completedSets) }
    fun continueAfterXp() {
        when {
            progress.levelAfter > progress.levelBefore -> phase = CompletionPhase.LevelUp
            isPlanCompleted -> phase = CompletionPhase.PlanComplete
            else -> onDone()
        }
    }
    // iOS presents the finish flow as a sequence of soft, progressive transitions.
    // Keep the phase state machine intact, but animate each screen change so the
    // completion experience does not snap from one celebration to the next.
    Crossfade(targetState = phase, animationSpec = tween(durationMillis = 420), label = "completion-phase") { currentPhase ->
        when (currentPhase) {
            CompletionPhase.DurationWarning -> DurationWarning(onCancelWorkout) { phase = firstRegularPhase }
            CompletionPhase.Records -> RecordsCelebration(records) { phase = CompletionPhase.Summary }
            CompletionPhase.Summary -> CompletionSummary(workout, durationSeconds, stats, completedMainExercises, useImperial) { phase = CompletionPhase.Score }
            CompletionPhase.Score -> ScoreCelebration(score, completedMainExercises, mainExercises.size, dominantFeedback(feedback)) { phase = if (progress.streakIncreased) CompletionPhase.Streak else CompletionPhase.Xp }
            CompletionPhase.Streak -> StreakCelebration(progress.streakAfter) { phase = CompletionPhase.Xp }
            CompletionPhase.Xp -> XpCelebration(progress) { continueAfterXp() }
            CompletionPhase.LevelUp -> LevelUpCelebration(progress.levelAfter) { if (isPlanCompleted) phase = CompletionPhase.PlanComplete else onDone() }
            CompletionPhase.PlanComplete -> PlanCompletedCelebration(workout, onDone)
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
    Text("🏅", style = MaterialTheme.typography.h2)
    Text(if (records.size == 1) "NUEVO RÉCORD" else "NUEVOS RÉCORDS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        records.forEach { record ->
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp)) {
                Text(record.exerciseName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text(record.label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold)
                Text(String.format(Locale.getDefault(), "%.1f %s", record.newValue, record.unit), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text(String.format(Locale.getDefault(), "Antes %.1f · +%.1f %s", record.previousValue, record.newValue - record.previousValue, record.unit), color = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

@Composable
private fun CompletionSummary(workout: WorkoutDaySummary, durationSeconds: Int, stats: Map<Int, ExerciseSessionStats>, completedExercises: Int, useImperial: Boolean, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    Text("✓", Modifier.size(76.dp).background(WildforceThemeTokens.accentGold.copy(alpha = 0.12f), CircleShape).padding(12.dp), style = MaterialTheme.typography.h3, color = WildforceThemeTokens.accentGold, textAlign = TextAlign.Center)
    Text("¡GRAN TRABAJO!", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Has completado ${workout.title}.", color = WildforceThemeTokens.textSecondary)
    val mainIndices = workout.pathBlocks().mainExercises().flatMap { it.executionIndices }.toSet()
    val mainStats = stats.filterKeys { it in mainIndices }.values
    val sets = mainStats.sumOf { it.sets }
    val volume = mainStats.sumOf { it.volumeKg }
    val repetitions = mainStats.sumOf { it.totalReps }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompletionStat("EJERCICIOS", completedExercises.toString(), Modifier.weight(1f))
            CompletionStat("SERIES", sets.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompletionStat("REPETICIONES", repetitions.toString(), Modifier.weight(1f))
            CompletionStat("VOLUMEN", String.format(Locale.getDefault(), "%.0f %s", if (useImperial) volume * KG_TO_LB else volume, if (useImperial) "lb" else "kg"), Modifier.weight(1f))
        }
        CompletionStat("TIEMPO", formatCompletionClock(durationSeconds), Modifier.fillMaxWidth())
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
    Text(if (streak == 1) "¡El primer paso es el más importante!" else "¡Estás en llamas! Sigue manteniendo el ritmo.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun XpCelebration(progress: CompletionProgress, onContinue: () -> Unit) = CelebrationFrame(onContinue = onContinue) {
    var revealXp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealXp = true }
    Spacer(Modifier.weight(1f))
    Text("⚡", style = MaterialTheme.typography.h1)
    Text("XP TOTAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    val displayedXp by animateIntAsState(if (revealXp) progress.xpAfter else progress.xpBefore, animationSpec = tween(850, delayMillis = 140))
    Text(displayedXp.toString(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
    val levelStart = WorkoutCompletionCalculator.minimumXp(progress.levelBefore)
    val levelEnd = WorkoutCompletionCalculator.minimumXp(progress.levelBefore + 1)
    val xpBeforeProgress = ((progress.xpBefore - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val xpAfterProgress = ((progress.xpAfter - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedXpProgress by animateFloatAsState(if (revealXp) xpAfterProgress else xpBeforeProgress, animationSpec = tween(950, delayMillis = 150))
    LinearProgressIndicator(
        progress = animatedXpProgress,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(14.dp), color = WildforceThemeTokens.accentGold,
        backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.18f),
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Nivel ${progress.levelBefore}", style = MaterialTheme.typography.caption)
        Spacer(Modifier.weight(1f))
        Text("+${WorkoutCompletionCalculator.WORKOUT_XP} XP", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
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
private fun PlanCompletedCelebration(workout: WorkoutDaySummary, onContinue: () -> Unit) = CelebrationFrame("TERMINAR", onContinue) {
    Spacer(Modifier.weight(1f))
    Text("✦", style = MaterialTheme.typography.h1, color = WildforceThemeTokens.accentGold)
    Text("PLAN COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
    Text("Ya no quedan sesiones pendientes en este plan.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Text(workout.focus.uppercase(), Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold, textAlign = TextAlign.Center)
    Text("Tómate un momento para recuperar y vuelve cuando estés listo para tu siguiente plan.", Modifier.padding(top = 8.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun CelebrationFrame(buttonLabel: String = "CONTINUAR", onContinue: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text(buttonLabel, fontWeight = FontWeight.Bold)
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

private fun dominantFeedback(feedback: Map<Int, String>): String = feedback.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "SIN DATOS"
private fun formatCompletionClock(seconds: Int): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
