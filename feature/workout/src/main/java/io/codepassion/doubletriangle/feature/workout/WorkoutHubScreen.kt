package io.codepassion.doubletriangle.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutMode
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.displayBlocks
import io.codepassion.doubletriangle.core.model.workoutsFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkoutHubScreen(
    contentPadding: PaddingValues,
    state: WorkoutHubState = PreviewWorkoutRepository.load(),
    planHistory: List<WorkoutHubState> = emptyList(),
    onWorkoutSelected: (WorkoutDaySummary) -> Unit = {},
    gender: String = "male",
    defaultCustomEquipment: String = "Peso corporal",
    customEquipmentPresets: List<Pair<String, String>> = emptyList(),
    generationError: String? = null,
    onRetryGeneration: () -> Unit = {},
    customAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null,
) {
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var mode by remember { mutableStateOf(WorkoutMode.Plan) }
    val customContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var customWorkouts by remember { mutableStateOf(CustomWorkoutStore.load(customContext)) }
    var editingCustomWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var showingPlanOverview by remember { mutableStateOf(false) }
    var generatingCustomWorkout by remember { mutableStateOf(false) }
    var customGenerationError by remember { mutableStateOf<String?>(null) }
    var lastCustomRequest by remember { mutableStateOf<CustomWorkoutRequest?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var hubEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hubEntered = true }

    editingCustomWorkout?.let { draft ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            CustomWorkoutEditorScreen(draft, gender, onCancel = { editingCustomWorkout = null }) { saved ->
                CustomWorkoutStore.save(customContext, saved)
                customWorkouts = CustomWorkoutStore.load(customContext)
                editingCustomWorkout = null
            }
        }
        return
    }
    if (showingPlanOverview) {
        WorkoutPlanOverviewScreen(state, planHistory, contentPadding, onBack = { showingPlanOverview = false }, onWorkoutSelected = onWorkoutSelected)
        return
    }
    Column(
        Modifier.fillMaxSize().padding(contentPadding).liquidGlassBackground(),
    ) {
        generationError?.let { error ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
                    .liquidGlass(RoundedCornerShape(14.dp)).padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("No se pudo generar el plan: $error", Modifier.weight(1f), color = Color(0xFFC62828), style = MaterialTheme.typography.caption)
                TextButton(onClick = onRetryGeneration) {
                    Text("REINTENTAR", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedVisibility(visible = hubEntered, enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { -it / 18 }) {
            Column(
                Modifier.fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp), emphasized = true)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                WorkoutHeader(state)
                PlanContextCard(state) { showingPlanOverview = true }
                Spacer(Modifier.height(16.dp))
                WeekCalendar(state, selectedDay) { selectedDay = if (selectedDay == it) null else it }
            }
        }

        AnimatedVisibility(visible = hubEntered, enter = fadeIn(tween(320, delayMillis = 70)) + slideInVertically(tween(320, delayMillis = 70)) { it / 18 }) {
            WorkoutModeSelector(
                selected = mode,
                onSelected = { mode = it },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
        AnimatedVisibility(mode == WorkoutMode.Plan) {
            WorkoutPlan(state, selectedDay, onWorkoutSelected, gender, Modifier.fillMaxSize())
        }
        AnimatedVisibility(mode == WorkoutMode.Custom) {
            CustomWorkoutsScreen(
                workouts = customWorkouts.filter { selectedDay == null || it.scheduledDay == selectedDay }, gender = gender,
                defaultEquipment = defaultCustomEquipment,
                equipmentPresets = customEquipmentPresets,
                onCreateManual = { editingCustomWorkout = CustomWorkoutStore.empty(selectedDay ?: LocalDate.now().dayOfWeek) },
                onCreateAutomatic = { request ->
                    lastCustomRequest = request
                    customGenerationError = null
                    if (customAiGenerator == null) {
                        editingCustomWorkout = CustomWorkoutStore.automatic(selectedDay ?: LocalDate.now().dayOfWeek)
                    } else {
                        generatingCustomWorkout = true
                        coroutineScope.launch {
                            runCatching { customAiGenerator.invoke(request) }
                                .onSuccess { generated -> lastCustomRequest = null; editingCustomWorkout = generated.copy(scheduledDay = selectedDay ?: LocalDate.now().dayOfWeek) }
                                .onFailure { error -> customGenerationError = error.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el entrenamiento." }
                            generatingCustomWorkout = false
                        }
                    }
                },
                onOpen = onWorkoutSelected, onEdit = { editingCustomWorkout = it },
                onDuplicate = { source ->
                    editingCustomWorkout = CustomWorkoutStore.duplicate(customContext, source)
                    customWorkouts = CustomWorkoutStore.load(customContext)
                },
                onDelete = { workout ->
                    WorkoutSessionStore.clear(customContext, workout.id)
                    CustomWorkoutStore.delete(customContext, workout.id)
                    customWorkouts = CustomWorkoutStore.load(customContext)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    if (generatingCustomWorkout) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = WildforceThemeTokens.accentGold)
                Text("GENERANDO ENTRENAMIENTO CON IA…", Modifier.padding(top = 12.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
    customGenerationError?.let { error ->
        AlertDialog(
            onDismissRequest = { customGenerationError = null },
            title = { Text("NO SE PUDO GENERAR CON IA", fontFamily = AntonFontFamily) },
            text = { Text(error) },
            confirmButton = {
                Row {
                    lastCustomRequest?.let { request ->
                        TextButton(onClick = {
                            customGenerationError = null
                            if (customAiGenerator != null) {
                                generatingCustomWorkout = true
                                coroutineScope.launch {
                                    runCatching { customAiGenerator.invoke(request) }
                                        .onSuccess { generated -> lastCustomRequest = null; editingCustomWorkout = generated.copy(scheduledDay = selectedDay ?: LocalDate.now().dayOfWeek) }
                                        .onFailure { retryError -> customGenerationError = retryError.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el entrenamiento." }
                                    generatingCustomWorkout = false
                                }
                            }
                        }) { Text("REINTENTAR", color = WildforceThemeTokens.accentGold) }
                    }
                    TextButton(onClick = {
                        customGenerationError = null
                        editingCustomWorkout = CustomWorkoutStore.automatic(selectedDay ?: LocalDate.now().dayOfWeek)
                    }) { Text("USAR PLANTILLA LOCAL", color = WildforceThemeTokens.accentGold) }
                }
            },
            dismissButton = { TextButton(onClick = { customGenerationError = null }) { Text("CERRAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

@Composable
private fun PlanContextCard(state: WorkoutHubState, onClick: () -> Unit) {
    val mesocycleProgress by animateFloatAsState(
        (state.weekIndex.toFloat() / state.cycleLength.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(500),
    )
    Row(
        Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(state.planName.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.subtitle1, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.phase, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Plan ${state.mesocycleNumber} · Mesociclo ${state.mesocycleIndex} · Semana ${state.weekIndex}/${state.cycleLength}", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            state.mesocyclePhase?.let { phase ->
                Text("${phase.label} · Semana de fase ${state.phaseWeek}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LinearProgressIndicator(
                progress = mesocycleProgress,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(4.dp).clip(RoundedCornerShape(4.dp)),
                color = WildforceThemeTokens.accentGold,
                backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.14f),
            )
        }
        Text("VER PLAN  ›", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
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
            Text(state.user.name, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("▲ ${state.user.goal}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("🔥", style = MaterialTheme.typography.h6)
        Text(
            state.user.currentStreak.toString(),
            Modifier.padding(start = 4.dp),
            color = WildforceThemeTokens.textPrimary,
            fontWeight = FontWeight.Bold,
        )
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
                    maxLines = 1,
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
    var cardsEntered by remember(selectedDay, state.planName) { mutableStateOf(false) }
    LaunchedEffect(selectedDay, state.planName) { cardsEntered = true }
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
        else itemsIndexed(workouts, key = { _, workout -> workout.id }) { index, workout ->
            AnimatedVisibility(visible = cardsEntered, enter = fadeIn(tween(280, delayMillis = 70 + index * 55)) + slideInVertically(tween(280, delayMillis = 70 + index * 55)) { it / 16 }) {
                WorkoutCard(workout, gender) { onWorkoutSelected(workout) }
            }
        }
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
                    when (workout.status) {
                        WorkoutStatus.Completed -> StatusBadge("COMPLETADO", Color(0xFF26A269))
                        WorkoutStatus.Skipped -> StatusBadge("OMITIDO", WildforceThemeTokens.textSecondary)
                        WorkoutStatus.Planned -> Unit
                    }
                }
                Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "◎ ${workout.focus}   ◆ ${workout.dayType}   ◷ ${workout.estimatedMinutes} min",
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, background: Color) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(12.dp)).background(background).padding(horizontal = 9.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RestDayCard() {
    Column(
        Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("DÍA DE DESCANSO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        Text("No hay entrenamiento planificado.", color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutHubPreview() = WildforceTheme { WorkoutHubScreen(PaddingValues()) }

@Composable
fun WorkoutDetailScreen(workout: WorkoutDaySummary, gender: String, useImperial: Boolean = false, onBack: () -> Unit, onStart: () -> Unit, onSkip: () -> Unit = {}, onUnskip: () -> Unit = {}, onWorkoutUpdated: (WorkoutDaySummary) -> Unit = {}, defaultAdaptEquipment: String = "Peso corporal", adaptEquipmentPresets: List<Pair<String, String>> = emptyList(), adaptAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null) {
    var expandedExercise by remember { mutableStateOf<Int?>(null) }
    var collapsedBlocks by remember(workout.id) { mutableStateOf(emptySet<Int>()) }
    val detailContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var hasSavedSession by remember(workout.id) { mutableStateOf(WorkoutSessionStore.load(detailContext, workout.id) != null) }
    var guideExercise by remember { mutableStateOf<ExerciseSummary?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showingEditor by remember(workout.id) { mutableStateOf(false) }
    var showingAdaptation by remember(workout.id) { mutableStateOf(false) }
    var adapting by remember(workout.id) { mutableStateOf(false) }
    var adaptationError by remember(workout.id) { mutableStateOf<String?>(null) }
    var pendingAdapted by remember(workout.id) { mutableStateOf<WorkoutDaySummary?>(null) }
    var lastAdaptRequest by remember(workout.id) { mutableStateOf<CustomWorkoutRequest?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showSkipConfirmation by remember { mutableStateOf(false) }
    var skipped by remember(workout.id) { mutableStateOf(workout.status == WorkoutStatus.Skipped) }
    if (showingEditor) {
        CustomWorkoutEditorScreen(
            initial = workout,
            gender = gender,
            onCancel = { showingEditor = false },
            onSave = { updated -> showingEditor = false; onWorkoutUpdated(updated) },
        )
        return
    }
    if (showingAdaptation) {
        AutomaticWorkoutRequestDialog(
            defaultEquipment = defaultAdaptEquipment,
            equipmentPresets = adaptEquipmentPresets,
            initialFocus = workout.focus,
            initialDuration = workout.estimatedMinutes,
            title = "ADAPTAR ENTRENAMIENTO",
            onDismiss = { showingAdaptation = false },
            onGenerate = { request ->
                showingAdaptation = false
                val generator = adaptAiGenerator ?: return@AutomaticWorkoutRequestDialog
                lastAdaptRequest = request
                adaptationError = null
                adapting = true
                coroutineScope.launch {
                    runCatching { generator(request) }
                        .onSuccess { pendingAdapted = it }
                        .onFailure { adaptationError = it.message ?: "No se pudo adaptar el entrenamiento." }
                    adapting = false
                }
            },
        )
    }
    guideExercise?.let { selected ->
        ExerciseGuideScreen(selected, gender) { guideExercise = null }
        return
    }
    if (skipped) {
        Box(Modifier.fillMaxSize().liquidGlassBackground(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("‹ VOLVER", Modifier.clickable(onClick = onBack).padding(12.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                Text("DÍA OMITIDO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text("Este entrenamiento se ha marcado como descanso.", Modifier.padding(top = 8.dp), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("DESHACER", Modifier.clickable { skipped = false; onUnskip() }.padding(14.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            }
        }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  VOLVER", Modifier.clickable(onClick = onBack).padding(horizontal = 18.dp, vertical = 18.dp), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box {
                    Text("•••", Modifier.clickable { showMenu = true }.padding(horizontal = 18.dp, vertical = 18.dp), color = Color.White, fontWeight = FontWeight.Bold)
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!workout.id.startsWith("custom-")) DropdownMenuItem(onClick = { showMenu = false; showingEditor = true }) { Text("Editar ejercicios") }
                        if (adaptAiGenerator != null) DropdownMenuItem(onClick = { showMenu = false; showingAdaptation = true }) { Text("Adaptar al equipamiento") }
                        DropdownMenuItem(onClick = { showMenu = false; showSkipConfirmation = true }) { Text("Omitir entrenamiento") }
                    }
                }
            }
            Spacer(Modifier.height(190.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(22.dp)) {
                Text("DÍA ${workout.order}", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("${workout.focus} · ${workout.dayType} · ${workout.estimatedMinutes} min", color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.height(20.dp))
                workout.displayBlocks().forEachIndexed { blockIndex, block ->
                    val collapsible = block.type == WorkoutBlockType.Warmup || block.type == WorkoutBlockType.Cooldown
                    val collapsed = blockIndex in collapsedBlocks
                    val showHeader = block.type != WorkoutBlockType.Standard || !block.notes.isNullOrBlank()
                    if (showHeader) {
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = collapsible) {
                                collapsedBlocks = if (collapsed) collapsedBlocks - blockIndex else collapsedBlocks + blockIndex
                            }.padding(top = 14.dp, bottom = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (collapsible) Text(if (collapsed) "›" else "⌄", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(end = 8.dp))
                                Box(
                                    Modifier.size(width = 4.dp, height = 24.dp).clip(RoundedCornerShape(3.dp)).background(
                                        when (block.type) {
                                            WorkoutBlockType.Warmup -> Color(0xFFF08A24)
                                            WorkoutBlockType.Cooldown -> Color(0xFF4A8FE7)
                                            else -> WildforceThemeTokens.accentGold
                                        },
                                    ),
                                )
                                Text(block.type.label.uppercase(), Modifier.padding(start = 10.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                if (block.rounds > 1) {
                                    Text("${block.rounds} RONDAS", Modifier.padding(start = 9.dp).background(WildforceThemeTokens.accentGold.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                                }
                            }
                            block.notes?.let { Text(it, Modifier.padding(top = 5.dp, start = if (collapsible) 22.dp else 14.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                        }
                    }
                    if (!collapsed) {
                        block.exercises.forEachIndexed { exerciseIndex, exercise ->
                            val itemKey = blockIndex * 1000 + exerciseIndex
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = if (block.type == WorkoutBlockType.Superset) 8.dp else 0.dp).liquidGlass(RoundedCornerShape(14.dp))
                                    .clickable { expandedExercise = if (expandedExercise == itemKey) null else itemKey }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (block.type == WorkoutBlockType.Superset) {
                                    Text("A${exerciseIndex + 1}", Modifier.padding(end = 9.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                                }
                                RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), null, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
                                Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                                    Text(exercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                    val setText = if (block.type == WorkoutBlockType.Superset) "${block.rounds} rondas" else "${exercise.sets} series"
                                    val restText = if (exercise.restSeconds > 0) " · ${exercise.restSeconds}s" else ""
                                    val targetWeightText = exercise.targetWeightKg?.let { " · ${formatTrainingWeight(it, useImperial)}" }.orEmpty()
                                    Text("$setText · ${exercise.reps} reps$restText$targetWeightText", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                                }
                                Text(if (expandedExercise == itemKey) "⌃" else "⌄", color = WildforceThemeTokens.accentGold)
                            }
                            if (expandedExercise == itemKey) {
                                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                                    Text("MÚSCULOS IMPLICADOS", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                                    MuscleStrip(exercise.imageKey, onDarkBackground = false, modifier = Modifier.padding(vertical = 5.dp))
                                    Text("Objetivo: ${exercise.reps} repeticiones${if (exercise.restSeconds > 0) " con ${exercise.restSeconds}s de recuperación" else ""}.", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                                    Text("CÓMO HACERLO  →", Modifier.padding(top = 10.dp).clickable { guideExercise = exercise }, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                        block.restAfterBlockSeconds?.let { rest ->
                            Text("◷  Descansa ${rest}s al terminar el bloque", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { hasSavedSession = true; onStart() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp)) {
                    Text(if (hasSavedSession) "REANUDAR ENTRENAMIENTO" else "EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showSkipConfirmation) {
        AlertDialog(
            onDismissRequest = { showSkipConfirmation = false },
            title = { Text("¿OMITIR ENTRENAMIENTO?", fontFamily = AntonFontFamily) },
            text = { Text("El día quedará marcado como descanso y podrás volver a abrirlo después.") },
            confirmButton = { TextButton(onClick = { skipped = true; showSkipConfirmation = false; onSkip() }) { Text("OMITIR", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = { showSkipConfirmation = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
    if (adapting) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { }, contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = WildforceThemeTokens.accentGold)
                Text("ADAPTANDO ENTRENAMIENTO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("La IA conserva el objetivo de la sesión y sustituye solo lo necesario.", color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
    adaptationError?.let { error ->
        AlertDialog(onDismissRequest = { adaptationError = null }, title = { Text("NO SE PUDO ADAPTAR") }, text = { Text(error) }, confirmButton = {
            TextButton(onClick = {
                val request = lastAdaptRequest
                val generator = adaptAiGenerator
                if (request != null && generator != null) {
                    adaptationError = null; adapting = true
                    coroutineScope.launch {
                        runCatching { generator(request) }
                            .onSuccess { pendingAdapted = it }
                            .onFailure { adaptationError = it.message ?: "No se pudo adaptar el entrenamiento." }
                        adapting = false
                    }
                } else adaptationError = null
            }) { Text("REINTENTAR", color = WildforceThemeTokens.accentGold) }
        }, dismissButton = { TextButton(onClick = { adaptationError = null }) { Text("CERRAR", color = WildforceThemeTokens.textSecondary) } })
    }
    pendingAdapted?.let { adapted ->
        AlertDialog(onDismissRequest = { pendingAdapted = null }, title = { Text("ENTRENAMIENTO ADAPTADO", fontFamily = AntonFontFamily) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Revisa los cambios antes de sustituir la sesión actual.", color = WildforceThemeTokens.textSecondary)
                Text(adapted.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("${adapted.exercises.size} ejercicios · ${adapted.estimatedMinutes} min", color = WildforceThemeTokens.textSecondary)
                Text("La sesión original se conservará hasta confirmar.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold)
            }
        }, confirmButton = { TextButton(onClick = { pendingAdapted = null; onWorkoutUpdated(adapted) }) { Text("APLICAR", color = WildforceThemeTokens.accentGold) } }, dismissButton = { TextButton(onClick = { pendingAdapted = null }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } })
    }
}
@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutDaySummary,
    gender: String,
    useImperial: Boolean = false,
    skipRestPeriods: Boolean = false,
    currentStreak: Int = 0,
    isPlanCompletedAfterWorkout: Boolean = false,
    planName: String = "",
    completedPlanWorkouts: Int = 0,
    totalPlanWorkouts: Int = 0,
    totalPlanExercises: Int = 0,
    onGenerateNextPlan: () -> Unit = {},
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double, streak: Int) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val restored = remember(workout.id) { WorkoutSessionStore.load(context, workout.id) }
    var exerciseIndex by remember(workout.id) { mutableStateOf((restored?.exerciseIndex ?: 0).coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))) }
    var completedByExercise by remember(workout.id) { mutableStateOf(restored?.completedByExercise ?: emptyMap()) }
    var reps by remember(workout.id) { mutableStateOf(restored?.reps ?: targetReps(workout.exercises.firstOrNull()?.reps)) }
    var weightKg by remember(workout.id) { mutableStateOf(restored?.weightKg ?: workout.exercises.firstOrNull()?.targetWeightKg ?: 0.0) }
    var restRemaining by remember(workout.id) { mutableStateOf(restored?.restRemaining) }
    var restInitialSeconds by remember(workout.id) { mutableStateOf(restored?.restInitialSeconds ?: 1) }
    var restBetweenExercises by remember(workout.id) { mutableStateOf(restored?.restBetweenExercises ?: false) }
    var elapsedSeconds by remember(workout.id) { mutableStateOf(restored?.elapsedSeconds ?: 0) }
    var totalCompletedSets by remember(workout.id) { mutableStateOf(restored?.totalCompletedSets ?: 0) }
    var totalVolumeKg by remember(workout.id) { mutableStateOf(restored?.totalVolumeKg ?: 0.0) }
    var showsSummary by remember(workout.id) { mutableStateOf(restored?.showsSummary ?: false) }
    var pendingFeedback by remember(workout.id) { mutableStateOf(restored?.pendingFeedback ?: false) }
    var selectedFeedback by remember(workout.id) { mutableStateOf(restored?.selectedFeedback) }
    var pendingNote by remember(workout.id) { mutableStateOf(restored?.pendingNote.orEmpty()) }
    var exerciseStats by remember(workout.id) { mutableStateOf(restored?.exerciseStats ?: emptyMap()) }
    var feedbackByExercise by remember(workout.id) { mutableStateOf(restored?.feedbackByExercise ?: emptyMap()) }
    var notesByExercise by remember(workout.id) { mutableStateOf(restored?.notesByExercise ?: emptyMap()) }
    var completedSetRecords by remember(workout.id) { mutableStateOf(restored?.completedSetRecords ?: emptyList()) }
    val initialExerciseDuration = targetDurationSeconds(workout.exercises.firstOrNull()?.reps)
    var exerciseTimeRemaining by remember(workout.id) { mutableStateOf(restored?.exerciseTimeRemaining ?: initialExerciseDuration) }
    var exerciseTimeInitial by remember(workout.id) { mutableStateOf(restored?.exerciseTimeInitial?.takeIf { it > 0 } ?: (initialExerciseDuration ?: 0)) }
    var exerciseTimerRunning by remember(workout.id) { mutableStateOf(restored?.exerciseTimerRunning ?: false) }
    var addedSetsByExercise by remember(workout.id) { mutableStateOf(restored?.addedSetsByExercise ?: emptyMap()) }
    var showsWorkoutPath by remember { mutableStateOf(false) }
    var showsExerciseHistory by remember { mutableStateOf(false) }
    var showsExerciseGuide by remember { mutableStateOf(false) }
    var showsSetStyleInfo by remember { mutableStateOf(false) }
    var showsExitDialog by remember { mutableStateOf(false) }
    var initializedExerciseIndex by remember(workout.id) { mutableStateOf(restored?.exerciseIndex ?: -1) }
    val exercise = workout.exercises.getOrNull(exerciseIndex)
    val completedForExercise = completedByExercise[exerciseIndex] ?: 0
    val effectiveSets = (exercise?.sets ?: 1) + (addedSetsByExercise[exerciseIndex] ?: 0)
    val effectiveExercise = exercise?.copy(sets = effectiveSets)
    val structuredPathBlocks = remember(workout) { workout.pathBlocks() }
    val logicalExercises = remember(structuredPathBlocks) { structuredPathBlocks.flatMap { it.exercises } }
    val logicalExerciseIndex = logicalExercises.indexOfFirst { exerciseIndex in it.executionIndices }.coerceAtLeast(0)
    val currentPathBlock = structuredPathBlocks.firstOrNull { block -> block.exercises.any { exerciseIndex in it.executionIndices } }
    fun advanceFromExercise(restSeconds: Int) {
        if (exerciseIndex < workout.exercises.lastIndex) {
            exerciseIndex++
            if (!skipRestPeriods && restSeconds > 0) {
                restInitialSeconds = restSeconds
                restBetweenExercises = true
                restRemaining = restSeconds
            } else {
                restRemaining = null
            }
        } else {
            showsSummary = true
        }
    }

    LaunchedEffect(showsSummary) { while (!showsSummary) { delay(1_000); elapsedSeconds++ } }
    LaunchedEffect(exerciseIndex) {
        if (initializedExerciseIndex != exerciseIndex) {
            reps = targetReps(exercise?.reps)
            weightKg = exercise?.targetWeightKg ?: 0.0
            val duration = targetDurationSeconds(exercise?.reps)
            exerciseTimeRemaining = duration
            exerciseTimeInitial = duration ?: 0
            exerciseTimerRunning = false
            initializedExerciseIndex = exerciseIndex
        }
    }
    LaunchedEffect(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, restBetweenExercises, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, pendingNote, showsSummary, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise) {
        WorkoutSessionStore.save(
            context, workout.id,
            WorkoutSessionSnapshot(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, restBetweenExercises, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, showsSummary, selectedFeedback, pendingNote, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise),
        )
    }
    LaunchedEffect(restRemaining) {
        val remaining = restRemaining ?: return@LaunchedEffect
        if (remaining > 0) {
            delay(1_000)
            restRemaining = remaining - 1
        } else {
            restRemaining = null
            restBetweenExercises = false
        }
    }
    LaunchedEffect(exerciseTimerRunning, exerciseTimeRemaining) {
        val remaining = exerciseTimeRemaining ?: return@LaunchedEffect
        if (exerciseTimerRunning && remaining > 0) {
            delay(1_000); exerciseTimeRemaining = remaining - 1
        } else if (remaining <= 0) exerciseTimerRunning = false
    }

    if (showsWorkoutPath) {
        WorkoutPathScreen(workout, gender, exerciseIndex, completedByExercise, addedSetsByExercise, elapsedSeconds) { showsWorkoutPath = false }
        return
    }

    if (showsExerciseHistory && exercise != null) {
        ExerciseHistoryScreen(exercise, gender, WorkoutHistoryStore.history(context, exercise), useImperial) { showsExerciseHistory = false }
        return
    }

    if (showsExerciseGuide && exercise != null) {
        ExerciseGuideScreen(exercise, gender) { showsExerciseGuide = false }
        return
    }

    if (showsSetStyleInfo && exercise != null) {
        SetStyleInfoScreen(exercise.setStyle, effectiveSets, exercise.setStyleParameters) { showsSetStyleInfo = false }
        return
    }

    if (showsExitDialog) {
        AlertDialog(
            onDismissRequest = { showsExitDialog = false },
            title = { Text("¿SALIR DEL ENTRENAMIENTO?", fontFamily = AntonFontFamily) },
            text = { Text("Puedes guardar el progreso para continuar más tarde o descartar esta sesión.") },
            confirmButton = {
                TextButton(onClick = { showsExitDialog = false; onExit() }) { Text("GUARDAR Y SALIR", color = WildforceThemeTokens.accentGold) }
            },
            dismissButton = {
                TextButton(onClick = { WorkoutSessionStore.clear(context, workout.id); showsExitDialog = false; onExit() }) { Text("DESCARTAR", color = Color(0xFFC62828)) }
            },
        )
    }

    fun commitCompletedSession(progress: CompletionProgress) {
        WorkoutHistoryStore.record(context, workout, exerciseStats, completedSetRecords, feedbackByExercise, notesByExercise)
        CompletionProgressStore.commit(context, progress)
        WorkoutSessionStore.clear(context, workout.id)
        onFinish(elapsedSeconds, totalCompletedSets, totalVolumeKg, progress.streakAfter)
    }

    if (showsSummary) {
        val completionProgress = remember(workout.id) { CompletionProgressStore.preview(context, currentStreak) }
        val recordEvents = remember(workout.id, exerciseStats) { WorkoutCompletionCalculator.records(context, workout, exerciseStats) }
        WorkoutCompletionFlowScreen(workout, elapsedSeconds, exerciseStats, feedbackByExercise, recordEvents, completionProgress, useImperial = useImperial, isPlanCompleted = isPlanCompletedAfterWorkout, planName = planName, completedPlanWorkouts = completedPlanWorkouts, totalPlanWorkouts = totalPlanWorkouts, totalPlanExercises = totalPlanExercises, onGenerateNextPlan = {
            commitCompletedSession(completionProgress)
            onGenerateNextPlan()
        }, onCancelWorkout = {
            WorkoutSessionStore.clear(context, workout.id)
            onExit()
        }, onDone = { commitCompletedSession(completionProgress) })
        return
    }

    Box(Modifier.fillMaxSize().background(WildforceThemeTokens.backgroundSecondary)) {
        Crossfade(
            targetState = exerciseIndex,
            animationSpec = tween(durationMillis = 360),
            label = "exerciseHero",
        ) { visibleExerciseIndex ->
            val visibleExercise = workout.exercises.getOrNull(visibleExerciseIndex)
            RemoteTrainingImage(
                url = exerciseImageUrl(visibleExercise?.imageKey, gender),
                contentDescription = visibleExercise?.name,
                modifier = Modifier.fillMaxWidth().height(610.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(610.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent, WildforceThemeTokens.backgroundSecondary), startY = 0f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹ SALIR", Modifier.clickable { showsExitDialog = true }.padding(10.dp), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("RUTA", Modifier.clickable { showsWorkoutPath = true }.padding(8.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text("DATOS", Modifier.clickable { showsExerciseHistory = true }.padding(8.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text("SALTAR", Modifier.clickable {
                    val skipsWholeBlock = exercise?.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)
                    val nextIndex = if (skipsWholeBlock) {
                        currentPathBlock?.exercises?.flatMap { it.executionIndices }?.maxOrNull()?.plus(1)
                    } else {
                        exerciseIndex + 1
                    }
                    restRemaining = null
                    if (nextIndex == null || nextIndex > workout.exercises.lastIndex) showsSummary = true else exerciseIndex = nextIndex
                }.padding(8.dp), color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text(formatClock(elapsedSeconds), Modifier.liquidGlass(RoundedCornerShape(18.dp), emphasized = true).padding(horizontal = 14.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = if (workout.exercises.isEmpty()) 0f else (exerciseIndex + completedForExercise.toFloat() / effectiveSets.coerceAtLeast(1)) / workout.exercises.size,
                modifier = Modifier.fillMaxWidth().height(4.dp), color = Color.White,
                backgroundColor = Color.White.copy(alpha = 0.28f),
            )
            Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                Text("EJERCICIO ${logicalExerciseIndex + 1} DE ${logicalExercises.size}", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                Text(exercise?.name?.uppercase().orEmpty(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color.White)
                exercise?.takeIf { it.blockType != WorkoutBlockType.Standard }?.let { blockedExercise ->
                    val blockProgress = blockedExercise.blockLabel?.let { "$it · " }.orEmpty() +
                        if (blockedExercise.blockRounds > 1) "RONDA ${blockedExercise.blockRound}/${blockedExercise.blockRounds}" else blockedExercise.blockType.label.uppercase()
                    Text(blockProgress, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                }
                currentPathBlock?.notes?.let { note ->
                    Text(note, Modifier.fillMaxWidth().padding(top = 5.dp), style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.78f), maxLines = 3)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val contextLabel = exercise?.blockType?.takeIf { it != WorkoutBlockType.Standard }?.label?.uppercase() ?: "FUERZA"
                    Text(contextLabel, Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Text("CÓMO HACERLO  ›", Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).clickable { showsExerciseGuide = true }.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
                MuscleStrip(exercise?.imageKey, onDarkBackground = true, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            val activePanelHeight = when {
                restRemaining == null -> 438.dp
                restBetweenExercises -> 390.dp
                else -> 290.dp
            }
            Column(
                Modifier.fillMaxWidth().height(activePanelHeight).animateContentSize(animationSpec = tween(280))
                    .clip(RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                val resting = restRemaining
                if (pendingFeedback && exercise != null) {
                    ExerciseFeedbackContent(
                        exerciseName = exercise.name,
                        selected = selectedFeedback,
                        note = pendingNote,
                        onSelected = { selectedFeedback = it },
                        onNoteChanged = { pendingNote = it },
                        onContinue = {
                            selectedFeedback?.let { feedbackByExercise = feedbackByExercise + (exerciseIndex to it) }
                            pendingNote.trim().takeIf(String::isNotEmpty)?.let { notesByExercise = notesByExercise + (exerciseIndex to it) }
                            pendingFeedback = false
                            selectedFeedback = null
                            pendingNote = ""
                            advanceFromExercise(exercise.restSeconds)
                        },
                    )
                } else if (resting != null) {
                    RestTimerContent(
                        seconds = resting,
                        totalSeconds = restInitialSeconds,
                        betweenExercises = restBetweenExercises,
                        nextExercise = exercise,
                        gender = gender,
                        onAddTime = {
                            restInitialSeconds += 30
                            restRemaining = resting + 30
                        },
                        onSkip = { restRemaining = null; restBetweenExercises = false },
                    )
                } else if (exercise != null) {
                    val isWarmupOrCooldown = exercise.blockType == WorkoutBlockType.Warmup || exercise.blockType == WorkoutBlockType.Cooldown
                    if (isWarmupOrCooldown) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(if (exercise.blockType == WorkoutBlockType.Warmup) "PREPÁRATE PARA ENTRENAR" else "RECUPERA Y BAJA PULSACIONES", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("Completa el movimiento con control y sin buscar fatiga.", Modifier.padding(top = 6.dp, bottom = 16.dp), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            if (targetDurationSeconds(exercise.reps) != null) {
                                ExerciseWorkTimer(
                                    seconds = exerciseTimeRemaining ?: exerciseTimeInitial,
                                    initialSeconds = exerciseTimeInitial,
                                    running = exerciseTimerRunning,
                                    onToggle = { exerciseTimerRunning = !exerciseTimerRunning },
                                    onAddTime = {
                                        exerciseTimeRemaining = (exerciseTimeRemaining ?: 0) + 15
                                        exerciseTimeInitial += 15
                                    },
                                )
                            } else {
                                Text(exercise.reps, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h2, color = WildforceThemeTokens.accentGold)
                                Text("REPETICIONES OBJETIVO", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    } else {
                        if (exercise.blockType == WorkoutBlockType.Superset && currentPathBlock != null) {
                            SupersetRoundMap(currentPathBlock, exerciseIndex, exercise.blockRound, gender)
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("SERIES", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                                Text("Objetivo ${exercise.reps} reps · ${exercise.restSeconds}s descanso", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                                Text("${exercise.setStyle.glyph}  ${exercise.setStyle.label}  ›", Modifier.clickable { showsSetStyleInfo = true }.padding(vertical = 4.dp), color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                                Text(styleParameterLabels(exercise.setStyle, exercise.setStyleParameters).joinToString(" · "), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${completedForExercise + 1}/$effectiveSets", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                                if (exercise.blockType == WorkoutBlockType.Standard && targetDurationSeconds(exercise.reps) == null && effectiveSets < 20) {
                                    Text("＋ AÑADIR SERIE", Modifier.clickable { addedSetsByExercise = addedSetsByExercise + (exerciseIndex to ((addedSetsByExercise[exerciseIndex] ?: 0) + 1)) }.padding(top = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                            SetTrackingRows(exerciseIndex, effectiveExercise ?: exercise, completedForExercise, completedSetRecords, useImperial) { original, changed ->
                                val updatedRecords = completedSetRecords.map { if (it.exerciseIndex == exerciseIndex && it.setNumber == original.setNumber) changed else it }
                                totalVolumeKg += changed.reps * changed.weightKg - original.reps * original.weightKg
                                completedSetRecords = updatedRecords
                                exerciseStats = exerciseStats + (exerciseIndex to statsForExercise(updatedRecords, exerciseIndex))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (targetDurationSeconds(exercise.reps) != null) {
                            ExerciseWorkTimer(
                                seconds = exerciseTimeRemaining ?: exerciseTimeInitial,
                                initialSeconds = exerciseTimeInitial,
                                running = exerciseTimerRunning,
                                onToggle = { exerciseTimerRunning = !exerciseTimerRunning },
                                onAddTime = {
                                    exerciseTimeRemaining = (exerciseTimeRemaining ?: 0) + 15
                                    exerciseTimeInitial += 15
                                },
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactMetricStepper("REPS", reps.toString(), { reps = (reps - 1).coerceAtLeast(0) }, { reps++ }, Modifier.weight(1f), onValueEntered = { value -> reps = value.toIntOrNull()?.coerceIn(0, 999) ?: reps })
                                CompactMetricStepper("PESO", formatTrainingWeight(weightKg, useImperial), { weightKg = (weightKg - if (useImperial) 5.0 / KG_TO_LB else 2.5).coerceAtLeast(0.0) }, { weightKg += if (useImperial) 5.0 / KG_TO_LB else 2.5 }, Modifier.weight(1f), inputValue = if (useImperial) "%.1f".format(Locale.US, weightKg * KG_TO_LB) else weightKg.toString(), decimalInput = true, onValueEntered = { value -> weightKg = (value.replace(',', '.').toDoubleOrNull()?.let { if (useImperial) it / KG_TO_LB else it })?.coerceIn(0.0, 750.0) ?: weightKg })
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val newCompleted = if (isWarmupOrCooldown) effectiveSets else completedForExercise + 1
                            val timedDuration = targetDurationSeconds(exercise.reps)
                            val loggedReps = timedDuration?.let { (exerciseTimeInitial - (exerciseTimeRemaining ?: 0)).coerceAtLeast(0) } ?: reps
                            val loggedWeight = if (timedDuration != null) 0.0 else weightKg
                            completedByExercise = completedByExercise + (exerciseIndex to newCompleted)
                            totalCompletedSets++; totalVolumeKg += loggedReps * loggedWeight
                            completedSetRecords = completedSetRecords + CompletedSetRecord(exerciseIndex, if (isWarmupOrCooldown) 1 else newCompleted, loggedReps, loggedWeight, setStyle = exercise.setStyle)
                            val previousStats = exerciseStats[exerciseIndex] ?: ExerciseSessionStats()
                            exerciseStats = exerciseStats + (exerciseIndex to previousStats.copy(
                                sets = previousStats.sets + 1,
                                totalReps = previousStats.totalReps + loggedReps,
                                maxWeightKg = maxOf(previousStats.maxWeightKg, loggedWeight),
                                volumeKg = previousStats.volumeKg + loggedReps * loggedWeight,
                            ))
                            if (newCompleted < effectiveSets) {
                                restInitialSeconds = exercise.restSeconds.coerceAtLeast(1)
                            exerciseTimerRunning = false
                            if (timedDuration != null) {
                                exerciseTimeRemaining = timedDuration; exerciseTimeInitial = timedDuration
                            }
                                restBetweenExercises = false
                                restRemaining = if (skipRestPeriods) null else exercise.restSeconds
                            } else {
                                val needsFeedback = when (exercise.blockType) {
                                    WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown -> false
                                    WorkoutBlockType.Superset -> exercise.blockRound == exercise.blockRounds
                                    WorkoutBlockType.Standard -> true
                                }
                                if (needsFeedback) {
                                    pendingFeedback = true
                                } else {
                                    val transitionRest = if (exercise.isLastInBlock) exercise.restAfterBlockSeconds ?: 0 else exercise.restSeconds
                                    advanceFromExercise(transitionRest)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp),
                    ) { Text(if (isWarmupOrCooldown || completedForExercise + 1 == effectiveSets) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE", fontWeight = FontWeight.Bold) }
                    workout.exercises.getOrNull(exerciseIndex + 1)?.let { Text("Siguiente ejercicio: ${it.name}", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            }
        }
    }
}

@Composable
private fun SupersetRoundMap(block: WorkoutPathBlock, currentExerciseIndex: Int, currentRound: Int, gender: String) {
    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.accentGold.copy(alpha = 0.09f), RoundedCornerShape(18.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⛓  SUPERSET", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption)
            Spacer(Modifier.weight(1f))
            Text("RONDA $currentRound/${block.rounds}", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption)
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            block.exercises.forEach { item ->
                val roundIndex = item.executionIndices.getOrNull((currentRound - 1).coerceAtLeast(0))
                val current = roundIndex == currentExerciseIndex
                val completed = roundIndex != null && roundIndex < currentExerciseIndex
                Row(
                    Modifier.weight(1f).background(if (current) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(12.dp)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteTrainingImage(exerciseImageUrl(item.exercise.imageKey, gender), item.exercise.name, Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)))
                    Column(Modifier.weight(1f).padding(start = 7.dp)) {
                        Text(item.label.orEmpty(), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption)
                        Text(item.exercise.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (completed) WildforceThemeTokens.textSecondary else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption)
                    }
                    Text(if (completed) "✓" else if (current) "●" else "·", color = if (current) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun SetTrackingRows(
    exerciseIndex: Int,
    exercise: ExerciseSummary,
    completedSets: Int,
    records: List<CompletedSetRecord>,
    useImperial: Boolean,
    onRecordChanged: (CompletedSetRecord, CompletedSetRecord) -> Unit,
) {
    var editingSetNumber by remember(exerciseIndex) { mutableStateOf<Int?>(null) }
    var editReps by remember(exerciseIndex) { mutableStateOf(0) }
    var editWeightKg by remember(exerciseIndex) { mutableStateOf(0.0) }
    repeat(exercise.sets) { setIndex ->
        val setNumber = setIndex + 1
        val completed = setIndex < completedSets
        val current = setIndex == completedSets
        val record = records.lastOrNull { it.exerciseIndex == exerciseIndex && it.setNumber == setNumber }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                .background(if (current) WildforceThemeTokens.accentGold.copy(alpha = 0.13f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.055f), RoundedCornerShape(14.dp))
                .clickable(enabled = record != null) {
                    record?.let { editingSetNumber = setNumber; editReps = it.reps; editWeightKg = it.weightKg }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (completed) "✓" else "$setNumber", color = if (completed) Color(0xFF26A269) else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("${setStyleInstruction(exercise.setStyle, setNumber, exercise.sets)} · ${if (current) "ACTUAL" else if (completed) "COMPLETADA" else "PENDIENTE"}", color = if (exercise.setStyle == ExerciseSetStyle.Straight) WildforceThemeTokens.textSecondary else WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                if (record != null) Text(if (targetDurationSeconds(exercise.reps) != null) "Objetivo ${exercise.reps} → ${record.reps}s" else "Objetivo ${exercise.reps} → ${record.reps} reps", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
            }
            Text(
                if (record != null && targetDurationSeconds(exercise.reps) != null) "${record.reps}s" else if (record != null) formatTrainingWeight(record.weightKg, useImperial) else exercise.reps,
                color = WildforceThemeTokens.textPrimary, fontWeight = if (record != null) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (editingSetNumber == setNumber && record != null) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 7.dp).background(WildforceThemeTokens.accentGold.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(10.dp)) {
                Text("CORREGIR SERIE $setNumber", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactMetricStepper("REPS", editReps.toString(), { editReps = (editReps - 1).coerceAtLeast(0) }, { editReps++ }, Modifier.weight(1f), onValueEntered = { value -> editReps = value.toIntOrNull()?.coerceIn(0, 999) ?: editReps })
                    CompactMetricStepper("PESO", formatTrainingWeight(editWeightKg, useImperial), { editWeightKg = (editWeightKg - if (useImperial) 5.0 / KG_TO_LB else 2.5).coerceAtLeast(0.0) }, { editWeightKg += if (useImperial) 5.0 / KG_TO_LB else 2.5 }, Modifier.weight(1f), inputValue = if (useImperial) "%.1f".format(Locale.US, editWeightKg * KG_TO_LB) else editWeightKg.toString(), decimalInput = true, onValueEntered = { value -> editWeightKg = (value.replace(',', '.').toDoubleOrNull()?.let { if (useImperial) it / KG_TO_LB else it })?.coerceIn(0.0, 750.0) ?: editWeightKg })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("CANCELAR", Modifier.clickable { editingSetNumber = null }.padding(10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                    Text("GUARDAR", Modifier.clickable {
                        onRecordChanged(record, record.copy(reps = editReps, weightKg = editWeightKg))
                        editingSetNumber = null
                    }.padding(10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
private fun ExerciseFeedbackContent(
    exerciseName: String,
    selected: String?,
    note: String,
    onSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val choices = listOf(
        Triple("🥱", "MUY FÁCIL", "Podías haber hecho muchas más repeticiones."),
        Triple("😌", "FÁCIL", "Te quedaban varias repeticiones en reserva."),
        Triple("💪", "JUSTO", "El esfuerzo y la técnica han sido adecuados."),
        Triple("🥵", "DIFÍCIL", "Has terminado cerca de tu límite."),
        Triple("🤯", "MUY DIFÍCIL", "No habrías podido completar otra repetición."),
    )
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("EJERCICIO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(exerciseName, color = WildforceThemeTokens.textSecondary)
        Text("¿CÓMO HA IDO?", Modifier.fillMaxWidth().padding(top = 12.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("Esta información ayudará a personalizar las siguientes sesiones.", Modifier.fillMaxWidth().padding(top = 2.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            choices.forEach { (emoji, title, _) ->
                Column(
                    Modifier.weight(1f).height(70.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (selected == title) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f))
                        .clickable { onSelected(title) }.padding(horizontal = 2.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(emoji, style = MaterialTheme.typography.h6)
                    Text(title, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = if (selected == title) Color.White else WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
        val description = choices.firstOrNull { it.second == selected }?.third ?: "Selecciona la sensación que mejor describa el ejercicio."
        Text(description, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        TextField(
            value = note,
            onValueChange = { onNoteChanged(it.take(300)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Añadir notas…") },
            maxLines = 3,
            colors = TextFieldDefaults.textFieldColors(backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            shape = RoundedCornerShape(13.dp),
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onContinue, enabled = selected != null, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text("MARCAR COMO COMPLETADO", fontWeight = FontWeight.Bold)
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
    val animatedProgress by animateFloatAsState((seconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f), animationSpec = tween(1_000, easing = LinearEasing))
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (betweenExercises) "DESCANSO ANTES DEL SIGUIENTE EJERCICIO" else "DESCANSO ANTES DE LA SIGUIENTE SERIE",
            fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RestActionButton("+", "+30s", primary = false, onClick = onAddTime)
            Box(Modifier.padding(horizontal = 8.dp).size(132.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = animatedProgress,
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
            Text("A CONTINUACIÓN", Modifier.fillMaxWidth().padding(start = 10.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteTrainingImage(exerciseImageUrl(nextExercise.imageKey, gender), null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(nextExercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${nextExercise.sets} series · ${nextExercise.reps} reps", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = if (primary) Color.White else WildforceThemeTokens.textPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}
@Composable
private fun CompactMetricStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, inputValue: String = value, decimalInput: Boolean = false, onValueEntered: ((String) -> Unit)? = null) {
    var showManualInput by remember { mutableStateOf(false) }
    var manualInput by remember(inputValue) { mutableStateOf(inputValue) }
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("−", Modifier.size(32.dp).clickable(onClick = onMinus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h6)
            Text(value, Modifier.clickable(enabled = onValueEntered != null) { manualInput = inputValue; showManualInput = true }.padding(horizontal = 3.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            Text("+", Modifier.size(32.dp).clickable(onClick = onPlus), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.h6)
        }
    }
    if (showManualInput && onValueEntered != null) {
        AlertDialog(
            onDismissRequest = { showManualInput = false },
            title = { Text("EDITAR $title", fontFamily = AntonFontFamily) },
            text = { TextField(manualInput, { value -> manualInput = value.filter { it.isDigit() || (decimalInput && (it == ',' || it == '.')) }.take(8) }, label = { Text(if (decimalInput) "Valor" else "Repeticiones") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (decimalInput) KeyboardType.Decimal else KeyboardType.Number)) },
            confirmButton = { TextButton(onClick = { onValueEntered(manualInput); showManualInput = false }) { Text("GUARDAR", color = WildforceThemeTokens.accentGold) } },
            dismissButton = { TextButton(onClick = { showManualInput = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
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

private fun targetReps(value: String?): Int = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull() ?: 10
private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
internal const val KG_TO_LB = 2.20462262
internal fun formatTrainingWeight(kilograms: Double, useImperial: Boolean): String = if (useImperial) String.format(Locale.getDefault(), "%.1f lb", kilograms * KG_TO_LB) else String.format(Locale.getDefault(), "%.1f kg", kilograms)


internal fun setStyleInstruction(style: ExerciseSetStyle, setNumber: Int, totalSets: Int): String = when (style) {
    ExerciseSetStyle.Warmup -> "APROXIMACIÓN"
    ExerciseSetStyle.Straight -> "SERIE NORMAL"
    ExerciseSetStyle.TopSetBackoff -> if (setNumber == 1) "TOP SET" else "BACKOFF"
    ExerciseSetStyle.AscendingPyramid -> if (setNumber == totalSets) "PESO MÁXIMO" else "SUBE EL PESO"
    ExerciseSetStyle.DropSet -> if (setNumber == totalSets) "DROP SET · SIN DESCANSO" else "SERIE BASE"
    ExerciseSetStyle.RestPause -> if (setNumber == totalSets) "REST-PAUSE · PAUSA BREVE" else "SERIE BASE"
    ExerciseSetStyle.Intervals -> "INTERVALO"
    ExerciseSetStyle.Tempo -> "TEMPO CONTROLADO"
}
private fun statsForExercise(records: List<CompletedSetRecord>, exerciseIndex: Int): ExerciseSessionStats {
    val exerciseRecords = records.filter { it.exerciseIndex == exerciseIndex }
    return ExerciseSessionStats(exerciseRecords.size, exerciseRecords.sumOf { it.reps }, exerciseRecords.maxOfOrNull { it.weightKg } ?: 0.0, exerciseRecords.sumOf { it.reps * it.weightKg })
}
