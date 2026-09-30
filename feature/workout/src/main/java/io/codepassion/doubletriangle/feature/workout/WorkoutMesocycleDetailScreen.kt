package io.codepassion.doubletriangle.feature.workout

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.MesocyclePhase
import io.codepassion.doubletriangle.core.model.WorkoutHubState

private data class MesocycleTimelineEntry(
    val state: WorkoutHubState?, val position: Int, val phase: MesocyclePhase, val phaseWeek: Int,
    val isCurrent: Boolean, val isPast: Boolean, val isProjected: Boolean,
)

private data class MesocycleTimelineGroup(val index: Int, val cycleLength: Int, val entries: List<MesocycleTimelineEntry>)

/** Android counterpart of iOS WorkoutMesocycleDetailView. */
@Composable
internal fun WorkoutMesocycleDetailScreen(state: WorkoutHubState, planHistory: List<WorkoutHubState>, contentPadding: PaddingValues, onBack: () -> Unit) {
    var expandedHistory by remember { mutableStateOf(setOf<Int>()) }
    val currentGroup = remember(state, planHistory) { currentMesocycleGroup(state, planHistory) }
    val previousGroups = remember(state, planHistory) { previousMesocycleGroups(state, planHistory) }
    val currentEntry = currentGroup.entries.firstOrNull { it.isCurrent }

    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(contentPadding)) {
        // iOS presents this as a sheet with a trailing Done action.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text("HECHO", Modifier.clickable(onClick = onBack).padding(8.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { MesocycleSummaryCard(state, currentGroup, currentEntry) }
            item { DetailSectionHeader("Mesociclo actual", "Tu posición actual dentro del ciclo activo.") }
            item { MesocycleGroupCard(currentGroup, currentGroup.entries.any { it.isProjected }) }
            if (previousGroups.isNotEmpty()) {
                item { DetailSectionHeader("Mesociclos anteriores", "Historial de entrenamientos completados por ciclo.") }
                previousGroups.forEach { group ->
                    item(key = "archived-mesocycle-${group.index}") {
                        val expanded = group.index in expandedHistory
                        CollapsibleMesocycleGroupCard(group, expanded) {
                            expandedHistory = if (expanded) expandedHistory - group.index else expandedHistory + group.index
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MesocycleSummaryCard(state: WorkoutHubState, group: MesocycleTimelineGroup, currentEntry: MesocycleTimelineEntry?) {
    val currentWeek = currentEntry?.position ?: state.positionInCycle.coerceIn(1, group.cycleLength)
    val phase = currentEntry?.phase ?: state.mesocyclePhase ?: phaseForMesocycleWeek(currentWeek, group.cycleLength)
    val phaseWeek = currentEntry?.phaseWeek ?: state.phaseWeek
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MESOCICLO ACTUAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Text("Mesociclo ${group.index}", fontFamily = Exo2FontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text("Semana $currentWeek de ${group.cycleLength} · ${phase.label}", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        MesocyclePillProgress(currentWeek, group.cycleLength)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The Android state only carries the goal display name; iOS resolves its specific goal symbol.
            ContextPill(state.user.goal, "▲")
            ContextPill("Activo", Icons.Filled.CheckCircle)
        }
        Spacer(Modifier.height(4.dp))
        DetailMetric("Esta semana", state.planName)
        DetailMetric("Cronología", "${phase.label} · semana $phaseWeek", phaseIcon(phase))
    }
}

/** Matches iOS's compact capsule progress bar, including its taller current week. */
@Composable
private fun MesocyclePillProgress(currentWeek: Int, totalWeeks: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..totalWeeks).forEach { week ->
            Box(Modifier.width(7.dp).height(if (week == currentWeek) 20.dp else 18.dp).clip(RoundedCornerShape(50)).background(
                when {
                    week < currentWeek -> WildforceThemeTokens.textPrimary.copy(alpha = .75f)
                    week == currentWeek -> WildforceThemeTokens.textPrimary
                    else -> WildforceThemeTokens.textSecondary.copy(alpha = .12f)
                },
            ))
        }
    }
}

@Composable
private fun DetailMetric(title: String, value: String, icon: ImageVector? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Medium, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        icon?.let { Icon(it, null, Modifier.padding(end = 6.dp).size(16.dp), WildforceThemeTokens.textSecondary) }
        Text(value, Modifier.widthIn(max = 160.dp), style = MaterialTheme.typography.subtitle2, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailSectionHeader(title: String, subtitle: String) = Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
    Text(subtitle, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
}

@Composable
private fun CollapsibleMesocycleGroupCard(group: MesocycleTimelineGroup, expanded: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).clickable(onClick = onToggle).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MesocycleGroupHeader(group, false, Modifier.weight(1f))
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = WildforceThemeTokens.textSecondary)
        }
        if (expanded) { Spacer(Modifier.height(12.dp)); MesocycleEntries(group) }
    }
}

@Composable
private fun MesocycleGroupCard(group: MesocycleTimelineGroup, showProjectedBadge: Boolean) = Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp)) {
    MesocycleGroupHeader(group, showProjectedBadge)
    Spacer(Modifier.height(12.dp))
    MesocycleEntries(group)
}

@Composable
private fun MesocycleGroupHeader(group: MesocycleTimelineGroup, showProjectedBadge: Boolean, modifier: Modifier = Modifier) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text("Mesociclo ${group.index}", fontFamily = Exo2FontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
        Text("${group.cycleLength} semanas", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
    if (showProjectedBadge) ContextPill("Proyectado", "•")
}

@Composable
private fun MesocycleEntries(group: MesocycleTimelineGroup) {
    group.entries.forEachIndexed { index, entry ->
        MesocycleEntryRow(entry)
        if (index < group.entries.lastIndex) Divider(Modifier.padding(start = 52.dp), color = WildforceThemeTokens.textSecondary.copy(alpha = .15f))
    }
}

@Composable
private fun MesocycleEntryRow(entry: MesocycleTimelineEntry) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(when {
            entry.isCurrent -> phaseColor(entry.phase).copy(alpha = .8f)
            entry.isProjected -> WildforceThemeTokens.textSecondary.copy(alpha = .14f)
            else -> WildforceThemeTokens.textSecondary.copy(alpha = .44f)
        }), contentAlignment = Alignment.Center) {
            Text(if (entry.isPast) "✓" else entry.position.toString(), fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (entry.isProjected) "Próximamente" else entry.state?.planName.orEmpty(), style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.phase.label} · semana ${entry.phaseWeek}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            if (!entry.isProjected && entry.state != null) {
                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp), WildforceThemeTokens.textSecondary)
                    Text("Semana ${entry.position}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
        // Keep the phase badge bounded. Without this, Compose measures this
        // unweighted child first and can leave the plan title only a few pixels
        // wide, which makes it wrap one character per line on narrow layouts.
        ContextPill(entry.phase.label, phaseIcon(entry.phase), Modifier.widthIn(max = 124.dp))
    }
}

private fun currentMesocycleGroup(current: WorkoutHubState, history: List<WorkoutHubState>): MesocycleTimelineGroup {
    val cycleLength = current.cycleLength.coerceIn(1, 12)
    val saved = (history.filter { it.mesocycleIndex == current.mesocycleIndex } + current).associateBy { it.positionInCycle.coerceIn(1, cycleLength) }
    val currentPosition = current.positionInCycle.coerceIn(1, cycleLength)
    return MesocycleTimelineGroup(current.mesocycleIndex, cycleLength, (1..cycleLength).map { position ->
        val plan = saved[position]
        val phase = plan?.mesocyclePhase ?: phaseForMesocycleWeek(position, cycleLength)
        MesocycleTimelineEntry(plan, position, phase, plan?.phaseWeek ?: phaseWeek(position, phase, cycleLength), position == currentPosition, position < currentPosition, plan == null && position > currentPosition)
    })
}

private fun previousMesocycleGroups(current: WorkoutHubState, history: List<WorkoutHubState>): List<MesocycleTimelineGroup> =
    history.filter { it.mesocycleIndex < current.mesocycleIndex }.groupBy { it.mesocycleIndex }.toSortedMap(compareByDescending { it }).map { (index, plans) ->
        val cycleLength = plans.maxOf { it.cycleLength }.coerceIn(1, 12)
        MesocycleTimelineGroup(index, cycleLength, plans.sortedBy { it.positionInCycle }.map { plan ->
            val position = plan.positionInCycle.coerceIn(1, cycleLength)
            val phase = plan.mesocyclePhase ?: phaseForMesocycleWeek(position, cycleLength)
            MesocycleTimelineEntry(plan, position, phase, plan.phaseWeek, false, true, false)
        })
    }

private fun phaseWeek(week: Int, phase: MesocyclePhase, totalWeeks: Int): Int = when (phase) {
    MesocyclePhase.Accumulation -> week
    MesocyclePhase.Intensification -> week - (if (totalWeeks <= 4) totalWeeks - 1 else totalWeeks / 2)
    MesocyclePhase.Deload -> 1
}

private fun phaseColor(phase: MesocyclePhase) = when (phase) {
    MesocyclePhase.Accumulation -> Color(0xFF3D8BFF)
    MesocyclePhase.Intensification -> Color(0xFFF28A29)
    MesocyclePhase.Deload -> Color(0xFF26A269)
}
