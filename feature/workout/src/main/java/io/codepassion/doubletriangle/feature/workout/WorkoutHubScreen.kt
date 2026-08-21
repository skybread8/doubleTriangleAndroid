package io.codepassion.doubletriangle.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutMode
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.workoutsFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WorkoutHubScreen(
    contentPadding: PaddingValues,
    state: WorkoutHubState = PreviewWorkoutRepository.load(),
onWorkoutSelected: (WorkoutDaySummary) -> Unit = {},
    gender: String = "male",
) {
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var mode by remember { mutableStateOf(WorkoutMode.Plan) }

    Column(
        Modifier.fillMaxSize().padding(contentPadding).liquidGlassBackground(),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .liquidGlass(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp), emphasized = true)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            WorkoutHeader(state)
            Spacer(Modifier.height(16.dp))
            WeekCalendar(state, selectedDay) { selectedDay = if (selectedDay == it) null else it }
        }

        WorkoutModeSelector(
            selected = mode,
            onSelected = { mode = it },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
        AnimatedVisibility(mode == WorkoutMode.Plan) {
            WorkoutPlan(state, selectedDay, onWorkoutSelected, gender, Modifier.fillMaxSize())
        }
        AnimatedVisibility(mode == WorkoutMode.Custom) {
            EmptyCustomWorkouts(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun WorkoutHeader(state: WorkoutHubState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(WildforceThemeTokens.accentGold),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(state.user.name, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text("▲ ${state.user.goal}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Text("🔥", style = MaterialTheme.typography.h6)
        Text(
            state.user.currentStreak.toString(),
            Modifier.padding(start = 4.dp),
            color = WildforceThemeTokens.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text("•••", Modifier.padding(start = 16.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeekCalendar(
    state: WorkoutHubState,
    selectedDay: DayOfWeek?,
    onDaySelected: (DayOfWeek) -> Unit,
) {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0L..6L).forEach { offset ->
            val date = monday.plusDays(offset)
            val day = date.dayOfWeek
            val selected = day == selectedDay
            val hasWorkout = day in state.trainingDays
            val completed = day in state.completedDays
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) WildforceThemeTokens.accent
                        else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f),
                    )
                    .clickable { onDaySelected(day) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    color = if (selected) MaterialTheme.colors.onPrimary
                    else if (date == today) WildforceThemeTokens.accentGold else WildforceThemeTokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when { completed -> "✓"; hasWorkout -> "●"; else -> " " },
                    color = when { selected -> MaterialTheme.colors.onPrimary; completed -> Color(0xFF26A269); else -> WildforceThemeTokens.accentGold },
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun WorkoutModeSelector(
    selected: WorkoutMode,
    onSelected: (WorkoutMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(4.dp),
    ) {
        WorkoutMode.values().forEach { mode ->
            Text(
                mode.label,
                Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                    .background(if (selected == mode) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelected(mode) }.padding(vertical = 9.dp, horizontal = 4.dp),
                color = WildforceThemeTokens.textPrimary,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WorkoutPlan(state: WorkoutHubState, selectedDay: DayOfWeek?, onWorkoutSelected: (WorkoutDaySummary) -> Unit, gender: String, modifier: Modifier = Modifier) {
    val workouts = state.workoutsFor(selectedDay)
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(state.planName.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
            Text(state.phase, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        if (workouts.isEmpty()) item { RestDayCard() }
        else items(workouts, key = { it.id }) { workout -> WorkoutCard(workout, gender) { onWorkoutSelected(workout) } }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutDaySummary, gender: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), elevation = 0.dp) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            RemoteTrainingImage(
                url = workoutCoverUrl(workout.focus, gender, workout.order),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DÍA ${workout.order}", Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color.White)
                    if (workout.status == WorkoutStatus.Completed) StatusBadge()
                }
                Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = Color.White)
                Text(
                    "◎ ${workout.focus}   ◆ ${workout.dayType}   ◷ ${workout.estimatedMinutes} min",
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge() {
    Text(
        "COMPLETADO",
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF26A269)).padding(horizontal = 9.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun RestDayCard() {
    Column(
        Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("DÍA DE DESCANSO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
        Text("No hay entrenamiento planificado.", color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun EmptyCustomWorkouts(modifier: Modifier = Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CREA TU ENTRENAMIENTO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
            Text("Los entrenamientos personalizados llegarán en esta vertical.", color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutHubPreview() = WildforceTheme { WorkoutHubScreen(PaddingValues()) }

@Composable
fun WorkoutDetailScreen(workout: WorkoutDaySummary, gender: String, onBack: () -> Unit, onStart: () -> Unit) {
    var expandedExercise by remember { mutableStateOf<Int?>(null) }
    val detailContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val hasSavedSession = remember(workout.id) { WorkoutSessionStore.load(detailContext, workout.id) != null }
    var guideExercise by remember { mutableStateOf<ExerciseSummary?>(null) }
    guideExercise?.let { selected ->
        ExerciseGuideScreen(selected, gender) { guideExercise = null }
        return
    }
    Box(Modifier.fillMaxSize().liquidGlassBackground()) {
        RemoteTrainingImage(
            url = workoutCoverUrl(workout.focus, gender, workout.order),
            contentDescription = workout.title,
            modifier = Modifier.fillMaxWidth().height(390.dp),
        )
        Box(Modifier.fillMaxWidth().height(390.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, WildforceThemeTokens.backgroundSecondary))))
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Text("‹  VOLVER", Modifier.clickable(onClick = onBack).padding(horizontal = 18.dp, vertical = 18.dp), color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(190.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(22.dp)) {
                Text("DÍA ${workout.order}", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("${workout.focus} · ${workout.dayType} · ${workout.estimatedMinutes} min", color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.height(20.dp))
                workout.exercises.forEachIndexed { index, exercise ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).liquidGlass(RoundedCornerShape(14.dp))
                            .clickable { expandedExercise = if (expandedExercise == index) null else index }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), null, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                            Text(exercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                            Text("${exercise.sets} series · ${exercise.reps} reps · ${exercise.restSeconds}s", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        Text(if (expandedExercise == index) "⌃" else "⌄", color = WildforceThemeTokens.accentGold)
                    }
                    if (expandedExercise == index) {
                        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                            Text("MÚSCULOS IMPLICADOS", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                            MuscleStrip(exercise.imageKey, onDarkBackground = false, modifier = Modifier.padding(vertical = 5.dp))
                            Text("Objetivo: ${exercise.sets} series de ${exercise.reps} repeticiones con ${exercise.restSeconds}s de recuperación.", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                            Text("CÓMO HACERLO  →", Modifier.padding(top = 10.dp).clickable { guideExercise = exercise }, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp)) {
                    Text(if (hasSavedSession) "REANUDAR ENTRENAMIENTO" else "EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutDaySummary,
    gender: String,
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val restored = remember(workout.id) { WorkoutSessionStore.load(context, workout.id) }
    var exerciseIndex by remember(workout.id) { mutableStateOf((restored?.exerciseIndex ?: 0).coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))) }
    var completedByExercise by remember(workout.id) { mutableStateOf(restored?.completedByExercise ?: emptyMap()) }
    var reps by remember(workout.id) { mutableStateOf(restored?.reps ?: targetReps(workout.exercises.firstOrNull()?.reps)) }
    var weightKg by remember(workout.id) { mutableStateOf(restored?.weightKg ?: 0.0) }
    var restRemaining by remember(workout.id) { mutableStateOf(restored?.restRemaining) }
    var restInitialSeconds by remember(workout.id) { mutableStateOf(restored?.restInitialSeconds ?: 1) }
    var restBetweenExercises by remember(workout.id) { mutableStateOf(restored?.restBetweenExercises ?: false) }
    var elapsedSeconds by remember(workout.id) { mutableStateOf(restored?.elapsedSeconds ?: 0) }
    var totalCompletedSets by remember(workout.id) { mutableStateOf(restored?.totalCompletedSets ?: 0) }
    var totalVolumeKg by remember(workout.id) { mutableStateOf(restored?.totalVolumeKg ?: 0.0) }
    var showsSummary by remember(workout.id) { mutableStateOf(restored?.showsSummary ?: false) }
    var pendingFeedback by remember(workout.id) { mutableStateOf(restored?.pendingFeedback ?: false) }
    var selectedFeedback by remember(workout.id) { mutableStateOf(restored?.selectedFeedback) }
    var showsExerciseGuide by remember { mutableStateOf(false) }
    var initializedExerciseIndex by remember(workout.id) { mutableStateOf(restored?.exerciseIndex ?: -1) }
    val exercise = workout.exercises.getOrNull(exerciseIndex)
    val completedForExercise = completedByExercise[exerciseIndex] ?: 0

    LaunchedEffect(Unit) { while (true) { delay(1_000); elapsedSeconds++ } }
    LaunchedEffect(exerciseIndex) {
        if (initializedExerciseIndex != exerciseIndex) {
            reps = targetReps(exercise?.reps)
            weightKg = 0.0
            initializedExerciseIndex = exerciseIndex
        }
    }
    LaunchedEffect(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, showsSummary) {
        WorkoutSessionStore.save(
            context, workout.id,
            WorkoutSessionSnapshot(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, restBetweenExercises, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, showsSummary, selectedFeedback),
        )
    }
    LaunchedEffect(restRemaining) {
        val remaining = restRemaining ?: return@LaunchedEffect
        if (remaining > 0) { delay(1_000); restRemaining = remaining - 1 } else restRemaining = null
    }

    if (showsExerciseGuide && exercise != null) {
        ExerciseGuideScreen(exercise, gender) { showsExerciseGuide = false }
        return
    }

    if (showsSummary) {
        WorkoutFinishedScreen(workout, elapsedSeconds, totalCompletedSets, totalVolumeKg) {
            WorkoutSessionStore.clear(context, workout.id)
            onFinish(elapsedSeconds, totalCompletedSets, totalVolumeKg)
        }
        return
    }

    Box(Modifier.fillMaxSize().background(WildforceThemeTokens.backgroundSecondary)) {
        RemoteTrainingImage(
            url = exerciseImageUrl(exercise?.imageKey, gender),
            contentDescription = exercise?.name,
            modifier = Modifier.fillMaxWidth().height(610.dp),
        )
        Box(Modifier.fillMaxWidth().height(610.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent, WildforceThemeTokens.backgroundSecondary), startY = 0f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹ SALIR", Modifier.clickable(onClick = onExit).padding(10.dp), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(formatClock(elapsedSeconds), Modifier.liquidGlass(RoundedCornerShape(18.dp), emphasized = true).padding(horizontal = 14.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = if (workout.exercises.isEmpty()) 0f else (exerciseIndex + completedForExercise.toFloat() / (exercise?.sets ?: 1)) / workout.exercises.size,
                modifier = Modifier.fillMaxWidth().height(4.dp), color = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.28f),
            )
            Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                Text("EJERCICIO ${exerciseIndex + 1} DE ${workout.exercises.size}", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                Text(exercise?.name?.uppercase().orEmpty(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FUERZA", Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Text("CÓMO HACERLO  ›", Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).clickable { showsExerciseGuide = true }.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
                MuscleStrip(exercise?.imageKey, onDarkBackground = true, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().height(438.dp)
                    .clip(RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                val resting = restRemaining
                if (pendingFeedback && exercise != null) {
                    ExerciseFeedbackContent(
                        exerciseName = exercise.name,
                        selected = selectedFeedback,
                        onSelected = { selectedFeedback = it },
                        onContinue = {
                            pendingFeedback = false
                            selectedFeedback = null
                            if (exerciseIndex < workout.exercises.lastIndex) {
                                exerciseIndex++
                                restRemaining = exercise.restSeconds
                            } else showsSummary = true
                        },
                    )
                } else if (resting != null) {
                    RestTimerContent(
                        seconds = resting,
                        totalSeconds = restInitialSeconds,
                        betweenExercises = restBetweenExercises,
                        nextExercise = exercise,
                        gender = gender,
                        onAddTime = { restRemaining = resting + 30 },
                        onSkip = { restRemaining = null },
                    )
                } else if (exercise != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SERIES", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                            Text("Objetivo ${exercise.reps} reps · ${exercise.restSeconds}s descanso", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                        }
                        Text("${completedForExercise + 1}/${exercise.sets}", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        repeat(exercise.sets) { setIndex ->
                            val completed = setIndex < completedForExercise
                            val current = setIndex == completedForExercise
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .background(if (current) WildforceThemeTokens.accentGold.copy(alpha = 0.13f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.055f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(if (completed) "✓" else "${setIndex + 1}", color = if (completed) Color(0xFF26A269) else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                                Text(if (current) "SERIE ACTUAL" else if (completed) "COMPLETADA" else "PENDIENTE", Modifier.padding(start = 12.dp).weight(1f), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                                Text("${exercise.reps} reps", color = WildforceThemeTokens.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactMetricStepper("REPS", reps.toString(), { reps = (reps - 1).coerceAtLeast(0) }, { reps++ }, Modifier.weight(1f))
                            CompactMetricStepper("PESO", String.format(Locale.getDefault(), "%.1f kg", weightKg), { weightKg = (weightKg - 2.5).coerceAtLeast(0.0) }, { weightKg += 2.5 }, Modifier.weight(1f))
                        }
                    }
                    Button(
                        onClick = {
                            val newCompleted = completedForExercise + 1
                            completedByExercise = completedByExercise + (exerciseIndex to newCompleted)
                            totalCompletedSets++; totalVolumeKg += reps * weightKg
                            if (newCompleted < exercise.sets) {
                                restInitialSeconds = exercise.restSeconds.coerceAtLeast(1)
                                restBetweenExercises = false
                                restRemaining = exercise.restSeconds
                            } else pendingFeedback = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp),
                    ) { Text(if (completedForExercise + 1 == exercise.sets) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE", fontWeight = FontWeight.Bold) }
                    workout.exercises.getOrNull(exerciseIndex + 1)?.let { Text("Siguiente ejercicio: ${it.name}", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            }
        }
    }
}
@Composable
private fun ExerciseFeedbackContent(
    exerciseName: String,
    selected: String?,
    onSelected: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("EJERCICIO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(exerciseName, color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.height(14.dp))
        Text("¿Cómo ha ido?", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        listOf("FÁCIL" to "Podía hacer bastante más", "CORRECTO" to "Esfuerzo adecuado", "DIFÍCIL" to "Casi no termino").forEach { (title, subtitle) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .background(if (selected == title) WildforceThemeTokens.accentGold.copy(alpha = 0.16f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .clickable { onSelected(title) }.padding(11.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (selected == title) "●" else "○", color = WildforceThemeTokens.accentGold)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onContinue, enabled = selected != null, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text("CONTINUAR", fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun RestTimerContent(
    seconds: Int,
    totalSeconds: Int,
    betweenExercises: Boolean,
    nextExercise: ExerciseSummary?,
    gender: String,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (betweenExercises) "DESCANSO ANTES DEL SIGUIENTE EJERCICIO" else "DESCANSO ANTES DE LA SIGUIENTE SERIE",
            fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            RestActionButton("+", "+30s", primary = false, onClick = onAddTime)
            Box(Modifier.padding(horizontal = 16.dp).size(148.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = (seconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(), color = WildforceThemeTokens.accentGold,
                    backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.18f), strokeWidth = 12.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${seconds}s", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h3, color = WildforceThemeTokens.textPrimary)
                    Text(if (betweenExercises) "EJERCICIO" else "SERIE", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
            RestActionButton("»", "OMITIR", primary = true, onClick = onSkip)
        }
        if (betweenExercises && nextExercise != null) {
            Spacer(Modifier.height(16.dp))
            Text("A CONTINUACIÓN", Modifier.fillMaxWidth().padding(start = 10.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteTrainingImage(exerciseImageUrl(nextExercise.imageKey, gender), null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(nextExercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                    Text("${nextExercise.sets} series · ${nextExercise.reps} reps", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RestActionButton(glyph: String, label: String, primary: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.size(68.dp).clip(RoundedCornerShape(20.dp))
            .background(if (primary) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f))
            .clickable(onClick = onClick).padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = if (primary) Color.White else WildforceThemeTokens.textPrimary)
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = if (primary) Color.White else WildforceThemeTokens.textPrimary)
    }
}
@Composable
private fun CompactMetricStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("−", Modifier.size(32.dp).clickable(onClick = onMinus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h6)
            Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            Text("+", Modifier.size(32.dp).clickable(onClick = onPlus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h6)
        }
    }
}
@Composable
private fun MetricStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(15.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            Text(value, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
        }
        Text("−", Modifier.size(44.dp).clickable(onClick = onMinus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h5)
        Text("+", Modifier.size(44.dp).clickable(onClick = onPlus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h5)
    }
}

@Composable
private fun WorkoutFinishedScreen(workout: WorkoutDaySummary, durationSeconds: Int, completedSets: Int, volumeKg: Double, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("✓", style = MaterialTheme.typography.h2, color = Color(0xFF26A269))
        Text("ENTRENAMIENTO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(workout.title, color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("TIEMPO", formatClock(durationSeconds), Modifier.weight(1f))
            SummaryStat("SERIES", completedSets.toString(), Modifier.weight(1f))
            SummaryStat("VOLUMEN", String.format(Locale.getDefault(), "%.0f kg", volumeKg), Modifier.weight(1f))
        }
        Spacer(Modifier.height(30.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text("GUARDAR Y SALIR", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SummaryStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(14.dp)).padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

private fun targetReps(value: String?): Int = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull() ?: 10
private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)