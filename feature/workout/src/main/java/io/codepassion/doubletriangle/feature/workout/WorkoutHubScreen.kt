package io.codepassion.doubletriangle.feature.workout

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.foundation.Image
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
import io.codepassion.doubletriangle.core.designsystem.PlanGenerationAnimation
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
import io.codepassion.doubletriangle.core.model.isPlanFinalized
import io.codepassion.doubletriangle.core.model.displayBlocks
import io.codepassion.doubletriangle.core.model.executionExercises
import io.codepassion.doubletriangle.core.model.workoutsFor
import io.codepassion.doubletriangle.core.model.hasNonDefaultSetStyle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
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
    allowAutomaticCreation: Boolean = true,
    canRegeneratePlan: Boolean = true,
    canGenerateNextPlan: Boolean = true,
    onGenerateNextPlan: () -> Unit = {},
    onShowAiCoachOffer: () -> Unit = {},
    generationError: String? = null,
    onRetryGeneration: () -> Unit = {},
    requiresPlanRegeneration: Boolean = false,
    onRecreatePlan: () -> Unit = {},
    avatarPath: String? = null,
    avatarRevision: Int = 0,
    appLanguage: String = "Español",
    openFeedback: Boolean = false,
    onFeedbackOpened: () -> Unit = {},
    customAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null,
    onCustomEditorVisibilityChanged: (Boolean) -> Unit = {},
) {
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var mode by remember { mutableStateOf(WorkoutMode.Plan) }
    val customContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var customWorkouts by remember { mutableStateOf(CustomWorkoutStore.load(customContext)) }
    var editingCustomWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var showingMesocycleDetails by remember { mutableStateOf(false) }
    var generatingCustomWorkout by remember { mutableStateOf(false) }
    var customGenerationError by remember { mutableStateOf<String?>(null) }
    var lastCustomRequest by remember { mutableStateOf<CustomWorkoutRequest?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var hubEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hubEntered = true }
    LaunchedEffect(editingCustomWorkout != null) {
        onCustomEditorVisibilityChanged(editingCustomWorkout != null)
    }
    DisposableEffect(Unit) {
        onDispose { onCustomEditorVisibilityChanged(false) }
    }

    editingCustomWorkout?.let { draft ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            CustomWorkoutEditorScreen(draft, gender, appLanguage, onCancel = { snapshot ->
                if (CustomWorkoutStore.shouldRetainManualDraft(snapshot)) {
                    CustomWorkoutStore.save(customContext, snapshot.copy(status = WorkoutStatus.Draft, creationSource = "manual"))
                } else {
                    CustomWorkoutStore.delete(customContext, snapshot.id)
                }
                customWorkouts = CustomWorkoutStore.load(customContext)
                editingCustomWorkout = null
            }) { saved ->
                CustomWorkoutStore.save(customContext, saved)
                customWorkouts = CustomWorkoutStore.load(customContext)
                editingCustomWorkout = null
            }
        }
        return
    }
    if (showingMesocycleDetails) {
        WorkoutMesocycleDetailScreen(
            state = state,
            planHistory = planHistory,
            contentPadding = contentPadding,
            appLanguage = appLanguage,
            onBack = { showingMesocycleDetails = false },
        )
        return
    }
    Column(
        // The root dock and Resume accessory are floating overlays. Keep the
        // page background full-height so the overlay never reveals a white
        // layout gap; WorkoutPlan keeps trailing scroll padding for the last
        // card to clear the controls.
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .liquidGlassBackground(),
    ) {
        generationError?.let { error ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
                    .liquidGlass(RoundedCornerShape(14.dp)).padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("No se pudo generar el plan: $error", Modifier.weight(1f), color = Color(0xFFC62828), style = MaterialTheme.typography.caption)
                TextButton(onClick = onRetryGeneration) {
                    Text("REINTENTAR", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        // iOS keeps this header deliberately quiet: identity and the week selector live
        // together on the secondary surface, while plan context starts below the mode picker.
        AnimatedVisibility(visible = hubEntered, enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { -it / 18 }) {
            Column(
                Modifier.fillMaxWidth()
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                WorkoutHeader(
                    state = state,
                    planHistory = planHistory,
                    customWorkouts = customWorkouts,
                    avatarPath = avatarPath,
                    avatarRevision = avatarRevision,
                    openFeedback = openFeedback,
                    onFeedbackOpened = onFeedbackOpened,
                    appLanguage = appLanguage,
                    onWorkoutSelected = onWorkoutSelected,
                )
                WeekCalendar(
                    state = state,
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = if (selectedDay == it) null else it },
                    appLanguage = appLanguage,
                    modifier = Modifier.padding(start = 28.dp, top = 12.dp, end = 28.dp),
                )
            }
        }

        AnimatedVisibility(visible = hubEntered, enter = fadeIn(tween(320, delayMillis = 70)) + slideInVertically(tween(320, delayMillis = 70)) { it / 18 }) {
            WorkoutModeSelector(
                selected = mode,
                onSelected = { mode = it },
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 8.dp),
            )
        }
        AnimatedVisibility(
            visible = mode == WorkoutMode.Plan,
            modifier = Modifier.weight(1f),
        ) {
            WorkoutPlan(
                state = state,
                selectedDay = selectedDay,
                onWorkoutSelected = onWorkoutSelected,
                onMesocycleSelected = { showingMesocycleDetails = true },
                onRecreatePlan = onRecreatePlan,
                requiresPlanRegeneration = requiresPlanRegeneration,
                canRegeneratePlan = canRegeneratePlan,
                canGenerateNextPlan = canGenerateNextPlan,
                onGenerateNextPlan = onGenerateNextPlan,
                showsAiCoach = allowAutomaticCreation,
                onShowAiCoachOffer = onShowAiCoachOffer,
                gender = gender,
                appLanguage = appLanguage,
                modifier = Modifier.fillMaxSize(),
            )
        }
        AnimatedVisibility(
            visible = mode == WorkoutMode.Custom,
            modifier = Modifier.weight(1f),
        ) {
            CustomWorkoutsScreen(
                workouts = customWorkouts.filter { selectedDay == null || it.scheduledDay == selectedDay }, gender = gender,
                appLanguage = appLanguage,
                defaultEquipment = defaultCustomEquipment,
                equipmentPresets = customEquipmentPresets,
                allowAutomaticCreation = allowAutomaticCreation,
                onCreateManual = { editingCustomWorkout = CustomWorkoutStore.empty(selectedDay ?: LocalDate.now().dayOfWeek) },
                onCreateAutomatic = { request ->
                    lastCustomRequest = request
                    customGenerationError = null
                    if (customAiGenerator == null) {
                        CustomWorkoutStore.save(customContext, CustomWorkoutStore.automatic(selectedDay ?: LocalDate.now().dayOfWeek, request))
                        customWorkouts = CustomWorkoutStore.load(customContext)
                        lastCustomRequest = null
                    } else {
                        generatingCustomWorkout = true
                        coroutineScope.launch {
                            runCatching { customAiGenerator.invoke(request) }
                                .onSuccess { generated ->
                                    CustomWorkoutStore.save(customContext, generated.copy(scheduledDay = selectedDay ?: LocalDate.now().dayOfWeek))
                                    customWorkouts = CustomWorkoutStore.load(customContext)
                                    lastCustomRequest = null
                                }
                                .onFailure { error -> customGenerationError = error.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el entrenamiento." }
                            generatingCustomWorkout = false
                        }
                    }
                },
                onOpen = onWorkoutSelected,
                onEdit = {
                    val draft = it.copy(status = WorkoutStatus.Draft, creationSource = "manual")
                    CustomWorkoutStore.save(customContext, draft)
                    editingCustomWorkout = draft
                    customWorkouts = CustomWorkoutStore.load(customContext)
                },
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
            PlanGenerationOverlay()
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
                                        .onSuccess { generated ->
                                            CustomWorkoutStore.save(customContext, generated.copy(scheduledDay = selectedDay ?: LocalDate.now().dayOfWeek))
                                            customWorkouts = CustomWorkoutStore.load(customContext)
                                            lastCustomRequest = null
                                        }
                                        .onFailure { retryError -> customGenerationError = retryError.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el entrenamiento." }
                                    generatingCustomWorkout = false
                                }
                            }
                        }) { Text("REINTENTAR", color = WildforceThemeTokens.accent) }
                    }
                    TextButton(onClick = {
                        customGenerationError = null
                        CustomWorkoutStore.save(customContext, CustomWorkoutStore.automatic(
                            selectedDay ?: LocalDate.now().dayOfWeek,
                            lastCustomRequest ?: CustomWorkoutRequest("Cuerpo completo", 45, "Peso corporal"),
                        ))
                        customWorkouts = CustomWorkoutStore.load(customContext)
                        lastCustomRequest = null
                    }) { Text("USAR PLANTILLA LOCAL", color = WildforceThemeTokens.accent) }
                }
            },
            dismissButton = { TextButton(onClick = { customGenerationError = null }) { Text("CERRAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

/** Native counterpart of iOS CreatePlanAnimationView for custom-workout AI generation. */
@Composable
private fun PlanGenerationOverlay() {
    PlanGenerationAnimation(
        icon = "⌁",
        modifier = Modifier.width(320.dp).height(436.dp).clip(RoundedCornerShape(24.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(vertical = 28.dp),
    )
}

@Composable
private fun WorkoutHeader(
    state: WorkoutHubState,
    planHistory: List<WorkoutHubState>,
    customWorkouts: List<WorkoutDaySummary>,
    avatarPath: String?,
    avatarRevision: Int,
    openFeedback: Boolean,
    onFeedbackOpened: () -> Unit,
    appLanguage: String,
    onWorkoutSelected: (WorkoutDaySummary) -> Unit,
) {
    var showsMoreOptions by remember { mutableStateOf(false) }
    var showsFeedback by remember { mutableStateOf(false) }
    var showsFaq by remember { mutableStateOf(false) }
    var showsStreakCalendar by remember { mutableStateOf(false) }
    LaunchedEffect(openFeedback) {
        if (openFeedback) {
            showsFeedback = true
            onFeedbackOpened()
        }
    }
    val today = LocalDate.now().dayOfWeek
    // Same condition as iOS: flag the streak only on a scheduled training day
    // when that day's workout has not yet been completed.
    val customCompletedToday = customWorkouts.any { workout ->
        workout.status == WorkoutStatus.Completed && workout.completedAt?.let { raw ->
            runCatching { java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }
                .getOrDefault(false)
        } == true
    }
    val isWorkoutIncomplete = today in state.trainingDays && today !in state.completedDays && !customCompletedToday
    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = remember(avatarPath, avatarRevision) { avatarPath?.let(BitmapFactory::decodeFile) }
            if (avatar != null) {
                Image(
                    bitmap = avatar.asImageBitmap(),
                    contentDescription = "Foto de perfil de ${state.user.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = WildforceThemeTokens.textSecondary,
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(state.user.name, fontFamily = FontFamily.Default, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    goalIcon(state.user.goal),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = WildforceThemeTokens.textSecondary,
                )
                Text(" ${state.user.goal}", fontFamily = FontFamily.Default, fontSize = 12.sp, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier.semantics {
                contentDescription = if (isWorkoutIncomplete) {
                    "Racha actual: ${state.user.currentStreak}. Entrenamiento pendiente hoy"
                } else {
                    "Racha actual: ${state.user.currentStreak}"
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable { showsStreakCalendar = true }.padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The warning belongs to the flame, not the complete streak
                // control: iOS layers it over the flame's bottom-right corner.
                Box(Modifier.size(18.dp)) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFF9500),
                    )
                    if (isWorkoutIncomplete) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                                .size(12.dp).background(Color.White, CircleShape).padding(1.dp),
                            tint = Color(0xFFFF3B30),
                        )
                    }
                }
                Text(
                    state.user.currentStreak.toString(),
                    Modifier.padding(start = 4.dp),
                    fontFamily = FontFamily.Default,
                    fontSize = 17.sp,
                    color = WildforceThemeTokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(Modifier.padding(start = 12.dp)) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .liquidGlass(CircleShape).clickable { showsMoreOptions = true }
                    .semantics { contentDescription = "Más opciones" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = WildforceThemeTokens.textPrimary)
            }
            DropdownMenu(
                expanded = showsMoreOptions,
                onDismissRequest = { showsMoreOptions = false },
                modifier = Modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(16.dp)),
            ) {
                DropdownMenuItem(onClick = { showsMoreOptions = false; showsFeedback = true }) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = WildforceThemeTokens.textPrimary)
                    Text("Feedback", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                }
                DropdownMenuItem(onClick = { showsMoreOptions = false; showsFaq = true }) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = WildforceThemeTokens.textPrimary)
                    Text("FAQ", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                }
            }
        }
    }
    if (showsFeedback) FeedbackDialog(onDismiss = { showsFeedback = false })
    if (showsFaq) FaqDialog(onDismiss = { showsFaq = false })
    if (showsStreakCalendar) {
        StreakCalendarDialog(
            state = state,
            planHistory = planHistory,
            customWorkouts = customWorkouts,
            appLanguage = appLanguage,
            onDismiss = { showsStreakCalendar = false },
            onWorkoutSelected = onWorkoutSelected,
        )
    }
}

internal fun goalIcon(goal: String): ImageVector = when {
    goal.contains("peso", ignoreCase = true) || goal.contains("loseweight", ignoreCase = true) -> Icons.Filled.Sync
    goal.contains("músculo", ignoreCase = true) || goal.contains("muscle", ignoreCase = true) || goal.contains("buildmuscle", ignoreCase = true) -> Icons.Filled.FitnessCenter
    goal.contains("fuerza", ignoreCase = true) || goal.contains("strength", ignoreCase = true) || goal.contains("gainstrength", ignoreCase = true) -> Icons.Filled.Bolt
    goal.contains("resistencia", ignoreCase = true) || goal.contains("endurance", ignoreCase = true) || goal.contains("improveendurance", ignoreCase = true) -> Icons.Filled.DirectionsWalk
    goal.contains("movilidad", ignoreCase = true) || goal.contains("mobility", ignoreCase = true) || goal.contains("improvemobility", ignoreCase = true) -> Icons.Filled.SelfImprovement
    goal.contains("recomposición", ignoreCase = true) || goal.contains("recomposition", ignoreCase = true) || goal.contains("bodyrecomposition", ignoreCase = true) -> Icons.Filled.MyLocation
    else -> Icons.Filled.Favorite
}

@Composable
private fun StreakCalendarDialog(
    state: WorkoutHubState,
    planHistory: List<WorkoutHubState>,
    customWorkouts: List<WorkoutDaySummary>,
    appLanguage: String,
    onDismiss: () -> Unit,
    onWorkoutSelected: (WorkoutDaySummary) -> Unit,
) {
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedWorkoutsForDay by remember { mutableStateOf<List<WorkoutDaySummary>?>(null) }
    val monthLocale = java.util.Locale.forLanguageTag(
        when (appLanguage) {
            "English" -> "en"
            "Català" -> "ca"
            "Français" -> "fr"
            "Italiano" -> "it"
            "Português" -> "pt"
            "Deutsch" -> "de"
            "中文（简体）" -> "zh-Hans"
            "Nederlands" -> "nl"
            "日本語" -> "ja"
            else -> "es"
        },
    )
    val completedByDate = remember(state.workouts, planHistory, customWorkouts) {
        ((listOf(state) + planHistory).flatMap { it.workouts } + customWorkouts)
            .distinctBy { it.id }
            .filter { it.status == WorkoutStatus.Completed }
            .mapNotNull { workout -> workout.completedAt?.let { raw -> runCatching { java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).toLocalDate() to workout }.getOrNull() } }
            .groupBy({ it.first }, { it.second })
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = WildforceThemeTokens.backgroundSecondary,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("HISTORIAL DE ENTRENAMIENTOS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                        Text(
                            if (state.user.currentStreak == 1) "1 día de racha" else "${state.user.currentStreak} días de racha",
                            color = WildforceThemeTokens.textSecondary,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("LISTO", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Mes anterior", modifier = Modifier.size(22.dp), tint = WildforceThemeTokens.textPrimary) }
                    Text(
                        month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", monthLocale)).replaceFirstChar { it.titlecase(monthLocale) },
                        Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary,
                    )
                    TextButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ArrowForward, contentDescription = "Mes siguiente", modifier = Modifier.size(22.dp), tint = WildforceThemeTokens.textPrimary) }
                }
                val firstDayOfMonth = month.withDayOfMonth(1)
                val leadingDays = firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
                val days: List<LocalDate?> = buildList {
                    repeat(leadingDays) { add(null) }
                    repeat(month.lengthOfMonth()) { dayOffset ->
                        add(firstDayOfMonth.plusDays(dayOffset.toLong()))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DayOfWeek.values().forEach { day ->
                        Text(
                            day.getDisplayName(java.time.format.TextStyle.NARROW, monthLocale).uppercase(monthLocale),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption,
                            color = WildforceThemeTokens.textSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { date ->
                                val workouts = date?.let(completedByDate::get).orEmpty()
                                Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                                    if (date != null) {
                                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(if (workouts.isNotEmpty()) WildforceThemeTokens.textSecondary.copy(alpha = 0.10f) else Color.Transparent).clickable(enabled = workouts.isNotEmpty()) {
                                            when (workouts.size) {
                                                0 -> Unit
                                                1 -> { onWorkoutSelected(workouts.first()); onDismiss() }
                                                else -> selectedWorkoutsForDay = workouts
                                            }
                                        }, contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(date.dayOfMonth.toString(), color = if (date == LocalDate.now()) WildforceThemeTokens.accent else WildforceThemeTokens.textPrimary, fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal)
                                                if (workouts.isNotEmpty()) Icon(Icons.Filled.CheckCircle, "Entrenamiento completado", Modifier.size(12.dp), tint = Color(0xFF34C759)) else Spacer(Modifier.height(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selectedWorkoutsForDay?.let { workouts ->
        AlertDialog(
            onDismissRequest = { selectedWorkoutsForDay = null },
            title = { Text("ENTRENAMIENTOS COMPLETADOS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    workouts.forEach { workout ->
                        TextButton(
                            onClick = {
                                selectedWorkoutsForDay = null
                                onWorkoutSelected(workout)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(workout.title, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    localizedWorkoutLabel(workout.focus, appLanguage),
                                    color = WildforceThemeTokens.textSecondary,
                                    style = MaterialTheme.typography.caption,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWorkoutsForDay = null }) {
                    Text("CERRAR", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("No es suficientemente útil") }
    var message by remember { mutableStateOf("") }
    // Matches iOS LeavingFeedbackReason.allCases; these are deliberately kept
    // in the same order so the authenticated submission has the same meaning.
    val categories = listOf(
        "Demasiado caro",
        "No es suficientemente útil",
        "Demasiado difícil de usar",
        "Faltan funciones",
        "Problemas técnicos o errores",
        "Solo estoy probando otra cosa",
    )
    var categoriesExpanded by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    if (sent) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Gracias", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) },
            text = { Text("Tu feedback nos ayuda a saber qué mejorar a continuación.", color = WildforceThemeTokens.textSecondary) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR", color = WildforceThemeTokens.textPrimary) } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FEEDBACK", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cuéntanos qué podemos mejorar.", color = WildforceThemeTokens.textSecondary)
                Box {
                    OutlinedButton(onClick = { categoriesExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
                        Icon(Icons.Filled.ExpandMore, contentDescription = "Abrir categorías", tint = WildforceThemeTokens.textSecondary)
                    }
                    DropdownMenu(expanded = categoriesExpanded, onDismissRequest = { categoriesExpanded = false }) {
                        categories.forEach { option ->
                            DropdownMenuItem(onClick = { category = option; categoriesExpanded = false }) { Text(option) }
                        }
                    }
                }
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                    label = { Text("Tu mensaje") },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = WildforceThemeTokens.textPrimary,
                        backgroundColor = WildforceThemeTokens.backgroundSecondary,
                        focusedIndicatorColor = WildforceThemeTokens.textPrimary,
                    ),
                )
                error?.let { Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = message.trim().isNotEmpty() && !sending,
                onClick = {
                    sending = true
                    error = null
                    scope.launch {
                        runCatching {
                            FeedbackClient.submit(context, "Leaving feedback: $category", message.trim())
                        }.onSuccess {
                            sent = true
                        }.onFailure {
                            error = it.message ?: "No se pudo enviar el feedback"
                        }
                        sending = false
                    }
                },
            ) { if (sending) CircularProgressIndicator(Modifier.size(18.dp)) else Text("ENVIAR", color = WildforceThemeTokens.textPrimary) }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
    )
}

@Composable
private fun FaqDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            backgroundColor = WildforceThemeTokens.background,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "PREGUNTAS FRECUENTES",
                        modifier = Modifier.weight(1f),
                        fontFamily = AntonFontFamily,
                        color = WildforceThemeTokens.textPrimary,
                    )
                    TextButton(onClick = onDismiss) {
                        Text("CERRAR", color = WildforceThemeTokens.textPrimary)
                    }
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = false
                            webViewClient = WebViewClient()
                            loadUrl("file:///android_asset/faq.html")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekCalendar(
    state: WorkoutHubState,
    selectedDay: DayOfWeek?,
    onDaySelected: (DayOfWeek) -> Unit,
    appLanguage: String,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val today = LocalDate.now()
    val locale = java.util.Locale.forLanguageTag(when (appLanguage) {
        "English" -> "en"; "Català" -> "ca"; "Français" -> "fr"; "Italiano" -> "it"
        "Português" -> "pt"; "Deutsch" -> "de"; "中文（简体）" -> "zh-Hans"
        "Nederlands" -> "nl"; "日本語" -> "ja"; else -> "es"
    })
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // Mirrors SevenDayWorkoutCalendarView on iOS: planned days have a 6 dp
    // accent dot, while completed days use the system-green filled checkmark.
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0L..6L).forEach { offset ->
            val date = monday.plusDays(offset)
            val day = date.dayOfWeek
            val selected = day == selectedDay
            val hasWorkout = day in state.trainingDays
            val completed = isWorkoutCompletedOnDate(state, date)
            val selectionScale by animateFloatAsState(
                targetValue = if (selected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 650f),
                label = "workout-calendar-$day",
            )
            Column(
                Modifier.weight(1f).alpha(if (hasWorkout) 1f else 0.7f)
                    .graphicsLayer { scaleX = selectionScale; scaleY = selectionScale }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) WildforceThemeTokens.accent
                        else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f),
                    )
                    .semantics {
                        contentDescription = buildString {
                            append(day.getDisplayName(TextStyle.FULL, locale))
                            if (date == today) append(", hoy")
                            if (hasWorkout) append(", entrenamiento programado")
                            if (completed) append(", entrenamiento completado")
                        }
                        stateDescription = if (selected) "Seleccionado" else "No seleccionado"
                        this.selected = selected
                    }
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDaySelected(day)
                    }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, locale),
                    color = if (selected) MaterialTheme.colors.onPrimary
                    else if (date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textPrimary,
                    fontFamily = FontFamily.Default,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(12.dp), contentAlignment = Alignment.Center) {
                    when {
                        completed -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Entrenamiento completado",
                            modifier = Modifier.size(12.dp),
                            tint = if (selected) MaterialTheme.colors.onPrimary else Color(0xFF34C759),
                        )
                        hasWorkout -> Box(
                            Modifier.size(6.dp).background(
                                if (selected) MaterialTheme.colors.onPrimary else WildforceThemeTokens.accent,
                                CircleShape,
                            ),
                        )
                    }
                }
            }
        }
    }
}

internal fun isWorkoutCompletedOnDate(state: WorkoutHubState, date: LocalDate): Boolean =
    state.workouts.any { workout ->
        workout.status == WorkoutStatus.Completed && workout.completedAt?.let { completedAt ->
            runCatching {
                java.time.Instant.parse(completedAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate() == date
            }.getOrDefault(false)
        } == true
    }

@Composable
private fun WorkoutModeSelector(
    selected: WorkoutMode,
    onSelected: (WorkoutMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    // Matches iOS's shared UISegmentedControl: the native neutral track,
    // text-primary selection tint, and primary-button text for the active tab.
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.fillMaxWidth().height(32.dp).clip(CircleShape)
                .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)).padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkoutMode.values().forEach { mode ->
                val isSelected = selected == mode
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .background(if (isSelected) WildforceThemeTokens.textPrimary else Color.Transparent)
                        .semantics {
                            contentDescription = mode.label
                            this.selected = isSelected
                        }
                        .clickable {
                            if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelected(mode)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        mode.label,
                        color = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutPlan(
    state: WorkoutHubState,
    selectedDay: DayOfWeek?,
    onWorkoutSelected: (WorkoutDaySummary) -> Unit,
    onMesocycleSelected: () -> Unit,
    onRecreatePlan: () -> Unit,
    requiresPlanRegeneration: Boolean,
    canRegeneratePlan: Boolean,
    canGenerateNextPlan: Boolean,
    onGenerateNextPlan: () -> Unit,
    showsAiCoach: Boolean,
    onShowAiCoachOffer: () -> Unit,
    gender: String,
    appLanguage: String,
    modifier: Modifier = Modifier,
) {
    val workouts = state.workoutsFor(selectedDay)
    val (upNextWorkouts, completedWorkouts) = groupWorkoutPlanDays(workouts)
    var cardsEntered by remember(selectedDay, state.planName) { mutableStateOf(false) }
    LaunchedEffect(selectedDay, state.planName) { cardsEntered = true }
    LazyColumn(
        modifier,
        // Keep the final card scrollable above both the tab bar and the optional
        // resume accessory, without shrinking the screen behind those controls.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.workouts.isEmpty() && state.planName.isBlank()) {
            item {
                PlanGenerationSection(
                    showsAiCoach = showsAiCoach,
                    canGenerateNextPlan = canGenerateNextPlan,
                    appLanguage = appLanguage,
                    onGenerateNextPlan = onGenerateNextPlan,
                    onShowAiCoachOffer = onShowAiCoachOffer,
                )
            }
            return@LazyColumn
        }
        if (state.planStatus.equals("completed", ignoreCase = true)) {
            item {
                PlanGenerationSection(
                    showsAiCoach = showsAiCoach,
                    canGenerateNextPlan = canGenerateNextPlan,
                    appLanguage = appLanguage,
                    onGenerateNextPlan = onGenerateNextPlan,
                    onShowAiCoachOffer = onShowAiCoachOffer,
                )
            }
        }
        if (requiresPlanRegeneration && showsAiCoach) {
            item {
                ProfileChangedPlanBanner(onRecreatePlan, appLanguage)
            }
        }
        item {
            MesocycleHeaderCard(state, onMesocycleSelected)
        }
        item {
            WeeklyPlanHeaderCard(state, onRecreatePlan, canRegeneratePlan, appLanguage)
        }
        if (workouts.isEmpty()) {
            item { RestDayCard() }
        } else {
            itemsIndexed(upNextWorkouts, key = { _, workout -> workout.id }) { index, workout ->
                AnimatedVisibility(visible = cardsEntered, enter = fadeIn(tween(280, delayMillis = 70 + index * 55)) + slideInVertically(tween(280, delayMillis = 70 + index * 55)) { it / 16 }) {
                    WorkoutCard(workout, gender, appLanguage) { onWorkoutSelected(workout) }
                }
            }
            if (completedWorkouts.isNotEmpty()) {
                item {
                    Text(
                        "COMPLETADOS",
                        Modifier.padding(top = 8.dp),
                        fontFamily = AntonFontFamily,
                        style = MaterialTheme.typography.h6,
                        color = WildforceThemeTokens.textPrimary,
                    )
                }
                itemsIndexed(completedWorkouts, key = { _, workout -> workout.id }) { index, workout ->
                    AnimatedVisibility(visible = cardsEntered, enter = fadeIn(tween(280, delayMillis = 70 + (upNextWorkouts.size + index) * 55)) + slideInVertically(tween(280, delayMillis = 70 + (upNextWorkouts.size + index) * 55)) { it / 16 }) {
                        WorkoutCard(workout, gender, appLanguage) { onWorkoutSelected(workout) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileChangedPlanBanner(onRecreatePlan: () -> Unit, appLanguage: String) {
    val title = localizedWorkoutCatalogText("570a0c02e699364e", "Profile Changed", appLanguage)
    val description = localizedWorkoutCatalogText(
        "4991d7c9a032c68d",
        "Your training profile has changed since this plan was created. Recreate it for better results.",
        appLanguage,
    )
    val action = localizedWorkoutCatalogText("03b2f5810f7fbed0", "Recreate Plan", appLanguage)
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFFF9500).copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF9500))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                Text(description, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            }
        }
        Button(
            onClick = onRecreatePlan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9500), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
        ) { Text(action, fontWeight = FontWeight.SemiBold) }
    }
}

/** Mirrors WorkoutPlanOverviewView.upNextWorkoutDays/completedWorkoutDays on iOS. */
internal fun groupWorkoutPlanDays(workouts: List<WorkoutDaySummary>): Pair<List<WorkoutDaySummary>, List<WorkoutDaySummary>> =
    workouts.partition { it.status != WorkoutStatus.Completed }

@Composable
private fun PlanGenerationSection(
    showsAiCoach: Boolean,
    canGenerateNextPlan: Boolean,
    appLanguage: String,
    onGenerateNextPlan: () -> Unit,
    onShowAiCoachOffer: () -> Unit,
) {
    if (showsAiCoach) {
        NextPhasePlanCard(
            appLanguage = appLanguage,
            onGenerate = if (canGenerateNextPlan) onGenerateNextPlan else onShowAiCoachOffer,
        )
    } else {
        CoachPlanUnavailableCard(appLanguage)
    }
}

@Composable
private fun CoachPlanUnavailableCard(appLanguage: String) {
    val title = localizedWorkoutCatalogText("335a2a5b55ad001b", "No workout plan yet", appLanguage)
    val description = localizedWorkoutCatalogText(
        "16c8183968434ee4",
        "Your coach will share your workout plan here.",
        appLanguage,
    )
    Column(
        Modifier.fillMaxWidth()
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = WildforceThemeTokens.textSecondary,
        )
        Text(title, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            description,
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NextPhasePlanCard(appLanguage: String, onGenerate: () -> Unit) {
    val title = localizedWorkoutCatalogText("28f9dca8243ebbb7", "Next Phase", appLanguage)
    val description = localizedWorkoutCatalogText(
        "b309c7b855803481",
        "Get a personalized training plan powered by AI, tailored to your goals and equipment.",
        appLanguage,
    )
    val action = localizedWorkoutCatalogText("898b391f5d35497e", "Generate Plan", appLanguage)
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(WildforceThemeTokens.accent, WildforceThemeTokens.accent.copy(alpha = .8f))),
                RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title.uppercase(), fontFamily = AntonFontFamily, fontSize = 32.sp, color = WildforceThemeTokens.primaryButtonText)
                Text(description, fontFamily = FontFamily.Default, fontSize = 15.sp, color = WildforceThemeTokens.primaryButtonText.copy(alpha = .9f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(28.dp), tint = WildforceThemeTokens.primaryButtonText)
            }
        }
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.primaryButtonText, contentColor = WildforceThemeTokens.accent),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.semantics { contentDescription = action },
        ) {
            Text(action, fontFamily = FontFamily.Default, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
    }
}

/** Mirrors iOS's MesocycleHeaderView: it is a distinct, tappable context card. */
@Composable
private fun MesocycleHeaderCard(state: WorkoutHubState, onClick: () -> Unit) {
    val totalWeeks = state.cycleLength.coerceIn(1, 12)
    val currentWeek = state.positionInCycle.coerceIn(1, totalWeeks)
    val currentPhase = state.mesocyclePhase ?: phaseForMesocycleWeek(currentWeek, totalWeeks)
    Column(
        // iOS's MesocycleHeaderView uses the opaque backgroundSecondary section
        // surface, rather than a translucent material.
        Modifier.fillMaxWidth()
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp))
            .semantics { contentDescription = "Mesociclo ${state.mesocycleIndex}, semana $currentWeek de $totalWeeks" }
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "MESOCICLO ${state.mesocycleIndex}",
                    fontFamily = Exo2FontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = WildforceThemeTokens.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Match iOS's `calendar` and phase SF Symbols with their
                    // Material counterparts instead of using text glyphs.
                    ContextPill("Semana $currentWeek de $totalWeeks", Icons.Filled.CalendarToday)
                    ContextPill(currentPhase.label, phaseIcon(currentPhase))
                }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.padding(top = 6.dp).size(18.dp),
                tint = WildforceThemeTokens.textSecondary,
            )
        }
        MesocycleRoadmap(currentWeek, totalWeeks)
    }
}

@Composable
private fun WeeklyPlanHeaderCard(state: WorkoutHubState, onRecreatePlan: () -> Unit, canRegeneratePlan: Boolean, appLanguage: String) {
    val finalized = state.workouts.count { it.status.isPlanFinalized }
    var showsPlanMenu by remember { mutableStateOf(false) }
    var showsPlanNotes by remember { mutableStateOf(false) }
    val isCompleted = state.planStatus.equals("completed", ignoreCase = true)
    val planGoal = state.planGoal?.takeIf(String::isNotBlank) ?: state.user.goal
    val planGoalLabel = localizedWorkoutLabel(planGoal, appLanguage)
    val planNotes = state.planNotes?.takeIf(String::isNotBlank)
    Column(
        // Match iOS's PlanHeaderView: both home context cards share the same
        // opaque secondary surface instead of mixing glass and solid whites.
        Modifier.fillMaxWidth()
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ESTA SEMANA", fontFamily = FontFamily.Default, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
                Text(state.planName, fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.Normal, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
                if (isCompleted) {
                    StatusBadge("COMPLETADO", Color(0xFF26A269))
                } else if (planNotes != null || canRegeneratePlan) {
                    Box {
                        Box(
                            Modifier.size(48.dp)
                                .clip(CircleShape)
                                .semantics { contentDescription = "Opciones del plan" }
                                .clickable { showsPlanMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(34.dp).liquidGlass(CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.MoreHoriz, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textSecondary)
                            }
                        }
                        DropdownMenu(expanded = showsPlanMenu, onDismissRequest = { showsPlanMenu = false }) {
                            if (planNotes != null) {
                                DropdownMenuItem(onClick = {
                                    showsPlanMenu = false
                                    showsPlanNotes = true
                                }) {
                                    Icon(Icons.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textPrimary)
                                    Text("Ver notas", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                                }
                            }
                            if (canRegeneratePlan) {
                                DropdownMenuItem(onClick = {
                                    showsPlanMenu = false
                                    onRecreatePlan()
                                }) {
                                    // Mirrors iOS PlanHeaderView's `Label(..., systemImage: "trash")`.
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textPrimary)
                                    Text("Rehacer plan", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                                }
                            }
                        }
                    }
                }
        }
        ContextPill(planGoalLabel, goalIcon(planGoal))
        if (state.workouts.isNotEmpty()) {
            Text("$finalized de ${state.workouts.size} sesiones finalizadas", fontFamily = FontFamily.Default, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(state.workouts.size) { index ->
                    val filled = index < finalized
                    val current = index == finalized - 1
                    Box(
                        Modifier.weight(1f)
                            .height(if (current) 10.dp else 8.dp)
                            .graphicsLayer {
                                val scale = if (current) 1.05f else if (filled) 1.02f else 1f
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(if (filled) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = 0.4f)),
                    )
                }
            }
        }
    }
    if (showsPlanNotes && planNotes != null) {
        AlertDialog(
            onDismissRequest = { showsPlanNotes = false },
            title = { Text("NOTAS DEL PLAN", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) },
            text = { Text(planNotes, color = WildforceThemeTokens.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showsPlanNotes = false }) {
                    Text("CERRAR", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
internal fun ContextPill(text: String, glyph: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)).padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Text(text, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ContextPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)).padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = WildforceThemeTokens.textSecondary)
        Text(text, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MesocycleRoadmap(currentWeek: Int, totalWeeks: Int) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..totalWeeks).forEach { week ->
            val phase = phaseForMesocycleWeek(week, totalWeeks)
            val active = week <= currentWeek
            Box(Modifier.size(if (week == currentWeek) 40.dp else 36.dp), contentAlignment = Alignment.Center) {
                if (week == currentWeek) Box(Modifier.size(40.dp).border(1.dp, WildforceThemeTokens.textPrimary.copy(alpha = 0.85f), CircleShape))
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (active) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                    Icon(
                        phaseIcon(phase),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (active) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary.copy(alpha = 0.40f),
                    )
                }
            }
            if (week != totalWeeks) Spacer(Modifier.weight(1f).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.20f)))
        }
    }
}

internal fun phaseForMesocycleWeek(week: Int, totalWeeks: Int): io.codepassion.doubletriangle.core.model.MesocyclePhase {
    val accumulationLength = if (totalWeeks <= 4) totalWeeks - 1 else totalWeeks / 2
    val intensificationLength = (totalWeeks - accumulationLength - 1).coerceAtLeast(0)
    return when {
        week <= accumulationLength -> io.codepassion.doubletriangle.core.model.MesocyclePhase.Accumulation
        week <= accumulationLength + intensificationLength -> io.codepassion.doubletriangle.core.model.MesocyclePhase.Intensification
        else -> io.codepassion.doubletriangle.core.model.MesocyclePhase.Deload
    }
}

internal fun phaseGlyph(phase: io.codepassion.doubletriangle.core.model.MesocyclePhase): String = when (phase) {
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Accumulation -> "↗"
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Intensification -> "↑"
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Deload -> "↓"
}

/** Android equivalents for the SF Symbols assigned to each iOS mesocycle phase. */
internal fun phaseIcon(phase: io.codepassion.doubletriangle.core.model.MesocyclePhase): ImageVector = when (phase) {
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Accumulation -> Icons.Filled.Layers
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Intensification -> Icons.Filled.NorthEast
    io.codepassion.doubletriangle.core.model.MesocyclePhase.Deload -> Icons.Filled.ArrowCircleDown
}

@Composable
private fun WorkoutCard(workout: WorkoutDaySummary, gender: String, appLanguage: String, onClick: () -> Unit) {
    // iOS persists `.inProgress` on the workout day. Android persists the active
    // session separately, so its presence is the equivalent source of truth.
    val sessionContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val isInProgress = workout.status == WorkoutStatus.InProgress ||
        (workout.status == WorkoutStatus.Planned && WorkoutSessionStore.load(sessionContext, workout.id) != null)
    val completionMetrics = remember(workout.id, workout.status) {
        if (workout.status == WorkoutStatus.Completed) WorkoutCompletionMetricsStore.load(sessionContext, workout.id) else null
    }
    val focusLabel = localizedWorkoutLabel(workout.focus, appLanguage)
    val dayTypeLabel = localizedWorkoutLabel(workout.dayType, appLanguage)
    val statusLabel = workoutCardStatus(workout.status, isInProgress)?.first
    val actionLabel = if (isInProgress) "Reanudar" else if (workout.status == WorkoutStatus.Completed) "Ver resultados" else "Abrir entrenamiento"
    val cardAccessibilityLabel = buildString {
        append("Día ${workout.order}, ${workout.title}")
        statusLabel?.let { append(", $it") }
        append(", $actionLabel")
        append(", $focusLabel, $dayTypeLabel, ${workout.estimatedMinutes} minutos")
    }
    Card(
        Modifier.fillMaxWidth()
            .clearAndSetSemantics { contentDescription = cardAccessibilityLabel }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
    ) {
        var cardSize by remember { mutableStateOf(IntSize.Zero) }
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 180.dp).onSizeChanged { cardSize = it }, propagateMinConstraints = true) {
            val cardWidthPx = cardSize.width.toFloat()
            val cardHeightPx = cardSize.height.toFloat()
            RemoteTrainingImage(
                url = workoutCoverUrl(workout.focus, gender, workout.id),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
            // Match WorkoutDayHeaderView on iOS: the fades run from the
            // bottom/leading edges to the card midpoint. These must use the
            // measured size, not fixed pixels, so density does not change the
            // apparent strength of the dark treatment.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                        startY = cardHeightPx,
                        endY = cardHeightPx / 2f,
                    ),
                ),
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent),
                        startX = 0f,
                        endX = cardWidthPx / 2f,
                    ),
                ),
            )
            Column(
                Modifier.fillMaxWidth()
                    .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Text(
                    "DÍA ${workout.order}",
                    fontFamily = AntonFontFamily,
                    fontSize = 30.sp,
                    color = Color.White,
                )

                // WorkoutDayHeaderView keeps status on the leading content
                // axis, between the day label and title. Planned days reserve
                // the same visual space without rendering a badge.
                val status = workoutCardStatus(workout.status, isInProgress)
                Spacer(Modifier.weight(1f, fill = true).heightIn(min = 18.dp))
                status?.let { (label, color) -> StatusBadge(label, color) }

                Text(
                    workout.title.uppercase(),
                    Modifier.padding(top = 2.dp),
                    fontFamily = AntonFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    color = Color.White,
                    // WorkoutDayHeaderView on iOS does not force the title
                    // into one line; long workout names can wrap.
                    maxLines = 2,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .semantics {
                            contentDescription = "$focusLabel, $dayTypeLabel, ${workout.estimatedMinutes} min"
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // iOS presents this as one non-scrolling HStack. The
                    // descriptive fields therefore truncate cleanly instead
                    // of exposing clipped, scrollable fragments.
                    WorkoutCardMetadata(Icons.Filled.MyLocation, focusLabel, modifier = Modifier.weight(1f))
                    WorkoutCardMetadata(workoutDayTypeIcon(workout.dayType), dayTypeLabel, modifier = Modifier.weight(1f))
                    WorkoutCardMetadata(
                        Icons.Filled.Timer,
                        if (workout.status == WorkoutStatus.Completed) {
                            "${(completionMetrics?.activeDurationSeconds ?: workout.estimatedMinutes * 60) / 60} min"
                        } else {
                            "${workout.estimatedMinutes} min"
                        },
                    )
                    formatScheduledWorkoutDate(workout.scheduledFor, appLanguage)?.let { scheduledDate ->
                        WorkoutCardMetadata(Icons.Filled.CalendarToday, scheduledDate, modifier = Modifier.weight(1f))
                    }
                    completionMetrics?.activeCaloriesBurned?.takeIf { it > 0 }?.let { calories ->
                        WorkoutCardMetadata(Icons.Filled.LocalFireDepartment, "${calories.toInt()} kcal", Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

internal fun formatScheduledWorkoutDate(value: String?, appLanguage: String): String? {
    val raw = value?.trim()?.substringBefore('T')?.substringBefore(' ')
    val date = raw?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val locale = java.util.Locale.forLanguageTag(
        when (appLanguage) {
            "English" -> "en"
            "Català" -> "ca"
            "Français" -> "fr"
            "Italiano" -> "it"
            "Português" -> "pt"
            "Deutsch" -> "de"
            "中文（简体）" -> "zh-Hans"
            "Nederlands" -> "nl"
            "日本語" -> "ja"
            else -> "es"
        },
    )
    return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
}

/**
 * Workout payloads store the stable iOS enum value (for example `fullBody`),
 * while the iOS views render its `englishName` through localization. Never
 * expose that storage key in the Android card or detail header.
 */
@Composable
fun localizedWorkoutLabel(rawValue: String, appLanguage: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val canonical = canonicalWorkoutLabelKey(rawValue)
    val fallback = workoutEnglishLabel(canonical)
    val resourceName = workoutLabelResourceName(canonical)
    return remember(rawValue, appLanguage, configuration) {
        val localizedConfiguration = Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(workoutLanguageTag(appLanguage)))
        }
        val resources = context.createConfigurationContext(localizedConfiguration).resources
        val resourceId = resourceName?.let { resources.getIdentifier(it, "string", context.packageName) } ?: 0
        resourceId.takeIf { it != 0 }?.let(resources::getString) ?: fallback
    }
}

@Composable
private fun localizedWorkoutCatalogText(hash: String, fallback: String, appLanguage: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(hash, fallback, appLanguage, configuration) {
        val localizedConfiguration = Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(workoutLanguageTag(appLanguage)))
        }
        val resources = context.createConfigurationContext(localizedConfiguration).resources
        val resourceId = resources.getIdentifier("ios_catalog_$hash", "string", context.packageName)
        resourceId.takeIf { it != 0 }?.let(resources::getString) ?: fallback
    }
}

internal fun canonicalWorkoutLabelKey(value: String): String = when (
    value.trim().lowercase(Locale.ROOT)
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
) {
    "fullbody", "cuerpocompleto" -> "fullBody"
    "upperbody", "trensuperior" -> "upperBody"
    "lowerbody", "treninferior" -> "lowerBody"
    "push", "empuje" -> "push"
    "pull", "tiron", "tirón" -> "pull"
    "legs", "piernas" -> "legs"
    "core" -> "core"
    "cardio" -> "cardio"
    "mobility", "movilidad" -> "mobility"
    "recovery", "recuperacion", "recuperación" -> "recovery"
    "strength", "fuerza" -> "strength"
    "hypertrophy", "hipertrofia" -> "hypertrophy"
    "technique", "tecnica", "técnica" -> "technique"
    "volume", "volumen" -> "volume"
    "deload", "descarga" -> "deload"
    "conditioning", "acondicionamiento" -> "conditioning"
    "loseweight", "perderpeso" -> "loseWeight"
    "buildmuscle", "ganarmusculo", "ganarmúsculo" -> "buildMuscle"
    "gainstrength", "ganarfuerza" -> "gainStrength"
    "improveendurance", "mejorarlaresistencia" -> "improveEndurance"
    "improvemobility", "mejorarlamovilidad" -> "improveMobility"
    "bodyrecomposition", "recomposicioncorporal", "recomposicióncorporal" -> "bodyRecomposition"
    "draft", "borrador" -> "draft"
    "active", "activo" -> "active"
    "completed", "completado", "completada" -> "completed"
    "archived", "archivado", "archivada" -> "archived"
    "startdate", "fechadeinicio" -> "startDate"
    else -> value.trim()
}

private fun workoutEnglishLabel(canonical: String): String = when (canonical) {
    "fullBody" -> "Full Body"
    "upperBody" -> "Upper Body"
    "lowerBody" -> "Lower Body"
    "push" -> "Push"
    "pull" -> "Pull"
    "legs" -> "Legs"
    "core" -> "Core"
    "cardio" -> "Cardio"
    "mobility" -> "Mobility"
    "recovery" -> "Recovery"
    "strength" -> "Strength"
    "hypertrophy" -> "Hypertrophy"
    "technique" -> "Technique"
    "volume" -> "Volume"
    "deload" -> "Deload"
    "conditioning" -> "Conditioning"
    "loseWeight" -> "Lose Weight"
    "buildMuscle" -> "Build Muscle"
    "gainStrength" -> "Gain Strength"
    "improveEndurance" -> "Improve Endurance"
    "improveMobility" -> "Improve Mobility"
    "bodyRecomposition" -> "Body Recomposition"
    "draft" -> "Draft"
    "active" -> "Active"
    "completed" -> "Completed"
    "archived" -> "Archived"
    "startDate" -> "Start date"
    else -> canonical
}

private fun workoutLabelResourceName(canonical: String): String? = when (canonical) {
    "fullBody" -> "ios_catalog_a54b18ef18c61ad3"
    "upperBody" -> "ios_catalog_b34453338002924c"
    "lowerBody" -> "ios_catalog_ced7f4270522aa64"
    "push" -> "ios_catalog_731ce7ed808913b2"
    "pull" -> "ios_catalog_b66d044181a0e296"
    "legs" -> "ios_catalog_38557594d0fce7fe"
    "core" -> "ios_catalog_70ea1983c983deac"
    "cardio" -> "ios_catalog_d67148a9c39d88d0"
    "mobility" -> "ios_catalog_e8f000eec8570e3b"
    "recovery" -> "ios_catalog_48f6a8d5688b0cf5"
    "strength" -> "ios_catalog_63ee3a1b965a7bd2"
    "volume" -> "ios_catalog_b10fb966d72063f5"
    "loseWeight" -> "ios_catalog_37863c9d5d8c70c7"
    "buildMuscle" -> "ios_catalog_365f6737d6298d3c"
    "gainStrength" -> "ios_catalog_777451d1a0293a42"
    "improveEndurance" -> "ios_catalog_7ea7d1954f278312"
    "improveMobility" -> "ios_catalog_41be78122a4d85ab"
    "bodyRecomposition" -> "ios_catalog_5b24f8d6c1fd8e35"
    "draft" -> "ios_catalog_ebf12ef47cf575b3"
    "active" -> "ios_catalog_92340695899bd2d8"
    "completed" -> "ios_catalog_22a970d2e5b1cc23"
    "archived" -> "ios_catalog_bdb86505f8062d15"
    "startDate" -> "ios_catalog_8169693101a4536c"
    else -> null
}

private fun workoutLanguageTag(language: String): String = when (language) {
    "English" -> "en"
    "Català" -> "ca"
    "Français" -> "fr"
    "Italiano" -> "it"
    "Português" -> "pt"
    "Deutsch" -> "de"
    "中文（简体）" -> "zh-Hans"
    "Nederlands" -> "nl"
    "日本語" -> "ja"
    else -> "es"
}

@Composable
private fun workoutCardStatus(status: WorkoutStatus, isInProgress: Boolean): Pair<String, Color>? = when (status) {
    WorkoutStatus.Completed -> "COMPLETADO" to Color(0xFF26A269)
    WorkoutStatus.Skipped -> "OMITIDO" to WildforceThemeTokens.textSecondary
    WorkoutStatus.Draft -> "BORRADOR" to WildforceThemeTokens.textSecondary
    WorkoutStatus.InProgress -> "EN CURSO" to Color(0xFFF28A29)
    WorkoutStatus.Planned -> if (isInProgress) "EN CURSO" to Color(0xFFF28A29) else null
}

@Composable
private fun workoutDetailStatus(status: WorkoutStatus): Pair<String, Color>? = when (status) {
    WorkoutStatus.Completed -> "COMPLETADO" to Color(0xFF26A269)
    WorkoutStatus.Skipped -> "OMITIDO" to WildforceThemeTokens.textSecondary
    WorkoutStatus.Draft -> "BORRADOR" to WildforceThemeTokens.textSecondary
    WorkoutStatus.InProgress -> "EN CURSO" to Color(0xFFF28A29)
    WorkoutStatus.Planned -> null
}

@Composable
private fun WorkoutCardMetadata(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.White.copy(alpha = 0.82f),
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = iconTint)
        Text(
            label,
            Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.caption,
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun workoutFocusIcon(focus: String): ImageVector = when (focus.lowercase(Locale.ROOT)) {
    "empuje", "push" -> Icons.Filled.ArrowCircleUp
    "tirón", "tiron", "pull" -> Icons.Filled.ArrowCircleDown
    "piernas", "legs" -> Icons.Filled.DirectionsWalk
    "movilidad", "mobility" -> Icons.Filled.SelfImprovement
    "recuperación", "recuperacion", "recovery" -> Icons.Filled.Hotel
    "cardio" -> Icons.Filled.Favorite
    else -> Icons.Filled.CenterFocusStrong // iOS scope
}

private fun workoutDayTypeIcon(dayType: String): ImageVector = when (dayType.lowercase(Locale.ROOT)) {
    "fuerza", "strength" -> Icons.Filled.Bolt // iOS bolt.fill
    "hipertrofia", "hypertrophy" -> Icons.Filled.FitnessCenter
    "técnica", "tecnica", "technique" -> Icons.Filled.CenterFocusStrong // iOS scope
    "volumen", "volume" -> Icons.Filled.Layers
    "descarga", "deload" -> Icons.Filled.ArrowCircleDown
    "recuperación", "recuperacion", "recovery" -> Icons.Filled.Favorite
    "acondicionamiento", "conditioning" -> Icons.Filled.Timeline
    else -> Icons.Filled.FitnessCenter
}

@Composable
private fun StatusBadge(label: String, background: Color) {
    var appeared by remember(label) { mutableStateOf(false) }
    LaunchedEffect(label) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else .8f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 560f),
        label = "workout-status-badge-$label",
    )
    Text(
        label,
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (appeared) 1f else 0f }
            .clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 2.dp),
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
        Modifier.fillMaxWidth()
            .liquidGlass(RoundedCornerShape(16.dp))
            .semantics { contentDescription = "Día de descanso. No hay entrenamiento planificado. Úsalo para recuperarte, estirar o dar un paseo ligero." }
            .padding(vertical = 20.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(80.dp), tint = WildforceThemeTokens.textSecondary.copy(alpha = 0.72f))
        }
        Text("Día de descanso", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
        Text("No hay entrenamiento planificado. Úsalo para recuperarte, estirar o dar un paseo ligero.", color = WildforceThemeTokens.textSecondary)
    }
}

private enum class WorkoutDetailTab(val label: String) {
    Exercises("EJERCICIOS"),
    Information("INFORMACIÓN"),
}

private enum class ExercisePickerMode { Add, Swap }

/** Cancelling must also release any session-scoped integrations. */
private fun cancelWorkoutSession(context: android.content.Context, workoutId: String) {
    // Clear queued notification actions first so they cannot be applied to the
    // next workout while the foreground service is shutting down.
    WorkoutNotificationActionStore.clear(context)
    WorkoutSessionStore.clear(context, workoutId)
    WorkoutActiveNotification.cancelRestFinished(context)
    WorkoutActiveNotification.cancel(context)
    context.stopService(android.content.Intent(context, WorkoutForegroundService::class.java))
    WildforceWatchLocalBridge.stop(context)
}

/** Public completed-workout card, matching iOS's `workoutDayShareURL`. */
internal fun completedWorkoutShareId(context: android.content.Context, workoutId: String): String? {
    if (runCatching { java.util.UUID.fromString(workoutId) }.isSuccess) return workoutId
    val planId = context.getSharedPreferences("wildforce_remote_sync", android.content.Context.MODE_PRIVATE)
        .getString("remote_workout_plan_id", null) ?: return null
    return java.util.UUID.nameUUIDFromBytes("$planId:$workoutId".toByteArray()).toString()
}

private fun shareCompletedWorkout(context: android.content.Context, workoutId: String) {
    val remoteId = completedWorkoutShareId(context, workoutId) ?: return
    val baseUrl = io.codepassion.doubletriangle.core.model.WildforceApiEnvironment.apiBaseUrl.removeSuffix("/api")
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, "$baseUrl/workout-days/$remoteId/share")
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Compartir entrenamiento"))
}

@Preview(showBackground = true)
@Composable
private fun WorkoutHubPreview() = WildforceTheme { WorkoutHubScreen(PaddingValues()) }

@Composable
private fun AdaptWorkoutLocationDialog(
    workout: WorkoutDaySummary,
    equipmentPresets: List<Pair<String, String>>,
    equipmentPresetIds: List<String>,
    onDismiss: () -> Unit,
    onGenerate: (CustomWorkoutRequest) -> Unit,
) {
    var selectedIndex by remember(equipmentPresets) { mutableStateOf(0) }
    val selected = equipmentPresets.getOrNull(selectedIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADAPTAR ENTRENAMIENTO", fontFamily = AntonFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Adapta esta sesión al equipamiento disponible en otra ubicación.", color = WildforceThemeTokens.textSecondary)
                if (equipmentPresets.isEmpty()) {
                    Text("Añade una ubicación de entrenamiento en tu perfil antes de adaptar esta sesión.", color = WildforceThemeTokens.textSecondary)
                } else {
                    Text("UBICACIÓN DE ENTRENAMIENTO", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                    equipmentPresets.forEachIndexed { index, (name, equipment) ->
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (index == selectedIndex) WildforceThemeTokens.accent.copy(alpha = 0.16f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f))
                                .semantics { contentDescription = name }
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(name, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(equipment.ifBlank { "Peso corporal" }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = {
                    val (_, equipment) = selected ?: return@TextButton
                    onGenerate(CustomWorkoutRequest(
                        focus = workout.focus,
                        durationMinutes = workout.estimatedMinutes,
                        equipment = equipment,
                        trainingLocationId = equipmentPresetIds.getOrNull(selectedIndex)?.takeIf(String::isNotBlank),
                    ))
                },
            ) { Text("GENERAR", color = WildforceThemeTokens.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
    )
}

@Composable
fun WorkoutDetailScreen(workout: WorkoutDaySummary, gender: String, useImperial: Boolean = false, showWearStartOption: Boolean = false, appLanguage: String = "Español", mesocyclePhase: io.codepassion.doubletriangle.core.model.MesocyclePhase? = null, onBack: () -> Unit, onStart: (Boolean) -> Unit, onSkip: () -> Unit = {}, onUnskip: () -> Unit = {}, onWorkoutUpdated: (WorkoutDaySummary) -> Unit = {}, adaptEquipmentPresets: List<Pair<String, String>> = emptyList(), adaptEquipmentPresetIds: List<String> = emptyList(), adaptAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null, canUseAiCoach: Boolean = true, onShowAiCoachOffer: () -> Unit = {}) {
    val detailHeroHeight = (LocalConfiguration.current.screenHeightDp * 0.43f).dp.coerceIn(330.dp, 430.dp)
    val focusLabel = localizedWorkoutLabel(workout.focus, appLanguage)
    val dayTypeLabel = localizedWorkoutLabel(workout.dayType, appLanguage)
    var collapsedBlocks by remember(workout.id) { mutableStateOf(emptySet<Int>()) }
    val detailContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var hasSavedSession by remember(workout.id) { mutableStateOf(WorkoutSessionStore.load(detailContext, workout.id) != null) }
    var guideExercise by remember { mutableStateOf<ExerciseSummary?>(null) }
    var startingWorkout by remember(workout.id) { mutableStateOf(false) }
    var startOnWear by remember(workout.id) { mutableStateOf(false) }
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
    var skipAppeared by remember(workout.id, skipped) { mutableStateOf(false) }
    var selectedTab by remember(workout.id) { mutableStateOf(WorkoutDetailTab.Exercises) }
    LaunchedEffect(skipped) {
        if (skipped) skipAppeared = true
    }
    if (showingEditor) {
        CustomWorkoutEditorScreen(
            initial = workout,
            gender = gender,
            appLanguage = appLanguage,
            onCancel = { _ -> showingEditor = false },
            onSave = { updated -> showingEditor = false; onWorkoutUpdated(updated) },
        )
        return
    }
    if (showingAdaptation) {
        AdaptWorkoutLocationDialog(
            equipmentPresets = adaptEquipmentPresets,
            equipmentPresetIds = adaptEquipmentPresetIds,
            workout = workout,
            onDismiss = { showingAdaptation = false },
            onGenerate = { request ->
                showingAdaptation = false
                if (!canUseAiCoach) {
                    onShowAiCoachOffer()
                    return@AdaptWorkoutLocationDialog
                }
                val generator = adaptAiGenerator ?: return@AdaptWorkoutLocationDialog
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
        val skipIconScale by animateFloatAsState(
            targetValue = if (skipAppeared) 1f else .68f,
            animationSpec = spring(dampingRatio = .58f, stiffness = 460f),
            label = "skipped-workout-icon",
        )
        Box(Modifier.fillMaxSize().liquidGlassBackground(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Row(
                    Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Volver" }.clickable(onClick = onBack).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                    Text("VOLVER", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.FastForward, contentDescription = null, modifier = Modifier.size(64.dp).graphicsLayer { scaleX = skipIconScale; scaleY = skipIconScale }, tint = WildforceThemeTokens.accent)
                Text("DÍA OMITIDO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text("Este entrenamiento se ha marcado como descanso.", Modifier.padding(top = 8.dp), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("DESHACER", Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Deshacer" }.clickable { skipped = false; onUnskip() }.padding(horizontal = 14.dp), color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
            }
        }
        return
    }
    Box(
        Modifier.fillMaxSize()
            .liquidGlassBackground()
            // Root navigation is rendered above this detail surface. Keep its
            // floating capsule outside the scroll viewport, as SwiftUI's tab
            // safe area does for WorkoutDayView.
            .padding(bottom = 96.dp),
    ) {
        RemoteTrainingImage(
            url = workoutCoverUrl(workout.focus, gender, workout.id),
            contentDescription = workout.title,
            modifier = Modifier.fillMaxWidth().height(detailHeroHeight),
        )
        Box(Modifier.fillMaxWidth().height(detailHeroHeight).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, WildforceThemeTokens.backgroundSecondary))))
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                // The root navigation is a floating overlay. Reserve its full
                // visual footprint so the last Information-tab rows remain
                // readable instead of sitting underneath the translucent bar.
                .padding(bottom = 200.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CenteredBackButton(onBack, 40.dp, 14.dp)
                Spacer(Modifier.weight(1f))
                if (workout.status != WorkoutStatus.Planned && workout.status != WorkoutStatus.InProgress) {
                    // WorkoutDayView places a non-planned status in the trailing
                    // header position. A completed pre-workout must therefore show
                    // its badge here, not between the day label and title.
                    workoutDetailStatus(workout.status)?.let { (label, color) ->
                        StatusBadge(label, color)
                    }
                    if (workout.status == WorkoutStatus.Completed && completedWorkoutShareId(detailContext, workout.id) != null) {
                        Text(
                            "COMPARTIR",
                            Modifier.heightIn(min = 48.dp).padding(start = 10.dp, end = 12.dp).semantics { contentDescription = "Compartir entrenamiento" }.clickable { shareCompletedWorkout(detailContext, workout.id) }.padding(10.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Box {
                        Box(
                            Modifier.size(48.dp).semantics { contentDescription = "Menú del entrenamiento" }.clickable { showMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.24f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MoreHoriz, contentDescription = "Menú del entrenamiento", tint = Color.White, modifier = Modifier.size(21.dp))
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            val canModify = workout.status == WorkoutStatus.Planned || workout.status == WorkoutStatus.InProgress || hasSavedSession
                            if (canModify) {
                                ActiveWorkoutMenuItem("＋", "Añadir ejercicio") { showMenu = false; showingEditor = true }
                                ActiveWorkoutMenuItem("⏭", "Saltar entrenamiento") { showMenu = false; showSkipConfirmation = true }
                                if (adaptAiGenerator != null) ActiveWorkoutMenuItem("🏢", "Adaptar a otra ubicación") { showMenu = false; showingAdaptation = true }
                            }
                            if (hasSavedSession || workout.status == WorkoutStatus.InProgress) {
                                Divider()
                                ActiveWorkoutMenuItem("cancel", "Cancelar entrenamiento", destructive = true) { showMenu = false; showCancelConfirmation = true }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
                Text("DÍA ${workout.order}", fontFamily = AntonFontFamily, fontSize = 38.sp, color = Color.White, maxLines = 1)
                // WorkoutDayView.swift uses an 8 pt VStack gap between the
                // day label and the workout title.
                Text(
                    workout.title.uppercase(),
                    Modifier.padding(top = 8.dp),
                    fontFamily = AntonFontFamily,
                    fontSize = 24.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WorkoutDetailMetadataPill(workoutFocusIcon(workout.focus), focusLabel)
                    WorkoutDetailMetadataPill(workoutDayTypeIcon(workout.dayType), dayTypeLabel)
                    mesocyclePhase?.let { phase ->
                        WorkoutDetailMetadataPill(phaseIcon(phase), phase.label)
                    }
                    WorkoutDetailMetadataPill(Icons.Filled.AccessTime, "${workout.estimatedMinutes} min")
                }
            }
            // WorkoutDayView on iOS keeps the content as one continuous
            // scroll surface below the hero. It is not a floating bottom
            // sheet: that distinction keeps the action, segmented control and
            // exercise prescription in the same information hierarchy.
            // The iOS content surface starts immediately after the header metadata.
            // Retaining only a small rhythm gap keeps this panel visibly higher.
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary)) {
                if (workout.status == WorkoutStatus.Completed) {
                    CompletedWorkoutMetrics(workout, useImperial)
                } else {
                    if (showWearStartOption) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("EMPEZAR EN WEAR OS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                Text("Abrir el entrenamiento en el reloj conectado", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            Switch(checked = startOnWear, onCheckedChange = { startOnWear = it }, enabled = !startingWorkout)
                        }
                    }
                    Button(onClick = { if (!startingWorkout) { startingWorkout = true; onStart(startOnWear) } }, enabled = !startingWorkout, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp)) {
                        Text(if (startingWorkout) "ABRIENDO ENTRENAMIENTO…" else if (hasSavedSession || workout.status == WorkoutStatus.InProgress) "REANUDAR ENTRENAMIENTO" else "EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                    }
                }
                Divider(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))
                WorkoutDetailTabSelector(selected = selectedTab, onSelected = { selectedTab = it }, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(18.dp))
                // Mirrors ExerciseDetailsView's opacity-and-scale transition
                // between information panels instead of replacing the body abruptly.
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = .98f, animationSpec = tween(220))) togetherWith
                            (fadeOut(tween(120)) + scaleOut(targetScale = .98f, animationSpec = tween(160)))
                    },
                    label = "workout-detail-tab",
                ) { tab ->
                if (tab == WorkoutDetailTab.Information) {
                    Column(Modifier.padding(horizontal = 12.dp)) { WorkoutInformationTab(workout, appLanguage) }
                } else {
                Column(Modifier.padding(horizontal = 12.dp)) {

                workout.displayBlocks().forEachIndexed { blockIndex, block ->
                    val collapsible = block.type == WorkoutBlockType.Warmup || block.type == WorkoutBlockType.Cooldown
                    val collapsed = blockIndex in collapsedBlocks
                    // iOS does not render an artificial “Main block” heading for
                    // standard exercises. Headers are reserved for warmup,
                    // cooldown and supersets.
                    val showHeader = block.type != WorkoutBlockType.Standard
                    if (showHeader) {
                        Column(
                            Modifier.fillMaxWidth().semantics { contentDescription = "${block.type.label}, ${if (collapsed) "plegado" else "desplegado"}" }.clickable(enabled = collapsible) {
                                collapsedBlocks = if (collapsed) collapsedBlocks - blockIndex else collapsedBlocks + blockIndex
                            }.padding(top = 14.dp, bottom = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (collapsible) Icon(if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (collapsed) "Expandir" else "Contraer", modifier = Modifier.padding(end = 8.dp), tint = WildforceThemeTokens.textSecondary)
                                when (block.type) {
                                    WorkoutBlockType.Warmup -> Box(
                                        Modifier.size(width = 4.dp, height = 24.dp).clip(RoundedCornerShape(3.dp)).background(WildforceThemeTokens.warmupAccent),
                                    )
                                    WorkoutBlockType.Cooldown -> Box(
                                        Modifier.size(width = 4.dp, height = 24.dp).clip(RoundedCornerShape(3.dp)).background(WildforceThemeTokens.cooldownAccent),
                                    )
                                    WorkoutBlockType.Standard, WorkoutBlockType.Superset -> Unit
                                }
                                Text(
                                    block.type.label.uppercase(),
                                    Modifier.padding(start = if (block.type == WorkoutBlockType.Warmup || block.type == WorkoutBlockType.Cooldown) 10.dp else 0.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = WildforceThemeTokens.textPrimary,
                                )
                                if (block.rounds > 1) {
                                    Text("${block.rounds} RONDAS", Modifier.padding(start = 9.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                }
                            }
                            block.notes?.let { Text(it, Modifier.padding(top = 5.dp, start = if (collapsible) 22.dp else 14.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                        }
                    }
                    if (!collapsed) {
                        block.notes?.takeIf { block.type == WorkoutBlockType.Standard && it.isNotBlank() }?.let { notes ->
                            Text(
                                notes,
                                Modifier.padding(top = 8.dp, bottom = 4.dp, start = 12.dp),
                                style = MaterialTheme.typography.caption,
                                color = WildforceThemeTokens.textSecondary,
                            )
                        }
                        block.exercises.forEachIndexed { exerciseIndex, exercise ->
                            val blockAccent = when (block.type) {
                                WorkoutBlockType.Warmup -> WildforceThemeTokens.warmupAccent
                                WorkoutBlockType.Cooldown -> WildforceThemeTokens.cooldownAccent
                                WorkoutBlockType.Superset -> WildforceThemeTokens.accentRed
                                WorkoutBlockType.Standard -> Color.Transparent
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = if (block.type == WorkoutBlockType.Superset) 8.dp else 0.dp)
                                    // iOS navigates directly to ExerciseDetailsView.  Do the same
                                    // here instead of exposing a partial inline guide.
                                    .heightIn(min = 48.dp)
                                    .semantics { contentDescription = "Guía de ${exerciseDisplayName(exercise.imageKey, exercise.name)}" }
                                    .clickable { guideExercise = exercise }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.width(4.dp).height(60.dp).clip(RoundedCornerShape(4.dp)).background(blockAccent),
                                )
                                RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), null, Modifier.padding(start = 12.dp).size(68.dp).clip(RoundedCornerShape(10.dp)))
                                Column(Modifier.weight(1f).padding(start = 16.dp, end = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (block.type == WorkoutBlockType.Superset) {
                                            Text(
                                                "A${exerciseIndex + 1}",
                                                Modifier.background(WildforceThemeTokens.accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = WildforceThemeTokens.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.caption,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(
                                            exerciseDisplayName(exercise.imageKey, exercise.name),
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WildforceThemeTokens.textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    WorkoutExercisePrescription(
                                        exercise = exercise,
                                        setCount = if (block.type == WorkoutBlockType.Superset) block.rounds else exercise.sets,
                                        useImperial = useImperial,
                                    )
                                }
                                Icon(Icons.Filled.MoreHoriz, contentDescription = "Más opciones", tint = WildforceThemeTokens.textPrimary)
                            }
                            if (exerciseIndex < block.exercises.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.14f)))
                            }
                        }
                        block.restAfterBlockSeconds?.let { rest ->
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textSecondary)
                                Text("Descansa ${rest}s al terminar el bloque", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                }
                }
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
                    cancelWorkoutSession(detailContext, workout.id)
                    hasSavedSession = false
                    onWorkoutUpdated(
                        workout.resetCancelledWorkout(),
                    )
                    showCancelConfirmation = false
                }) { Text("CANCELAR ENTRENAMIENTO", color = Color(0xFFC62828)) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirmation = false }) { Text("SEGUIR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
    if (adapting) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { }, contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = WildforceThemeTokens.accent)
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
            }) { Text("REINTENTAR", color = WildforceThemeTokens.accent) }
        }, dismissButton = { TextButton(onClick = { adaptationError = null }) { Text("CERRAR", color = WildforceThemeTokens.textSecondary) } })
    }
    pendingAdapted?.let { adapted ->
        AlertDialog(onDismissRequest = { pendingAdapted = null }, title = { Text("ENTRENAMIENTO ADAPTADO", fontFamily = AntonFontFamily) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Revisa los cambios antes de sustituir la sesión actual.", color = WildforceThemeTokens.textSecondary)
                Text(adapted.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("${adapted.exercises.size} ejercicios · ${adapted.estimatedMinutes} min", color = WildforceThemeTokens.textSecondary)
                adapted.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    Text(notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                adapted.editableBlocks().forEach { block ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(block.type.label.uppercase(), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
                        block.exercises.forEachIndexed { index, exercise ->
                            Text(
                                "${index + 1}. ${exerciseDisplayName(exercise.imageKey, exercise.name)} · ${restTimerExercisePrescription(exercise, false).joinToString(" · ")}",
                                style = MaterialTheme.typography.caption,
                                color = WildforceThemeTokens.textPrimary,
                            )
                        }
                    }
                }
                Text("La sesión original se conservará hasta confirmar.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accent)
            }
        }, confirmButton = { TextButton(onClick = { pendingAdapted = null; onWorkoutUpdated(adapted) }) { Text("APLICAR", color = WildforceThemeTokens.accent) } }, dismissButton = { TextButton(onClick = { pendingAdapted = null }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } })
    }
}

@Composable
private fun CompletedWorkoutMetrics(workout: WorkoutDaySummary, useImperial: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val metrics = remember(workout.id) { WorkoutCompletionMetricsStore.load(context, workout.id) }
    val fallbackDurationSeconds = remember(workout.startedAt, workout.completedAt) {
        runCatching {
            if (workout.startedAt != null && workout.completedAt != null) {
                java.time.Duration.between(
                    java.time.Instant.parse(workout.startedAt),
                    java.time.Instant.parse(workout.completedAt),
                ).seconds.toInt().coerceAtLeast(0)
            } else {
                0
            }
        }.getOrDefault(0)
    }
    val durationSeconds = metrics?.activeDurationSeconds ?: workout.activeDurationSeconds ?: fallbackDurationSeconds
    val calories = metrics?.activeCaloriesBurned ?: workout.activeCaloriesBurned
    val volumeKg = metrics?.totalVolumeKg ?: workout.totalVolumeKg
    val completedDate = workout.completedAt?.let { raw ->
        runCatching { java.util.Date.from(java.time.Instant.parse(raw)) }.getOrNull()
    } ?: metrics?.completedAtMillis?.let(::Date)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF26A269), modifier = Modifier.size(18.dp))
            Text(
                "ENTRENAMIENTO COMPLETADO",
                Modifier.padding(start = 8.dp),
                color = WildforceThemeTokens.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            completedDate?.let {
                Spacer(Modifier.weight(1f))
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocalLocale.current.platformLocale)
                        .format(it),
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompletedWorkoutMetric("DURACIÓN", "${durationSeconds / 60} min", Icons.Filled.Timer, Color(0xFF42A5F5), Modifier.weight(1f))
            calories?.let {
                CompletedWorkoutMetric("CALORÍAS", "${it.toInt()}", Icons.Filled.LocalFireDepartment, Color(0xFFFF9800), Modifier.weight(1f))
            }
            volumeKg?.takeIf { it > 0 }?.let {
                val volume = if (useImperial) it * KG_TO_LB else it
                CompletedWorkoutMetric("VOLUMEN", String.format(LocalLocale.current.platformLocale, "%.0f %s", volume, if (useImperial) "lb" else "kg"), Icons.Filled.Scale, Color(0xFFAB47BC), Modifier.weight(1f))
            }
        }
        if ((workout.averageHeartRate ?: 0.0) > 0 || (workout.maximumHeartRate ?: 0.0) > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                workout.averageHeartRate?.takeIf { it > 0 }?.let {
                    CompletedWorkoutMetric("FC MEDIA", "${it.roundToInt()}", Icons.Filled.Favorite, Color(0xFFE91E63), Modifier.weight(1f))
                }
                workout.maximumHeartRate?.takeIf { it > 0 }?.let {
                    CompletedWorkoutMetric("FC MÁXIMA", "${it.roundToInt()}", Icons.Filled.Timeline, Color(0xFFE91E63), Modifier.weight(1f))
                }
            }
        }
        if (calories != null) {
            Text(
                "Calorías estimadas sin wearable a partir de tu perfil, duración e intensidad de los ejercicios.",
                style = MaterialTheme.typography.caption,
                color = WildforceThemeTokens.textSecondary,
            )
        }
    }
}

@Composable
private fun CompletedWorkoutMetric(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(WildforceThemeTokens.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(value, Modifier.padding(top = 5.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title, style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PerformanceContextChip(label: String, value: String) {
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.imageControlBackground)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (label == "PR") Icons.Filled.EmojiEvents else Icons.Filled.ArrowCircleUp,
            contentDescription = null,
            // The iOS header keeps both record symbols neutral; the pill itself
            // provides enough context without turning the PR trophy gold.
            tint = WildforceThemeTokens.imageControlContent,
            modifier = Modifier.size(18.dp),
        )
        Text("$label: $value", Modifier.padding(start = 6.dp), color = WildforceThemeTokens.imageControlContent, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ActiveWorkoutMenuItem(
    glyph: String,
    label: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(onClick = onClick, enabled = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    menuIcon(glyph),
                    contentDescription = null,
                    tint = if (!enabled) WildforceThemeTokens.textSecondary.copy(alpha = 0.5f) else if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(label, color = if (!enabled) WildforceThemeTokens.textSecondary.copy(alpha = 0.5f) else if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary)
        }
    }
}

private fun menuIcon(glyph: String): ImageVector = when (glyph) {
    "＋", "+" -> Icons.Filled.Add
    "↔" -> Icons.Filled.SwapHoriz
    "↓" -> Icons.Filled.ArrowDownward
    "↑" -> Icons.Filled.ArrowUpward
    "⛓" -> Icons.Filled.Link
    "□" -> Icons.Filled.ViewColumn
    "−" -> Icons.Filled.RemoveCircleOutline
    "round+" -> Icons.Filled.AddCircleOutline
    "round-" -> Icons.Filled.RemoveCircleOutline
    "◷" -> Icons.Filled.Timer
    "⏭" -> Icons.Filled.FastForward
    "🏢" -> Icons.Filled.Business
    "×" -> Icons.Filled.Delete
    "cancel" -> Icons.Filled.Close
    else -> Icons.Filled.Info
}

@Composable
private fun ActiveWorkoutToolbarAction(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = WildforceThemeTokens.textPrimary,
        )
    }
}

@Composable
private fun WorkoutDetailMetadataPill(icon: ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.82f))
        Text(value, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Mirrors PlannedExercicePrescriptionView.swift: each typed target is shown
 * in its own metric, rather than deriving the type from the display string. */
@Composable
private fun WorkoutExercisePrescription(exercise: ExerciseSummary, setCount: Int, useImperial: Boolean) {
    val primaryMetrics = buildList {
        if (setCount > 0) add("▦ $setCount ${if (setCount == 1) "serie" else "series"}")
        when (exercise.trackingMode) {
            io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> {
                val reps = exercise.repRange?.displayText
                    ?: exercise.targetRepsPerSet?.takeIf { it.isNotEmpty() }?.let { targets ->
                        val low = targets.minOrNull() ?: 0
                        val high = targets.maxOrNull() ?: low
                        if (low == high) low.toString() else "$low-$high"
                    }
                    ?: exercise.reps
                add("# $reps reps")
            }
            io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration,
            io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> {
                exercise.targetDurationMinutes?.takeIf { it > 0 }?.let { add("$it min") }
                exercise.targetDurationSeconds?.takeIf { it > 0 }?.let { add("$it s") }
            }
        }
        val weight = exercise.targetWeightKg ?: exercise.targetWeightsKg?.maxOrNull()
        weight?.takeIf { it > 0.0 }?.let { add(formatTrainingWeight(it, useImperial)) }
        exercise.targetDistanceKm?.takeIf { it > 0.0 }?.let { kilometers ->
            val distance = if (useImperial) kilometers * 0.621371 else kilometers
            add("${"%.1f".format(Locale.US, distance)} ${if (useImperial) "mi" else "km"}")
        }
    }
    if (primaryMetrics.isNotEmpty()) {
        Text(primaryMetrics.joinToString("   "), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    exercise.targetPaceSecondsPerKm?.takeIf { it > 0 }?.let { pace ->
        val seconds = (if (useImperial) pace / 0.621371 else pace.toDouble()).roundToInt().coerceAtLeast(0)
        Text("Ritmo ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} /${if (useImperial) "mi" else "km"}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (exercise.hasNonDefaultSetStyle()) {
        val styleScope = if (exercise.setStyleParameters.appliesToFinalSetOnly) " · solo última serie" else ""
        Text("= ${exercise.setStyle.label}$styleScope", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun workoutExerciseObjective(exercise: ExerciseSummary, useImperial: Boolean): String = when (exercise.trackingMode) {
    io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> {
        val reps = exercise.repRange?.displayText ?: exercise.targetRepsPerSet?.takeIf { it.isNotEmpty() }?.joinToString("/") ?: exercise.reps
        "$reps repeticiones"
    }
    io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration -> exercise.targetDurationMinutes?.let { "$it min" }
        ?: exercise.targetDurationSeconds?.let { "$it s" } ?: exercise.reps
    io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> {
        val duration = exercise.targetDurationMinutes?.let { "$it min" } ?: exercise.targetDurationSeconds?.let { "$it s" } ?: exercise.reps
        val distance = exercise.targetDistanceKm?.let { if (useImperial) "${"%.1f".format(Locale.US, it * 0.621371)} mi" else "${"%.1f".format(Locale.US, it)} km" }
        listOfNotNull(duration, distance).joinToString(" · ")
    }
}

@Composable
private fun WorkoutDetailTabSelector(selected: WorkoutDetailTab, onSelected: (WorkoutDetailTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.12f)).padding(4.dp)) {
        WorkoutDetailTab.entries.forEach { tab ->
            val active = tab == selected
            Text(
                tab.label,
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) WildforceThemeTokens.textPrimary else Color.Transparent)
                    .semantics {
                        contentDescription = tab.label
                        this.selected = active
                    }
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
private fun WorkoutInformationTab(workout: WorkoutDaySummary, appLanguage: String) {
    // Keep this companion location in sync with iOS's Information tab as well.
    WorkoutMuscleEffort(workout)
    Spacer(Modifier.height(24.dp))
    if (workout.displayBlocks().flatMap { it.exercises }.any { ExerciseVisualCatalog.equipmentVisualsFor(it.imageKey).isNotEmpty() }) {
        WorkoutEquipment(workout)
        Spacer(Modifier.height(24.dp))
    }
    workout.notes?.trim()?.takeIf { it.isNotBlank() }?.let { notes ->
        Text("NOTAS", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
        Text(notes, Modifier.padding(top = 9.dp), color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.height(18.dp))
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(workoutDayTypeIcon(workout.dayType), contentDescription = null, tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(18.dp))
        Text(
            localizedWorkoutLabel(workout.dayType, appLanguage),
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.body2,
        )
    }
}

private fun blockIcon(type: WorkoutBlockType): String = when (type) {
    WorkoutBlockType.Warmup -> "◌"
    WorkoutBlockType.Standard -> "≡"
    WorkoutBlockType.Superset -> "⛓"
    WorkoutBlockType.Cooldown -> "↓"
}

private fun watchMotionStatusTitle(statusCode: Int): String = when (statusCode) {
    1 -> "Repeticiones manuales"
    2 -> "Ejercicio manual en el reloj"
    3 -> "Listo para detectar repeticiones"
    4 -> "Repeticiones automáticas"
    5 -> "Confianza baja"
    else -> "Fallback manual"
}

private fun watchMotionStatusSubtitle(snapshot: WatchMotionSnapshotData): String = when (snapshot.statusCode) {
    1 -> "El seguimiento automático está desactivado"
    2 -> "Este ejercicio se registra manualmente en el reloj"
    3 -> "Empieza a moverte para capturar repeticiones"
    4 -> "${snapshot.autoDetectedRepCount} detectadas automáticamente"
    5 -> "Valida las repeticiones manualmente"
    else -> "Movimiento no disponible; se usan repeticiones manuales"
}

@Composable
private fun WatchMotionInsightsSection(snapshot: WatchMotionSnapshotData) {
    if (!snapshot.isVisible) return
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("REP INSIGHTS", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
        Text(watchMotionStatusTitle(snapshot.statusCode), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        Text(watchMotionStatusSubtitle(snapshot), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        if (snapshot.reps.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.reps.takeLast(8).forEachIndexed { index, rep ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            Modifier.width(10.dp).height(42.dp).clip(RoundedCornerShape(50)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .height((rep.rangeOfMotionPermille.coerceIn(80, 1_000) / 1_000f * 38f).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (rep.velocityCode == 0) Color.Red.copy(alpha = 0.72f) else if (rep.velocityCode == 2) Color.Yellow.copy(alpha = 0.82f) else WildforceThemeTokens.accentGold),
                            )
                        }
                        Text("${index + 1}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutScreen(
    initialWorkout: WorkoutDaySummary,
    gender: String,
    userBmr: Double = 1750.0,
    userAgeYears: Int = 30,
    useImperial: Boolean = false,
    skipRestPeriods: Boolean = false,
    currentStreak: Int = 0,
    isPlanCompletedAfterWorkout: Boolean = false,
    isMesocycleCompletedAfterWorkout: Boolean = false,
    planName: String = "",
    completedPlanWorkouts: Int = 0,
    totalPlanExercises: Int = 0,
    mesocycleIndex: Int = 0,
    cycleLength: Int = 0,
    canGenerateNextPlan: Boolean = true,
    onGenerateNextPlan: () -> Unit = {},
    onWorkoutChanged: (WorkoutDaySummary) -> Unit = {},
    onShowExerciseGuide: (ExerciseSummary) -> Unit = {},
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double, streak: Int, completedOn: LocalDate, skippedExerciseIndices: Set<Int>) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Load the session before creating the local workout value so edits made
    // from the path/menu (add, swap, move, remove, superset changes) survive
    // process recreation just like the set progress does.
    val restored = remember(initialWorkout.id) { WorkoutSessionStore.load(context, initialWorkout.id) }
    var workout by remember(initialWorkout.id) {
        mutableStateOf(restored?.editableBlocks?.let { local ->
            initialWorkout.withEditableBlocks(reconcileImportedSkips(local, initialWorkout.editableBlocks()))
        } ?: initialWorkout)
    }
    LaunchedEffect(workout) { onWorkoutChanged(workout) }
    DisposableEffect(workout.id) {
        val notificationsEnabled = WorkoutNotificationPreferences.enabled(context)
        val serviceIntent = android.content.Intent(context, WorkoutForegroundService::class.java)
            .putExtra("title", workout.title)
            .putExtra("detail", "Sesión activa")
            .putExtra("workoutId", workout.id)
            .putExtra("totalExercises", workout.exercises.size)
            .putExtra("currentExerciseHasTimer", workout.exercises.firstOrNull()?.resolvedTargetDurationSeconds() != null)
        android.util.Log.d("WorkoutHub", "Starting workout foreground service. Alerts enabled: $notificationsEnabled")
        if (notificationsEnabled) {
            runCatching { WorkoutActiveNotification.show(context, workout.title, exerciseIndex = 0, totalExercises = workout.exercises.size) }
        }
        // The foreground service owns recovery, timers and Resume while the
        // app is backgrounded. Alert preferences may suppress updates, but
        // must never disable the active workout itself.
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        }
        // Keep the native BLE bridge in the same lifecycle as the local
        // Amazfit/Zepp bridge. It is a no-op until Bluetooth access is granted.
        WildforceWatchBridge.start(context)
        WildforceWatchLocalBridge.start(context)
        onDispose {
            // Notification actions are scoped to this workout. Leaving one
            // behind lets a newly started foreground service replay it.
            WorkoutNotificationActionStore.clear(context)
            WorkoutActiveNotification.cancel(context)
            context.stopService(serviceIntent)
            WildforceWatchBridge.stop(context)
            WildforceWatchLocalBridge.stop(context)
        }
    }
    val initialSkipped = remember(workout.id) {
        workout.exercises.indices.filter { workout.exercises[it].skippedAt != null }.toSet() + restored?.skippedExerciseIndices.orEmpty()
    }
    val initialDiscardedRecords = remember(workout.id) { restored?.completedSetRecords.orEmpty().filter { it.exerciseIndex in initialSkipped } }
    val restoredStart = (restored?.exerciseIndex ?: 0).coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))
    val firstUnskipped = remember(workout.id) { firstUnskippedExecutionIndex(restoredStart, workout.exercises.size, initialSkipped) }
    val movedPastSkipped = restored != null && firstUnskipped != restoredStart
    var exerciseIndex by remember(workout.id) { mutableStateOf(firstUnskipped ?: 0) }
    var hapticExerciseIndex by remember(workout.id) { mutableStateOf(exerciseIndex) }
    var completedByExercise by remember(workout.id) { mutableStateOf(restored?.completedByExercise.orEmpty().filterKeys { it !in initialSkipped }) }
    var reps by remember(workout.id) {
        mutableStateOf(restored?.reps?.takeUnless { movedPastSkipped } ?: workout.exercises.getOrNull(exerciseIndex)?.targetRepsPerSet?.firstOrNull() ?: targetReps(workout.exercises.getOrNull(exerciseIndex)?.reps))
    }
    var weightKg by remember(workout.id) {
        mutableStateOf(restored?.weightKg?.takeUnless { movedPastSkipped } ?: workout.exercises.getOrNull(exerciseIndex)?.targetWeightsKg?.firstOrNull() ?: workout.exercises.getOrNull(exerciseIndex)?.targetWeightKg ?: 0.0)
    }
    var restRemaining by remember(workout.id) { mutableStateOf(restored?.restRemaining?.takeUnless { movedPastSkipped }) }
    var restTimerPaused by remember(workout.id) { mutableStateOf(false) }
    var restEndsAtMillis by remember(workout.id) { mutableStateOf(restored?.restRemaining?.let { System.currentTimeMillis() + it * 1_000L }) }
    var restInitialSeconds by remember(workout.id) { mutableStateOf(restored?.restInitialSeconds ?: 1) }
    var restBetweenExercises by remember(workout.id) { mutableStateOf(restored?.restBetweenExercises ?: false) }
    var restContext by remember(workout.id) {
        mutableStateOf(restored?.restContext ?: restored?.restRemaining?.let {
            if (restored.restBetweenExercises) WorkoutRestContext.BeforeNextBlock else WorkoutRestContext.BetweenSets
        })
    }
    var elapsedSeconds by remember(workout.id) { mutableStateOf(restored?.elapsedSeconds ?: 0) }
    var totalCompletedSets by remember(workout.id) { mutableStateOf(((restored?.totalCompletedSets ?: 0) - initialDiscardedRecords.size).coerceAtLeast(0)) }
    var totalVolumeKg by remember(workout.id) { mutableStateOf(((restored?.totalVolumeKg ?: 0.0) - initialDiscardedRecords.sumOf { it.reps * it.weightKg }).coerceAtLeast(0.0)) }
    var showsSummary by remember(workout.id) { mutableStateOf(restored?.showsSummary == true || (workout.exercises.isNotEmpty() && firstUnskipped == null)) }
    var completedOn by remember(workout.id) { mutableStateOf<LocalDate?>(null) }
    var completionCommitted by remember(workout.id) { mutableStateOf(false) }
    var pendingFeedback by remember(workout.id) { mutableStateOf(restored?.pendingFeedback ?: false) }
    var selectedFeedback by remember(workout.id) { mutableStateOf(restored?.selectedFeedback ?: "JUSTO") }
    var pendingNote by remember(workout.id) { mutableStateOf(restored?.pendingNote.orEmpty()) }
    var exerciseStats by remember(workout.id) { mutableStateOf(restored?.exerciseStats.orEmpty().filterKeys { it !in initialSkipped }) }
    var feedbackByExercise by remember(workout.id) { mutableStateOf(restored?.feedbackByExercise.orEmpty().filterKeys { it !in initialSkipped }) }
    var notesByExercise by remember(workout.id) { mutableStateOf(restored?.notesByExercise.orEmpty().filterKeys { it !in initialSkipped }) }
    var completedSetRecords by remember(workout.id) { mutableStateOf(restored?.completedSetRecords.orEmpty().filterNot { it.exerciseIndex in initialSkipped }) }
    var skippedExerciseIndices by remember(workout.id) { mutableStateOf(initialSkipped) }
    val initialExerciseDuration = workout.exercises.getOrNull(exerciseIndex)?.resolvedTargetDurationSeconds()
    var exerciseTimeRemaining by remember(workout.id) { mutableStateOf(restored?.exerciseTimeRemaining ?: initialExerciseDuration) }
    var exerciseTimeInitial by remember(workout.id) { mutableStateOf(restored?.exerciseTimeInitial?.takeIf { it > 0 } ?: (initialExerciseDuration ?: 0)) }
    var exerciseTimerRunning by remember(workout.id) { mutableStateOf(restored?.exerciseTimerRunning ?: (initialExerciseDuration != null)) }
    var addedSetsByExercise by remember(workout.id) { mutableStateOf(restored?.addedSetsByExercise ?: emptyMap()) }
    var completedDistanceKm by remember(workout.id) { mutableStateOf(restored?.completedDistanceKm ?: 0.0) }
    var exerciseTimerFinishedWhileAway by remember(workout.id) { mutableStateOf(restored?.exerciseTimerFinishedWhileAway ?: false) }
    var watchMotionSnapshot by remember(workout.id) { mutableStateOf(WatchMotionSnapshotData()) }
    var showsWorkoutPath by remember { mutableStateOf(false) }
    var showsExerciseHistory by remember { mutableStateOf(false) }
    var showsSetStyleInfo by remember { mutableStateOf(false) }
    var showsSkipExerciseConfirmation by remember { mutableStateOf(false) }
    var showsExerciseMenu by remember { mutableStateOf(false) }
    var showsRemoveExerciseConfirmation by remember { mutableStateOf(false) }
    var showsCancelWorkoutConfirmation by remember { mutableStateOf(false) }
    var exercisePickerMode by remember { mutableStateOf<ExercisePickerMode?>(null) }
    var addTargetBlockIndex by remember { mutableStateOf<Int?>(null) }
    // Equivalent to iOS ActiveWorkoutFeatureOnboardingState: each exercise,
    // set style and superserie is introduced once, without interrupting rest,
    // feedback or another active workout sheet.
    val featureOnboardingStore = remember { WorkoutFeatureOnboardingStore(context) }
    var activeFeatureOnboarding by remember(workout.id) { mutableStateOf<WorkoutFeatureOnboarding?>(null) }
    var featureOnboardingRevision by remember(workout.id) { mutableStateOf(0) }
    var initializedExerciseIndex by remember(workout.id) { mutableStateOf(restored?.exerciseIndex?.takeUnless { movedPastSkipped } ?: -1) }
    BackHandler {
        when {
            exercisePickerMode != null -> {
                exercisePickerMode = null
                addTargetBlockIndex = null
            }
            showsWorkoutPath -> showsWorkoutPath = false
            showsExerciseHistory -> showsExerciseHistory = false
            showsSetStyleInfo -> showsSetStyleInfo = false
            showsExerciseMenu -> showsExerciseMenu = false
            showsSkipExerciseConfirmation -> showsSkipExerciseConfirmation = false
            showsRemoveExerciseConfirmation -> showsRemoveExerciseConfirmation = false
            showsCancelWorkoutConfirmation -> showsCancelWorkoutConfirmation = false
            activeFeatureOnboarding != null -> activeFeatureOnboarding = null
            else -> onExit()
        }
    }
    val exercise = workout.exercises.getOrNull(exerciseIndex)
    LaunchedEffect(exerciseIndex) { watchMotionSnapshot = WatchMotionSnapshotData() }
    val exerciseHistory = remember(exercise?.historyKey()) {
        exercise?.let { WorkoutHistoryStore.history(context, it) }.orEmpty()
    }
    val activeRecords = exercise?.let { activeExerciseRecords(it, exerciseHistory, useImperial) }
    val completedForExercise = completedByExercise[exerciseIndex] ?: 0
    val effectiveSets = (exercise?.sets ?: 1) + (addedSetsByExercise[exerciseIndex] ?: 0)
    // More sets require more vertical room below the hero. Scale the hero
    // down only for those sessions so the complete editor and CTA remain
    // visible without leaving a blank tail or forcing an initial scroll.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    // Keep the hero continuous with the sheet. Extra series expand the sheet
    // first; only the scroll container yields space when the device truly
    // cannot fit the complete editor.
    val heroFraction = 0.46f
    val heroMinimum = 340.dp
    val activeHeroHeight = (screenHeightDp * heroFraction).dp.coerceIn(heroMinimum, 430.dp)
    // ActiveWorkoutSceneView renders its image and both gradient layers at a
    // fixed 700 pt height. The sheet overlays that scene, so limiting Android
    // to the header's 340–430 dp height made the lower part of the photo fade
    // to the surface substantially earlier than on iOS.
    val activeBackgroundHeight = 700.dp
    val effectiveExercise = exercise?.copy(sets = effectiveSets)
    val structuredPathBlocks = remember(workout) { workout.pathBlocks() }
    val logicalExercises = remember(structuredPathBlocks) { structuredPathBlocks.flatMap { it.exercises } }
    val logicalExerciseIndex = logicalExercises.indexOfFirst { exerciseIndex in it.executionIndices }.coerceAtLeast(0)
    val currentPathBlock = structuredPathBlocks.firstOrNull { block -> block.exercises.any { exerciseIndex in it.executionIndices } }
    fun applyReindexedBlocks(oldBlocks: List<WorkoutBlockSummary>, newBlocks: List<WorkoutBlockSummary>, nextOldIndex: Int = exerciseIndex, sourceOverrides: Map<Int, Int> = emptyMap()) {
        val oldCurrent = exerciseIndex
        val mapping = executionIndexRemap(oldBlocks, newBlocks, sourceOverrides)
        completedByExercise = completedByExercise.sumRemappedExecutionIndices(mapping)
        exerciseStats = exerciseStats.remapAndCombineStats(mapping)
        feedbackByExercise = feedbackByExercise.remapExecutionIndices(mapping)
        notesByExercise = notesByExercise.remapExecutionIndices(mapping)
        addedSetsByExercise = addedSetsByExercise.sumRemappedExecutionIndices(mapping)
        skippedExerciseIndices = skippedExerciseIndices.mapNotNull(mapping::get).toSet()
        val removedRecords = completedSetRecords.filter { it.exerciseIndex !in mapping }
        completedSetRecords = completedSetRecords.remapSetRecords(mapping)
        val updatedWorkout = workout.withEditableBlocks(newBlocks)
        val nextIndex = mapping[nextOldIndex]
            ?: (nextOldIndex until workout.exercises.size).firstNotNullOfOrNull(mapping::get)
            ?: updatedWorkout.exercises.lastIndex.coerceAtLeast(0)
        workout = updatedWorkout
        exerciseIndex = nextIndex
        initializedExerciseIndex = if (nextOldIndex == oldCurrent && mapping[oldCurrent] == nextIndex) nextIndex else -1
        totalCompletedSets = (totalCompletedSets - removedRecords.size).coerceAtLeast(0)
        totalVolumeKg = (totalVolumeKg - removedRecords.sumOf { it.reps * it.weightKg }).coerceAtLeast(0.0)
    }
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

    fun currentSourceIndex(): Int? {
        var executionCursor = 0
        var sourceCursor = 0
        workout.editableBlocks().forEach { block ->
            val executionCount = block.executionExercises().size
            if (exerciseIndex in executionCursor until (executionCursor + executionCount)) {
                return sourceCursor + ((exerciseIndex - executionCursor) % block.exercises.size.coerceAtLeast(1))
            }
            executionCursor += executionCount
            sourceCursor += block.exercises.size
        }
        return null
    }

    fun addOrReplaceExercise(choice: ExerciseChoice, replace: Boolean, targetBlockIndex: Int? = addTargetBlockIndex) {
        val selectedExercise = choice.toExerciseSummary()
        if (replace) {
            currentSourceIndex()?.let { sourceIndex ->
                rewriteExercises { all, _ -> if (sourceIndex in all.indices) all[sourceIndex] = all[sourceIndex].swappedTo(choice) }
                initializedExerciseIndex = -1
            }
        } else {
            // iOS's Add Exercise sheet is anchored to the selected exercise,
            // not the end of the workout. Keep that placement even when the
            // selected item is one round of a superset.
            val oldBlocks = workout.editableBlocks()
            val updated = oldBlocks.toMutableList()
            var executionCursor = 0
            val blockIndex = targetBlockIndex ?: updated.indexOfFirst { block ->
                val containsCurrent = exerciseIndex in executionCursor until (executionCursor + block.executionExercises().size)
                executionCursor += block.executionExercises().size
                containsCurrent
            }
            if (blockIndex in updated.indices) {
                val block = updated[blockIndex]
                val executionStart = updated.take(blockIndex).sumOf { it.executionExercises().size }
                val itemIndex = if (targetBlockIndex != null) block.exercises.lastIndex else ((exerciseIndex - executionStart) % block.exercises.size.coerceAtLeast(1)).coerceIn(0, block.exercises.lastIndex.coerceAtLeast(0))
                applyReindexedBlocks(oldBlocks, updated.insertExerciseAfter(blockIndex, itemIndex, selectedExercise))
            } else {
                updated += WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(selectedExercise))
                applyReindexedBlocks(oldBlocks, updated)
            }
        }
        exercisePickerMode = null
        addTargetBlockIndex = null
    }

    fun currentSectionSourceIndices(): List<Int> {
        val blocks = workout.editableBlocks()
        var executionCursor = 0
        var sourceCursor = 0
        var currentType: WorkoutBlockType? = null
        var currentIsMain = false
        blocks.forEach { block ->
            if (exerciseIndex in executionCursor until (executionCursor + block.executionExercises().size)) {
                currentType = block.type
                currentIsMain = block.type == WorkoutBlockType.Standard || block.type == WorkoutBlockType.Superset
            }
            executionCursor += block.executionExercises().size
            sourceCursor += block.exercises.size
        }
        if (currentType == null) return emptyList()
        sourceCursor = 0
        return blocks.flatMap { block ->
            val belongsToSection = if (currentIsMain) {
                block.type == WorkoutBlockType.Standard || block.type == WorkoutBlockType.Superset
            } else {
                block.type == currentType
            }
            val indices = block.exercises.indices.map { sourceCursor + it }
            sourceCursor += block.exercises.size
            if (belongsToSection) indices else emptyList()
        }
    }

    fun moveCurrentExercise(direction: Int) {
        val current = currentSourceIndex() ?: return
        val section = currentSectionSourceIndices()
        val position = section.indexOf(current)
        val destination = section.getOrNull(position + direction) ?: return
        val oldBlocks = workout.editableBlocks()
        val all = oldBlocks.flatMap { it.exercises }.toMutableList()
        val item = all[current]
        all[current] = all[destination]
        all[destination] = item
        var cursor = 0
        val updated = oldBlocks.map { block -> block.copy(exercises = block.exercises.map { all[cursor++] }) }
        applyReindexedBlocks(oldBlocks, updated, (exerciseIndex + 1).coerceAtMost(workout.exercises.lastIndex))
    }
    fun removeCurrentExercise() {
        val sourceBlocks = workout.editableBlocks()
        val removedSourceIndex = currentSourceIndex() ?: return
        var cursor = 0
        val updatedBlocks = sourceBlocks.mapNotNull { block ->
            val exercises = block.exercises.filter {
                val keep = cursor != removedSourceIndex
                cursor++
                keep
            }
            when {
                exercises.isEmpty() -> null
                block.type == WorkoutBlockType.Superset && exercises.size == 1 ->
                    WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = exercises)
                else -> block.copy(exercises = exercises)
            }
        }
        applyReindexedBlocks(sourceBlocks, updatedBlocks, exerciseIndex + 1)
    }
    fun removeActiveSuperset() {
        val sourceBlocks = workout.editableBlocks()
        val blockIndex = sourceBlocks.blockIndexForExecution(exerciseIndex) ?: return
        if (sourceBlocks[blockIndex].type != WorkoutBlockType.Superset) return
        val startSource = sourceBlocks.take(blockIndex).sumOf { it.exercises.size }
        val overrides = sourceBlocks[blockIndex].exercises.indices.associate { (startSource + it) to (startSource + it) }
        applyReindexedBlocks(sourceBlocks, sourceBlocks.splitActiveSuperset(blockIndex), sourceOverrides = overrides)
    }
    fun updateActiveSuperset(roundsDelta: Int = 0, restDelta: Int = 0) {
        val sourceBlocks = workout.editableBlocks()
        val currentBlockIndex = sourceBlocks.blockIndexForExecution(exerciseIndex)
        val updatedBlocks = sourceBlocks.mapIndexed { index, block ->
            if (index == currentBlockIndex && block.type == WorkoutBlockType.Superset) {
                block.copy(rounds = (block.rounds + roundsDelta).coerceAtLeast(1), restAfterBlockSeconds = ((block.restAfterBlockSeconds ?: 0) + restDelta).coerceAtLeast(0))
            } else block
        }
        applyReindexedBlocks(sourceBlocks, updatedBlocks)
    }
    fun createActiveSuperset() {
        val oldBlocks = workout.editableBlocks()
        if (!oldBlocks.canCreateActiveSuperset(exerciseIndex)) return
        applyReindexedBlocks(oldBlocks, oldBlocks.createActiveSuperset(exerciseIndex))
    }
    var notificationArtwork by remember(workout.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(exercise?.imageKey, gender) {
        val url = exerciseImageUrl(exercise?.imageKey, gender)
        notificationArtwork = url?.let { loadTrainingBitmap(context, it) }
    }
    fun advanceFromExercise(restSeconds: Int) {
        val nextIndex = nextUnskippedExecutionIndex(exerciseIndex, workout.exercises.size, skippedExerciseIndices)
        if (nextIndex != null) {
            val nextExercise = workout.exercises[nextIndex]
            val staysInBlock = currentPathBlock?.exercises?.any { nextIndex in it.executionIndices } == true
            val crossesRound = exercise?.blockType == WorkoutBlockType.Superset && staysInBlock && nextExercise.blockRound > exercise.blockRound
            val nextRestContext = if (crossesRound) {
                WorkoutRestContext.BetweenRounds
            } else {
                WorkoutRestContext.BeforeNextBlock
            }
            val transitionSeconds = if (crossesRound) currentPathBlock?.restAfterBlockSeconds ?: restSeconds else restSeconds
            exerciseIndex = nextIndex
            if (!skipRestPeriods && transitionSeconds > 0) {
                restInitialSeconds = transitionSeconds
                restBetweenExercises = true
                restContext = nextRestContext
                restRemaining = transitionSeconds
                restTimerPaused = false
                restEndsAtMillis = System.currentTimeMillis() + transitionSeconds * 1_000L
            } else {
                restRemaining = null
                restContext = null
            }
        } else {
            completedOn = completedOn ?: LocalDate.now()
            showsSummary = true
        }
    }

    fun completeCurrentStep() {
        val activeExercise = exercise ?: return
        runCatching {
            val isWarmupOrCooldown = activeExercise.blockType == WorkoutBlockType.Warmup || activeExercise.blockType == WorkoutBlockType.Cooldown
            val newCompleted = if (isWarmupOrCooldown) effectiveSets else completedForExercise + 1
            val timedDuration = activeExercise.resolvedTargetDurationSeconds()
            val completedMetrics = completedExerciseMetrics(
                exercise = activeExercise,
                repetitions = reps,
                weightKg = weightKg,
                initialDurationSeconds = exerciseTimeInitial,
                remainingDurationSeconds = exerciseTimeRemaining ?: 0,
            )
            val completedDuration = completedMetrics.durationSeconds
            val loggedReps = completedMetrics.repetitions
            val loggedWeight = completedMetrics.weightKg
            completedByExercise = completedByExercise + (exerciseIndex to newCompleted)
            totalCompletedSets++
            totalVolumeKg += loggedReps * loggedWeight
            completedSetRecords = completedSetRecords + CompletedSetRecord(
                exerciseIndex = exerciseIndex,
                setNumber = if (isWarmupOrCooldown) 1 else newCompleted,
                reps = loggedReps,
                weightKg = loggedWeight,
                setStyle = activeExercise.setStyle,
                durationSeconds = completedDuration,
            )
            val previousStats = exerciseStats[exerciseIndex] ?: ExerciseSessionStats()
            exerciseStats = exerciseStats + (exerciseIndex to previousStats.copy(
                sets = previousStats.sets + 1,
                totalReps = previousStats.totalReps + loggedReps,
                maxWeightKg = maxOf(previousStats.maxWeightKg, loggedWeight),
                volumeKg = previousStats.volumeKg + loggedReps * loggedWeight,
                durationSeconds = previousStats.durationSeconds + completedDuration,
                averageWeightKg = if (loggedWeight > 0) {
                    ((previousStats.averageWeightKg * previousStats.sets) + loggedWeight) / (previousStats.sets + 1)
                } else previousStats.averageWeightKg,
            ))
            if (newCompleted < effectiveSets) {
                val completedSetIndex = newCompleted - 1
                val completedSetTargetReps = activeExercise.targetRepsPerSet?.getOrNull(completedSetIndex)
                    ?: targetReps(activeExercise.reps)
                val completedSetTargetWeightKg = activeExercise.targetWeightsKg?.getOrNull(completedSetIndex)
                    ?: activeExercise.targetWeightKg
                // When the athlete deviates from the AI target, continue with
                // that actual result in the next set. The planned target stays
                // intact and is still displayed as the reference value.
                reps = if (loggedReps != completedSetTargetReps) {
                    loggedReps
                } else {
                    activeExercise.targetRepsPerSet?.getOrNull(newCompleted) ?: targetReps(activeExercise.reps)
                }
                weightKg = if (completedSetTargetWeightKg != null && loggedWeight != completedSetTargetWeightKg) {
                    loggedWeight
                } else {
                    activeExercise.targetWeightsKg?.getOrNull(newCompleted) ?: activeExercise.targetWeightKg ?: weightKg
                }
                exerciseTimerRunning = false
                if (timedDuration != null) {
                    exerciseTimeRemaining = timedDuration
                    exerciseTimeInitial = timedDuration
                }
                restBetweenExercises = false
                restContext = WorkoutRestContext.BetweenSets
                restTimerPaused = false
                val restSeconds = activeExercise.restSeconds.coerceAtLeast(0)
                if (!shouldStartRest(restSeconds, skipRestPeriods)) {
                    restInitialSeconds = 0
                    restRemaining = null
                    restEndsAtMillis = null
                    restContext = null
                    exerciseTimerRunning = timedDuration != null
                } else {
                    restInitialSeconds = restSeconds
                    restRemaining = restSeconds
                    restEndsAtMillis = System.currentTimeMillis() + restSeconds * 1_000L
                }
            } else {
                val isLastUnskippedInBlock = currentPathBlock?.exercises
                    ?.flatMap { it.executionIndices }
                    ?.none { it > exerciseIndex && it !in skippedExerciseIndices }
                    ?: activeExercise.isLastInBlock
                if (shouldRequestExerciseFeedback(activeExercise.copy(isLastInBlock = isLastUnskippedInBlock))) {
                    pendingFeedback = true
                } else {
                    val transitionRest = if (activeExercise.isLastInBlock) activeExercise.restAfterBlockSeconds ?: 0 else 0
                    advanceFromExercise(transitionRest)
                }
            }
            // Match iOS's separate selection/success feedback: a regular set
            // is a light acknowledgement, while the last set advances the
            // exercise or opens feedback/completion and needs a stronger cue.
            haptics.performHapticFeedback(
                if (newCompleted >= effectiveSets) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
            )
        }.onFailure { error ->
            android.util.Log.e("Workout", "Unable to complete exercise", error)
        }
    }

    fun applyWatchCommand(command: WildforceWatchLocalBridge.Command) {
        when (command.operation) {
            "set_reps" -> command.reps?.let { reps = it.coerceIn(0, 999) }
            "set_weight" -> command.weightKg?.let { weightKg = it.coerceIn(0.0, 999.0) }
            "set_duration" -> command.durationMinutes?.let { minutes ->
                val seconds = minutes.coerceIn(0, 999) * 60
                exerciseTimeInitial = seconds
                exerciseTimeRemaining = seconds
                exerciseTimerRunning = false
            }
            "set_distance" -> command.distanceKm?.let { completedDistanceKm = it.coerceIn(0.0, 999.0) }
            "motion" -> command.motion?.let { watchMotionSnapshot = it }
            "set_feedback" -> command.feedback?.let { value ->
                if (value in setOf("MUY FÁCIL", "FÁCIL", "JUSTO", "DIFÍCIL", "MUY DIFÍCIL")) selectedFeedback = value
            }
            "complete_feedback" -> if (pendingFeedback && selectedFeedback.isNotBlank()) {
                exercise?.let { current ->
                    feedbackByExercise = feedbackByExercise + (exerciseIndex to selectedFeedback)
                    pendingNote.trim().takeIf(String::isNotEmpty)?.let { notesByExercise = notesByExercise + (exerciseIndex to it) }
                    if (current.targetDistanceKm != null) {
                        val latestRecordIndex = completedSetRecords.indexOfLast { it.exerciseIndex == exerciseIndex }
                        if (latestRecordIndex >= 0) {
                            completedSetRecords = completedSetRecords.toMutableList().also { records ->
                                records[latestRecordIndex] = records[latestRecordIndex].copy(distanceKm = completedDistanceKm)
                            }
                        }
                        val currentStats = exerciseStats[exerciseIndex] ?: ExerciseSessionStats()
                        exerciseStats = exerciseStats + (exerciseIndex to currentStats.copy(distanceKm = completedDistanceKm))
                    }
                    pendingFeedback = false
                    selectedFeedback = "JUSTO"
                    pendingNote = ""
                    advanceFromExercise(if (current.blockType == WorkoutBlockType.Superset) 0 else current.restSeconds)
                }
            }
            "complete_set" -> if (restRemaining == null && !pendingFeedback && !showsSummary) completeCurrentStep()
            "skip_rest" -> if (restRemaining != null) {
                restRemaining = null
                restEndsAtMillis = null
                restTimerPaused = false
                restBetweenExercises = false
                restContext = null
                exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
            }
            "add_rest" -> if (restRemaining != null) {
                val now = System.currentTimeMillis()
                val end = restEndsAtMillis ?: now + restRemaining!!.coerceAtLeast(0) * 1_000L
                restEndsAtMillis = end + 30_000L
                restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                restInitialSeconds += 30
                restTimerPaused = false
            }
        }
    }

    DisposableEffect(workout.id) {
        WildforceWatchLocalBridge.commandListener = ::applyWatchCommand
        WildforceWatchBridge.commandListener = { command ->
            applyWatchCommand(
                WildforceWatchLocalBridge.Command(
                    operation = command.operation,
                    reps = command.reps,
                    weightKg = command.weightKg,
                    feedback = command.feedback,
                    durationMinutes = command.durationMinutes,
                    distanceKm = command.distanceKm,
                    motion = command.motion,
                ),
            )
        }
        onDispose {
            WildforceWatchLocalBridge.commandListener = null
            WildforceWatchBridge.commandListener = null
        }
    }

    LaunchedEffect(workout.id) {
        if (exerciseTimerFinishedWhileAway) {
            exerciseTimerFinishedWhileAway = false
            completeCurrentStep()
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
                        if (WorkoutNotificationActionStore.wasHandled(context, timestamp)) {
                            // The foreground service already changed the
                            // persisted session. Mirror that exact value to
                            // Compose instead of applying +30/skip twice.
                            WorkoutSessionStore.load(context, workout.id)?.let { latest ->
                                restRemaining = latest.restRemaining
                                restInitialSeconds = latest.restInitialSeconds
                                restBetweenExercises = latest.restBetweenExercises
                                restContext = latest.restContext
                                restTimerPaused = false
                                restEndsAtMillis = latest.restRemaining?.let {
                                    System.currentTimeMillis() + it * 1_000L
                                }
                                exerciseTimerRunning = latest.exerciseTimerRunning
                            }
                        } else when (action) {
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
                    WorkoutActiveNotification.ACTION_SKIP_CURRENT -> if (restRemaining != null) {
                        restRemaining = null
                        restEndsAtMillis = null
                        restBetweenExercises = false
                        restContext = null
                        restTimerPaused = false
                        exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
                    }
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
                    restContext = latest.restContext?.takeIf { resumedRest != null }
                    elapsedSeconds = latest.elapsedSeconds
                    exerciseTimeRemaining = latest.exerciseTimeRemaining
                    exerciseTimerRunning = latest.exerciseTimerRunning
                    skippedExerciseIndices = latest.skippedExerciseIndices
                }
                // If Android had released the workout composition, watch
                // commands were persisted by the local bridge. Apply them only
                // after restoring the snapshot so the action uses current data.
                WatchCommandStore.drain(context).forEach(::applyWatchCommand)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                context.getSharedPreferences("wildforce_notification_settings", 0).edit().putBoolean("app_foreground", false).apply()
                // Force an immediate save before the foreground service takes over.
                val snapshot = WorkoutSessionSnapshot(
                    exerciseIndex = exerciseIndex, completedByExercise = completedByExercise,
                    reps = reps, weightKg = weightKg, restRemaining = restRemaining,
                    restInitialSeconds = restInitialSeconds, restBetweenExercises = restBetweenExercises,
                    elapsedSeconds = elapsedSeconds, totalCompletedSets = totalCompletedSets,
                    totalVolumeKg = totalVolumeKg, pendingFeedback = pendingFeedback,
                    showsSummary = showsSummary, selectedFeedback = selectedFeedback,
                    pendingNote = pendingNote, exerciseStats = exerciseStats,
                    feedbackByExercise = feedbackByExercise, notesByExercise = notesByExercise,
                    completedSetRecords = completedSetRecords, exerciseTimeRemaining = exerciseTimeRemaining,
                    exerciseTimeInitial = exerciseTimeInitial, exerciseTimerRunning = exerciseTimerRunning,
                    addedSetsByExercise = addedSetsByExercise, completedDistanceKm = completedDistanceKm,
                    restContext = restContext,
                    skippedExerciseIndices = skippedExerciseIndices,
                    editableBlocks = workout.editableBlocks(),
                )
                WorkoutSessionStore.save(context, workout.id, snapshot)
                val refreshIntent = android.content.Intent(context, WorkoutForegroundService::class.java)
                    .putExtra("workoutId", workout.id)
                    .putExtra("totalExercises", workout.exercises.size)
                    .putExtra("currentExerciseHasTimer", exercise?.resolvedTargetDurationSeconds() != null)
                    .putExtra("refresh", true)
                runCatching { androidx.core.content.ContextCompat.startForegroundService(context, refreshIntent) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showsSummary) { while (!showsSummary) { delay(1_000); elapsedSeconds++ } }
    LaunchedEffect(exerciseIndex, completedByExercise, restRemaining, restInitialSeconds, restTimerPaused, restBetweenExercises, showsSummary, notificationArtwork) {
        if (!showsSummary) {
        val currentName = exercise?.let { exerciseDisplayName(it.imageKey, it.name) } ?: "Entrenamiento"
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
                        type = if (restRemaining != null) WorkoutNotificationType.REST else WorkoutNotificationType.PROGRESS,
                        artwork = notificationArtwork,
                        exerciseIndex = exerciseIndex,
                        totalExercises = workout.exercises.size,
                        exerciseTimerSeconds = exerciseTimeRemaining?.takeIf { restRemaining == null },
                        canToggleExerciseTimer = restRemaining == null && exercise?.resolvedTargetDurationSeconds() != null,
                        restEndAtMillis = restEndsAtMillis,
                    )
                }
            }
        }
    }
    LaunchedEffect(exerciseIndex, initializedExerciseIndex) {
        if (hapticExerciseIndex != exerciseIndex) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            hapticExerciseIndex = exerciseIndex
        }
        if (initializedExerciseIndex != exerciseIndex) {
            val nextSetIndex = completedByExercise[exerciseIndex] ?: 0
            reps = exercise?.targetRepsPerSet?.getOrNull(nextSetIndex) ?: targetReps(exercise?.reps)
            weightKg = exercise?.targetWeightsKg?.getOrNull(nextSetIndex) ?: exercise?.targetWeightKg ?: 0.0
            val duration = exercise?.resolvedTargetDurationSeconds()
            exerciseTimeRemaining = duration
            exerciseTimeInitial = duration ?: 0
            exerciseTimerRunning = duration != null && restRemaining == null
            completedDistanceKm = 0.0
            initializedExerciseIndex = exerciseIndex
        }
    }
    LaunchedEffect(workout, exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, restBetweenExercises, restContext, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, pendingNote, showsSummary, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise, completedDistanceKm, skippedExerciseIndices) {
        // After ON_PAUSE the foreground service owns the persisted session.
        // Compose can remain alive briefly in the background, so its stale
        // local timer must never overwrite an action (for example Omitir)
        // that has already been applied by the service.
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return@LaunchedEffect
        }
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
                completedDistanceKm = completedDistanceKm,
                restContext = restContext,
                skippedExerciseIndices = skippedExerciseIndices,
                editableBlocks = workout.editableBlocks(),
            )
            WorkoutSessionStore.save(context, workout.id, snapshot)
            WorkoutSessionStore.saveLegacy(/*
                context, workout.id,
                WorkoutSessionSnapshot(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, pendingNote, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise),
            */)
        }
    }
    LaunchedEffect(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, elapsedSeconds, showsSummary) {
        val activeExercise = exercise ?: return@LaunchedEffect
        if (!showsSummary) {
            WildforceWatchLocalBridge.publish(
                context,
                WildforceWatchLocalBridge.State(
                    workoutId = workout.id,
                    workoutTitle = workout.title,
                    exerciseName = exerciseDisplayName(activeExercise.imageKey, activeExercise.name),
                    exerciseIndex = exerciseIndex,
                    exerciseCount = workout.exercises.size,
                    setNumber = completedForExercise + 1,
                    targetSets = effectiveSets,
                    reps = reps,
                    weightKg = weightKg,
                    restRemaining = restRemaining,
                    restInitialSeconds = restInitialSeconds,
                    exercises = workout.exercises.map { exerciseDisplayName(it.imageKey, it.name) },
                    elapsedSeconds = elapsedSeconds,
                ),
            )
        }
        // Keep publishing the compact BLE state after completion as well. The
        // watch uses this terminal frame to enter its summary screen; stopping
        // publication at `showsSummary` left it displaying the last set.
        WildforceWatchBridge.publish(
            context,
            WildforceWatchBridge.State(
                workoutId = workout.id,
                workoutTitle = workout.title,
                exerciseName = exerciseDisplayName(activeExercise.imageKey, activeExercise.name),
                exerciseIndex = exerciseIndex,
                exerciseCount = workout.exercises.size,
                setNumber = completedForExercise + 1,
                targetSets = effectiveSets,
                reps = reps,
                weightKg = weightKg,
                restRemaining = restRemaining,
                restInitialSeconds = restInitialSeconds,
                elapsedSeconds = elapsedSeconds,
                isSummary = showsSummary,
                    pendingFeedback = pendingFeedback,
                    selectedFeedback = selectedFeedback,
                    durationMinutes = activeExercise.resolvedTargetDurationSeconds()?.div(60),
                    distanceKm = activeExercise.targetDistanceKm?.let { completedDistanceKm },
                pathExercises = workout.exercises.map { exerciseDisplayName(it.imageKey, it.name) },
                setStyleLabel = activeExercise.setStyle.label,
                setStyleDetails = buildList {
                    setStyleInstruction(
                        activeExercise.setStyle,
                        completedForExercise + 1,
                        effectiveSets,
                        activeExercise.setStyleParameters.appliesToFinalSetOnly,
                    ).takeIf(String::isNotBlank)?.let(::add)
                    addAll(styleParameterLabels(activeExercise.setStyle, activeExercise.setStyleParameters))
                },
            ),
        )
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
        // iOS emits a selection haptic when the rest phase completes. Keep
        // this separate from set-completion feedback so a zero-rest
        // transition does not produce a synthetic extra pulse.
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        restRemaining = null
        restEndsAtMillis = null
        restBetweenExercises = false
        restContext = null
        exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
    }
    LaunchedEffect(exerciseTimerRunning, exerciseTimeRemaining) {
        val remaining = exerciseTimeRemaining ?: return@LaunchedEffect
        if (exerciseTimerRunning && remaining > 0) {
            delay(1_000)
            if (remaining <= 1) {
                exerciseTimeRemaining = 0
                exerciseTimerRunning = false
                completeCurrentStep()
            } else {
                exerciseTimeRemaining = remaining - 1
            }
        } else if (remaining <= 0) {
            // iOS finishes the timed exercise when an accessibility adjustment
            // reduces the running timer to zero; do not merely pause it.
            val wasRunning = exerciseTimerRunning
            exerciseTimerRunning = false
            if (wasRunning) completeCurrentStep()
        }
    }

    LaunchedEffect(exerciseIndex, restRemaining, pendingFeedback, showsSummary, featureOnboardingRevision) {
        if (activeFeatureOnboarding == null && restRemaining == null && !pendingFeedback && !showsSummary) {
            exercise?.let { current ->
                workoutFeatureOnboardings(current, currentPathBlock, featureOnboardingStore).firstOrNull()?.let { next ->
                    featureOnboardingStore.markSeen(next)
                    activeFeatureOnboarding = next
                }
            }
        }
    }

    if (exercisePickerMode != null) {
        ExercisePickerScreen(
            gender = gender,
            onBack = { exercisePickerMode = null },
            replacing = exercise.takeIf { exercisePickerMode == ExercisePickerMode.Swap },
        ) { choice ->
            addOrReplaceExercise(choice, exercisePickerMode == ExercisePickerMode.Swap)
        }
        return
    }
    activeFeatureOnboarding?.let { onboarding ->
        WorkoutFeatureOnboardingDialog(onboarding, gender) {
            activeFeatureOnboarding = null
            featureOnboardingRevision++
        }
    }
    ActiveWorkoutToolbarDialogs(
        workout = workout,
        gender = gender,
        exerciseIndex = exerciseIndex,
        exercise = exercise,
        completedByExercise = completedByExercise,
        addedSetsByExercise = addedSetsByExercise,
        elapsedSeconds = elapsedSeconds,
        useImperial = useImperial,
        context = context,
        showsWorkoutPath = showsWorkoutPath,
        showsExerciseHistory = showsExerciseHistory,
        onDismissWorkoutPath = { showsWorkoutPath = false },
        onAddExerciseToBlock = { blockIndex -> addTargetBlockIndex = blockIndex; exercisePickerMode = ExercisePickerMode.Add },
        onDismissExerciseHistory = { showsExerciseHistory = false },
    )

    if (showsSetStyleInfo && exercise != null) {
        SetStyleInfoScreen(exercise.setStyle, effectiveSets, exercise.setStyleParameters) { showsSetStyleInfo = false }
        return
    }

    ActiveWorkoutConfirmationDialogs(
        showsSkipExerciseConfirmation = showsSkipExerciseConfirmation,
        showsRemoveExerciseConfirmation = showsRemoveExerciseConfirmation,
        showsCancelWorkoutConfirmation = showsCancelWorkoutConfirmation,
        isSkippingWholeBlock = exercise?.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown),
        onDismissSkip = { showsSkipExerciseConfirmation = false },
        onConfirmSkip = {
            showsSkipExerciseConfirmation = false
            val skipsWholeBlock = exercise?.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)
            val skippedNow = currentPathBlock?.skippedExecutionIndices(exerciseIndex, skipsWholeBlock) ?: setOf(exerciseIndex)
            skippedExerciseIndices = skippedExerciseIndices + skippedNow
            val blockIndex = workout.editableBlocks().blockIndexForExecution(exerciseIndex)
            val sourceIndex = currentSourceIndex()
            val skippedAt = java.time.Instant.now().toString()
            rewriteExercises { all, blocks ->
                val first = blocks.take(blockIndex ?: 0).sumOf { it.exercises.size }
                val affected = if (skipsWholeBlock && blockIndex != null) first until first + blocks[blockIndex].exercises.size
                    else listOfNotNull(sourceIndex)
                affected.forEach { index -> if (index in all.indices) all[index] = all[index].copy(skippedAt = skippedAt) }
            }
            val discardedRecords = completedSetRecords.filter { it.exerciseIndex in skippedNow }
            completedSetRecords = completedSetRecords.filterNot { it.exerciseIndex in skippedNow }
            completedByExercise = completedByExercise.filterKeys { it !in skippedNow }
            exerciseStats = exerciseStats.filterKeys { it !in skippedNow }
            feedbackByExercise = feedbackByExercise.filterKeys { it !in skippedNow }
            notesByExercise = notesByExercise.filterKeys { it !in skippedNow }
            totalCompletedSets = (totalCompletedSets - discardedRecords.size).coerceAtLeast(0)
            totalVolumeKg = (totalVolumeKg - discardedRecords.sumOf { it.reps * it.weightKg }).coerceAtLeast(0.0)
            restRemaining = null
            exerciseTimerRunning = false
            val nextIndex = nextUnskippedExecutionIndex(exerciseIndex, workout.exercises.size, skippedExerciseIndices)
            if (nextIndex == null) {
                completedOn = completedOn ?: LocalDate.now()
                showsSummary = true
            } else exerciseIndex = nextIndex
        },
        onDismissRemove = { showsRemoveExerciseConfirmation = false },
        onConfirmRemove = { showsRemoveExerciseConfirmation = false; removeCurrentExercise() },
        onDismissCancel = { showsCancelWorkoutConfirmation = false },
        onConfirmCancel = { showsCancelWorkoutConfirmation = false; cancelWorkoutSession(context, workout.id); onWorkoutChanged(workout.resetCancelledWorkout()); onExit() },
    )

    /*
    if (showsSkipExerciseConfirmation) {
        val skipsWholeBlock = exercise?.blockType in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)
        AlertDialog(
            onDismissRequest = { showsSkipExerciseConfirmation = false },
            title = { Text(if (skipsWholeBlock) "¿SALTAR BLOQUE?" else "¿SALTAR EJERCICIO?", fontFamily = AntonFontFamily) },
            text = { Text(if (skipsWholeBlock) "Se omitirá todo el bloque actual y pasarás al siguiente." else "Este ejercicio quedará sin completar y pasarás al siguiente.") },
            confirmButton = { TextButton(onClick = {
                showsSkipExerciseConfirmation = false
                val nextIndex = if (skipsWholeBlock) currentPathBlock?.exercises?.flatMap { it.executionIndices }?.maxOrNull()?.plus(1) else exerciseIndex + 1
                val skippedNow = if (skipsWholeBlock) {
                    currentPathBlock?.exercises?.flatMap { it.executionIndices }?.toSet().orEmpty()
                } else setOf(exerciseIndex)
                skippedExerciseIndices = skippedExerciseIndices + skippedNow
                restRemaining = null
                if (nextIndex == null || nextIndex > workout.exercises.lastIndex) {
                    completedOn = completedOn ?: LocalDate.now()
                    showsSummary = true
                } else {
                    exerciseIndex = nextIndex
                }
            }) { Text("SALTAR", color = WildforceThemeTokens.accent) } },
            dismissButton = { TextButton(onClick = { showsSkipExerciseConfirmation = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }

    if (showsRemoveExerciseConfirmation) {
        AlertDialog(
            onDismissRequest = { showsRemoveExerciseConfirmation = false },
            title = { Text("¿ELIMINAR EJERCICIO?", fontFamily = AntonFontFamily) },
            text = { Text("Esta acción eliminará el ejercicio de este entrenamiento.") },
            confirmButton = {
                TextButton(onClick = {
                    showsRemoveExerciseConfirmation = false
                    removeCurrentExercise()
                }) { Text("ELIMINAR", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showsRemoveExerciseConfirmation = false }) {
                    Text("CANCELAR", color = WildforceThemeTokens.textSecondary)
                }
            },
        )
    }

    if (showsCancelWorkoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showsCancelWorkoutConfirmation = false },
            title = { Text("¿CANCELAR ENTRENAMIENTO?", fontFamily = AntonFontFamily) },
            text = { Text("Se eliminará todo el progreso de esta sesión.") },
            confirmButton = {
                TextButton(onClick = {
                    showsCancelWorkoutConfirmation = false
                    cancelWorkoutSession(context, workout.id)
                    onExit()
                }) { Text("CANCELAR ENTRENAMIENTO", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showsCancelWorkoutConfirmation = false }) {
                    Text("SEGUIR ENTRENANDO", color = WildforceThemeTokens.textSecondary)
                }
            },
        )
    }

    }
    */

    fun persistCompletedSession(progress: CompletionProgress) {
        // Completion can be triggered twice by a fast tap during the animated
        // finish flow. Make it idempotent and keep persistence failures from
        // crashing the activity while returning to the home screen.
        if (completionCommitted) return
        completionCommitted = true
        runCatching {
            WorkoutHistoryStore.record(context, workout, exerciseStats, completedSetRecords, feedbackByExercise, notesByExercise)
            WorkoutCompletionMetricsStore.save(
                context,
                workout.id,
                WorkoutCompletionMetrics(
                    completedAtMillis = System.currentTimeMillis(),
                    activeDurationSeconds = elapsedSeconds,
                    activeCaloriesBurned = WorkoutMetricCalculator.estimateCalories(workout, exerciseStats, elapsedSeconds, userBmr),
                    totalVolumeKg = totalVolumeKg,
                ),
            )
            CompletionProgressStore.commit(context, progress, completedOn ?: LocalDate.now())
            WorkoutSessionStore.clear(context, workout.id)
        }
    }

    fun finishCompletedSession(progress: CompletionProgress) {
        persistCompletedSession(progress)
        runCatching { onFinish(elapsedSeconds, totalCompletedSets, totalVolumeKg, progress.streakAfter, completedOn ?: LocalDate.now(), skippedExerciseIndices) }
    }

    if (showsSummary) {
        val completionProgress = remember(workout.id, isPlanCompletedAfterWorkout, isMesocycleCompletedAfterWorkout) {
            CompletionProgressStore.preview(
                context,
                currentStreak,
                completesPlan = isPlanCompletedAfterWorkout,
                completesMesocycle = isMesocycleCompletedAfterWorkout,
                completedOn = completedOn ?: LocalDate.now(),
            )
        }
        val recordEvents = remember(workout.id, exerciseStats) { WorkoutCompletionCalculator.records(context, workout, exerciseStats) }
        WorkoutCompletionFlowScreen(workout, elapsedSeconds, exerciseStats, feedbackByExercise, recordEvents, completionProgress, useImperial = useImperial, userAgeYears = userAgeYears, canGenerateNextPlan = canGenerateNextPlan, isPlanCompleted = isPlanCompletedAfterWorkout, isMesocycleCompleted = isMesocycleCompletedAfterWorkout, planName = planName, completedPlanWorkouts = completedPlanWorkouts, totalPlanExercises = totalPlanExercises, mesocycleIndex = mesocycleIndex, cycleLength = cycleLength, skippedExerciseIndices = skippedExerciseIndices, onCompletionConfirmed = {
            persistCompletedSession(completionProgress)
        }, onGenerateNextPlan = {
            finishCompletedSession(completionProgress)
            onGenerateNextPlan()
        }, onCancelWorkout = {
            cancelWorkoutSession(context, workout.id)
            onWorkoutChanged(workout.resetCancelledWorkout())
            onExit()
        }, onDone = { finishCompletedSession(completionProgress) })
        return
    }

    /**
     * iOS leaves the feedback phase without committing the exercise so the
     * athlete can correct the last set. Android records set progress eagerly
     * for crash recovery, so remove that pending record and rebuild the
     * exercise totals before returning to the set editor.
     */
    fun editFeedbackSet() {
        val lastRecordIndex = completedSetRecords.indexOfLast { it.exerciseIndex == exerciseIndex }
        val lastRecord = completedSetRecords.getOrNull(lastRecordIndex)
        if (lastRecord != null) {
            val updatedRecords = completedSetRecords.toMutableList().also { it.removeAt(lastRecordIndex) }
            completedSetRecords = updatedRecords
            completedByExercise = completedByExercise + (exerciseIndex to (completedForExercise - 1).coerceAtLeast(0))
            totalCompletedSets = (totalCompletedSets - 1).coerceAtLeast(0)
            totalVolumeKg = (totalVolumeKg - lastRecord.reps * lastRecord.weightKg).coerceAtLeast(0.0)
            exerciseStats = exerciseStats + (exerciseIndex to statsForExercise(updatedRecords, exerciseIndex))
            reps = lastRecord.reps
            weightKg = lastRecord.weightKg
            completedDistanceKm = lastRecord.distanceKm
        }
        pendingFeedback = false
        selectedFeedback = "JUSTO"
        pendingNote = ""
        restRemaining = null
        restEndsAtMillis = null
        restInitialSeconds = 0
        restBetweenExercises = false
        restContext = null
        restTimerPaused = false
        exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
    }

    fun continueFeedback() {
        selectedFeedback?.let { feedbackByExercise = feedbackByExercise + (exerciseIndex to it) }
        pendingNote.trim().takeIf(String::isNotEmpty)?.let { notesByExercise = notesByExercise + (exerciseIndex to it) }
        if (exercise?.targetDistanceKm != null) {
            val latestRecordIndex = completedSetRecords.indexOfLast { it.exerciseIndex == exerciseIndex }
            if (latestRecordIndex >= 0) {
                completedSetRecords = completedSetRecords.toMutableList().also { records ->
                    records[latestRecordIndex] = records[latestRecordIndex].copy(distanceKm = completedDistanceKm)
                }
            }
            val currentStats = exerciseStats[exerciseIndex] ?: ExerciseSessionStats()
            exerciseStats = exerciseStats + (exerciseIndex to currentStats.copy(distanceKm = completedDistanceKm))
        }
        pendingFeedback = false
        selectedFeedback = "JUSTO"
        pendingNote = ""
        exercise?.let { current -> advanceFromExercise(if (current.blockType == WorkoutBlockType.Superset) 0 else current.restSeconds) }
    }

    fun addRestTime() {
        restInitialSeconds += 30
        val now = System.currentTimeMillis()
        val current = (restRemaining ?: 0).coerceAtLeast(0)
        restEndsAtMillis = if (restTimerPaused) now + (current + 30) * 1_000L else (restEndsAtMillis ?: now) + 30_000L
        restTimerPaused = false
        restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
    }

    fun removeRestTime() {
        val now = System.currentTimeMillis()
        val current = (restRemaining ?: 0).coerceAtLeast(0)
        val removed = minOf(30, current)
        restInitialSeconds = (restInitialSeconds - removed).coerceAtLeast(0)
        restEndsAtMillis = if (restTimerPaused) now + (current - removed).coerceAtLeast(0) * 1_000L else (restEndsAtMillis ?: now) - removed * 1_000L
        restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
    }

    fun skipRest() {
        restRemaining = null
        restEndsAtMillis = null
        restTimerPaused = false
        restBetweenExercises = false
        restContext = null
        exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
        val completedExercises = workout.exercises.indices.count { index ->
            index < exerciseIndex || (completedByExercise[index] ?: 0) >= (workout.exercises[index].sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
        }
        runCatching {
            WorkoutActiveNotification.show(
                context,
                workout.title,
                "${exercise?.let { exerciseDisplayName(it.imageKey, it.name) } ?: "Entrenamiento"} · ${completedExercises}/${workout.exercises.size} ejercicios",
                progress = completedExercises,
                progressMax = workout.exercises.size.coerceAtLeast(1),
                type = WorkoutNotificationType.PROGRESS,
                artwork = notificationArtwork,
                exerciseIndex = exerciseIndex,
                totalExercises = workout.exercises.size,
                exerciseTimerSeconds = exerciseTimeRemaining,
                canToggleExerciseTimer = exercise?.resolvedTargetDurationSeconds() != null,
            )
        }
    }

    fun addExerciseTime() {
        exerciseTimeRemaining = (exerciseTimeRemaining ?: 0) + 15
        exerciseTimeInitial += 15
    }

    fun removeExerciseTime() {
        exerciseTimeRemaining = ((exerciseTimeRemaining ?: 0) - 15).coerceAtLeast(0)
        exerciseTimeInitial = (exerciseTimeInitial - 15).coerceAtLeast(0)
    }

    fun updateSetRecord(original: CompletedSetRecord, changed: CompletedSetRecord) {
        completedSetRecords = completedSetRecords.map { if (it.exerciseIndex == exerciseIndex && it.setNumber == original.setNumber) changed else it }
        totalVolumeKg += changed.reps * changed.weightKg - original.reps * original.weightKg
        exerciseStats = exerciseStats + (exerciseIndex to statsForExercise(completedSetRecords, exerciseIndex))
    }

    fun addSet() {
        addedSetsByExercise = addedSetsByExercise + (exerciseIndex to ((addedSetsByExercise[exerciseIndex] ?: 0) + 1))
    }

    ActiveWorkoutSurface {
        // iOS's ActiveWorkoutSceneView ignores the safe area for the image
        // and both gradient layers. The parent app shell keeps foreground
        // controls below the status bar, so extend only this visual scene
        // upward into that inset.
        val statusBarInset = with(LocalDensity.current) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                LocalView.current.rootWindowInsets
                    ?.getInsets(android.view.WindowInsets.Type.statusBars())
                    ?.top
                    ?.toDp()
                    ?: 0.dp
            } else {
                0.dp
            }
        }
        val sceneHeight = activeBackgroundHeight + statusBarInset
        Crossfade(
            targetState = exerciseIndex,
            animationSpec = tween(durationMillis = 360),
            label = "exerciseHero",
        ) { visibleExerciseIndex ->
            val visibleExercise = workout.exercises.getOrNull(visibleExerciseIndex)
            RemoteTrainingImage(
                url = exerciseImageUrl(visibleExercise?.imageKey, gender),
                contentDescription = visibleExercise?.let { exerciseDisplayName(it.imageKey, it.name) },
                modifier = Modifier.fillMaxWidth().height(sceneHeight).offset(y = -statusBarInset),
                showUnavailableLabel = false,
            )
        }
        // Mirrors iOS: a dark top treatment that is fully transparent by the
        // center, then a separate surface fade only in the lower half.
        Box(
            Modifier.fillMaxWidth().height(sceneHeight).offset(y = -statusBarInset).background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.60f),
                    0.25f to Color.Black.copy(alpha = 0.50f),
                    0.50f to Color.Transparent,
                    1f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxWidth().height(sceneHeight).offset(y = -statusBarInset).padding(top = 200.dp).background(
                Brush.verticalGradient(
                    0f to WildforceThemeTokens.backgroundSecondary.copy(alpha = 0f),
                    1f to WildforceThemeTokens.backgroundSecondary,
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            val sectionIndices = currentSectionSourceIndices()
            val sectionPosition = currentSourceIndex()?.let(sectionIndices::indexOf) ?: -1
            ActiveWorkoutHeroHeader(
                workout = workout,
                exerciseIndex = exerciseIndex,
                exercise = exercise,
                completedByExercise = completedByExercise,
                addedSetsByExercise = addedSetsByExercise,
                exerciseRecords = activeRecords,
                currentPathBlock = currentPathBlock,
                canMoveDown = sectionPosition >= 0 && sectionPosition < sectionIndices.lastIndex,
                showsExerciseMenu = showsExerciseMenu,
                onExit = onExit,
                onShowWorkoutPath = { showsWorkoutPath = true },
                onShowExerciseHistory = { showsExerciseHistory = true },
                onShowExerciseGuide = { exercise?.let(onShowExerciseGuide) },
                onShowExerciseMenu = { showsExerciseMenu = true },
                onDismissExerciseMenu = { showsExerciseMenu = false },
                onAddExercise = { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Add },
                onReplaceExercise = { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Swap },
                onSkipExercise = { showsExerciseMenu = false; showsSkipExerciseConfirmation = true },
                onMoveExercise = { showsExerciseMenu = false; moveCurrentExercise(1) },
                onCreateSuperset = { showsExerciseMenu = false; createActiveSuperset() },
                onRemoveSuperset = { showsExerciseMenu = false; removeActiveSuperset() },
                onAddRound = { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = 1) },
                onRemoveRound = { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = -1) },
                onAddSupersetRest = { showsExerciseMenu = false; updateActiveSuperset(restDelta = 15) },
                onRemoveSupersetRest = { showsExerciseMenu = false; updateActiveSuperset(restDelta = -15) },
                onRemoveExercise = { showsExerciseMenu = false; showsRemoveExerciseConfirmation = true },
                onCancelWorkout = { showsExerciseMenu = false; showsCancelWorkoutConfirmation = true },
            )
            // iOS pins the phase sheet to the bottom edge; let the hero/header
            // absorb all remaining space instead of leaving a floating gap.
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(0.dp))
            val timerMode = restRemaining != null || exercise?.resolvedTargetDurationSeconds() != null
            // Keep the bottom sheet at the same visual proportion as iOS.
            // Additional sets belong inside the sheet's scroll surface; letting
            // the sheet grow to 72% made Bicycle Crunch look split in two and
            // pushed the exercise hero/muscle header out of balance.
            // iOS keeps a regular exercise sheet visually substantial even
            // when the first set is empty. Without a minimum, Bicycle Crunch
            // collapses to the measured controls and leaves an oversized gap
            // below the muscle strip, making the scene look vertically split.
            val minSheetHeight = if (timerMode) 420.dp else 390.dp
            val maxSheetHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.66f)
                .coerceAtLeast(minSheetHeight)
            Column(
                Modifier.fillMaxWidth()
                    // Let the sheet grow naturally until the available lower
                    // viewport is exhausted. Only then does verticalScroll
                    // become useful; this preserves the rounded edges and
                    // keeps the next-exercise text visible whenever possible.
                    .heightIn(min = minSheetHeight, max = maxSheetHeight)
                    .animateContentSize(animationSpec = tween(280))
                     // iOS applies the sheet's default 8pt vertical inset;
                     // 16dp made the header float too far from the hero and
                     // left the Bicycle Crunch screen visibly split in two.
                     .padding(horizontal = 10.dp, vertical = 8.dp)
                    .shadow(12.dp, RoundedCornerShape(54.dp), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                    .clip(RoundedCornerShape(54.dp))
                    .background(WildforceThemeTokens.backgroundSecondary)
                    // ActiveWorkoutSceneView keeps the sheet clear of the
                    // home-indicator area. Without this outer inset the
                    // completion CTA is clipped at the bottom on gesture-nav
                    // devices, making the whole active screen look displaced.
                    .navigationBarsPadding()
                    .then(if (timerMode) Modifier else Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
                    // The set-style marker (for example the straight-set "=")
                    // starts the series phase.  Keep it outside the 54 dp
                    // corner arc instead of letting the sheet clip its square.
                    .padding(
                        start = 18.dp,
                         // Matches ActiveWorkoutSceneView's 12pt inline
                         // set-style instruction padding on iOS.
                         top = 12.dp,
                        end = 18.dp,
                        bottom = if (effectiveSets >= 4) 12.dp else 16.dp,
                    ),
                // Keep the sheet content flush to its top edge. Available
                // space is handled by the responsive hero; never introduce an
                // artificial blank band above the repetitions card.
                verticalArrangement = Arrangement.Top,
            ) {
                val phaseState = ActiveWorkoutPhaseState(
                    exerciseIndex = exerciseIndex,
                    exercise = exercise,
                    effectiveExercise = effectiveExercise,
                    effectiveSets = effectiveSets,
                    completedForExercise = completedForExercise,
                    completedSetRecords = completedSetRecords,
                    workout = workout,
                    useImperial = useImperial,
                    gender = gender,
                    restRemaining = restRemaining,
                    restInitialSeconds = restInitialSeconds,
                    restContext = restContext,
                    restBetweenExercises = restBetweenExercises,
                    currentPathBlock = currentPathBlock,
                    pendingFeedback = pendingFeedback,
                    selectedFeedback = selectedFeedback,
                    pendingNote = pendingNote,
                    completedDistanceKm = completedDistanceKm,
                    watchMotionSnapshot = watchMotionSnapshot,
                    restTimerPaused = restTimerPaused,
                    exerciseTimerRunning = exerciseTimerRunning,
                    exerciseTimeRemaining = exerciseTimeRemaining,
                    exerciseTimeInitial = exerciseTimeInitial,
                    reps = reps,
                    weightKg = weightKg,
                    notificationArtwork = notificationArtwork,
                )
                val phaseActions = ActiveWorkoutPhaseActions(
                    onFeedbackSelected = { selectedFeedback = it },
                    onNoteChanged = { pendingNote = it },
                    onDistanceChanged = { completedDistanceKm = it.coerceAtLeast(0.0) },
                    onEditFeedback = ::editFeedbackSet,
                    onContinueFeedback = ::continueFeedback,
                    onAddRestTime = ::addRestTime,
                    onRemoveRestTime = ::removeRestTime,
                    onSkipRest = ::skipRest,
                    onSetStyleInfo = { showsSetStyleInfo = true },
                    onRepsChanged = { reps = it },
                    onWeightChanged = { weightKg = it },
                    onToggleExerciseTimer = { exerciseTimerRunning = !exerciseTimerRunning },
                    onAddExerciseTime = ::addExerciseTime,
                    onRemoveExerciseTime = ::removeExerciseTime,
                    onRecordChanged = ::updateSetRecord,
                    onAddSet = ::addSet,
                    onCompleteCurrentStep = ::completeCurrentStep,
                )
                /*
                val resting = restRemaining
                AnimatedContent(
                    targetState = "${exerciseIndex}_${pendingFeedback}_${resting != null}",
                    transitionSpec = {
                        (slideInHorizontally(animationSpec = tween(320)) { fullWidth -> fullWidth } + fadeIn(tween(320)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(260)) { fullWidth -> -fullWidth } + fadeOut(tween(260)),
                            )
                    },
                    label = "active-workout-sheet-transition",
                ) { targetState ->
                    androidx.compose.runtime.key(targetState) {
                    // AnimatedContent lays out its content as a single
                    // transition node. Keep the exercise phase itself in a
                    // Column so the set editor, secondary actions and CTA
                    // participate in normal vertical measurement instead of
                    // being painted on top of one another.
                    Column(Modifier.fillMaxWidth()) {
                    if (pendingFeedback && exercise != null) {
                        ExerciseFeedbackContent(
                        exerciseName = exerciseDisplayName(exercise.imageKey, exercise.name),
                        selected = selectedFeedback,
                        note = pendingNote,
                        targetDistanceKm = exercise.targetDistanceKm,
                        completedDistanceKm = completedDistanceKm,
                        useImperial = useImperial,
                        onSelected = { selectedFeedback = it },
                        onNoteChanged = { pendingNote = it },
                        onDistanceChanged = { completedDistanceKm = it.coerceAtLeast(0.0) },
                        onContinue = {
                            selectedFeedback?.let { feedbackByExercise = feedbackByExercise + (exerciseIndex to it) }
                            pendingNote.trim().takeIf(String::isNotEmpty)?.let { notesByExercise = notesByExercise + (exerciseIndex to it) }
                            if (exercise.targetDistanceKm != null) {
                                val latestRecordIndex = completedSetRecords.indexOfLast { it.exerciseIndex == exerciseIndex }
                                if (latestRecordIndex >= 0) {
                                    completedSetRecords = completedSetRecords.toMutableList().also { records ->
                                        records[latestRecordIndex] = records[latestRecordIndex].copy(distanceKm = completedDistanceKm)
                                    }
                                }
                                val currentStats = exerciseStats[exerciseIndex] ?: ExerciseSessionStats()
                                exerciseStats = exerciseStats + (exerciseIndex to currentStats.copy(distanceKm = completedDistanceKm))
                            }
                            pendingFeedback = false
                            selectedFeedback = "JUSTO"
                            pendingNote = ""
                            // iOS applies rest for standard exercises only. A
                            // superset rests between rounds from its block,
                            // never after its final exercise.
                            advanceFromExercise(
                                if (exercise.blockType == WorkoutBlockType.Superset) 0 else exercise.restSeconds,
                            )
                        },
                        )
                    } else if (resting != null) {
                        RestTimerContent(
                        seconds = resting,
                        totalSeconds = restInitialSeconds,
                        context = restContext ?: if (restBetweenExercises) WorkoutRestContext.BeforeNextBlock else WorkoutRestContext.BetweenSets,
                        nextExercise = exercise,
                        nextRoundExercises = if (restContext == WorkoutRestContext.BetweenRounds) currentPathBlock?.exercises?.map { it.exercise }.orEmpty() else emptyList(),
                        gender = gender,
                        useImperial = useImperial,
                        onAddTime = {
                            restInitialSeconds += 30
                            val now = System.currentTimeMillis()
                            val current = resting.coerceAtLeast(0)
                            restEndsAtMillis = if (restTimerPaused) now + (current + 30) * 1_000L
                            else (restEndsAtMillis ?: now) + 30_000L
                            restTimerPaused = false
                            restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                        },
                        onRemoveTime = {
                            val now = System.currentTimeMillis()
                            val current = resting.coerceAtLeast(0)
                            val removed = minOf(30, current)
                            restInitialSeconds = (restInitialSeconds - removed).coerceAtLeast(0)
                            restEndsAtMillis = if (restTimerPaused) {
                                now + (current - removed).coerceAtLeast(0) * 1_000L
                            } else {
                                (restEndsAtMillis ?: now) - removed * 1_000L
                            }
                            restRemaining = ((restEndsAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                        },
                        onSkip = {
                            restRemaining = null
                            restEndsAtMillis = null
                            restTimerPaused = false
                            restBetweenExercises = false
                            restContext = null
                            exerciseTimerRunning = exercise?.resolvedTargetDurationSeconds() != null
                            val completedExercises = workout.exercises.indices.count { index ->
                                index < exerciseIndex || (completedByExercise[index] ?: 0) >= (workout.exercises[index].sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
                            }
                            runCatching { WorkoutActiveNotification.show(
                                context,
                                workout.title,
                                "${exercise?.let { exerciseDisplayName(it.imageKey, it.name) } ?: "Entrenamiento"} · ${completedExercises}/${workout.exercises.size} ejercicios",
                                progress = completedExercises,
                                progressMax = workout.exercises.size.coerceAtLeast(1),
                                type = WorkoutNotificationType.PROGRESS,
                                artwork = notificationArtwork,
                                exerciseIndex = exerciseIndex,
                                totalExercises = workout.exercises.size,
                                exerciseTimerSeconds = exerciseTimeRemaining,
                                canToggleExerciseTimer = exercise?.resolvedTargetDurationSeconds() != null,
                            ) }
                        },
                        )
                    } else if (exercise != null) {
                        val isWarmupOrCooldown = exercise.blockType == WorkoutBlockType.Warmup || exercise.blockType == WorkoutBlockType.Cooldown
                        if (isWarmupOrCooldown) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(if (exercise.blockType == WorkoutBlockType.Warmup) "PREPÁRATE PARA ENTRENAR" else "RECUPERA Y BAJA PULSACIONES", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("Completa el movimiento con control y sin buscar fatiga.", Modifier.padding(top = 6.dp, bottom = 16.dp), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            if (exercise.resolvedTargetDurationSeconds() != null) {
                                ExerciseWorkTimer(
                                    seconds = exerciseTimeRemaining ?: exerciseTimeInitial,
                                    initialSeconds = exerciseTimeInitial,
                                    running = exerciseTimerRunning,
                                    onToggle = { exerciseTimerRunning = !exerciseTimerRunning },
                                    onAddTime = {
                                        exerciseTimeRemaining = (exerciseTimeRemaining ?: 0) + 15
                                        exerciseTimeInitial += 15
                                    },
                                    onRemoveTime = {
                                        exerciseTimeRemaining = ((exerciseTimeRemaining ?: 0) - 15).coerceAtLeast(0)
                                        exerciseTimeInitial = (exerciseTimeInitial - 15).coerceAtLeast(0)
                                    },
                                )
                            }
                        }
                        } else {
                        if (exercise.blockType == WorkoutBlockType.Superset && currentPathBlock != null) {
                            SupersetRoundMap(currentPathBlock, exerciseIndex, exercise.blockRound)
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // iOS renders the set-style glyph in a 30pt frame
                            // plus 2pt padding (34pt overall). The previous
                            // 48dp Android box made this header dominate the
                            // sheet and displaced the rows below it.
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.textPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                SetStyleGlyph(exercise.setStyle, Modifier.size(17.dp), WildforceThemeTokens.backgroundSecondary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(exercise.setStyle.label, Modifier.padding(start = 10.dp), fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
                                setStyleScopeLabel(exercise.setStyle, effectiveSets, exercise.setStyleParameters.appliesToFinalSetOnly)?.let { scopeLabel ->
                                    Text(
                                        scopeLabel,
                                        Modifier.padding(start = 10.dp),
                                        color = WildforceThemeTokens.textSecondary,
                                        fontFamily = Exo2FontFamily,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                            Box(
                                Modifier.size(48.dp)
                                    .semantics { contentDescription = "Información de la serie" }
                                    .clickable { showsSetStyleInfo = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                // iOS keeps this as an accent-colored info.circle
                                // glyph; the large gray disk made this header look
                                // visually displaced even though its hit target is
                                // still 48pt.
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(22.dp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            styleParameterLabels(exercise.setStyle, exercise.setStyleParameters).forEach { label ->
                                Text(
                                    label,
                                    Modifier.clip(CircleShape)
                                        .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.06f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = WildforceThemeTokens.textPrimary,
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().height(6.dp).clearAndSetSemantics {
                            contentDescription = "Progreso de series"
                            stateDescription = "$completedForExercise de $effectiveSets series"
                            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                                completedForExercise.toFloat().coerceIn(0f, effectiveSets.coerceAtLeast(1).toFloat()),
                                0f..effectiveSets.coerceAtLeast(1).toFloat(),
                            )
                        }.focusable(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(effectiveSets) { setIndex ->
                                val setCompleted = setIndex < completedForExercise
                                val setCurrent = setIndex == completedForExercise
                                val setSegmentHeight by animateDpAsState(
                                    targetValue = if (setCurrent) 10.dp else 8.dp,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 650f),
                                    label = "set-progress-height-$setIndex",
                                )
                                val setSegmentScale by animateFloatAsState(
                                    targetValue = if (setCurrent) 1.05f else if (setCompleted) 1.02f else 1f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 650f),
                                    label = "set-progress-scale-$setIndex",
                                )
                                Box(
                                    Modifier.weight(1f).height(setSegmentHeight)
                                        .graphicsLayer { scaleX = setSegmentScale; scaleY = setSegmentScale }
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (setCompleted) WildforceThemeTokens.textPrimary else Color.Gray),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (exercise.resolvedTargetDurationSeconds() != null) {
                            ExerciseWorkTimer(
                                seconds = exerciseTimeRemaining ?: exerciseTimeInitial,
                                initialSeconds = exerciseTimeInitial,
                                running = exerciseTimerRunning,
                                onToggle = { exerciseTimerRunning = !exerciseTimerRunning },
                                onAddTime = {
                                    exerciseTimeRemaining = (exerciseTimeRemaining ?: 0) + 15
                                    exerciseTimeInitial += 15
                                },
                                onRemoveTime = {
                                    exerciseTimeRemaining = ((exerciseTimeRemaining ?: 0) - 15).coerceAtLeast(0)
                                    exerciseTimeInitial = (exerciseTimeInitial - 15).coerceAtLeast(0)
                                },
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            SetTrackingRows(exerciseIndex, effectiveExercise ?: exercise, completedForExercise, completedSetRecords, useImperial, showWeight = exerciseTracksWeight(effectiveExercise ?: exercise), currentEditor = if (timerMode) null else {
                                {
                                    ActiveSetEditor(
                                        targetReps = targetRepsLabel(exercise, completedForExercise),
                                        targetWeightKg = exercise.targetWeightsKg?.getOrNull(completedForExercise) ?: exercise.targetWeightKg ?: weightKg,
                                        reps = reps,
                                        weightKg = weightKg,
                                        useImperial = useImperial,
                                        showWeight = exerciseTracksWeight(exercise),
                                        isPerSideLoad = exercise.isPerSideLoad,
                                        onRepsChanged = { reps = it },
                                        onWeightChanged = { weightKg = it },
                                    )
                                }
                            }) { original, changed ->
                                val updatedRecords = completedSetRecords.map { if (it.exerciseIndex == exerciseIndex && it.setNumber == original.setNumber) changed else it }
                                totalVolumeKg += changed.reps * changed.weightKg - original.reps * original.weightKg
                                completedSetRecords = updatedRecords
                                exerciseStats = exerciseStats + (exerciseIndex to statsForExercise(updatedRecords, exerciseIndex))
                            }
                            WatchMotionInsightsSection(watchMotionSnapshot)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (exercise.blockType == WorkoutBlockType.Standard && exercise.resolvedTargetDurationSeconds() == null && effectiveSets < 20) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Row(Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(CircleShape)
                                    .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f))
                                    .semantics { contentDescription = "Añadir serie" }
                                    .clickable { addedSetsByExercise = addedSetsByExercise + (exerciseIndex to ((addedSetsByExercise[exerciseIndex] ?: 0) + 1)) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textPrimary)
                                    Text("AÑADIR SERIE", fontFamily = Exo2FontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                }
                            }
                        }
                        }
                        // Match iOS vertical rhythm: the add-set capsule is a
                        // secondary action and must not touch the completion CTA.
                        Spacer(Modifier.height(12.dp))
                        val completionLabel = when {
                        isWarmupOrCooldown || completedForExercise + 1 < effectiveSets ->
                            if (isWarmupOrCooldown) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE ${completedForExercise + 1}"
                        exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock ->
                            "COMPLETAR ${exercise.blockLabel.orEmpty()}"
                        exercise.blockType == WorkoutBlockType.Superset && exercise.blockRound < exercise.blockRounds ->
                            "FINALIZAR RONDA"
                        else -> "COMPLETAR EJERCICIO"
                    }
                        Button(
                        onClick = ::completeCurrentStep,
                        modifier = Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = completionLabel }, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                        ) {
                            Text(completionLabel, fontWeight = FontWeight.Bold)
                        }
                        val nextExercise = workout.exercises.getOrNull(exerciseIndex + 1)
                        if (nextExercise != null) {
                            val nextLabel = if (exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock) {
                                "Siguiente en el bloque: ${exerciseDisplayName(nextExercise.imageKey, nextExercise.name)}"
                            } else {
                                "Siguiente ejercicio: ${exerciseDisplayName(nextExercise.imageKey, nextExercise.name)}"
                            }
                            Text(
                                nextLabel,
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                style = MaterialTheme.typography.caption,
                                color = WildforceThemeTokens.textSecondary,
                            )
                        } else {
                            // ActiveWorkoutSceneView keeps a small trailing
                            // spacer when the exercise is the last one.
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    }
                    }
                }
                */
                ActiveWorkoutPhaseContent(phaseState, phaseActions)
            }
        }
    }
}

@Composable
private fun ActiveWorkoutHeroHeader(
    workout: WorkoutDaySummary,
    exerciseIndex: Int,
    exercise: ExerciseSummary?,
    completedByExercise: Map<Int, Int>,
    addedSetsByExercise: Map<Int, Int>,
    exerciseRecords: ActiveExerciseRecords?,
    currentPathBlock: WorkoutPathBlock?,
    canMoveDown: Boolean,
    showsExerciseMenu: Boolean,
    onExit: () -> Unit,
    onShowWorkoutPath: () -> Unit,
    onShowExerciseHistory: () -> Unit,
    onShowExerciseGuide: () -> Unit,
    onShowExerciseMenu: () -> Unit,
    onDismissExerciseMenu: () -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onSkipExercise: () -> Unit,
    onMoveExercise: () -> Unit,
    onCreateSuperset: () -> Unit,
    onRemoveSuperset: () -> Unit,
    onAddRound: () -> Unit,
    onRemoveRound: () -> Unit,
    onAddSupersetRest: () -> Unit,
    onRemoveSupersetRest: () -> Unit,
    onRemoveExercise: () -> Unit,
    onCancelWorkout: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        CenteredBackButton(onExit, 40.dp)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.68f) else Color.Black.copy(alpha = 0.46f))
                .border(1.dp, Color.White.copy(alpha = 0.46f), RoundedCornerShape(26.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActiveWorkoutToolbarAction(Icons.Filled.Timeline, "Ruta del entrenamiento", onShowWorkoutPath)
            ActiveWorkoutToolbarAction(Icons.Filled.Insights, "Datos del ejercicio", onShowExerciseHistory)
            ActiveWorkoutToolbarAction(Icons.AutoMirrored.Filled.Assignment, "Guía del ejercicio", onShowExerciseGuide)
            ActiveWorkoutToolbarAction(Icons.Filled.MoreHoriz, "Opciones del ejercicio", onShowExerciseMenu)
        }
        DropdownMenu(
            expanded = showsExerciseMenu,
            onDismissRequest = onDismissExerciseMenu,
            modifier = Modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)),
        ) {
            val canCreateSuperset = workout.editableBlocks().canCreateActiveSuperset(exerciseIndex)
            val canEditSuperset = currentPathBlock?.type == WorkoutBlockType.Superset
            ActiveWorkoutMenuItem("＋", "Añadir ejercicio", onClick = onAddExercise)
            ActiveWorkoutMenuItem("↔", "Sustituir ejercicio", onClick = onReplaceExercise)
            ActiveWorkoutMenuItem("⏭", "Saltar ejercicio", destructive = true, onClick = onSkipExercise)
            ActiveWorkoutMenuItem("↓", "Mover abajo", enabled = canMoveDown, onClick = onMoveExercise)
            if (canCreateSuperset) {
                Divider()
                ActiveWorkoutMenuItem("⛓", "Crear superserie", onClick = onCreateSuperset)
            }
            if (canEditSuperset && currentPathBlock != null) {
                Divider()
                ActiveWorkoutMenuItem("□", "Quitar superserie", onClick = onRemoveSuperset)
                ActiveWorkoutMenuItem("round+", "Añadir ronda", onClick = onAddRound)
                ActiveWorkoutMenuItem("round-", "Quitar ronda", enabled = currentPathBlock.rounds > 1, onClick = onRemoveRound)
                ActiveWorkoutMenuItem("◷", "Añadir 15 s de descanso", onClick = onAddSupersetRest)
                ActiveWorkoutMenuItem("◷", "Quitar 15 s de descanso", enabled = (currentPathBlock.restAfterBlockSeconds ?: 0) > 0, onClick = onRemoveSupersetRest)
            }
            Divider()
            ActiveWorkoutMenuItem("×", "Eliminar ejercicio", destructive = true, onClick = onRemoveExercise)
            Divider()
            ActiveWorkoutMenuItem("cancel", "Cancelar entrenamiento", destructive = true, onClick = onCancelWorkout)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(10.dp).clearAndSetSemantics {
            contentDescription = "Progreso del entrenamiento"
            stateDescription = "Ejercicio ${(exerciseIndex + 1).coerceAtMost(workout.exercises.size)} de ${workout.exercises.size}"
            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                (exerciseIndex + 1).toFloat().coerceIn(0f, workout.exercises.size.coerceAtLeast(1).toFloat()),
                0f..workout.exercises.size.coerceAtLeast(1).toFloat(),
            )
        }.focusable(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        workout.exercises.forEachIndexed { index, item ->
            val setsForItem = (item.sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
            val completedSets = completedByExercise[index] ?: 0
            val completed = index < exerciseIndex || completedSets >= setsForItem
            val current = index == exerciseIndex
            val segmentHeight by animateDpAsState(if (current) 10.dp else 8.dp, spring(dampingRatio = 0.7f, stiffness = 650f), label = "exercise-progress-height-$index")
            val segmentScale by animateFloatAsState(if (current) 1.05f else if (completed) 1.02f else 1f, spring(dampingRatio = 0.7f, stiffness = 650f), label = "exercise-progress-scale-$index")
            Box(
                Modifier.weight(1f).height(segmentHeight).align(Alignment.CenterVertically)
                    .graphicsLayer { scaleX = segmentScale; scaleY = segmentScale }
                    .clip(RoundedCornerShape(5.dp)).background(if (completed) Color.White.copy(alpha = 0.88f) else Color.Gray),
            )
        }
    }
    Column(Modifier.padding(start = 10.dp, top = 14.dp, end = 10.dp, bottom = 10.dp)) {
        Text(exercise?.let { exerciseDisplayName(it.imageKey, it.name) }.orEmpty(), fontFamily = Exo2FontFamily, fontSize = 26.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val contextLabel = exercise?.blockType?.takeIf { it != WorkoutBlockType.Standard }?.label?.uppercase() ?: "STANDARD"
                Text(contextLabel, Modifier.clip(CircleShape).background(Color.Black).padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                MuscleStrip(exercise?.imageKey, onDarkBackground = true, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                exerciseRecords?.let { records ->
                    records.pr?.let { PerformanceContextChip("PR", it) }
                    records.lastBest?.let { PerformanceContextChip("Último", it) }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutToolbarDialogs(
    workout: WorkoutDaySummary,
    gender: String,
    exerciseIndex: Int,
    exercise: ExerciseSummary?,
    completedByExercise: Map<Int, Int>,
    addedSetsByExercise: Map<Int, Int>,
    elapsedSeconds: Int,
    useImperial: Boolean,
    context: android.content.Context,
    showsWorkoutPath: Boolean,
    showsExerciseHistory: Boolean,
    onDismissWorkoutPath: () -> Unit,
    onAddExerciseToBlock: (Int) -> Unit,
    onDismissExerciseHistory: () -> Unit,
) {
    if (showsWorkoutPath) {
        Dialog(onDismissRequest = onDismissWorkoutPath, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxWidth().widthIn(max = 380.dp).height(520.dp).clip(RoundedCornerShape(28.dp))) {
                WorkoutPathScreen(
                    workout,
                    gender,
                    exerciseIndex,
                    completedByExercise,
                    addedSetsByExercise,
                    elapsedSeconds,
                    useImperial,
                    onAddExerciseToBlock = onAddExerciseToBlock,
                    onBack = onDismissWorkoutPath,
                )
            }
        }
    }
    if (showsExerciseHistory && exercise != null) {
        Dialog(onDismissRequest = onDismissExerciseHistory, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxWidth().widthIn(max = 380.dp).height(420.dp).clip(RoundedCornerShape(28.dp))) {
                ExerciseHistoryScreen(exercise, gender, WorkoutHistoryStore.history(context, exercise), useImperial, onDismissExerciseHistory)
            }
        }
    }
}

@Composable
private fun ActiveWorkoutConfirmationDialogs(
    showsSkipExerciseConfirmation: Boolean,
    showsRemoveExerciseConfirmation: Boolean,
    showsCancelWorkoutConfirmation: Boolean,
    isSkippingWholeBlock: Boolean,
    onDismissSkip: () -> Unit,
    onConfirmSkip: () -> Unit,
    onDismissRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onDismissCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    if (showsSkipExerciseConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissSkip,
            title = { Text(if (isSkippingWholeBlock) "¿SALTAR BLOQUE?" else "¿SALTAR EJERCICIO?", fontFamily = AntonFontFamily) },
            text = { Text(if (isSkippingWholeBlock) "Se omitirá todo el bloque actual y pasarás al siguiente." else "Este ejercicio quedará sin completar y pasarás al siguiente.") },
            confirmButton = { TextButton(onClick = onConfirmSkip) { Text("SALTAR", color = WildforceThemeTokens.accent) } },
            dismissButton = { TextButton(onClick = onDismissSkip) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
    if (showsRemoveExerciseConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRemove,
            title = { Text("¿ELIMINAR EJERCICIO?", fontFamily = AntonFontFamily) },
            text = { Text("Esta acción eliminará el ejercicio de este entrenamiento.") },
            confirmButton = { TextButton(onClick = onConfirmRemove) { Text("ELIMINAR", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = onDismissRemove) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
    if (showsCancelWorkoutConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissCancel,
            title = { Text("¿CANCELAR ENTRENAMIENTO?", fontFamily = AntonFontFamily) },
            text = { Text("Se eliminará todo el progreso de esta sesión.") },
            confirmButton = { TextButton(onClick = onConfirmCancel) { Text("CANCELAR ENTRENAMIENTO", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = onDismissCancel) { Text("SEGUIR ENTRENANDO", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

private data class ActiveWorkoutPhaseState(
    val exerciseIndex: Int,
    val exercise: ExerciseSummary?,
    val effectiveExercise: ExerciseSummary?,
    val effectiveSets: Int,
    val completedForExercise: Int,
    val completedSetRecords: List<CompletedSetRecord>,
    val workout: WorkoutDaySummary,
    val useImperial: Boolean,
    val gender: String,
    val restRemaining: Int?,
    val restInitialSeconds: Int,
    val restContext: WorkoutRestContext?,
    val restBetweenExercises: Boolean,
    val currentPathBlock: WorkoutPathBlock?,
    val pendingFeedback: Boolean,
    val selectedFeedback: String,
    val pendingNote: String,
    val completedDistanceKm: Double,
    val watchMotionSnapshot: WatchMotionSnapshotData,
    val restTimerPaused: Boolean,
    val exerciseTimerRunning: Boolean,
    val exerciseTimeRemaining: Int?,
    val exerciseTimeInitial: Int,
    val reps: Int,
    val weightKg: Double,
    val notificationArtwork: android.graphics.Bitmap?,
)

private data class ActiveWorkoutPhaseActions(
    val onFeedbackSelected: (String) -> Unit,
    val onNoteChanged: (String) -> Unit,
    val onDistanceChanged: (Double) -> Unit,
    val onEditFeedback: () -> Unit,
    val onContinueFeedback: () -> Unit,
    val onAddRestTime: () -> Unit,
    val onRemoveRestTime: () -> Unit,
    val onSkipRest: () -> Unit,
    val onSetStyleInfo: () -> Unit,
    val onRepsChanged: (Int) -> Unit,
    val onWeightChanged: (Double) -> Unit,
    val onToggleExerciseTimer: () -> Unit,
    val onAddExerciseTime: () -> Unit,
    val onRemoveExerciseTime: () -> Unit,
    val onRecordChanged: (CompletedSetRecord, CompletedSetRecord) -> Unit,
    val onAddSet: () -> Unit,
    val onCompleteCurrentStep: () -> Unit,
)

/**
 * Keeps the large active-workout phase renderer out of ActiveWorkoutScreen's
 * generated Compose method. Besides reducing startup/JIT work, this mirrors
 * iOS's separate exercise, rest and feedback phase views.
 */
@Composable
private fun ActiveWorkoutPhaseContent(
    state: ActiveWorkoutPhaseState,
    actions: ActiveWorkoutPhaseActions,
) {
    val resting = state.restRemaining
    AnimatedContent(
        targetState = "${state.exerciseIndex}_${state.pendingFeedback}_${resting != null}",
        transitionSpec = {
            (slideInHorizontally(animationSpec = tween(320)) { fullWidth -> fullWidth } + fadeIn(tween(320)))
                .togetherWith(
                    slideOutHorizontally(animationSpec = tween(260)) { fullWidth -> -fullWidth } + fadeOut(tween(260)),
                )
        },
        label = "active-workout-sheet-transition",
    ) { targetState ->
        androidx.compose.runtime.key(targetState) {
            Column(Modifier.fillMaxWidth()) {
                if (state.pendingFeedback && state.exercise != null) {
                    ExerciseFeedbackContent(
                        exerciseName = exerciseDisplayName(state.exercise.imageKey, state.exercise.name),
                        selected = state.selectedFeedback,
                        note = state.pendingNote,
                        targetDistanceKm = state.exercise.targetDistanceKm,
                        completedDistanceKm = state.completedDistanceKm,
                        useImperial = state.useImperial,
                        isTimeBased = state.exercise.resolvedTargetDurationSeconds() != null,
                        elapsedSeconds = (state.exerciseTimeInitial - (state.exerciseTimeRemaining ?: 0)).coerceAtLeast(0),
                        onSelected = actions.onFeedbackSelected,
                        onNoteChanged = actions.onNoteChanged,
                        onDistanceChanged = actions.onDistanceChanged,
                        onEditSets = if (state.exercise.resolvedTargetDurationSeconds() == null) actions.onEditFeedback else null,
                        onContinue = actions.onContinueFeedback,
                    )
                } else if (resting != null) {
                    RestTimerContent(
                        seconds = resting,
                        totalSeconds = state.restInitialSeconds,
                        context = state.restContext ?: if (state.restBetweenExercises) WorkoutRestContext.BeforeNextBlock else WorkoutRestContext.BetweenSets,
                        nextExercise = state.exercise,
                        nextRoundExercises = if (state.restContext == WorkoutRestContext.BetweenRounds) {
                            state.currentPathBlock?.exercises?.map { it.exercise }.orEmpty()
                        } else {
                            emptyList()
                        },
                        gender = state.gender,
                        useImperial = state.useImperial,
                        onAddTime = actions.onAddRestTime,
                        onRemoveTime = actions.onRemoveRestTime,
                        onSkip = actions.onSkipRest,
                    )
                } else if (state.exercise != null) {
                    val exercise = state.exercise
                    val isWarmupOrCooldown = exercise.blockType == WorkoutBlockType.Warmup || exercise.blockType == WorkoutBlockType.Cooldown
                    if (isWarmupOrCooldown) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                if (exercise.blockType == WorkoutBlockType.Warmup) "PREPÁRATE PARA ENTRENAR" else "RECUPERA Y BAJA PULSACIONES",
                                fontFamily = AntonFontFamily,
                                style = MaterialTheme.typography.h5,
                                color = WildforceThemeTokens.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Text(
                                "Completa el movimiento con control y sin buscar fatiga.",
                                Modifier.padding(top = 6.dp, bottom = 16.dp),
                                color = WildforceThemeTokens.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            if (exercise.resolvedTargetDurationSeconds() != null) {
                                ExerciseWorkTimer(
                                    seconds = state.exerciseTimeRemaining ?: state.exerciseTimeInitial,
                                    initialSeconds = state.exerciseTimeInitial,
                                    running = state.exerciseTimerRunning,
                                    onToggle = actions.onToggleExerciseTimer,
                                    onAddTime = actions.onAddExerciseTime,
                                    onRemoveTime = actions.onRemoveExerciseTime,
                                )
                            }
                        }
                    } else {
                        if (exercise.blockType == WorkoutBlockType.Superset && state.currentPathBlock != null) {
                            SupersetRoundMap(state.currentPathBlock, state.exerciseIndex, exercise.blockRound)
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.textPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                SetStyleGlyph(exercise.setStyle, Modifier.size(17.dp), WildforceThemeTokens.backgroundSecondary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(exercise.setStyle.label, Modifier.padding(start = 10.dp), fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
                                setStyleScopeLabel(exercise.setStyle, state.effectiveSets, exercise.setStyleParameters.appliesToFinalSetOnly)?.let { scopeLabel ->
                                    Text(scopeLabel, Modifier.padding(start = 10.dp), color = WildforceThemeTokens.textSecondary, fontFamily = Exo2FontFamily, fontSize = 14.sp)
                                }
                            }
                            Box(
                                Modifier.size(48.dp).semantics { contentDescription = "Información de la serie" }.clickable(onClick = actions.onSetStyleInfo),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(22.dp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            styleParameterLabels(exercise.setStyle, exercise.setStyleParameters).forEach { label ->
                                Text(
                                    label,
                                    Modifier.clip(CircleShape).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = WildforceThemeTokens.textPrimary,
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().height(6.dp).clearAndSetSemantics {
                                contentDescription = "Progreso de series"
                                stateDescription = "${state.completedForExercise} de ${state.effectiveSets} series"
                                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                                    state.completedForExercise.toFloat().coerceIn(0f, state.effectiveSets.coerceAtLeast(1).toFloat()),
                                    0f..state.effectiveSets.coerceAtLeast(1).toFloat(),
                                )
                            }.focusable(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            repeat(state.effectiveSets) { setIndex ->
                                val setCompleted = setIndex < state.completedForExercise
                                val setCurrent = setIndex == state.completedForExercise
                                val setSegmentHeight by animateDpAsState(
                                    targetValue = if (setCurrent) 10.dp else 8.dp,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 650f),
                                    label = "set-progress-height-$setIndex",
                                )
                                val setSegmentScale by animateFloatAsState(
                                    targetValue = if (setCurrent) 1.05f else if (setCompleted) 1.02f else 1f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 650f),
                                    label = "set-progress-scale-$setIndex",
                                )
                                Box(
                                    Modifier.weight(1f).height(setSegmentHeight)
                                        .graphicsLayer { scaleX = setSegmentScale; scaleY = setSegmentScale }
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (setCompleted) WildforceThemeTokens.textPrimary else Color.Gray),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (exercise.resolvedTargetDurationSeconds() != null) {
                            ExerciseWorkTimer(
                                seconds = state.exerciseTimeRemaining ?: state.exerciseTimeInitial,
                                initialSeconds = state.exerciseTimeInitial,
                                running = state.exerciseTimerRunning,
                                onToggle = actions.onToggleExerciseTimer,
                                onAddTime = actions.onAddExerciseTime,
                                onRemoveTime = actions.onRemoveExerciseTime,
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            SetTrackingRows(
                                state.exerciseIndex,
                                state.effectiveExercise ?: exercise,
                                state.completedForExercise,
                                state.completedSetRecords,
                                state.useImperial,
                                showWeight = exerciseTracksWeight(state.effectiveExercise ?: exercise),
                                currentEditor = if (state.restRemaining != null || exercise.resolvedTargetDurationSeconds() != null) null else {
                                    {
                                        ActiveSetEditor(
                                            targetReps = targetRepsLabel(exercise, state.completedForExercise),
                                            targetWeightKg = exercise.targetWeightsKg?.getOrNull(state.completedForExercise) ?: exercise.targetWeightKg ?: state.weightKg,
                                            reps = state.reps,
                                            weightKg = state.weightKg,
                                            useImperial = state.useImperial,
                                            showWeight = exerciseTracksWeight(exercise),
                                            isPerSideLoad = exercise.isPerSideLoad,
                                            onRepsChanged = actions.onRepsChanged,
                                            onWeightChanged = actions.onWeightChanged,
                                        )
                                    }
                                },
                                onRecordChanged = actions.onRecordChanged,
                            )
                            WatchMotionInsightsSection(state.watchMotionSnapshot)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (exercise.blockType == WorkoutBlockType.Standard && exercise.resolvedTargetDurationSeconds() == null && state.effectiveSets < 20) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Row(
                                    Modifier.heightIn(min = 48.dp)
                                        .clip(CircleShape)
                                        .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f))
                                        .semantics { contentDescription = "Añadir serie" }
                                        .clickable(onClick = actions.onAddSet)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textPrimary)
                                    Text("AÑADIR SERIE", fontFamily = Exo2FontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val completionLabel = when {
                        isWarmupOrCooldown || state.completedForExercise + 1 < state.effectiveSets ->
                            if (isWarmupOrCooldown) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE ${state.completedForExercise + 1}"
                        exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock -> "COMPLETAR ${exercise.blockLabel.orEmpty()}"
                        exercise.blockType == WorkoutBlockType.Superset && exercise.blockRound < exercise.blockRounds -> "FINALIZAR RONDA"
                        else -> "COMPLETAR EJERCICIO"
                    }
                    Button(
                        onClick = actions.onCompleteCurrentStep,
                        modifier = Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = completionLabel },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
                        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                    ) {
                        Text(completionLabel, fontWeight = FontWeight.Bold)
                    }
                    state.workout.exercises.getOrNull(state.exerciseIndex + 1)?.let { nextExercise ->
                        val nextLabel = if (exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock) {
                            "Siguiente en el bloque: ${exerciseDisplayName(nextExercise.imageKey, nextExercise.name)}"
                        } else {
                            "Siguiente ejercicio: ${exerciseDisplayName(nextExercise.imageKey, nextExercise.name)}"
                        }
                        Text(nextLabel, Modifier.fillMaxWidth().padding(top = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    } ?: Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SupersetRoundMap(block: WorkoutPathBlock, currentExerciseIndex: Int, currentRound: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.backgroundSecondary)
            }
            Text("SUPERSERIE · RONDA $currentRound/${block.rounds}", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.body2)
            Row(Modifier.weight(1f).height(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(block.rounds) { roundIndex ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(
                            if (roundIndex < currentRound) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = 0.22f),
                        ),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            block.exercises.forEachIndexed { index, item ->
                val roundIndex = item.executionIndices.getOrNull((currentRound - 1).coerceAtLeast(0))
                val current = roundIndex == currentExerciseIndex
                Text(
                    "${item.label.orEmpty()} ${exerciseDisplayName(item.exercise.imageKey, item.exercise.name)}",
                    Modifier.clip(CircleShape)
                        .background(if (current) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (current) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textSecondary,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (index < block.exercises.lastIndex) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textSecondary)
                }
            }
        }
    }
}

internal fun exerciseTracksWeight(exercise: ExerciseSummary): Boolean {
    if (exercise.targetWeightKg != null || !exercise.targetWeightsKg.isNullOrEmpty()) return true
    return ExerciseVisualCatalog.equipmentFor(exercise.imageKey).any { it != "Peso corporal" }
}

@Composable
private fun SetTrackingRows(
    exerciseIndex: Int,
    exercise: ExerciseSummary,
    completedSets: Int,
    records: List<CompletedSetRecord>,
    useImperial: Boolean,
    showWeight: Boolean,
    currentEditor: (@Composable () -> Unit)? = null,
    onRecordChanged: (CompletedSetRecord, CompletedSetRecord) -> Unit,
) {
    var editingSetNumber by remember(exerciseIndex) { mutableStateOf<Int?>(null) }
    var editReps by remember(exerciseIndex) { mutableStateOf(0) }
    var editWeightKg by remember(exerciseIndex) { mutableStateOf(0.0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    repeat(exercise.sets) { setIndex ->
        val setNumber = setIndex + 1
        val completed = setIndex < completedSets
        val current = setIndex == completedSets
        // Mirrors the iOS checkmark's spring/bounce when a set flips from pending
        // to complete, rather than only animating insertion/removal of the row.
        val completionScale by animateFloatAsState(
            targetValue = if (completed) 1f else .72f,
            animationSpec = spring(dampingRatio = .5f, stiffness = 520f),
            label = "set-completion-$exerciseIndex-$setNumber",
        )
        val record = records.lastOrNull { it.exerciseIndex == exerciseIndex && it.setNumber == setNumber }
        if (current && currentEditor != null) {
            // Keep the current editor in the parent Column's normal measure
            // pass. AnimatedVisibility uses a transient overlay/measurement
            // while entering, which can place the completion CTA over the
            // editor on a tall emulator. Exercise changes are still animated
            // by the surrounding AnimatedContent.
            currentEditor()
        } else if (!current) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
            ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (completed) Brush.horizontalGradient(listOf(Color(0x162EAF68), Color(0x082EAF68))) else Brush.horizontalGradient(listOf(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), WildforceThemeTokens.textSecondary.copy(alpha = 0.07f))), RoundedCornerShape(14.dp))
                    .clickable(enabled = record != null) {
                        record?.let { editingSetNumber = setNumber; editReps = it.reps; editWeightKg = it.weightKg }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (completed) Icon(Icons.Filled.CheckCircle, contentDescription = "Serie completada", modifier = Modifier.width(34.dp).graphicsLayer { scaleX = completionScale; scaleY = completionScale }, tint = Color(0xFF2EAF68))
                else Text(setNumber.toString(), Modifier.width(34.dp).graphicsLayer { scaleX = completionScale; scaleY = completionScale }, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reps", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(if (record != null) record.reps.toString() else targetRepsLabel(exercise, setIndex), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                }
                if (showWeight) {
                    Row(Modifier.weight(1.25f), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (exercise.isPerSideLoad) "Peso/lado" else "Peso", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text(record?.let { formatTrainingWeight(it.weightKg, useImperial) } ?: exercise.targetWeightsKg?.getOrNull(setIndex)?.let { formatTrainingWeight(it, useImperial) } ?: exercise.targetWeightKg?.let { formatTrainingWeight(it, useImperial) }.orEmpty().ifBlank { "—" }, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            }
        }
        if (!current && editingSetNumber == setNumber && record != null) {
            Column(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
                Text("CORREGIR SERIE $setNumber", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
                ActiveSetEditor(
                    targetReps = targetRepsLabel(exercise, setIndex),
                    targetWeightKg = exercise.targetWeightsKg?.getOrNull(setIndex) ?: exercise.targetWeightKg ?: editWeightKg,
                    reps = editReps,
                    weightKg = editWeightKg,
                    useImperial = useImperial,
                    showWeight = showWeight,
                    isPerSideLoad = exercise.isPerSideLoad,
                    onRepsChanged = { editReps = it },
                    onWeightChanged = { editWeightKg = it },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("CANCELAR", Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Cancelar" }.clickable { editingSetNumber = null }.padding(horizontal = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                    Text("GUARDAR", Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Guardar" }.clickable {
                        onRecordChanged(record, record.copy(reps = editReps, weightKg = editWeightKg))
                        editingSetNumber = null
                    }.padding(10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
                }
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
    showWeight: Boolean = true,
    isPerSideLoad: Boolean = false,
    onRepsChanged: (Int) -> Unit,
    onWeightChanged: (Double) -> Unit,
) {
    var editingWeight by remember { mutableStateOf<Boolean?>(null) }
    val weightStep = if (useImperial) 5.0 / KG_TO_LB else 2.5
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            // This is the iOS expanded current-set row: one subtle card,
            // compact metric rows inside it, and no oversized nested panels.
            .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActiveSetMetricRow(
            label = "Reps",
            target = "Objetivo $targetReps",
            value = reps.toString(),
            onMinus = { onRepsChanged((reps - 1).coerceAtLeast(0)) },
            onPlus = { onRepsChanged(reps + 1) },
            onEdit = { editingWeight = false },
        )
        if (showWeight) {
            ActiveSetMetricRow(
                label = (if (isPerSideLoad) "Peso/lado" else "Peso") + if (useImperial) " (lb)" else " (kg)",
                target = "Objetivo ${formatTrainingWeight(targetWeightKg, useImperial)}",
                value = if (useImperial) "%.1f".format(Locale.US, weightKg * KG_TO_LB) else "%.1f".format(Locale.US, weightKg),
                onMinus = { onWeightChanged((weightKg - weightStep).coerceAtLeast(0.0)) },
                onPlus = { onWeightChanged(weightKg + weightStep) },
                onEdit = { editingWeight = true },
            )
        }
    }
    editingWeight?.let { isWeight ->
        WorkoutNumericInputDialog(
            onDismissRequest = { editingWeight = null },
            title = if (isWeight) "EDITAR PESO" else "EDITAR REPETICIONES",
            initialValue = if (isWeight) {
                if (useImperial) weightKg * KG_TO_LB else weightKg
            } else {
                reps.toDouble()
            },
            allowsDecimal = isWeight,
            step = if (isWeight) if (useImperial) 5.0 else 2.5 else 1.0,
            onSave = { value ->
                if (isWeight) {
                    onWeightChanged((if (useImperial) value / KG_TO_LB else value).coerceIn(0.0, 750.0))
                } else {
                    onRepsChanged(value.toInt().coerceIn(0, 999))
                }
                editingWeight = null
            },
        )
    }
}

/**
 * Compact value editor for the active set. It uses the system numeric keyboard
 * for direct entry and exposes the usual training increment as +/- controls.
 */
@Composable
private fun WorkoutNumericInputDialog(
    title: String,
    initialValue: Double,
    allowsDecimal: Boolean,
    step: Double,
    onDismissRequest: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var draft by remember(title, initialValue, allowsDecimal) {
        mutableStateOf(if (allowsDecimal) String.format(Locale.US, "%.1f", initialValue) else initialValue.toInt().toString())
    }
    fun parsedDraft(): Double? = draft.replace(',', '.').toDoubleOrNull()
    fun applyStep(direction: Double) {
        val next = ((parsedDraft() ?: initialValue) + direction * step).coerceAtLeast(0.0)
        draft = if (allowsDecimal) String.format(Locale.US, "%.1f", next) else next.toInt().toString()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, fontFamily = AntonFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                TextField(
                    value = draft,
                    onValueChange = { draft = sanitizeNumericInput(it, allowsDecimal) },
                    modifier = Modifier.fillMaxWidth()
                        .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = Exo2FontFamily,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        color = WildforceThemeTokens.textPrimary,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowsDecimal) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenteredRoundControl("−", 56.dp, { applyStep(-1.0) }, "Reducir ${numberForInput(step, allowsDecimal)}")
                    Text(
                        numberForInput(step, allowsDecimal),
                        Modifier.width(88.dp),
                        fontFamily = Exo2FontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = WildforceThemeTokens.textSecondary,
                    )
                    CenteredRoundControl("+", 56.dp, { applyStep(1.0) }, "Aumentar ${numberForInput(step, allowsDecimal)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsedDraft()?.let(onSave) }) {
                Text("LISTO", color = WildforceThemeTokens.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
    )
}

private fun numberForInput(value: Double, allowsDecimal: Boolean): String =
    if (allowsDecimal) String.format(Locale.US, "%.1f", value) else value.toInt().toString()

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
        // iOS expanded rows use a fixed title, a small target caption and the
        // compact controls on the same baseline. Do not switch to a vertical
        // layout: an S23 Ultra can cross an arbitrary dp breakpoint when its
        // display or font scale changes, even though it has ample screen area.
        val narrowControls = maxWidth < 390.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontFamily = Exo2FontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(target, Modifier.weight(1f).padding(start = 8.dp), fontFamily = Exo2FontFamily, fontSize = 14.sp, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // SF Symbols minus.circle.fill/plus.circle.fill render as
            // compact ~22pt controls on iOS, not large filled buttons.
            MetricControls(value, onMinus, onPlus, onEdit, controlSize = if (narrowControls) 26.dp else 28.dp, accessibilityLabel = label)
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
    accessibilityLabel: String = "Valor",
) {
    // iOS uses `HStack(spacing: 10)` around a 48 pt value frame. Keep that
    // visible rhythm exactly; CenteredRoundControl expands only its hit area.
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CenteredRoundControl("−", controlSize, onMinus, "Reducir $accessibilityLabel")
        Text(
            value,
            Modifier
                .widthIn(min = 48.dp, max = 64.dp)
                .semantics { contentDescription = "Editar $accessibilityLabel"; stateDescription = value }
                .clickable(onClick = onEdit)
                .padding(horizontal = 4.dp),
            fontFamily = Exo2FontFamily,
            color = WildforceThemeTokens.textPrimary,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CenteredRoundControl("+", controlSize, onPlus, "Aumentar $accessibilityLabel")
    }
}
@Composable
private fun ExerciseFeedbackContent(
    exerciseName: String,
    selected: String?,
    note: String,
    targetDistanceKm: Double?,
    completedDistanceKm: Double,
    useImperial: Boolean,
    isTimeBased: Boolean = false,
    elapsedSeconds: Int = 0,
    onSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDistanceChanged: (Double) -> Unit,
    onEditSets: (() -> Unit)? = null,
    onContinue: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val choices = listOf(
        // Keep these in the same order and with the same icons as
        // ExerciseFeedback.icon in the canonical iOS implementation.
        Triple("😴", "MUY FÁCIL", "Podías haber hecho muchas más repeticiones."),
        Triple("😊", "FÁCIL", "Te quedaban varias repeticiones en reserva."),
        Triple("👍", "JUSTO", "El esfuerzo y la técnica han sido adecuados."),
        Triple("🥵", "DIFÍCIL", "Has terminado cerca de tu límite."),
        Triple("💀", "MUY DIFÍCIL", "No habrías podido completar otra repetición."),
    )
    // This content already lives inside the active sheet's verticalScroll.
    // A second scroll container receives infinite height constraints and
    // crashes Compose when feedback appears after completing a set.
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isTimeBased) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("TUS RESULTADOS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duración", color = WildforceThemeTokens.textSecondary)
                    Text(formatClock(elapsedSeconds), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                }
                targetDistanceKm?.let {
                    FeedbackDistanceStepper(
                        completedDistanceKm = completedDistanceKm,
                        useImperial = useImperial,
                        onDistanceChanged = onDistanceChanged,
                    )
                }
            }
        }
        onEditSets?.let { editSets ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Editar series" }
                    .clickable(onClick = editSets)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                Text("Editar series", color = WildforceThemeTokens.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("EJERCICIO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(exerciseName, color = WildforceThemeTokens.textSecondary)
        Text("¿CÓMO HA IDO?", Modifier.fillMaxWidth().padding(top = 12.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("Esta información ayudará a personalizar las siguientes sesiones.", Modifier.fillMaxWidth().padding(top = 2.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { (emoji, title, _) ->
                val isSelected = selected == title
                val selectionOffset by animateDpAsState(
                    targetValue = if (isSelected) (-6).dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
                    label = "feedback-offset-$title",
                )
                val selectionScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
                    label = "feedback-scale-$title",
                )
                Column(
                    Modifier.weight(1f)
                        .offset(y = selectionOffset)
                        .graphicsLayer { scaleX = selectionScale; scaleY = selectionScale }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.12f))
                        .border(1.dp, if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .semantics {
                            contentDescription = "Esfuerzo $title"
                            stateDescription = if (isSelected) "Seleccionado" else "No seleccionado"
                        }
                        .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelected(title) }
                        .heightIn(min = 92.dp)
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    Text(emoji, style = MaterialTheme.typography.h6)
                    Text(title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = if (isSelected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
        val description = choices.firstOrNull { it.second == selected }?.third ?: "Selecciona la sensación que mejor describa el ejercicio."
        Text(description, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        if (!isTimeBased && targetDistanceKm != null) {
            FeedbackDistanceStepper(completedDistanceKm, useImperial, onDistanceChanged)
        }
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
        Button(onClick = onContinue, enabled = selected != null, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) {
            Text("MARCAR COMO COMPLETADO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackDistanceStepper(
    completedDistanceKm: Double,
    useImperial: Boolean,
    onDistanceChanged: (Double) -> Unit,
) {
    val displayedDistance = if (useImperial) completedDistanceKm * MILES_PER_KM else completedDistanceKm
    val distanceStepKm = if (useImperial) 0.1 / MILES_PER_KM else 0.1
    CompactMetricStepper(
        title = if (useImperial) "DISTANCIA (MI)" else "DISTANCIA (KM)",
        value = String.format(Locale.US, "%.1f", displayedDistance),
        inputValue = String.format(Locale.US, "%.1f", displayedDistance),
        decimalInput = true,
        onMinus = { onDistanceChanged((completedDistanceKm - distanceStepKm).coerceAtLeast(0.0)) },
        onPlus = { onDistanceChanged(completedDistanceKm + distanceStepKm) },
        onValueEntered = { input ->
            input.replace(',', '.').toDoubleOrNull()?.let { entered ->
                onDistanceChanged(if (useImperial) entered / MILES_PER_KM else entered)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

@Composable
private fun RestTimerContent(
    seconds: Int,
    totalSeconds: Int,
    context: WorkoutRestContext,
    nextExercise: ExerciseSummary?,
    nextRoundExercises: List<ExerciseSummary>,
    gender: String,
    useImperial: Boolean,
    onAddTime: () -> Unit,
    onRemoveTime: () -> Unit,
    onSkip: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val animatedProgress by animateFloatAsState((seconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f), animationSpec = tween(1_000, easing = LinearEasing))
    Column(
        // Match the iOS rest phase: its VStack takes only the space its
        // contents need. Filling the sheet here was making Compose center the
        // timer inside the sheet's maximum height, leaving empty bands above
        // and below it.
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            when (context) {
                WorkoutRestContext.BetweenSets -> "DESCANSO ANTES DE LA SIGUIENTE SERIE"
                WorkoutRestContext.BetweenRounds -> "DESCANSO ANTES DE LA SIGUIENTE RONDA"
                WorkoutRestContext.BeforeNextBlock -> "DESCANSO ANTES DEL SIGUIENTE BLOQUE"
            },
            fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RestActionButton(Icons.Filled.Add, "+30s", primary = false, onClick = onAddTime)
            Box(
                Modifier.padding(horizontal = 8.dp).size(148.dp).semantics {
                    contentDescription = "Temporizador de descanso, $seconds segundos restantes"
                    stateDescription = when (context) {
                        WorkoutRestContext.BetweenSets -> "Antes de la siguiente serie"
                        WorkoutRestContext.BetweenRounds -> "Antes de la siguiente ronda"
                        WorkoutRestContext.BeforeNextBlock -> "Antes del siguiente bloque"
                    }
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(animatedProgress, 0f..1f)
                        customActions = listOf(
                            CustomAccessibilityAction("Añadir 30 segundos") {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAddTime()
                                true
                            },
                            CustomAccessibilityAction("Quitar 30 segundos") {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRemoveTime()
                                true
                            },
                        )
                },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxSize(), color = WildforceThemeTokens.accent,
                    backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.18f), strokeWidth = 12.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${seconds}s", fontFamily = Exo2FontFamily, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(
                        when (context) {
                            WorkoutRestContext.BetweenSets -> "SERIE"
                            WorkoutRestContext.BetweenRounds -> "RONDA"
                            WorkoutRestContext.BeforeNextBlock -> "BLOQUE"
                        },
                        style = MaterialTheme.typography.caption,
                        color = WildforceThemeTokens.textSecondary,
                    )
                }
            }
            RestActionButton(Icons.Filled.FastForward, "OMITIR", primary = true, onClick = {
                onSkip()
            })
        }
        if (context == WorkoutRestContext.BetweenRounds && nextRoundExercises.isNotEmpty()) {
            Text("SIGUIENTE RONDA", Modifier.fillMaxWidth().padding(start = 10.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nextRoundExercises.forEachIndexed { index, roundExercise ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A${index + 1}", Modifier.background(WildforceThemeTokens.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)).padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        RemoteTrainingImage(exerciseImageUrl(roundExercise.imageKey, gender), null, Modifier.padding(start = 8.dp).size(42.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(exerciseDisplayName(roundExercise.imageKey, roundExercise.name), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            RestTimerExercisePrescription(roundExercise, useImperial)
                        }
                    }
                }
            }
        } else if (context != WorkoutRestContext.BetweenSets && nextExercise != null) {
            Text("A CONTINUACIÓN", Modifier.fillMaxWidth().padding(start = 10.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteTrainingImage(exerciseImageUrl(nextExercise.imageKey, gender), null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(exerciseDisplayName(nextExercise.imageKey, nextExercise.name), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RestTimerExercisePrescription(nextExercise, useImperial)
                }
            }
        }
    }
}

/** iOS's compact PlannedExercicePrescriptionView equivalent for the rest sheet. */
@Composable
private fun RestTimerExercisePrescription(exercise: ExerciseSummary, useImperial: Boolean) {
    val prescription = restTimerExercisePrescription(exercise, useImperial)
    if (prescription.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            prescription.zip(restTimerPrescriptionIcons(exercise)).forEach { (value, icon) ->
                RestTimerPrescriptionMetric(
                    icon = icon,
                    text = value,
                )
            }
        }
    }
}

/** Mirrors the symbols that accompany the compact prescription on iOS. */
@Composable
private fun RestTimerPrescriptionMetric(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = WildforceThemeTokens.textSecondary,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.caption,
            color = WildforceThemeTokens.textSecondary,
            maxLines = 1,
        )
    }
}

private fun restTimerPrescriptionIcons(exercise: ExerciseSummary): List<ImageVector> = buildList {
    if (exercise.sets > 0) add(Icons.Filled.Layers)
    when (exercise.trackingMode) {
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> {
            val hasReps = exercise.repRange != null || !exercise.targetRepsPerSet.isNullOrEmpty() || exercise.reps.isNotBlank()
            if (hasReps) add(Icons.Filled.FormatListNumbered)
        }
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration,
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> {
            exercise.targetDurationMinutes?.takeIf { it > 0 }?.let { add(Icons.Filled.AccessTime) }
            exercise.targetDurationSeconds?.takeIf { it > 0 }?.let { add(Icons.Filled.AccessTime) }
        }
    }
    if ((exercise.targetWeightKg ?: exercise.targetWeightsKg?.maxOrNull() ?: 0.0) > 0.0) add(Icons.Filled.Scale)
    if ((exercise.targetDistanceKm ?: 0.0) > 0.0) add(Icons.Filled.MyLocation)
}

/** Typed compact prescription shared by the next-exercise and next-round cards. */
internal fun restTimerExercisePrescription(exercise: ExerciseSummary, useImperial: Boolean): List<String> = buildList {
    exercise.sets.takeIf { it > 0 }?.let { sets -> add("$sets ${if (sets == 1) "serie" else "series"}") }
    when (exercise.trackingMode) {
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> {
            val reps = exercise.repRange?.displayText
                ?: exercise.targetRepsPerSet?.takeIf { it.isNotEmpty() }?.let { targets ->
                    val minimum = targets.minOrNull() ?: 0
                    val maximum = targets.maxOrNull() ?: minimum
                    if (minimum == maximum) minimum.toString() else "$minimum-$maximum"
                }
                ?: exercise.reps
            reps.takeIf { it.isNotBlank() }?.let { add("$it reps") }
        }
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration,
        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> {
            exercise.targetDurationMinutes?.takeIf { it > 0 }?.let { add("$it min") }
            exercise.targetDurationSeconds?.takeIf { it > 0 }?.let { add("$it s") }
        }
    }
    val weight = exercise.targetWeightKg ?: exercise.targetWeightsKg?.maxOrNull()
    weight?.takeIf { it > 0.0 }?.let { add(formatTrainingWeight(it, useImperial)) }
    exercise.targetDistanceKm?.takeIf { it > 0.0 }?.let { kilometers ->
        val distance = if (useImperial) kilometers * 0.621371 else kilometers
        add("${"%.1f".format(Locale.US, distance)} ${if (useImperial) "mi" else "km"}")
    }
}

@Composable
private fun RestActionButton(icon: ImageVector, label: String, primary: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
            .background(if (primary) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f))
            .semantics { contentDescription = label }
            .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }.padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (primary) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = if (primary) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}
@Composable
private fun CompactMetricStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, inputValue: String = value, decimalInput: Boolean = false, onValueEntered: ((String) -> Unit)? = null) {
    var showManualInput by remember { mutableStateOf(false) }
    var manualInput by remember(inputValue) { mutableStateOf(TextFieldValue(inputValue)) }
    // Compose does not provide SwiftUI's numericText transition. A short
    // spring pulse keeps every +/- change legible without shifting the row.
    val numericScale = remember(value) { Animatable(.84f) }
    LaunchedEffect(value) { numericScale.animateTo(1f, spring(dampingRatio = .58f, stiffness = 520f)) }
    val manualInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CenteredRoundControl("−", 40.dp, onMinus)
            Text(
                value,
                Modifier.widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = title
                        stateDescription = value
                    }
                    .graphicsLayer { scaleX = numericScale.value; scaleY = numericScale.value }
                    .clickable(enabled = onValueEntered != null) { manualInput = TextFieldValue(inputValue); showManualInput = true }
                    .padding(horizontal = 3.dp),
                fontWeight = FontWeight.Bold,
                color = WildforceThemeTokens.textPrimary,
                maxLines = 1,
            )
            CenteredRoundControl("+", 40.dp, onPlus)
        }
    }
    if (showManualInput && onValueEntered != null) {
        LaunchedEffect(showManualInput) {
            manualInput = manualInput.copy(selection = TextRange(0, manualInput.text.length))
            manualInputFocusRequester.requestFocus()
            keyboardController?.show()
        }
        AlertDialog(
            onDismissRequest = { showManualInput = false },
            title = { Text("EDITAR $title", fontFamily = AntonFontFamily) },
            text = { TextField(manualInput, { value ->
                val sanitized = sanitizeNumericInput(value.text, allowsDecimal = decimalInput)
                manualInput = value.copy(text = sanitized, selection = TextRange(sanitized.length))
            }, modifier = Modifier.focusRequester(manualInputFocusRequester), label = { Text(if (decimalInput) "Valor" else "Repeticiones") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (decimalInput) KeyboardType.Decimal else KeyboardType.Number)) },
            confirmButton = { TextButton(onClick = { onValueEntered(manualInput.text); showManualInput = false }) { Text("GUARDAR", color = WildforceThemeTokens.accent) } },
            dismissButton = { TextButton(onClick = { showManualInput = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

/** Allows digits and, for decimal values, one locale-neutral decimal separator only. */
private fun sanitizeNumericInput(value: String, allowsDecimal: Boolean): String = buildString {
    var hasDecimalSeparator = false
    value.forEach { character ->
        when {
            character.isDigit() -> append(character)
            allowsDecimal && !hasDecimalSeparator && (character == ',' || character == '.') -> {
                append(character)
                hasDecimalSeparator = true
            }
        }
    }
}.take(8)

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
private fun CenteredRoundControl(glyph: String, size: Dp, onClick: () -> Unit, accessibilityLabel: String? = null) {
    val haptics = LocalHapticFeedback.current
    // Its measured width is the visible circle, matching the compact iOS
    // HStack. The larger child deliberately overflows by equal amounts on
    // both sides, preserving the required 48 dp Android hit target without
    // changing the visual gap to its neighbours.
    Box(
        Modifier.width(size).height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp)
                .semantics { contentDescription = accessibilityLabel ?: if (glyph == "+") "Aumentar" else "Reducir" }
                .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(size).clip(CircleShape).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (glyph == "+") Icons.Filled.Add else Icons.Filled.Remove,
                    contentDescription = null,
                    tint = WildforceThemeTokens.backgroundSecondary,
                    modifier = Modifier.size(size * 0.45f),
                )
            }
        }
    }
}

@Composable
private fun CenteredBackButton(onClick: () -> Unit, size: Dp, padding: Dp = 0.dp) {
    Box(
        Modifier.padding(padding).size(maxOf(size, 48.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(WildforceThemeTokens.imageControlBackground), contentAlignment = Alignment.Center) {
            // iOS renders the navigation chevron at its standard toolbar size.
            // A 22 dp Material icon has the same visible glyph size once the
            // vector's built-in viewport padding is accounted for. Keep this
            // fixed even when a screen uses a different circular hit target.
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.imageControlContent, modifier = Modifier.size(22.dp))
        }
    }
}

private fun targetReps(value: String?): Int = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull() ?: 10

private fun targetRepsLabel(exercise: ExerciseSummary, setIndex: Int): String {
    exercise.repRange?.displayText?.let { return it }
    exercise.targetRepsPerSet?.getOrNull(setIndex)?.toString()?.let { return it }
    exercise.targetRepsPerSet?.lastOrNull()?.toString()?.let { return it }
    // A few legacy payloads carried a duration-formatted `reps` value even
    // though the exercise was typed as repetitions. Never expose that value
    // in a rep row; use its numeric component as the safe fallback.
    return exercise.reps.takeUnless { it.trim().matches(Regex("\\d+\\s*s", RegexOption.IGNORE_CASE)) }
        ?: targetReps(exercise.reps).toString()
}
private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
internal const val KG_TO_LB = 2.20462262
internal const val MILES_PER_KM = 0.621371192
internal fun formatTrainingWeight(kilograms: Double, useImperial: Boolean): String = if (useImperial) String.format(Locale.getDefault(), "%.1f lb", kilograms * KG_TO_LB) else String.format(Locale.getDefault(), "%.1f kg", kilograms)


internal fun setStyleInstruction(style: ExerciseSetStyle, setNumber: Int, totalSets: Int, appliesToFinalSetOnly: Boolean = false): String = when (style) {
    ExerciseSetStyle.Warmup -> "APROXIMACIÓN"
    ExerciseSetStyle.Straight -> "SERIE NORMAL"
    ExerciseSetStyle.TopSetBackoff -> if (setNumber == 1) "TOP SET" else "BACKOFF"
    ExerciseSetStyle.AscendingPyramid -> if (setNumber == totalSets) "PESO MÁXIMO" else "SUBE EL PESO"
    ExerciseSetStyle.DropSet -> if (!appliesToFinalSetOnly || setNumber == totalSets) "DROP SET · SIN DESCANSO" else "SERIE BASE"
    ExerciseSetStyle.RestPause -> if (!appliesToFinalSetOnly || setNumber == totalSets) "REST-PAUSE · PAUSA BREVE" else "SERIE BASE"
    ExerciseSetStyle.Intervals -> "INTERVALO"
    ExerciseSetStyle.Tempo -> "TEMPO CONTROLADO"
}
private fun statsForExercise(records: List<CompletedSetRecord>, exerciseIndex: Int): ExerciseSessionStats {
    val exerciseRecords = records.filter { it.exerciseIndex == exerciseIndex }
    return ExerciseSessionStats(
        sets = exerciseRecords.size,
        totalReps = exerciseRecords.sumOf { it.reps },
        maxWeightKg = exerciseRecords.maxOfOrNull { it.weightKg } ?: 0.0,
        volumeKg = exerciseRecords.sumOf { it.reps * it.weightKg },
        durationSeconds = exerciseRecords.sumOf { it.durationSeconds },
        distanceKm = exerciseRecords.sumOf { it.distanceKm },
        averageWeightKg = exerciseRecords.map { it.weightKg }.filter { it > 0 }.average().takeUnless(Double::isNaN) ?: 0.0,
    )
}
