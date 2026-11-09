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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.isPlanFinalized
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun WorkoutPlanOverviewScreen(state: WorkoutHubState, planHistory: List<WorkoutHubState> = emptyList(), contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(), onBack: () -> Unit, onWorkoutSelected: (WorkoutDaySummary) -> Unit) {
    val skipped = state.workouts.count { it.status == WorkoutStatus.Skipped }
    val finalized = state.workouts.count { it.status.isPlanFinalized }
    val progress = if (state.workouts.isEmpty()) 0f else finalized.toFloat() / state.workouts.size
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(650))
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Volver" }.clickable(onClick = onBack).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.go_back).uppercase(), color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.training_plan).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), emphasized = true).padding(20.dp)) {
                    Text(state.planName.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(state.phase, Modifier.padding(top = 3.dp), color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_6334941613f202f4, state.mesocycleNumber, state.mesocycleIndex, state.weekIndex, state.cycleLength), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    state.mesocyclePhase?.let { phase -> Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_7bc4873e06901e86, phase.label.uppercase(), state.phaseWeek), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                            Text("${(animatedProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.mesocycle_progress).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_247465696a9e46a7, finalized, state.workouts.size), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (skipped > 0) Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_932171ddecdd9f81, skipped), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    LinearProgressIndicator(progress = animatedProgress, Modifier.fillMaxWidth().padding(top = 16.dp).height(7.dp).clip(RoundedCornerShape(6.dp)), color = WildforceThemeTokens.textPrimary, backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.15f))
                }
            }
            item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_177fbe3bbd460630), Modifier.padding(top = 4.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary) }
            items(state.workouts, key = { it.id }) { workout -> PlanSessionRow(workout) { onWorkoutSelected(workout) } }
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp)) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.next).uppercase(), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(if (finalized == state.workouts.size && state.workouts.isNotEmpty()) "Mesociclo completado. Ya puedes preparar la siguiente fase." else "Completa u omite las sesiones previstas para avanzar en tu fase actual.", Modifier.padding(top = 5.dp), color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            if (planHistory.isNotEmpty()) {
                item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_962180b7dadf0cd5), Modifier.padding(top = 4.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary) }
                items(planHistory.sortedByDescending { it.mesocycleNumber }.take(8), key = { "archive-${it.mesocycleNumber}-${it.planName}" }) { archived ->
                    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Text(archived.planName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_b221067dab9b73d3, archived.mesocycleNumber, archived.mesocycleIndex, archived.weekIndex, archived.cycleLength), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_beb27d1c23e4f96d, archived.workouts.size, archived.mesocyclePhase?.label ?: archived.phase), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSessionRow(workout: WorkoutDaySummary, onClick: () -> Unit) {
    val statusColor = when (workout.status) {
        WorkoutStatus.Completed -> Color(0xFF26A269)
        WorkoutStatus.Skipped, WorkoutStatus.Draft -> WildforceThemeTokens.textSecondary
        WorkoutStatus.InProgress -> Color(0xFFF28A29)
        // iOS WorkoutDayStatus.planned uses the system blue; orange is reserved
        // for an in-progress workout and explicit regeneration warnings.
        WorkoutStatus.Planned -> Color(0xFF0A84FF)
    }
    val statusScale = remember(workout.status) { Animatable(.78f) }
    LaunchedEffect(workout.status) { statusScale.animateTo(1f, spring(dampingRatio = .55f, stiffness = 560f)) }
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).semantics { contentDescription = "${workout.title}, ${workout.status}, ${workout.estimatedMinutes} minutos" }.clickable(onClick = onClick).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            when (workout.status) {
                WorkoutStatus.Completed -> Icon(Icons.Filled.CheckCircle, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.completed), modifier = Modifier.graphicsLayer { scaleX = statusScale.value; scaleY = statusScale.value }, tint = statusColor)
                WorkoutStatus.Skipped -> Icon(Icons.Filled.Remove, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.skipped), modifier = Modifier.graphicsLayer { scaleX = statusScale.value; scaleY = statusScale.value }, tint = statusColor)
                WorkoutStatus.InProgress -> Icon(Icons.Filled.PlayArrow, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.in_progress), modifier = Modifier.graphicsLayer { scaleX = statusScale.value; scaleY = statusScale.value }, tint = statusColor)
                WorkoutStatus.Draft, WorkoutStatus.Planned -> Text(workout.order.toString(), Modifier.graphicsLayer { scaleX = statusScale.value; scaleY = statusScale.value }, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(workout.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(workout.scheduledDay.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")) + " · ${workout.estimatedMinutes} min", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text(when (workout.status) {
            WorkoutStatus.Completed -> "COMPLETADO"
            WorkoutStatus.Skipped -> "OMITIDO"
            WorkoutStatus.InProgress -> "EN CURSO"
            WorkoutStatus.Draft -> "BORRADOR"
            WorkoutStatus.Planned -> "ABRIR"
        }, Modifier.graphicsLayer { scaleX = statusScale.value; scaleY = statusScale.value }, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = statusColor)
    }
}
