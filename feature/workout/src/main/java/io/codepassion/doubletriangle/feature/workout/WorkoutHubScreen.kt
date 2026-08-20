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
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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

@Composable
fun WorkoutHubScreen(
    contentPadding: PaddingValues,
    state: WorkoutHubState = PreviewWorkoutRepository.load(),
) {
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var mode by remember { mutableStateOf(WorkoutMode.Plan) }

    Column(
        Modifier.fillMaxSize().padding(contentPadding).liquidGlassBackground(),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .liquidGlass(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp), emphasized = true)
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
            WorkoutPlan(state, selectedDay, Modifier.fillMaxSize())
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
private fun WorkoutPlan(state: WorkoutHubState, selectedDay: DayOfWeek?, modifier: Modifier = Modifier) {
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
        else items(workouts, key = { it.id }) { WorkoutCard(it) }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutDaySummary) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = 0.dp) {
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
    Surface(Modifier.fillMaxWidth(), color = Color.Transparent, elevation = 0.dp) {
        Column(Modifier.liquidGlass(RoundedCornerShape(18.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DÍA DE DESCANSO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
            Text("No hay entrenamiento planificado.", color = WildforceThemeTokens.textSecondary)
        }
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
