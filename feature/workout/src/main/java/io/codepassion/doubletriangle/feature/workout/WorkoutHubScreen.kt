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
            WorkoutPlan(state, selectedDay, onWorkoutSelected, Modifier.fillMaxSize())
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
private fun WorkoutPlan(state: WorkoutHubState, selectedDay: DayOfWeek?, onWorkoutSelected: (WorkoutDaySummary) -> Unit, modifier: Modifier = Modifier) {
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
        else items(workouts, key = { it.id }) { workout -> WorkoutCard(workout) { onWorkoutSelected(workout) } }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutDaySummary, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), elevation = 0.dp) {
        Box(
            Modifier.fillMaxWidth().height(180.dp).background(
                Brush.linearGradient(listOf(Color(0xFF171717), Color(0xFF514A3D), Color(0xFF87785D))),
            ),
        ) {
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
fun WorkoutDetailScreen(workout: WorkoutDaySummary, onBack: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text("‹  VOLVER", Modifier.clickable(onClick = onBack).padding(vertical = 12.dp), color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        Text("DÍA ${workout.order}", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
        Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text("${workout.focus} · ${workout.dayType} · ${workout.estimatedMinutes} min", color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.height(22.dp))
        workout.exercises.forEachIndexed { index, exercise ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).liquidGlass(RoundedCornerShape(14.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(exercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text("${exercise.sets} series · ${exercise.reps} reps · ${exercise.restSeconds}s descanso", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp)) {
            Text("EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutDaySummary,
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double) -> Unit,
) {
    var exerciseIndex by remember { mutableStateOf(0) }
    var completedByExercise by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var reps by remember { mutableStateOf(targetReps(workout.exercises.firstOrNull()?.reps)) }
    var weightKg by remember { mutableStateOf(0.0) }
    var restRemaining by remember { mutableStateOf<Int?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var totalCompletedSets by remember { mutableStateOf(0) }
    var totalVolumeKg by remember { mutableStateOf(0.0) }
    var showsSummary by remember { mutableStateOf(false) }

    val exercise = workout.exercises.getOrNull(exerciseIndex)
    val completedForExercise = completedByExercise[exerciseIndex] ?: 0

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsedSeconds++
        }
    }
    LaunchedEffect(exerciseIndex) {
        reps = targetReps(exercise?.reps)
        weightKg = 0.0
    }
    LaunchedEffect(restRemaining) {
        val remaining = restRemaining ?: return@LaunchedEffect
        if (remaining > 0) {
            delay(1_000)
            restRemaining = remaining - 1
        } else restRemaining = null
    }

    if (showsSummary) {
        WorkoutFinishedScreen(
            workout = workout,
            durationSeconds = elapsedSeconds,
            completedSets = totalCompletedSets,
            volumeKg = totalVolumeKg,
            onDone = { onFinish(elapsedSeconds, totalCompletedSets, totalVolumeKg) },
        )
        return
    }

    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ SALIR", Modifier.clickable(onClick = onExit).padding(vertical = 12.dp), color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(formatClock(elapsedSeconds), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = if (workout.exercises.isEmpty()) 0f else (exerciseIndex + completedForExercise.toFloat() / (exercise?.sets ?: 1)) / workout.exercises.size,
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = WildforceThemeTokens.accentGold,
            backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.12f),
        )
        Spacer(Modifier.height(24.dp))

        val resting = restRemaining
        if (resting != null) {
            RestTimerContent(
                seconds = resting,
                nextLabel = exercise?.let { "Serie ${completedForExercise + 1} · ${it.name}" }.orEmpty(),
                onAddTime = { restRemaining = resting + 15 },
                onSkip = { restRemaining = null },
            )
        } else if (exercise != null) {
            Text("EJERCICIO ${exerciseIndex + 1} DE ${workout.exercises.size}", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Text(exercise.name.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
            Text("Objetivo · ${exercise.reps} reps · ${exercise.restSeconds}s descanso", color = WildforceThemeTokens.textSecondary)
            Spacer(Modifier.height(20.dp))

            Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                repeat(exercise.sets) { setIndex ->
                    val completed = setIndex < completedForExercise
                    val current = setIndex == completedForExercise
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .then(if (current) Modifier.liquidGlass(RoundedCornerShape(15.dp), emphasized = true) else Modifier.liquidGlass(RoundedCornerShape(15.dp)))
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (completed) "✓" else "${setIndex + 1}", color = if (completed) Color(0xFF26A269) else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                        Text(if (current) "SERIE ACTUAL" else if (completed) "COMPLETADA" else "PENDIENTE", Modifier.padding(start = 14.dp).weight(1f), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                        Text("${exercise.reps} reps", color = WildforceThemeTokens.textPrimary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                MetricStepper("REPETICIONES", reps.toString(), onMinus = { reps = (reps - 1).coerceAtLeast(0) }, onPlus = { reps++ })
                Spacer(Modifier.height(10.dp))
                MetricStepper("PESO", String.format(Locale.getDefault(), "%.1f kg", weightKg), onMinus = { weightKg = (weightKg - 2.5).coerceAtLeast(0.0) }, onPlus = { weightKg += 2.5 })
            }

            Button(
                onClick = {
                    val newCompleted = completedForExercise + 1
                    completedByExercise = completedByExercise + (exerciseIndex to newCompleted)
                    totalCompletedSets++
                    totalVolumeKg += reps * weightKg
                    if (newCompleted < exercise.sets) {
                        restRemaining = exercise.restSeconds
                    } else if (exerciseIndex < workout.exercises.lastIndex) {
                        exerciseIndex++
                        restRemaining = exercise.restSeconds
                    } else showsSummary = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
                elevation = ButtonDefaults.elevation(0.dp),
            ) { Text(if (completedForExercise + 1 == exercise.sets) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE", fontWeight = FontWeight.Bold) }
        } else {
            Text("Este entrenamiento no contiene ejercicios.", color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Composable
private fun RestTimerContent(seconds: Int, nextLabel: String, onAddTime: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("DESCANSO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.accentGold)
        Text(formatClock(seconds), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h2, color = WildforceThemeTokens.textPrimary)
        Text(nextLabel, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onAddTime, shape = RoundedCornerShape(14.dp)) { Text("+15 s") }
            Button(onClick = onSkip, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text("OMITIR") }
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