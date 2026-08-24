package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun WorkoutPlanOverviewScreen(state: WorkoutHubState, contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(), onBack: () -> Unit, onWorkoutSelected: (WorkoutDaySummary) -> Unit) {
    val completed = state.workouts.count { it.status == WorkoutStatus.Completed }
    val skipped = state.workouts.count { it.status == WorkoutStatus.Skipped }
    val progress = if (state.workouts.isEmpty()) 0f else completed.toFloat() / state.workouts.size
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(650))
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ VOLVER", Modifier.clickable(onClick = onBack).padding(vertical = 9.dp, horizontal = 4.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("PLAN DE ENTRENAMIENTO", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), emphasized = true).padding(20.dp)) {
                    Text(state.planName.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                    Text(state.phase, Modifier.padding(top = 3.dp), color = WildforceThemeTokens.textSecondary)
                    Text("MESOCICLO ${state.mesocycleIndex}  ·  SEMANA ${state.weekIndex}/${state.cycleLength}", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(WildforceThemeTokens.accentGold.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Text("${(animatedProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("PROGRESO DEL MESOCICLO", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                            Text("$completed de ${state.workouts.size} sesiones completadas", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                            if (skipped > 0) Text("$skipped sesión(es) omitida(s)", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                    LinearProgressIndicator(progress = animatedProgress, Modifier.fillMaxWidth().padding(top = 16.dp).height(7.dp).clip(RoundedCornerShape(6.dp)), color = WildforceThemeTokens.accentGold, backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.15f))
                }
            }
            item { Text("SESIONES DE LA SEMANA", Modifier.padding(top = 4.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary) }
            items(state.workouts, key = { it.id }) { workout -> PlanSessionRow(workout) { onWorkoutSelected(workout) } }
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp)) {
                    Text("SIGUIENTE PASO", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    Text(if (completed == state.workouts.size && state.workouts.isNotEmpty()) "Mesociclo completado. Ya puedes preparar la siguiente fase." else "Completa las sesiones previstas para avanzar en tu fase actual.", Modifier.padding(top = 5.dp), color = WildforceThemeTokens.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PlanSessionRow(workout: WorkoutDaySummary, onClick: () -> Unit) {
    val statusColor = when (workout.status) { WorkoutStatus.Completed -> Color(0xFF26A269); WorkoutStatus.Skipped -> WildforceThemeTokens.textSecondary; WorkoutStatus.Planned -> WildforceThemeTokens.accentGold }
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Text(if (workout.status == WorkoutStatus.Completed) "✓" else if (workout.status == WorkoutStatus.Skipped) "–" else workout.order.toString(), fontWeight = FontWeight.Bold, color = statusColor)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(workout.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            Text(workout.scheduledDay.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")) + " · ${workout.estimatedMinutes} min", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Text(when (workout.status) { WorkoutStatus.Completed -> "COMPLETADO"; WorkoutStatus.Skipped -> "OMITIDO"; WorkoutStatus.Planned -> "ABRIR" }, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = statusColor)
    }
}
