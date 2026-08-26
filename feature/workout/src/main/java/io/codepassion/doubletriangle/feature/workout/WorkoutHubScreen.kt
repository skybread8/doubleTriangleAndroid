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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
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
                onAdapt = onWorkoutSelected,
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
    val locale = LocalLocale.current.platformLocale
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
                    day.getDisplayName(TextStyle.SHORT, locale).take(2),
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
                    .background(if (selected == mode) WildforceThemeTokens.textPrimary.copy(alpha = 0.12f) else Color.Transparent)
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

private enum class WorkoutDetailTab(val label: String) {
    Exercises("EJERCICIOS"),
    Information("INFORMACIÓN"),
}

private enum class ExercisePickerMode { Add, Swap }

@Preview(showBackground = true)
@Composable
private fun WorkoutHubPreview() = WildforceTheme { WorkoutHubScreen(PaddingValues()) }

@Composable
fun WorkoutDetailScreen(workout: WorkoutDaySummary, gender: String, useImperial: Boolean = false, onBack: () -> Unit, onStart: () -> Unit, onSkip: () -> Unit = {}, onUnskip: () -> Unit = {}, onWorkoutUpdated: (WorkoutDaySummary) -> Unit = {}, defaultAdaptEquipment: String = "Peso corporal", adaptEquipmentPresets: List<Pair<String, String>> = emptyList(), adaptAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null) {
    val detailHeroHeight = (LocalConfiguration.current.screenHeightDp * 0.43f).dp.coerceIn(330.dp, 430.dp)
    var expandedExercise by remember { mutableStateOf<Int?>(null) }
    var collapsedBlocks by remember(workout.id) { mutableStateOf(emptySet<Int>()) }
    val detailContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var hasSavedSession by remember(workout.id) { mutableStateOf(WorkoutSessionStore.load(detailContext, workout.id) != null) }
    var guideExercise by remember { mutableStateOf<ExerciseSummary?>(null) }
    var startingWorkout by remember(workout.id) { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showingEditor by remember(workout.id) { mutableStateOf(false) }
    var showingAdaptation by remember(workout.id) { mutableStateOf(false) }
    var adapting by remember(workout.id) { mutableStateOf(false) }
    var adaptationError by remember(workout.id) { mutableStateOf<String?>(null) }
    var pendingAdapted by remember(workout.id) { mutableStateOf<WorkoutDaySummary?>(null) }
    var lastAdaptRequest by remember(workout.id) { mutableStateOf<CustomWorkoutRequest?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showSkipConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember(workout.id) { mutableStateOf(false) }
    var skipped by remember(workout.id) { mutableStateOf(workout.status == WorkoutStatus.Skipped) }
    var selectedTab by remember(workout.id) { mutableStateOf(WorkoutDetailTab.Exercises) }
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
            modifier = Modifier.fillMaxWidth().height(detailHeroHeight),
        )
        Box(Modifier.fillMaxWidth().height(detailHeroHeight).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, WildforceThemeTokens.backgroundSecondary))))
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CenteredBackButton(onBack, 52.dp, 14.dp)
                Spacer(Modifier.weight(1f))
                Box {
                    Box(
                        Modifier.size(48.dp).clickable { showMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.24f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "Menú del entrenamiento", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        val canModify = workout.status == WorkoutStatus.Planned || hasSavedSession
                        if (canModify) {
                            DropdownMenuItem(onClick = { showMenu = false; showingEditor = true }) { Text("Añadir ejercicio") }
                            DropdownMenuItem(onClick = { showMenu = false; showSkipConfirmation = true }) { Text("Saltar entrenamiento") }
                            if (adaptAiGenerator != null) DropdownMenuItem(onClick = { showMenu = false; showingAdaptation = true }) { Text("Adaptar a otra ubicación") }
                        }
                        if (hasSavedSession) {
                            Divider()
                            DropdownMenuItem(onClick = { showMenu = false; showCancelConfirmation = true }) { Text("Cancelar entrenamiento", color = Color(0xFFC62828)) }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
                Text("DÍA ${workout.order}", fontFamily = AntonFontFamily, fontSize = 50.sp, color = Color.White, maxLines = 1)
                Text(workout.title.uppercase(), fontFamily = AntonFontFamily, fontSize = 34.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("◎ ${workout.focus}   ◆ ${workout.dayType}   ◷ ${workout.estimatedMinutes} min", Modifier.padding(top = 8.dp), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(110.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(22.dp)) {
                Button(onClick = { if (!startingWorkout) { startingWorkout = true; onStart() } }, enabled = !startingWorkout, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp)) {
                    Text(if (startingWorkout) "ABRIENDO ENTRENAMIENTO…" else if (hasSavedSession) "REANUDAR ENTRENAMIENTO" else "EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(22.dp))
                WorkoutDetailTabSelector(selected = selectedTab, onSelected = { selectedTab = it })
                Spacer(Modifier.height(18.dp))
                if (selectedTab == WorkoutDetailTab.Information) {
                    WorkoutInformationTab(workout)
                } else {
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
                                Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = if (block.type == WorkoutBlockType.Superset) 8.dp else 0.dp)
                                    .clickable { expandedExercise = if (expandedExercise == itemKey) null else itemKey }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (block.type == WorkoutBlockType.Superset) {
                                    Text("A${exerciseIndex + 1}", Modifier.padding(end = 9.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                                }
                                RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), null, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
                                Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                                    Text(exercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                    val setText = if (block.type == WorkoutBlockType.Superset) "${block.rounds} rondas" else "${exercise.sets} series"
                                    val targetWeightText = exercise.targetWeightKg?.let { formatTrainingWeight(it, useImperial) } ?: "—"
                                    Text("▦ $setText   # ${exercise.reps} reps   ⚖ $targetWeightText", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("= ${exercise.setStyle.label}${if (exercise.restSeconds > 0) " · ${exercise.restSeconds}s descanso" else ""}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("•••", color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                            }
                            if (expandedExercise == itemKey) {
                                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                                    Text("MÚSCULOS IMPLICADOS", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                                    MuscleStrip(exercise.imageKey, onDarkBackground = false, modifier = Modifier.padding(vertical = 5.dp))
                                    Text("Objetivo: ${exercise.reps} repeticiones${if (exercise.restSeconds > 0) " con ${exercise.restSeconds}s de recuperación" else ""}.", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                                    Text("CÓMO HACERLO  →", Modifier.padding(top = 10.dp).clickable { guideExercise = exercise }, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                                }
                            }
                            if (exerciseIndex < block.exercises.lastIndex && expandedExercise != itemKey) {
                                Box(Modifier.fillMaxWidth().padding(start = 82.dp, end = 12.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.14f)))
                            }
                        }
                        block.restAfterBlockSeconds?.let { rest ->
                            Text("◷  Descansa ${rest}s al terminar el bloque", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("¿CANCELAR ENTRENAMIENTO?", fontFamily = AntonFontFamily) },
            text = { Text("Se eliminará el progreso guardado de esta sesión. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    WorkoutSessionStore.clear(detailContext, workout.id)
                    hasSavedSession = false
                    showCancelConfirmation = false
                }) { Text("CANCELAR ENTRENAMIENTO", color = Color(0xFFC62828)) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirmation = false }) { Text("SEGUIR", color = WildforceThemeTokens.textSecondary) } },
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
private fun PerformanceContextChip(label: String, value: String) {
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.88f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (label == "PR") Icons.Filled.EmojiEvents else Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = if (label == "PR") Color(0xFFFFC107) else WildforceThemeTokens.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text("$label: $value", Modifier.padding(start = 6.dp), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ActiveWorkoutMenuItem(glyph: String, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Text(label, color = if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary)
        }
    }
}

@Composable
private fun WorkoutDetailTabSelector(selected: WorkoutDetailTab, onSelected: (WorkoutDetailTab) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.12f)).padding(4.dp)) {
        WorkoutDetailTab.entries.forEach { tab ->
            val active = tab == selected
            Text(
                tab.label,
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) WildforceThemeTokens.textPrimary else Color.Transparent)
                    .clickable { onSelected(tab) }.padding(vertical = 11.dp),
                color = if (active) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.caption,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WorkoutInformationTab(workout: WorkoutDaySummary) {
    val muscles = workout.exercises.flatMap { ExerciseVisualCatalog.metadata(it.imageKey)?.primary.orEmpty() }
        .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    val equipment = workout.exercises.flatMap { ExerciseVisualCatalog.equipmentFor(it.imageKey) }.distinct()
    Text("MÚSCULOS OBJETIVO", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        muscles.take(3).forEach { (muscle, frequency) ->
            Column(Modifier.weight(1f).liquidGlass(RoundedCornerShape(16.dp)).padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(muscle.drawable),
                    contentDescription = muscle.label,
                    modifier = Modifier.size(58.dp),
                )
                Text(muscle.label, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$frequency ejercicios", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Text("EQUIPAMIENTO NECESARIO", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        equipment.forEach { item ->
            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("◈", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                Text(item, Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Text("ESTRUCTURA DE LA SESIÓN", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        workout.displayBlocks().forEach { block ->
            val count = if (block.type == WorkoutBlockType.Superset) "${block.rounds} rondas · ${block.exercises.size} ejercicios" else "${block.exercises.size} ejercicios"
            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(blockIcon(block.type), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(block.type.label, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                    Text(count, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                }
            }
        }
    }
    workout.displayBlocks().mapNotNull { it.notes }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { notes ->
        Spacer(Modifier.height(24.dp))
        Text("NOTAS", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
        Text(notes.joinToString("\n"), Modifier.padding(top = 9.dp), color = WildforceThemeTokens.textSecondary)
    }
    Spacer(Modifier.height(18.dp))
}

private fun blockIcon(type: WorkoutBlockType): String = when (type) {
    WorkoutBlockType.Warmup -> "◌"
    WorkoutBlockType.Standard -> "≡"
    WorkoutBlockType.Superset -> "⛓"
    WorkoutBlockType.Cooldown -> "↓"
}
@Composable
fun ActiveWorkoutScreen(
    initialWorkout: WorkoutDaySummary,
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
    onWorkoutChanged: (WorkoutDaySummary) -> Unit = {},
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double, streak: Int) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var workout by remember(initialWorkout.id) { mutableStateOf(initialWorkout) }
    LaunchedEffect(workout) { onWorkoutChanged(workout) }
    DisposableEffect(workout.id) {
        val notificationsEnabled = WorkoutNotificationPreferences.enabled(context)
        val serviceIntent = android.content.Intent(context, WorkoutForegroundService::class.java).putExtra("title", workout.title).putExtra("detail", "Sesión activa").putExtra("workoutId", workout.id).putExtra("totalExercises", workout.exercises.size)
        if (notificationsEnabled) {
            runCatching { WorkoutActiveNotification.show(context, workout.title) }
            // Notification updates are handled safely in-process. Starting a
            // foreground service here can be rejected asynchronously by some
            // Android versions and crash the app when a set is completed.
        }
        onDispose { WorkoutActiveNotification.cancel(context); context.stopService(serviceIntent) }
    }
    val restored = remember(workout.id) { WorkoutSessionStore.load(context, workout.id) }
    var exerciseIndex by remember(workout.id) { mutableStateOf((restored?.exerciseIndex ?: 0).coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))) }
    var completedByExercise by remember(workout.id) { mutableStateOf(restored?.completedByExercise ?: emptyMap()) }
    var reps by remember(workout.id) { mutableStateOf(restored?.reps ?: targetReps(workout.exercises.firstOrNull()?.reps)) }
    var weightKg by remember(workout.id) { mutableStateOf(restored?.weightKg ?: workout.exercises.firstOrNull()?.targetWeightKg ?: 0.0) }
    var restRemaining by remember(workout.id) { mutableStateOf(restored?.restRemaining) }
    var restTimerPaused by remember(workout.id) { mutableStateOf(false) }
    var restEndsAtMillis by remember(workout.id) { mutableStateOf(restored?.restRemaining?.let { System.currentTimeMillis() + it * 1_000L }) }
    var restInitialSeconds by remember(workout.id) { mutableStateOf(restored?.restInitialSeconds ?: 1) }
    var restBetweenExercises by remember(workout.id) { mutableStateOf(restored?.restBetweenExercises ?: false) }
    var elapsedSeconds by remember(workout.id) { mutableStateOf(restored?.elapsedSeconds ?: 0) }
    var totalCompletedSets by remember(workout.id) { mutableStateOf(restored?.totalCompletedSets ?: 0) }
    var totalVolumeKg by remember(workout.id) { mutableStateOf(restored?.totalVolumeKg ?: 0.0) }
    var showsSummary by remember(workout.id) { mutableStateOf(restored?.showsSummary ?: false) }
    var completionCommitted by remember(workout.id) { mutableStateOf(false) }
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
    var showsSkipExerciseConfirmation by remember { mutableStateOf(false) }
    var showsExerciseMenu by remember { mutableStateOf(false) }
    var exercisePickerMode by remember { mutableStateOf<ExercisePickerMode?>(null) }
    var initializedExerciseIndex by remember(workout.id) { mutableStateOf(restored?.exerciseIndex ?: -1) }
    BackHandler { showsExitDialog = true }
    val exercise = workout.exercises.getOrNull(exerciseIndex)
    val exerciseHistory = remember(exercise?.historyKey()) {
        exercise?.let { WorkoutHistoryStore.history(context, it) }.orEmpty()
    }
    val lastRecordedWeight = exerciseHistory.firstOrNull()?.maxWeightKg ?: 0.0
    val personalBestWeight = exerciseHistory.maxOfOrNull { it.maxWeightKg } ?: 0.0
    val completedForExercise = completedByExercise[exerciseIndex] ?: 0
    val effectiveSets = (exercise?.sets ?: 1) + (addedSetsByExercise[exerciseIndex] ?: 0)
    // More sets require more vertical room below the hero. Scale the hero
    // down only for those sessions so the complete editor and CTA remain
    // visible without leaving a blank tail or forcing an initial scroll.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val heroFraction = when {
        effectiveSets >= 5 -> 0.24f
        effectiveSets >= 4 -> 0.29f
        else -> 0.46f
    }
    val heroMinimum = if (effectiveSets >= 5) 210.dp else if (effectiveSets >= 4) 235.dp else 340.dp
    val activeHeroHeight = (screenHeightDp * heroFraction).dp.coerceIn(heroMinimum, 430.dp)
    val effectiveExercise = exercise?.copy(sets = effectiveSets)
    val structuredPathBlocks = remember(workout) { workout.pathBlocks() }
    val logicalExercises = remember(structuredPathBlocks) { structuredPathBlocks.flatMap { it.exercises } }
    val logicalExerciseIndex = logicalExercises.indexOfFirst { exerciseIndex in it.executionIndices }.coerceAtLeast(0)
    val currentPathBlock = structuredPathBlocks.firstOrNull { block -> block.exercises.any { exerciseIndex in it.executionIndices } }
    fun rewriteExercises(transform: (MutableList<ExerciseSummary>, List<WorkoutBlockSummary>) -> Unit) {
        val sourceBlocks = workout.editableBlocks()
        val all = sourceBlocks.flatMap { it.exercises }.toMutableList()
        transform(all, sourceBlocks)
        var cursor = 0
        val updatedBlocks = sourceBlocks.map { block ->
            block.copy(exercises = block.exercises.map { all[cursor++] })
        }
        workout = workout.withEditableBlocks(updatedBlocks)
    }
    fun addOrReplaceExercise(choice: ExerciseChoice, replace: Boolean) {
        val exercise = ExerciseSummary(choice.name, choice.imageKey, 3, "10", 90)
        if (replace) {
            rewriteExercises { all, _ -> if (exerciseIndex in all.indices) all[exerciseIndex] = exercise }
        } else {
            val updated = workout.editableBlocks().toMutableList()
            updated += WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(exercise))
            workout = workout.withEditableBlocks(updated.workoutOrder())
        }
        exercisePickerMode = null
    }
    fun removeCurrentExercise() {
        val sourceBlocks = workout.editableBlocks()
        var cursor = 0
        val updatedBlocks = sourceBlocks.mapNotNull { block ->
            val exercises = block.exercises.filter {
                val keep = cursor != exerciseIndex
                cursor++
                keep
            }
            block.copy(exercises = exercises).takeIf { it.exercises.isNotEmpty() }
        }
        workout = workout.withEditableBlocks(updatedBlocks)
        exerciseIndex = exerciseIndex.coerceAtMost(workout.exercises.lastIndex.coerceAtLeast(0))
    }
    fun removeActiveSuperset() {
        val sourceBlocks = workout.editableBlocks()
        var cursor = 0
        val updatedBlocks = sourceBlocks.flatMap { block ->
            if (block.type != WorkoutBlockType.Superset || exerciseIndex !in cursor..(cursor + block.exercises.lastIndex)) {
                cursor += block.exercises.size
                listOf(block)
            } else {
                cursor += block.exercises.size
                block.exercises.map { item -> WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(item)) }
            }
        }
        workout = workout.withEditableBlocks(updatedBlocks)
    }
    fun updateActiveSuperset(roundsDelta: Int = 0, restDelta: Int = 0) {
        val sourceBlocks = workout.editableBlocks()
        var cursor = 0
        val updatedBlocks = sourceBlocks.map { block ->
            val containsCurrent = exerciseIndex in cursor..(cursor + block.exercises.lastIndex)
            cursor += block.exercises.size
            if (containsCurrent && block.type == WorkoutBlockType.Superset) {
                block.copy(rounds = (block.rounds + roundsDelta).coerceAtLeast(1), restAfterBlockSeconds = ((block.restAfterBlockSeconds ?: 0) + restDelta).coerceAtLeast(0))
            } else block
        }
        workout = workout.withEditableBlocks(updatedBlocks)
    }
    fun createActiveSuperset() {
        val sourceBlocks = workout.editableBlocks().toMutableList()
        var cursor = 0
        val blockIndex = sourceBlocks.indexOfFirst { block ->
            val found = exerciseIndex in cursor..(cursor + block.exercises.lastIndex)
            cursor += block.exercises.size
            found
        }
        if (blockIndex !in sourceBlocks.indices || blockIndex >= sourceBlocks.lastIndex) return
        val first = sourceBlocks[blockIndex]
        val second = sourceBlocks[blockIndex + 1]
        if (first.type != WorkoutBlockType.Standard || second.type != WorkoutBlockType.Standard) return
        sourceBlocks[blockIndex] = WorkoutBlockSummary(WorkoutBlockType.Superset, exercises = first.exercises + second.exercises, rounds = 1)
        sourceBlocks.removeAt(blockIndex + 1)
        workout = workout.withEditableBlocks(sourceBlocks)
    }
    var notificationArtwork by remember(workout.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(exercise?.imageKey, gender) {
        val url = exerciseImageUrl(exercise?.imageKey, gender)
        notificationArtwork = url?.let { loadTrainingBitmap(context, it) }
    }
    fun advanceFromExercise(restSeconds: Int) {
        if (exerciseIndex < workout.exercises.lastIndex) {
            exerciseIndex++
            if (!skipRestPeriods && restSeconds > 0) {
                restInitialSeconds = restSeconds
                restBetweenExercises = true
                restRemaining = restSeconds
                restTimerPaused = false
                restEndsAtMillis = System.currentTimeMillis() + restSeconds * 1_000L
            } else {
                restRemaining = null
            }
        } else {
            showsSummary = true
        }
    }

    var lastNotificationActionTimestamp by remember(workout.id) { mutableStateOf(WorkoutNotificationActionStore.read(context)?.second ?: 0L) }
    LaunchedEffect(workout.id) {
        while (true) {
            // En segundo plano el servicio foreground es quien consume los
            // botones de la notificación. Si Compose los leyera aquí primero,
            // "OMITIR" podría desaparecer sin actualizar el temporizador remoto.
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                WorkoutNotificationActionStore.read(context)?.let { (action, timestamp) ->
                    if (timestamp > lastNotificationActionTimestamp) {
                        lastNotificationActionTimestamp = timestamp
                        when (action) {
                    WorkoutActiveNotification.ACTION_TOGGLE_TIMER -> if (restRemaining != null) {
                        if (restTimerPaused) {
                            restEndsAtMillis = System.currentTimeMillis() + restRemaining!!.coerceAtLeast(0) * 1_000L
                            restTimerPaused = false
                        } else {
                            restRemaining = ((restEndsAtMillis ?: System.currentTimeMillis()) - System.currentTimeMillis()).div(1_000L).coerceAtLeast(0L).toInt()
                            restEndsAtMillis = null
                            restTimerPaused = true
                        }
                    } else exerciseTimerRunning = !exerciseTimerRunning
                    WorkoutActiveNotification.ACTION_SKIP_CURRENT -> if (restRemaining != null) { restRemaining = null; restEndsAtMillis = null; restBetweenExercises = false; restTimerPaused = false } else advanceFromExercise(exercise?.restSeconds ?: 0)
                    WorkoutActiveNotification.ACTION_ADD_REST -> if (restRemaining != null) {
                        val now = System.currentTimeMillis()
                        val current = restRemaining!!.coerceAtLeast(0)
                        restEndsAtMillis = if (restTimerPaused) now + (current + 30) * 1_000L
                        else (restEndsAtMillis ?: now) + 30_000L
                        restTimerPaused = false
                        restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                        restInitialSeconds += 30
                    }
                    }
                        WorkoutNotificationActionStore.clear(context)
                    }
                }
            }
            delay(250)
        }
    }

    DisposableEffect(lifecycleOwner, workout.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                context.getSharedPreferences("wildforce_notification_settings", 0).edit().putBoolean("app_foreground", true).apply()
                WorkoutActiveNotification.cancelRestFinished(context)
                WorkoutSessionStore.load(context, workout.id)?.let { latest ->
                    exerciseIndex = latest.exerciseIndex.coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))
                    completedByExercise = latest.completedByExercise
                    val resumedRest = latest.restRemaining?.takeIf { it > 0 }
                    restRemaining = resumedRest
                    restEndsAtMillis = resumedRest?.let { System.currentTimeMillis() + it * 1_000L }
                    restInitialSeconds = latest.restInitialSeconds
                    restBetweenExercises = latest.restBetweenExercises && resumedRest != null
                    elapsedSeconds = latest.elapsedSeconds
                    exerciseTimeRemaining = latest.exerciseTimeRemaining
                    exerciseTimerRunning = latest.exerciseTimerRunning
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                context.getSharedPreferences("wildforce_notification_settings", 0).edit().putBoolean("app_foreground", false).apply()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showsSummary) { while (!showsSummary) { delay(1_000); elapsedSeconds++ } }
    LaunchedEffect(exerciseIndex, completedByExercise, restRemaining, restInitialSeconds, restTimerPaused, restBetweenExercises, showsSummary, notificationArtwork) {
        if (!showsSummary) {
            val currentName = exercise?.name ?: "Entrenamiento"
            val completedExercises = workout.exercises.indices.count { index ->
                index < exerciseIndex || (completedByExercise[index] ?: 0) >= (workout.exercises[index].sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
            }
            val detail = if (restRemaining != null) {
                if (restTimerPaused) "Pausado" else "${restRemaining}s"
            } else {
                "$currentName · $completedForExercise/$effectiveSets series"
            }
            // Mientras la app está en segundo plano, el servicio foreground es el
            // único dueño de la notificación. Evita que Compose publique otra
            // versión del contador y produzca dos temporizadores visibles.
            val appIsForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (WorkoutNotificationPreferences.enabled(context) && appIsForeground) {
                runCatching {
                    WorkoutActiveNotification.show(
                        context,
                        workout.title,
                        detail,
                        headsUp = WorkoutNotificationPreferences.restAlertsEnabled(context) && restRemaining != null && restRemaining == restInitialSeconds,
                        progress = if (restRemaining != null) restRemaining!!.coerceIn(0, restInitialSeconds) else completedExercises,
                        progressMax = if (restRemaining != null) restInitialSeconds else workout.exercises.size.coerceAtLeast(1),
                        isResting = restRemaining != null,
                        artwork = notificationArtwork,
                        segmentedProgress = restRemaining == null,
                    )
                }
            }
        }
    }
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
        runCatching {
            val snapshot = WorkoutSessionSnapshot(
                exerciseIndex = exerciseIndex,
                completedByExercise = completedByExercise,
                reps = reps,
                weightKg = weightKg,
                restRemaining = restRemaining,
                restInitialSeconds = restInitialSeconds,
                restBetweenExercises = restBetweenExercises,
                elapsedSeconds = elapsedSeconds,
                totalCompletedSets = totalCompletedSets,
                totalVolumeKg = totalVolumeKg,
                pendingFeedback = pendingFeedback,
                showsSummary = showsSummary,
                selectedFeedback = selectedFeedback,
                pendingNote = pendingNote,
                exerciseStats = exerciseStats,
                feedbackByExercise = feedbackByExercise,
                notesByExercise = notesByExercise,
                completedSetRecords = completedSetRecords,
                exerciseTimeRemaining = exerciseTimeRemaining,
                exerciseTimeInitial = exerciseTimeInitial,
                exerciseTimerRunning = exerciseTimerRunning,
                addedSetsByExercise = addedSetsByExercise,
            )
            WorkoutSessionStore.save(context, workout.id, snapshot)
            WorkoutSessionStore.saveLegacy(/*
                context, workout.id,
                WorkoutSessionSnapshot(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, pendingNote, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise),
            */)
        }
    }
    LaunchedEffect(restEndsAtMillis, restTimerPaused) {
        val end = restEndsAtMillis ?: return@LaunchedEffect
        if (restTimerPaused) return@LaunchedEffect
        while (true) {
            val remaining = ((end - System.currentTimeMillis()).coerceAtLeast(0L) / 1_000L).toInt()
            restRemaining = remaining
            if (remaining <= 0) break
            delay(250)
        }
        delay(800)
        restRemaining = null
        restEndsAtMillis = null
        restBetweenExercises = false
    }
    LaunchedEffect(exerciseTimerRunning, exerciseTimeRemaining) {
        val remaining = exerciseTimeRemaining ?: return@LaunchedEffect
        if (exerciseTimerRunning && remaining > 0) {
            delay(1_000); exerciseTimeRemaining = remaining - 1
        } else if (remaining <= 0) exerciseTimerRunning = false
    }

    if (exercisePickerMode != null) {
        ExercisePickerScreen(gender, onBack = { exercisePickerMode = null }) { choice ->
            addOrReplaceExercise(choice, exercisePickerMode == ExercisePickerMode.Swap)
        }
        return
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

    if (showsSkipExerciseConfirmation) {
        val skipsWholeBlock = exercise?.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)
        AlertDialog(
            onDismissRequest = { showsSkipExerciseConfirmation = false },
            title = { Text(if (skipsWholeBlock) "¿SALTAR BLOQUE?" else "¿SALTAR EJERCICIO?", fontFamily = AntonFontFamily) },
            text = { Text(if (skipsWholeBlock) "Se omitirá todo el bloque actual y pasarás al siguiente." else "Este ejercicio quedará sin completar y pasarás al siguiente.") },
            confirmButton = { TextButton(onClick = {
                showsSkipExerciseConfirmation = false
                val nextIndex = if (skipsWholeBlock) currentPathBlock?.exercises?.flatMap { it.executionIndices }?.maxOrNull()?.plus(1) else exerciseIndex + 1
                restRemaining = null
                if (nextIndex == null || nextIndex > workout.exercises.lastIndex) showsSummary = true else exerciseIndex = nextIndex
            }) { Text("SALTAR", color = WildforceThemeTokens.accentGold) } },
            dismissButton = { TextButton(onClick = { showsSkipExerciseConfirmation = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }

    fun commitCompletedSession(progress: CompletionProgress) {
        // Completion can be triggered twice by a fast tap during the animated
        // finish flow. Make it idempotent and keep persistence failures from
        // crashing the activity while returning to the home screen.
        if (completionCommitted) return
        completionCommitted = true
        runCatching {
            WorkoutHistoryStore.record(context, workout, exerciseStats, completedSetRecords, feedbackByExercise, notesByExercise)
            CompletionProgressStore.commit(context, progress)
            WorkoutSessionStore.clear(context, workout.id)
        }
        runCatching { onFinish(elapsedSeconds, totalCompletedSets, totalVolumeKg, progress.streakAfter) }
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

    Box(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        Crossfade(
            targetState = exerciseIndex,
            animationSpec = tween(durationMillis = 360),
            label = "exerciseHero",
        ) { visibleExerciseIndex ->
            val visibleExercise = workout.exercises.getOrNull(visibleExerciseIndex)
            RemoteTrainingImage(
                url = exerciseImageUrl(visibleExercise?.imageKey, gender),
                contentDescription = visibleExercise?.name,
                modifier = Modifier.fillMaxWidth().height(activeHeroHeight),
            )
        }
        Box(Modifier.fillMaxWidth().height(activeHeroHeight).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent, WildforceThemeTokens.backgroundSecondary), startY = 0f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CenteredBackButton({ showsExitDialog = true }, 40.dp)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.68f)
                            else Color.Black.copy(alpha = 0.46f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.46f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Timeline, "Ruta del entrenamiento", Modifier.size(44.dp).clickable { showsWorkoutPath = true }.padding(10.dp), tint = WildforceThemeTokens.textPrimary)
                    Icon(Icons.Filled.Insights, "Datos del ejercicio", Modifier.size(44.dp).clickable { showsExerciseHistory = true }.padding(10.dp), tint = WildforceThemeTokens.textPrimary)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = "Guía del ejercicio",
                        modifier = Modifier.size(44.dp).clickable { showsExerciseGuide = true }.padding(10.dp),
                        tint = WildforceThemeTokens.textPrimary,
                    )
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Opciones del ejercicio",
                        modifier = Modifier.size(44.dp).clickable { showsExerciseMenu = true }.padding(10.dp),
                        tint = WildforceThemeTokens.textPrimary,
                    )
                }
                DropdownMenu(expanded = showsExerciseMenu, onDismissRequest = { showsExerciseMenu = false }, modifier = Modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp))) {
                    ActiveWorkoutMenuItem("＋", "Añadir ejercicio") { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Add }
                    ActiveWorkoutMenuItem("↔", "Sustituir ejercicio") { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Swap }
                    ActiveWorkoutMenuItem("↑", "Mover arriba") { showsExerciseMenu = false; rewriteExercises { all, _ -> if (exerciseIndex > 0) { val item = all.removeAt(exerciseIndex); all.add(exerciseIndex - 1, item) } } }
                    ActiveWorkoutMenuItem("↓", "Mover abajo") { showsExerciseMenu = false; rewriteExercises { all, _ -> if (exerciseIndex < all.lastIndex) { val item = all.removeAt(exerciseIndex); all.add(exerciseIndex + 1, item) } } }
                    ActiveWorkoutMenuItem("›", "Saltar ejercicio") { showsExerciseMenu = false; showsSkipExerciseConfirmation = true }
                    if (currentPathBlock?.type == WorkoutBlockType.Standard && exerciseIndex < workout.exercises.lastIndex) {
                        Divider()
                        ActiveWorkoutMenuItem("⛓", "Crear superserie") { showsExerciseMenu = false; createActiveSuperset() }
                    }
                    if (currentPathBlock?.type == WorkoutBlockType.Superset) {
                        Divider()
                        ActiveWorkoutMenuItem("+", "Añadir ronda") { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = 1) }
                        ActiveWorkoutMenuItem("−", "Quitar ronda") { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = -1) }
                        ActiveWorkoutMenuItem("◷", "Añadir 15 s de descanso") { showsExerciseMenu = false; updateActiveSuperset(restDelta = 15) }
                        ActiveWorkoutMenuItem("◷", "Quitar 15 s de descanso") { showsExerciseMenu = false; updateActiveSuperset(restDelta = -15) }
                    }
                    Divider()
                    ActiveWorkoutMenuItem("ⓘ", "Ver información de la serie") { showsExerciseMenu = false; showsSetStyleInfo = true }
                    if (currentPathBlock?.type == WorkoutBlockType.Superset) {
                        ActiveWorkoutMenuItem("□", "Quitar superserie") { showsExerciseMenu = false; removeActiveSuperset() }
                    }
                    Divider()
                    ActiveWorkoutMenuItem("×", "Eliminar ejercicio", destructive = true) { showsExerciseMenu = false; removeCurrentExercise() }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                workout.exercises.forEachIndexed { index, item ->
                    val setsForItem = (item.sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
                    val completedSets = completedByExercise[index] ?: 0
                    val completed = index < exerciseIndex || completedSets >= setsForItem
                    val current = index == exerciseIndex
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(
                            when {
                                completed -> Color.White.copy(alpha = 0.88f)
                                current && completedSets > 0 -> Color.White.copy(alpha = 0.62f)
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                        ),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Text(exercise?.name.orEmpty(), fontFamily = Exo2FontFamily, fontSize = 30.sp, fontWeight = FontWeight.Normal, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val contextLabel = exercise?.blockType?.takeIf { it != WorkoutBlockType.Standard }?.label?.uppercase() ?: "STANDARD"
                    Text(contextLabel, Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
                MuscleStrip(exercise?.imageKey, onDarkBackground = true, modifier = Modifier.padding(top = 4.dp))
                run {
                    val referenceWeight = exercise?.targetWeightKg ?: weightKg
                    Column(Modifier.fillMaxWidth().offset(y = (-54).dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PerformanceContextChip("PR", formatTrainingWeight(if (personalBestWeight > 0.0) personalBestWeight else referenceWeight, useImperial))
                        PerformanceContextChip("ÚLTIMO", formatTrainingWeight(if (lastRecordedWeight > 0.0) lastRecordedWeight else referenceWeight, useImperial))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val timerMode = restRemaining != null || targetDurationSeconds(exercise?.reps) != null
            Column(
                Modifier.fillMaxWidth()
                    .then(if (timerMode || effectiveSets > 3) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                    .animateContentSize(animationSpec = tween(280))
                    .clip(RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .then(if (timerMode) Modifier else Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
                    .padding(horizontal = 12.dp, vertical = if (effectiveSets >= 4) 8.dp else 16.dp),
                // Keep the sheet content flush to its top edge. Available
                // space is handled by the responsive hero; never introduce an
                // artificial blank band above the repetitions card.
                verticalArrangement = Arrangement.Top,
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
                            val now = System.currentTimeMillis()
                            val current = resting.coerceAtLeast(0)
                            restEndsAtMillis = if (restTimerPaused) now + (current + 30) * 1_000L
                            else (restEndsAtMillis ?: now) + 30_000L
                            restTimerPaused = false
                            restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                        },
                        onSkip = {
                            restRemaining = null
                            restEndsAtMillis = null
                            restTimerPaused = false
                            restBetweenExercises = false
                            val completedExercises = workout.exercises.indices.count { index ->
                                index < exerciseIndex || (completedByExercise[index] ?: 0) >= (workout.exercises[index].sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
                            }
                            runCatching { WorkoutActiveNotification.show(
                                context,
                                workout.title,
                                "${exercise?.name ?: "Entrenamiento"} · ${completedExercises}/${workout.exercises.size} ejercicios",
                                progress = completedExercises,
                                progressMax = workout.exercises.size.coerceAtLeast(1),
                                isResting = false,
                                artwork = notificationArtwork,
                                segmentedProgress = true,
                            ) }
                        },
                    )
                } else if (exercise != null) {
                    val isWarmupOrCooldown = exercise.blockType == WorkoutBlockType.Warmup || exercise.blockType == WorkoutBlockType.Cooldown
                    if (isWarmupOrCooldown) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(WildforceThemeTokens.textPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(exercise.setStyle.glyph, color = WildforceThemeTokens.backgroundSecondary, fontFamily = Exo2FontFamily, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(exercise.setStyle.label, Modifier.padding(start = 12.dp), fontFamily = Exo2FontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                                Text("Todas las series", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2)
                            }
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.12f)).clickable { showsSetStyleInfo = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = "Información de la serie", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Text("RIR ${exercise.setStyleParameters.targetRir}", Modifier.padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 5.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(effectiveSets) { setIndex ->
                                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(when {
                                setIndex < completedForExercise -> WildforceThemeTokens.textPrimary
                                    setIndex == completedForExercise -> WildforceThemeTokens.textPrimary.copy(alpha = 0.92f)
                                    else -> WildforceThemeTokens.textSecondary.copy(alpha = 0.22f)
                                }))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
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
                            ActiveSetEditor(
                                targetReps = exercise.reps,
                                targetWeightKg = exercise.targetWeightKg ?: weightKg,
                                reps = reps,
                                weightKg = weightKg,
                                useImperial = useImperial,
                                onRepsChanged = { reps = it },
                                onWeightChanged = { weightKg = it },
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            SetTrackingRows(exerciseIndex, effectiveExercise ?: exercise, completedForExercise, completedSetRecords, useImperial, dense = effectiveSets >= 4) { original, changed ->
                                val updatedRecords = completedSetRecords.map { if (it.exerciseIndex == exerciseIndex && it.setNumber == original.setNumber) changed else it }
                                totalVolumeKg += changed.reps * changed.weightKg - original.reps * original.weightKg
                                completedSetRecords = updatedRecords
                                exerciseStats = exerciseStats + (exerciseIndex to statsForExercise(updatedRecords, exerciseIndex))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (exercise.blockType == WorkoutBlockType.Standard && targetDurationSeconds(exercise.reps) == null && effectiveSets < 20) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("＋ AÑADIR SERIE", Modifier.clickable { addedSetsByExercise = addedSetsByExercise + (exerciseIndex to ((addedSetsByExercise[exerciseIndex] ?: 0) + 1)) }.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            runCatching {
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
                                restTimerPaused = false
                                restRemaining = if (skipRestPeriods) null else exercise.restSeconds.coerceAtLeast(1)
                                restEndsAtMillis = if (skipRestPeriods) null else System.currentTimeMillis() + exercise.restSeconds.coerceAtLeast(1) * 1_000L
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
                            }.onFailure { error ->
                                // Keep a malformed/restored session from
                                // terminating the activity on any device.
                                android.util.Log.e("Workout", "Unable to complete exercise", error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(0.dp),
                    ) { Text(if (isWarmupOrCooldown || completedForExercise + 1 == effectiveSets) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE ${completedForExercise + 1}", fontWeight = FontWeight.Bold) }
                    workout.exercises.getOrNull(exerciseIndex + 1)?.let { Text("Siguiente ejercicio: ${it.name}", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                    Spacer(Modifier.height(24.dp))
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
    dense: Boolean = false,
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
        if (!current) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = if (dense) 2.dp else 4.dp)
                    .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(if (dense) 13.dp else 16.dp))
                    .clickable(enabled = record != null) {
                        record?.let { editingSetNumber = setNumber; editReps = it.reps; editWeightKg = it.weightKg }
                    }
                    .padding(horizontal = if (dense) 12.dp else 15.dp, vertical = if (dense) 8.dp else 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (completed) "✓" else setNumber.toString(), Modifier.width(34.dp), color = if (completed) WildforceThemeTokens.accentGold else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reps", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(if (record != null) record.reps.toString() else exercise.reps, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.weight(1.25f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Peso", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(record?.let { formatTrainingWeight(it.weightKg, useImperial) } ?: exercise.targetWeightKg?.let { formatTrainingWeight(it, useImperial) }.orEmpty().ifBlank { "—" }, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (!current && editingSetNumber == setNumber && record != null) {
            Column(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
                Text("CORREGIR SERIE $setNumber", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                ActiveSetEditor(
                    targetReps = exercise.reps,
                    targetWeightKg = exercise.targetWeightKg ?: editWeightKg,
                    reps = editReps,
                    weightKg = editWeightKg,
                    useImperial = useImperial,
                    onRepsChanged = { editReps = it },
                    onWeightChanged = { editWeightKg = it },
                )
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
private fun ActiveSetEditor(
    targetReps: String,
    targetWeightKg: Double,
    reps: Int,
    weightKg: Double,
    useImperial: Boolean,
    onRepsChanged: (Int) -> Unit,
    onWeightChanged: (Double) -> Unit,
) {
    var editingWeight by remember { mutableStateOf<Boolean?>(null) }
    var manualValue by remember(reps, weightKg, editingWeight) {
        mutableStateOf(if (editingWeight == true) (if (useImperial) "%.1f".format(Locale.US, weightKg * KG_TO_LB) else weightKg.toString()) else reps.toString())
    }
    val weightStep = if (useImperial) 5.0 / KG_TO_LB else 2.5
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActiveSetMetricRow(
            label = "Reps",
            target = "Objetivo $targetReps",
            value = reps.toString(),
            onMinus = { onRepsChanged((reps - 1).coerceAtLeast(0)) },
            onPlus = { onRepsChanged(reps + 1) },
            onEdit = { editingWeight = false },
        )
        Spacer(Modifier.height(2.dp))
        ActiveSetMetricRow(
            label = "Peso",
            target = "Objetivo ${formatTrainingWeight(targetWeightKg, useImperial)}",
            value = if (useImperial) "%.1f".format(Locale.US, weightKg * KG_TO_LB) else "%.1f".format(Locale.US, weightKg),
            onMinus = { onWeightChanged((weightKg - weightStep).coerceAtLeast(0.0)) },
            onPlus = { onWeightChanged(weightKg + weightStep) },
            onEdit = { editingWeight = true },
        )
    }
    editingWeight?.let { isWeight ->
        AlertDialog(
            onDismissRequest = { editingWeight = null },
            title = { Text(if (isWeight) "EDITAR PESO" else "EDITAR REPETICIONES", fontFamily = AntonFontFamily) },
            text = {
                TextField(
                    value = manualValue,
                    onValueChange = { value -> manualValue = value.filter { it.isDigit() || (isWeight && (it == ',' || it == '.')) }.take(8) },
                    label = { Text(if (isWeight) "Peso" else "Repeticiones") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isWeight) KeyboardType.Decimal else KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isWeight) {
                        manualValue.replace(',', '.').toDoubleOrNull()?.let { value -> onWeightChanged((if (useImperial) value / KG_TO_LB else value).coerceIn(0.0, 750.0)) }
                    } else {
                        manualValue.toIntOrNull()?.let { value -> onRepsChanged(value.coerceIn(0, 999)) }
                    }
                    editingWeight = null
                }) { Text("GUARDAR", color = WildforceThemeTokens.accentGold) }
            },
            dismissButton = { TextButton(onClick = { editingWeight = null }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

@Composable
private fun ActiveSetMetricRow(
    label: String,
    target: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Keep the iOS-style single-line metric rows on normal phones. The
        // previous 360dp cutoff made Galaxy S23 Ultra's reported content width
        // enter a stacked layout, doubling the editor height and hiding the
        // completion CTA below the fold. Only extremely narrow windows use the
        // stacked fallback.
        val compact = maxWidth < 300.dp
        val narrowControls = maxWidth < 390.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                    Text(target, color = WildforceThemeTokens.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MetricControls(value, onMinus, onPlus, onEdit, Modifier.align(Alignment.CenterHorizontally), 36.dp)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.width(64.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                Text(target, Modifier.weight(1f).padding(horizontal = 6.dp), color = WildforceThemeTokens.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Galaxy devices commonly report a 360dp content width. Keep
                // the three controls on one line there instead of forcing the
                // whole exercise sheet to grow and become scroll-only.
                MetricControls(value, onMinus, onPlus, onEdit, controlSize = if (narrowControls) 34.dp else 38.dp)
            }
        }
    }
}

@Composable
private fun MetricControls(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    controlSize: Dp = 44.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CenteredRoundControl("−", controlSize, onMinus)
        Text(
            value,
            Modifier
                .widthIn(min = 42.dp, max = 64.dp)
                .clickable(onClick = onEdit)
                .padding(horizontal = 4.dp),
            color = WildforceThemeTokens.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CenteredRoundControl("+", controlSize, onPlus)
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
    // This content already lives inside the active sheet's verticalScroll.
    // A second scroll container receives infinite height constraints and
    // crashes Compose when feedback appears after completing a set.
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
            CenteredRoundControl("−", 40.dp, onMinus)
            Text(value, Modifier.clickable(enabled = onValueEntered != null) { manualInput = inputValue; showManualInput = true }.padding(horizontal = 3.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            CenteredRoundControl("+", 40.dp, onPlus)
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
        CenteredRoundControl("−", 44.dp, onMinus)
        CenteredRoundControl("+", 44.dp, onPlus)
    }
}

@Composable
private fun CenteredRoundControl(glyph: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(WildforceThemeTokens.textPrimary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (glyph == "+") Icons.Filled.Add else Icons.Filled.Remove,
            contentDescription = null,
            tint = WildforceThemeTokens.backgroundSecondary,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

@Composable
private fun CenteredBackButton(onClick: () -> Unit, size: Dp, padding: Dp = 0.dp) {
    Box(
        Modifier.padding(padding).size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.84f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.Black, modifier = Modifier.size(size * 0.48f))
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
