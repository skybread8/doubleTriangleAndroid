package io.codepassion.doubletriangle.feature.workout

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.text.DateFormat
import java.util.Date
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
    requiresPlanRegeneration: Boolean = false,
    onRecreatePlan: () -> Unit = {},
    avatarPath: String? = null,
    avatarRevision: Int = 0,
    customAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null,
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
    if (showingMesocycleDetails) {
        WorkoutMesocycleDetailScreen(
            state = state,
            planHistory = planHistory,
            contentPadding = contentPadding,
            onBack = { showingMesocycleDetails = false },
        )
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
        if (requiresPlanRegeneration) {
            ProfileChangedPlanBanner(onRecreatePlan)
        }
        // iOS keeps this header deliberately quiet: identity and the week selector live
        // together on the secondary surface, while plan context starts below the mode picker.
        AnimatedVisibility(visible = hubEntered, enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { -it / 18 }) {
            Column(
                Modifier.fillMaxWidth()
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                WorkoutHeader(state, avatarPath, avatarRevision)
                WeekCalendar(
                    state = state,
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = if (selectedDay == it) null else it },
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
        AnimatedVisibility(mode == WorkoutMode.Plan) {
            WorkoutPlan(
                state = state,
                selectedDay = selectedDay,
                onWorkoutSelected = onWorkoutSelected,
                onMesocycleSelected = { showingMesocycleDetails = true },
                onRecreatePlan = onRecreatePlan,
                gender = gender,
                modifier = Modifier.fillMaxSize(),
            )
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
                        }) { Text("REINTENTAR", color = WildforceThemeTokens.accentGold) }
                    }
                    TextButton(onClick = {
                        customGenerationError = null
                        CustomWorkoutStore.save(customContext, CustomWorkoutStore.automatic(
                            selectedDay ?: LocalDate.now().dayOfWeek,
                            lastCustomRequest ?: CustomWorkoutRequest("Cuerpo completo", 45, "Peso corporal"),
                        ))
                        customWorkouts = CustomWorkoutStore.load(customContext)
                        lastCustomRequest = null
                    }) { Text("USAR PLANTILLA LOCAL", color = WildforceThemeTokens.accentGold) }
                }
            },
            dismissButton = { TextButton(onClick = { customGenerationError = null }) { Text("CERRAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

/** Native counterpart of iOS CreatePlanAnimationView for custom-workout AI generation. */
@Composable
private fun PlanGenerationOverlay() {
    val transition = rememberInfiniteTransition(label = "custom-workout-generation")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1_800, easing = LinearEasing)), label = "custom-workout-ring")
    val pulse by transition.animateFloat(1f, .55f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "custom-workout-copy")
    Column(
        Modifier.clip(RoundedCornerShape(24.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = 1f, modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }, color = WildforceThemeTokens.accentGold, strokeWidth = 4.dp)
            Icon(Icons.Filled.AutoAwesome, null, tint = WildforceThemeTokens.accentGold, modifier = Modifier.size(26.dp))
        }
        Text("GENERANDO ENTRENAMIENTO CON IA…", color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(pulse))
        Text("Personalizando ejercicios y volumen", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun ProfileChangedPlanBanner(onRecreatePlan: () -> Unit) {
    Column(
        // Keep this as a quiet, full-width warning row. The iOS home puts the
        // recreation prompt in an orange-tinted list row rather than in a
        // floating glass card.
        Modifier.fillMaxWidth()
            .background(Color(0xFFFF9800).copy(alpha = 0.10f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFFF9800),
            )
            Column {
                Text("Perfil modificado", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                Text(
                    "Tu perfil de entrenamiento ha cambiado desde que se creó este plan. Recrea el plan para obtener mejores resultados.",
                    style = MaterialTheme.typography.body2,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        }
        Button(
            onClick = onRecreatePlan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
        ) { Text("Recrear plan", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun WorkoutHeader(state: WorkoutHubState, avatarPath: String?, avatarRevision: Int) {
    var showsMoreOptions by remember { mutableStateOf(false) }
    var showsFeedback by remember { mutableStateOf(false) }
    var showsFaq by remember { mutableStateOf(false) }
    val today = LocalDate.now().dayOfWeek
    // Same condition as iOS: flag the streak only on a scheduled training day
    // when that day's workout has not yet been completed.
    val isWorkoutIncomplete = today in state.trainingDays && today !in state.completedDays
    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(WildforceThemeTokens.accentGold),
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
                Text(state.user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(state.user.name, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("▲ ${state.user.goal}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text("🔥", style = MaterialTheme.typography.h6)
            if (isWorkoutIncomplete) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).size(12.dp)
                        .background(Color.White, CircleShape).padding(1.dp),
                    tint = Color(0xFFD32F2F),
                )
            }
        }
        Text(
            state.user.currentStreak.toString(),
            Modifier.padding(start = 4.dp),
            color = WildforceThemeTokens.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.padding(start = 12.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
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
                    Icon(Icons.Filled.Info, contentDescription = null, tint = WildforceThemeTokens.textPrimary)
                    Text("Feedback", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                }
                DropdownMenuItem(onClick = { showsMoreOptions = false; showsFaq = true }) {
                    Icon(Icons.Filled.QuestionMark, contentDescription = null, tint = WildforceThemeTokens.textPrimary)
                    Text("FAQ", Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                }
            }
        }
    }
    if (showsFeedback) FeedbackDialog(onDismiss = { showsFeedback = false })
    if (showsFaq) FaqDialog(onDismiss = { showsFaq = false })
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var category by remember { mutableStateOf("Comentarios generales") }
    var message by remember { mutableStateOf("") }
    val categories = listOf("Error", "Sugerencia", "Comentarios generales", "Problema con la cuenta")
    var categoriesExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FEEDBACK", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cuéntanos qué podemos mejorar.", color = WildforceThemeTokens.textSecondary)
                Box {
                    OutlinedButton(onClick = { categoriesExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
                        Text("⌄", color = WildforceThemeTokens.textSecondary)
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = message.trim().isNotEmpty(),
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:jordi@gloobus.net")
                        putExtra(Intent.EXTRA_SUBJECT, "Wildforce Feedback: $category")
                        putExtra(Intent.EXTRA_TEXT, "Categoría: $category\n\n${message.trim()}")
                    }
                    context.startActivity(Intent.createChooser(emailIntent, "Enviar feedback"))
                    onDismiss()
                },
            ) { Text("ENVIAR", color = WildforceThemeTokens.textPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
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
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val locale = LocalLocale.current.platformLocale
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // Mirrors SevenDayWorkoutCalendarView on iOS: planned days have a 6 dp
    // accent dot, while completed days use the system-green filled checkmark.
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0L..6L).forEach { offset ->
            val date = monday.plusDays(offset)
            val day = date.dayOfWeek
            val selected = day == selectedDay
            val hasWorkout = day in state.trainingDays
            val completed = day in state.completedDays
            Column(
                Modifier.weight(1f).alpha(if (hasWorkout) 1f else 0.7f).clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) WildforceThemeTokens.accent
                        else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f),
                    )
                    .clickable { onDaySelected(day) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, locale),
                    color = if (selected) MaterialTheme.colors.onPrimary
                    else if (date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textPrimary,
                    style = MaterialTheme.typography.subtitle1,
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

@Composable
private fun WorkoutModeSelector(
    selected: WorkoutMode,
    onSelected: (WorkoutMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Matches iOS's shared UISegmentedControl: the native neutral track,
    // text-primary selection tint, and primary-button text for the active tab.
    Row(
        modifier.fillMaxWidth().height(32.dp).clip(CircleShape)
            .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkoutMode.values().forEach { mode ->
            val isSelected = selected == mode
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .background(if (isSelected) WildforceThemeTokens.textPrimary else Color.Transparent)
                    .clickable { onSelected(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label,
                    color = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    gender: String,
    modifier: Modifier = Modifier,
) {
    val workouts = state.workoutsFor(selectedDay)
    var cardsEntered by remember(selectedDay, state.planName) { mutableStateOf(false) }
    LaunchedEffect(selectedDay, state.planName) { cardsEntered = true }
    LazyColumn(
        modifier,
        // Keep the final card scrollable above both the tab bar and the optional
        // resume accessory, without shrinking the screen behind those controls.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MesocycleHeaderCard(state, onMesocycleSelected)
        }
        item {
            WeeklyPlanHeaderCard(state, onRecreatePlan)
        }
        if (workouts.isEmpty()) item { RestDayCard() }
        else itemsIndexed(workouts, key = { _, workout -> workout.id }) { index, workout ->
            AnimatedVisibility(visible = cardsEntered, enter = fadeIn(tween(280, delayMillis = 70 + index * 55)) + slideInVertically(tween(280, delayMillis = 70 + index * 55)) { it / 16 }) {
                WorkoutCard(workout, gender) { onWorkoutSelected(workout) }
            }
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
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "MESOCICLO ${state.mesocycleIndex}",
                    fontFamily = Exo2FontFamily,
                    style = MaterialTheme.typography.h5,
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
private fun WeeklyPlanHeaderCard(state: WorkoutHubState, onRecreatePlan: () -> Unit) {
    val finalized = state.workouts.count { it.status.isPlanFinalized }
    var showsPlanMenu by remember { mutableStateOf(false) }
    Column(
        // Match iOS's PlanHeaderView: both home context cards share the same
        // opaque secondary surface instead of mixing glass and solid whites.
        Modifier.fillMaxWidth()
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ESTA SEMANA", Modifier.weight(1f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
            Box {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "Opciones del plan",
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable { showsPlanMenu = true }.padding(5.dp),
                    tint = WildforceThemeTokens.textSecondary,
                )
                DropdownMenu(expanded = showsPlanMenu, onDismissRequest = { showsPlanMenu = false }) {
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
        Text(state.planName, fontFamily = Exo2FontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        ContextPill(state.user.goal, "▲")
        if (state.workouts.isNotEmpty()) {
            Text("$finalized de ${state.workouts.size} sesiones finalizadas", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            LinearProgressIndicator(
                progress = finalized.toFloat() / state.workouts.size,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                color = WildforceThemeTokens.textPrimary,
                backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.20f),
            )
        }
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
private fun WorkoutCard(workout: WorkoutDaySummary, gender: String, onClick: () -> Unit) {
    // iOS persists `.inProgress` on the workout day. Android persists the active
    // session separately, so its presence is the equivalent source of truth.
    val sessionContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val isInProgress = workout.status == WorkoutStatus.Planned &&
        WorkoutSessionStore.load(sessionContext, workout.id) != null
    val completionMetrics = remember(workout.id, workout.status) {
        if (workout.status == WorkoutStatus.Completed) WorkoutCompletionMetricsStore.load(sessionContext, workout.id) else null
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), elevation = 0.dp) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            RemoteTrainingImage(
                url = workoutCoverUrl(workout.focus, gender, workout.id),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            // Same two-layer treatment as WorkoutDayHeaderView on iOS. The
            // previous single gradient began too high and muddied the artwork.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                        startY = 180f,
                        endY = 90f,
                    ),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent),
                        startX = 0f,
                        endX = 180f,
                    ),
                ),
            )
            Column(
                Modifier.fillMaxSize()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp),
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
                Spacer(Modifier.height(if (status == null) 56.dp else 34.dp))
                status?.let { (label, color) -> StatusBadge(label, color) }

                Text(
                    workout.title.uppercase(),
                    Modifier.padding(top = 8.dp),
                    fontFamily = AntonFontFamily,
                    fontSize = 22.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .semantics {
                            contentDescription = "${workout.focus}, ${workout.dayType}, ${workout.estimatedMinutes} min"
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // iOS presents this as one non-scrolling HStack. The
                    // descriptive fields therefore truncate cleanly instead
                    // of exposing clipped, scrollable fragments.
                    WorkoutCardMetadata(Icons.Filled.MyLocation, workout.focus, modifier = Modifier.weight(1f))
                    WorkoutCardMetadata(workoutDayTypeIcon(workout.dayType), workout.dayType, modifier = Modifier.weight(1f))
                    WorkoutCardMetadata(
                        Icons.Filled.Timer,
                        if (workout.status == WorkoutStatus.Completed) {
                            "${(completionMetrics?.activeDurationSeconds ?: workout.estimatedMinutes * 60) / 60} min"
                        } else {
                            "${workout.estimatedMinutes} min"
                        },
                    )
                    completionMetrics?.activeCaloriesBurned?.takeIf { it > 0 }?.let { calories ->
                        WorkoutCardMetadata(Icons.Filled.LocalFireDepartment, "${calories.toInt()} kcal", Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

@Composable
private fun workoutCardStatus(status: WorkoutStatus, isInProgress: Boolean): Pair<String, Color>? = when (status) {
    WorkoutStatus.Completed -> "COMPLETADO" to Color(0xFF26A269)
    WorkoutStatus.Skipped -> "OMITIDO" to WildforceThemeTokens.textSecondary
    WorkoutStatus.Planned -> if (isInProgress) "EN CURSO" to Color(0xFFF28A29) else null
}

@Composable
private fun workoutDetailStatus(status: WorkoutStatus): Pair<String, Color>? = when (status) {
    WorkoutStatus.Completed -> "COMPLETADO" to Color(0xFF26A269)
    WorkoutStatus.Skipped -> "OMITIDO" to WildforceThemeTokens.textSecondary
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
    Text(
        label,
        Modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 2.dp),
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

@Preview(showBackground = true)
@Composable
private fun WorkoutHubPreview() = WildforceTheme { WorkoutHubScreen(PaddingValues()) }

@Composable
fun WorkoutDetailScreen(workout: WorkoutDaySummary, gender: String, useImperial: Boolean = false, onBack: () -> Unit, onStart: () -> Unit, onSkip: () -> Unit = {}, onUnskip: () -> Unit = {}, onWorkoutUpdated: (WorkoutDaySummary) -> Unit = {}, defaultAdaptEquipment: String = "Peso corporal", adaptEquipmentPresets: List<Pair<String, String>> = emptyList(), adaptAiGenerator: (suspend (CustomWorkoutRequest) -> WorkoutDaySummary)? = null) {
    val detailHeroHeight = (LocalConfiguration.current.screenHeightDp * 0.43f).dp.coerceIn(330.dp, 430.dp)
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
            url = workoutCoverUrl(workout.focus, gender, workout.id),
            contentDescription = workout.title,
            modifier = Modifier.fillMaxWidth().height(detailHeroHeight),
        )
        Box(Modifier.fillMaxWidth().height(detailHeroHeight).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, WildforceThemeTokens.backgroundSecondary))))
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(bottom = 112.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CenteredBackButton(onBack, 40.dp, 14.dp)
                Spacer(Modifier.weight(1f))
                if (workout.status != WorkoutStatus.Planned) {
                    // WorkoutDayView places a non-planned status in the trailing
                    // header position. A completed pre-workout must therefore show
                    // its badge here, not between the day label and title.
                    workoutDetailStatus(workout.status)?.let { (label, color) ->
                        StatusBadge(label, color)
                    }
                } else {
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
                                // Keep the leading symbols in sync with iOS's SF Symbols.  Android
                                // renders them as emoji so the menu keeps its meaning when a system
                                // icon is unavailable in the current font/theme.
                                DropdownMenuItem(onClick = { showMenu = false; showingEditor = true }) { Text("➕  Añadir ejercicio") }
                                DropdownMenuItem(onClick = { showMenu = false; showSkipConfirmation = true }) { Text("⏭️  Saltar entrenamiento") }
                                if (adaptAiGenerator != null) DropdownMenuItem(onClick = { showMenu = false; showingAdaptation = true }) { Text("🏢  Adaptar a otra ubicación") }
                            }
                            if (hasSavedSession) {
                                Divider()
                                DropdownMenuItem(onClick = { showMenu = false; showCancelConfirmation = true }) { Text("✖️  Cancelar entrenamiento", color = Color(0xFFC62828)) }
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
                    WorkoutDetailMetadataPill(workoutFocusIcon(workout.focus), workout.focus)
                    WorkoutDetailMetadataPill(workoutDayTypeIcon(workout.dayType), workout.dayType)
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
                    Button(onClick = { if (!startingWorkout) { startingWorkout = true; onStart() } }, enabled = !startingWorkout, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp)) {
                        Text(if (startingWorkout) "ABRIENDO ENTRENAMIENTO…" else if (hasSavedSession) "REANUDAR ENTRENAMIENTO" else "EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                    }
                }
                Divider(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))
                WorkoutDetailTabSelector(selected = selectedTab, onSelected = { selectedTab = it }, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(18.dp))
                if (selectedTab == WorkoutDetailTab.Information) {
                    Column(Modifier.padding(horizontal = 12.dp)) { WorkoutInformationTab(workout) }
                } else {
                Column(Modifier.padding(horizontal = 12.dp)) {
                // Keep the target-muscle images and effective-set involvement visible
                // before the planned exercises, as requested for the pre-workout view.
                WorkoutMuscleEffort(workout)
                Spacer(Modifier.height(24.dp))
                // Mirrors iOS WorkoutEquipmentScroll with the canonical equipment
                // artwork rather than the former emoji-only Android treatment.
                WorkoutEquipment(workout)
                Spacer(Modifier.height(16.dp))
                workout.displayBlocks().forEachIndexed { blockIndex, block ->
                    val collapsible = block.type == WorkoutBlockType.Warmup || block.type == WorkoutBlockType.Cooldown
                    val collapsed = blockIndex in collapsedBlocks
                    // iOS does not render an artificial “Main block” heading for
                    // standard exercises. Headers are reserved for warmup,
                    // cooldown and supersets.
                    val showHeader = block.type != WorkoutBlockType.Standard
                    if (showHeader) {
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = collapsible) {
                                collapsedBlocks = if (collapsed) collapsedBlocks - blockIndex else collapsedBlocks + blockIndex
                            }.padding(top = 14.dp, bottom = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (collapsible) Text(if (collapsed) "›" else "⌄", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(end = 8.dp))
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
                                            exercise.name,
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
                                Text("•••", color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                            }
                            if (exerciseIndex < block.exercises.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.14f)))
                            }
                        }
                        block.restAfterBlockSeconds?.let { rest ->
                            Text("◷  Descansa ${rest}s al terminar el bloque", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
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
private fun CompletedWorkoutMetrics(workout: WorkoutDaySummary, useImperial: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val metrics = remember(workout.id) { WorkoutCompletionMetricsStore.load(context, workout.id) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF26A269), modifier = Modifier.size(18.dp))
            Text(
                "ENTRENAMIENTO COMPLETADO",
                Modifier.padding(start = 8.dp),
                color = WildforceThemeTokens.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            metrics?.let {
                Spacer(Modifier.weight(1f))
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocalLocale.current.platformLocale)
                        .format(Date(it.completedAtMillis)),
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        }
        if (metrics != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompletedWorkoutMetric("DURACIÓN", "${metrics.activeDurationSeconds / 60} min", Icons.Filled.Timer, Color(0xFF42A5F5), Modifier.weight(1f))
                CompletedWorkoutMetric("CALORÍAS", "${metrics.activeCaloriesBurned.toInt()}", Icons.Filled.LocalFireDepartment, Color(0xFFFF9800), Modifier.weight(1f))
                val volume = if (useImperial) metrics.totalVolumeKg * KG_TO_LB else metrics.totalVolumeKg
                CompletedWorkoutMetric("VOLUMEN", String.format(LocalLocale.current.platformLocale, "%.0f %s", volume, if (useImperial) "lb" else "kg"), Icons.Filled.Scale, Color(0xFFAB47BC), Modifier.weight(1f))
            }
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
                Text(glyph, color = if (!enabled) WildforceThemeTokens.textSecondary.copy(alpha = 0.5f) else if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Text(label, color = if (!enabled) WildforceThemeTokens.textSecondary.copy(alpha = 0.5f) else if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary)
        }
    }
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
                exercise.targetDurationMinutes?.takeIf { it > 0 }?.let { add("◷ $it min") }
                exercise.targetDurationSeconds?.takeIf { it > 0 }?.let { add("◷ $it s") }
            }
        }
        val weight = exercise.targetWeightKg ?: exercise.targetWeightsKg?.maxOrNull()
        weight?.takeIf { it > 0.0 }?.let { add("⚖ ${formatTrainingWeight(it, useImperial)}") }
        exercise.targetDistanceKm?.takeIf { it > 0.0 }?.let { kilometers ->
            val distance = if (useImperial) kilometers * 0.621371 else kilometers
            add("⌁ ${"%.1f".format(Locale.US, distance)} ${if (useImperial) "mi" else "km"}")
        }
    }
    if (primaryMetrics.isNotEmpty()) {
        Text(primaryMetrics.joinToString("   "), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val styleScope = if (exercise.setStyleParameters.appliesToFinalSetOnly) " · solo última serie" else ""
    Text("= ${exercise.setStyle.label}$styleScope", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    // Keep this companion location in sync with iOS's Information tab as well.
    WorkoutMuscleEffort(workout)
    Spacer(Modifier.height(24.dp))
    WorkoutEquipment(workout)
    Spacer(Modifier.height(24.dp))
    workout.displayBlocks().mapNotNull { it.notes }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { notes ->
        Text("NOTAS", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
        Text(notes.joinToString("\n"), Modifier.padding(top = 9.dp), color = WildforceThemeTokens.textSecondary)
        Spacer(Modifier.height(18.dp))
    }
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
    userBmr: Double = 1750.0,
    useImperial: Boolean = false,
    skipRestPeriods: Boolean = false,
    currentStreak: Int = 0,
    isPlanCompletedAfterWorkout: Boolean = false,
    isMesocycleCompletedAfterWorkout: Boolean = false,
    planName: String = "",
    completedPlanWorkouts: Int = 0,
    totalPlanWorkouts: Int = 0,
    totalPlanExercises: Int = 0,
    onGenerateNextPlan: () -> Unit = {},
    onWorkoutChanged: (WorkoutDaySummary) -> Unit = {},
    onExit: () -> Unit,
    onFinish: (durationSeconds: Int, completedSets: Int, volumeKg: Double, streak: Int, completedOn: LocalDate, skippedExerciseIndices: Set<Int>) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var workout by remember(initialWorkout.id) { mutableStateOf(initialWorkout) }
    LaunchedEffect(workout) { onWorkoutChanged(workout) }
    DisposableEffect(workout.id) {
        val notificationsEnabled = WorkoutNotificationPreferences.enabled(context)
        val serviceIntent = android.content.Intent(context, WorkoutForegroundService::class.java)
            .putExtra("title", workout.title)
            .putExtra("detail", "Sesión activa")
            .putExtra("workoutId", workout.id)
            .putExtra("totalExercises", workout.exercises.size)
            .putExtra("currentExerciseHasTimer", workout.exercises.firstOrNull()?.resolvedTargetDurationSeconds() != null)
        android.util.Log.d("WorkoutHub", "Starting service. Enabled: $notificationsEnabled")
        if (notificationsEnabled) {
            runCatching { WorkoutActiveNotification.show(context, workout.title, exerciseIndex = 0, totalExercises = workout.exercises.size) }
            // The foreground service is the durable owner of the Live Update
            // while the app is backgrounded. Without it the notification is
            // only a static snapshot and its timer/actions cannot work.
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
        WildforceWatchLocalBridge.start(context)
        onDispose {
            // Notification actions are scoped to this workout. Leaving one
            // behind lets a newly started foreground service replay it.
            WorkoutNotificationActionStore.clear(context)
            WorkoutActiveNotification.cancel(context)
            context.stopService(serviceIntent)
            WildforceWatchLocalBridge.stop(context)
        }
    }
    val restored = remember(workout.id) { WorkoutSessionStore.load(context, workout.id) }
    var exerciseIndex by remember(workout.id) { mutableStateOf((restored?.exerciseIndex ?: 0).coerceIn(0, workout.exercises.lastIndex.coerceAtLeast(0))) }
    var completedByExercise by remember(workout.id) { mutableStateOf(restored?.completedByExercise ?: emptyMap()) }
    var reps by remember(workout.id) {
        mutableStateOf(restored?.reps ?: workout.exercises.firstOrNull()?.targetRepsPerSet?.firstOrNull() ?: targetReps(workout.exercises.firstOrNull()?.reps))
    }
    var weightKg by remember(workout.id) {
        mutableStateOf(restored?.weightKg ?: workout.exercises.firstOrNull()?.targetWeightsKg?.firstOrNull() ?: workout.exercises.firstOrNull()?.targetWeightKg ?: 0.0)
    }
    var restRemaining by remember(workout.id) { mutableStateOf(restored?.restRemaining) }
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
    var totalCompletedSets by remember(workout.id) { mutableStateOf(restored?.totalCompletedSets ?: 0) }
    var totalVolumeKg by remember(workout.id) { mutableStateOf(restored?.totalVolumeKg ?: 0.0) }
    var showsSummary by remember(workout.id) { mutableStateOf(restored?.showsSummary ?: false) }
    var completedOn by remember(workout.id) { mutableStateOf<LocalDate?>(null) }
    var completionCommitted by remember(workout.id) { mutableStateOf(false) }
    var pendingFeedback by remember(workout.id) { mutableStateOf(restored?.pendingFeedback ?: false) }
    var selectedFeedback by remember(workout.id) { mutableStateOf(restored?.selectedFeedback ?: "JUSTO") }
    var pendingNote by remember(workout.id) { mutableStateOf(restored?.pendingNote.orEmpty()) }
    var exerciseStats by remember(workout.id) { mutableStateOf(restored?.exerciseStats ?: emptyMap()) }
    var feedbackByExercise by remember(workout.id) { mutableStateOf(restored?.feedbackByExercise ?: emptyMap()) }
    var notesByExercise by remember(workout.id) { mutableStateOf(restored?.notesByExercise ?: emptyMap()) }
    var completedSetRecords by remember(workout.id) { mutableStateOf(restored?.completedSetRecords ?: emptyList()) }
    var skippedExerciseIndices by remember(workout.id) { mutableStateOf(restored?.skippedExerciseIndices ?: emptySet()) }
    val initialExerciseDuration = workout.exercises.firstOrNull()?.resolvedTargetDurationSeconds()
    var exerciseTimeRemaining by remember(workout.id) { mutableStateOf(restored?.exerciseTimeRemaining ?: initialExerciseDuration) }
    var exerciseTimeInitial by remember(workout.id) { mutableStateOf(restored?.exerciseTimeInitial?.takeIf { it > 0 } ?: (initialExerciseDuration ?: 0)) }
    var exerciseTimerRunning by remember(workout.id) { mutableStateOf(restored?.exerciseTimerRunning ?: (initialExerciseDuration != null)) }
    var addedSetsByExercise by remember(workout.id) { mutableStateOf(restored?.addedSetsByExercise ?: emptyMap()) }
    var completedDistanceKm by remember(workout.id) { mutableStateOf(restored?.completedDistanceKm ?: 0.0) }
    var exerciseTimerFinishedWhileAway by remember(workout.id) { mutableStateOf(restored?.exerciseTimerFinishedWhileAway ?: false) }
    var showsWorkoutPath by remember { mutableStateOf(false) }
    var showsExerciseHistory by remember { mutableStateOf(false) }
    var showsExerciseGuide by remember { mutableStateOf(false) }
    var showsSetStyleInfo by remember { mutableStateOf(false) }
    var showsSkipExerciseConfirmation by remember { mutableStateOf(false) }
    var showsExerciseMenu by remember { mutableStateOf(false) }
    var showsRemoveExerciseConfirmation by remember { mutableStateOf(false) }
    var showsCancelWorkoutConfirmation by remember { mutableStateOf(false) }
    var exercisePickerMode by remember { mutableStateOf<ExercisePickerMode?>(null) }
    // Equivalent to iOS ActiveWorkoutFeatureOnboardingState: each exercise,
    // set style and superserie is introduced once, without interrupting rest,
    // feedback or another active workout sheet.
    val featureOnboardingStore = remember { WorkoutFeatureOnboardingStore(context) }
    var activeFeatureOnboarding by remember(workout.id) { mutableStateOf<WorkoutFeatureOnboarding?>(null) }
    var featureOnboardingRevision by remember(workout.id) { mutableStateOf(0) }
    var initializedExerciseIndex by remember(workout.id) { mutableStateOf(restored?.exerciseIndex ?: -1) }
    BackHandler(onBack = onExit)
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

    fun addOrReplaceExercise(choice: ExerciseChoice, replace: Boolean) {
        val selectedExercise = ExerciseSummary(choice.name, choice.imageKey, 3, "10", 90)
        if (replace) {
            currentSourceIndex()?.let { sourceIndex ->
                rewriteExercises { all, _ -> if (sourceIndex in all.indices) all[sourceIndex] = selectedExercise }
            }
        } else {
            // iOS's Add Exercise sheet is anchored to the selected exercise,
            // not the end of the workout. Keep that placement even when the
            // selected item is one round of a superset.
            val updated = workout.editableBlocks().toMutableList()
            var executionCursor = 0
            val blockIndex = updated.indexOfFirst { block ->
                val containsCurrent = exerciseIndex in executionCursor until (executionCursor + block.executionExercises().size)
                executionCursor += block.executionExercises().size
                containsCurrent
            }
            if (blockIndex in updated.indices) {
                val block = updated[blockIndex]
                val executionStart = updated.take(blockIndex).sumOf { it.executionExercises().size }
                val itemIndex = ((exerciseIndex - executionStart) % block.exercises.size.coerceAtLeast(1)).coerceIn(0, block.exercises.lastIndex.coerceAtLeast(0))
                updated[blockIndex] = block.copy(exercises = block.exercises.toMutableList().apply { add(itemIndex + 1, selectedExercise) })
            } else {
                updated += WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(selectedExercise))
            }
            workout = workout.withEditableBlocks(updated)
        }
        exercisePickerMode = null
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
        rewriteExercises { all, _ ->
            val item = all[current]
            all[current] = all[destination]
            all[destination] = item
        }
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
            val nextRestContext = if (exercise?.blockType == WorkoutBlockType.Superset && exercise.blockRound < exercise.blockRounds) {
                WorkoutRestContext.BetweenRounds
            } else {
                WorkoutRestContext.BeforeNextBlock
            }
            exerciseIndex++
            if (!skipRestPeriods && restSeconds > 0) {
                restInitialSeconds = restSeconds
                restBetweenExercises = true
                restContext = nextRestContext
                restRemaining = restSeconds
                restTimerPaused = false
                restEndsAtMillis = System.currentTimeMillis() + restSeconds * 1_000L
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
                if (shouldRequestExerciseFeedback(activeExercise)) {
                    pendingFeedback = true
                } else {
                    val transitionRest = if (activeExercise.isLastInBlock) activeExercise.restAfterBlockSeconds ?: 0 else 0
                    advanceFromExercise(transitionRest)
                }
            }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }.onFailure { error ->
            android.util.Log.e("Workout", "Unable to complete exercise", error)
        }
    }

    fun applyWatchCommand(command: WildforceWatchLocalBridge.Command) {
        when (command.operation) {
            "set_reps" -> command.reps?.let { reps = it.coerceIn(0, 999) }
            "set_weight" -> command.weightKg?.let { weightKg = it.coerceIn(0.0, 999.0) }
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
        onDispose { WildforceWatchLocalBridge.commandListener = null }
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
    LaunchedEffect(exerciseIndex) {
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
    LaunchedEffect(exerciseIndex, completedByExercise, reps, weightKg, restRemaining, restInitialSeconds, restBetweenExercises, restContext, elapsedSeconds, totalCompletedSets, totalVolumeKg, pendingFeedback, selectedFeedback, pendingNote, showsSummary, exerciseStats, feedbackByExercise, notesByExercise, completedSetRecords, exerciseTimeRemaining, exerciseTimeInitial, exerciseTimerRunning, addedSetsByExercise, completedDistanceKm, skippedExerciseIndices) {
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
                    exerciseName = activeExercise.name,
                    exerciseIndex = exerciseIndex,
                    exerciseCount = workout.exercises.size,
                    setNumber = completedForExercise + 1,
                    targetSets = effectiveSets,
                    reps = reps,
                    weightKg = weightKg,
                    restRemaining = restRemaining,
                    restInitialSeconds = restInitialSeconds,
                    exercises = workout.exercises.map { it.name },
                    elapsedSeconds = elapsedSeconds,
                ),
            )
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
        } else if (remaining <= 0) exerciseTimerRunning = false
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
            }) { Text("SALTAR", color = WildforceThemeTokens.accentGold) } },
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

    fun commitCompletedSession(progress: CompletionProgress) {
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
        WorkoutCompletionFlowScreen(workout, elapsedSeconds, exerciseStats, feedbackByExercise, recordEvents, completionProgress, useImperial = useImperial, isPlanCompleted = isPlanCompletedAfterWorkout, isMesocycleCompleted = isMesocycleCompletedAfterWorkout, planName = planName, completedPlanWorkouts = completedPlanWorkouts, totalPlanWorkouts = totalPlanWorkouts, totalPlanExercises = totalPlanExercises, onGenerateNextPlan = {
            commitCompletedSession(completionProgress)
            onGenerateNextPlan()
        }, onCancelWorkout = {
            cancelWorkoutSession(context, workout.id)
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
                modifier = Modifier.fillMaxWidth().height(activeBackgroundHeight),
            )
        }
        // Mirrors iOS: a dark top treatment that is fully transparent by the
        // center, then a separate surface fade only in the lower half.
        Box(
            Modifier.fillMaxWidth().height(activeBackgroundHeight).background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.60f),
                    0.25f to Color.Black.copy(alpha = 0.50f),
                    0.50f to Color.Transparent,
                    1f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxWidth().height(activeBackgroundHeight).background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.50f to Color.Transparent,
                    1f to WildforceThemeTokens.backgroundSecondary,
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CenteredBackButton(onExit, 40.dp)
                Spacer(Modifier.weight(1f))
                Row(
                    // Each action owns a 48 dp touch square, just like an iOS
                    // toolbar item. The capsule frames those squares without
                    // resizing or clipping their glyphs.
                    Modifier.height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.68f)
                            else Color.Black.copy(alpha = 0.46f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.46f), RoundedCornerShape(26.dp))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActiveWorkoutToolbarAction(Icons.Filled.Timeline, "Ruta del entrenamiento") { showsWorkoutPath = true }
                    ActiveWorkoutToolbarAction(Icons.Filled.Insights, "Datos del ejercicio") { showsExerciseHistory = true }
                    ActiveWorkoutToolbarAction(Icons.AutoMirrored.Filled.Assignment, "Guía del ejercicio") { showsExerciseGuide = true }
                    ActiveWorkoutToolbarAction(Icons.Filled.MoreHoriz, "Opciones del ejercicio") { showsExerciseMenu = true }
                }
                DropdownMenu(expanded = showsExerciseMenu, onDismissRequest = { showsExerciseMenu = false }, modifier = Modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp))) {
                    val sectionIndices = currentSectionSourceIndices()
                    val sourceIndex = currentSourceIndex()
                    val sectionPosition = sourceIndex?.let(sectionIndices::indexOf) ?: -1
                    val canCreateSuperset = currentPathBlock?.type == WorkoutBlockType.Standard && exerciseIndex < workout.exercises.lastIndex
                    val canEditSuperset = currentPathBlock?.type == WorkoutBlockType.Superset
                    ActiveWorkoutMenuItem("＋", "Añadir ejercicio") { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Add }
                    ActiveWorkoutMenuItem("↔", "Sustituir ejercicio") { showsExerciseMenu = false; exercisePickerMode = ExercisePickerMode.Swap }
                    ActiveWorkoutMenuItem("↑", "Mover arriba", enabled = sectionPosition > 0) { showsExerciseMenu = false; moveCurrentExercise(-1) }
                    ActiveWorkoutMenuItem("↓", "Mover abajo", enabled = sectionPosition >= 0 && sectionPosition < sectionIndices.lastIndex) { showsExerciseMenu = false; moveCurrentExercise(1) }
                    if (canCreateSuperset) {
                        Divider()
                        ActiveWorkoutMenuItem("⛓", "Crear superserie") { showsExerciseMenu = false; createActiveSuperset() }
                    }
                    if (canEditSuperset) {
                        Divider()
                        ActiveWorkoutMenuItem("□", "Quitar superserie") { showsExerciseMenu = false; removeActiveSuperset() }
                        ActiveWorkoutMenuItem("+", "Añadir ronda") { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = 1) }
                        ActiveWorkoutMenuItem("−", "Quitar ronda", enabled = currentPathBlock.rounds > 1) { showsExerciseMenu = false; updateActiveSuperset(roundsDelta = -1) }
                        ActiveWorkoutMenuItem("◷", "Añadir 15 s de descanso") { showsExerciseMenu = false; updateActiveSuperset(restDelta = 15) }
                        ActiveWorkoutMenuItem("◷", "Quitar 15 s de descanso", enabled = (currentPathBlock.restAfterBlockSeconds ?: 0) > 0) { showsExerciseMenu = false; updateActiveSuperset(restDelta = -15) }
                    }
                    Divider()
                    ActiveWorkoutMenuItem("×", "Eliminar ejercicio", destructive = true) { showsExerciseMenu = false; showsRemoveExerciseConfirmation = true }
                    Divider()
                    ActiveWorkoutMenuItem("×", "Cancelar entrenamiento", destructive = true) { showsExerciseMenu = false; showsCancelWorkoutConfirmation = true }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                workout.exercises.forEachIndexed { index, item ->
                    val setsForItem = (item.sets + (addedSetsByExercise[index] ?: 0)).coerceAtLeast(1)
                    val completedSets = completedByExercise[index] ?: 0
                    val completed = index < exerciseIndex || completedSets >= setsForItem
                    val current = index == exerciseIndex
                    Box(
                        Modifier.weight(1f)
                            .height(if (current) 10.dp else 8.dp)
                            .align(Alignment.CenterVertically)
                            .clip(RoundedCornerShape(5.dp)).background(
                            when {
                                completed -> Color.White.copy(alpha = 0.88f)
                                current && completedSets > 0 -> Color.White.copy(alpha = 0.62f)
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                        ),
                    )
                }
            }
            Column(Modifier.padding(start = 10.dp, top = 14.dp, end = 10.dp, bottom = 10.dp)) {
                Text(exercise?.name.orEmpty(), fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.Normal, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // ActiveWorkoutSceneView keeps the block/status bubble in the
                // following header row, separated from the exercise title.
                Spacer(Modifier.height(8.dp))
                val muscleCount = ExerciseVisualCatalog.metadata(exercise?.imageKey)?.let { it.primary.size + it.secondary.size } ?: 0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        val contextLabel = exercise?.blockType?.takeIf { it != WorkoutBlockType.Standard }?.label?.uppercase() ?: "STANDARD"
                        Text(contextLabel, Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                        MuscleStrip(
                            exercise?.imageKey,
                            onDarkBackground = true,
                            modifier = Modifier.padding(top = 4.dp),
                            wrapContent = muscleCount > 3,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val referenceWeight = exercise?.targetWeightKg ?: weightKg
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PerformanceContextChip("PR", formatTrainingWeight(if (personalBestWeight > 0.0) personalBestWeight else referenceWeight, useImperial))
                        PerformanceContextChip("Último", formatTrainingWeight(if (lastRecordedWeight > 0.0) lastRecordedWeight else referenceWeight, useImperial))
                    }
                }
            }
            // iOS pins the phase sheet to the bottom edge; let the hero/header
            // absorb all remaining space instead of leaving a floating gap.
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(0.dp))
            val timerMode = restRemaining != null || exercise?.resolvedTargetDurationSeconds() != null
            val maxSheetHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.66f).coerceAtLeast(420.dp)
            Column(
                Modifier.fillMaxWidth()
                    // Let the sheet grow naturally until the available lower
                    // viewport is exhausted. Only then does verticalScroll
                    // become useful; this preserves the rounded edges and
                    // keeps the next-exercise text visible whenever possible.
                    .heightIn(max = maxSheetHeight)
                    .animateContentSize(animationSpec = tween(280))
                    .padding(horizontal = 10.dp, vertical = 16.dp)
                    .shadow(12.dp, RoundedCornerShape(54.dp), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                    .clip(RoundedCornerShape(54.dp))
                    .background(WildforceThemeTokens.backgroundSecondary)
                    .then(if (timerMode) Modifier else Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
                    // The set-style marker (for example the straight-set "=")
                    // starts the series phase.  Keep it outside the 54 dp
                    // corner arc instead of letting the sheet clip its square.
                    .padding(
                        start = 18.dp,
                        top = 20.dp,
                        end = 18.dp,
                        bottom = if (effectiveSets >= 4) 12.dp else 16.dp,
                    ),
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
                                "${exercise?.name ?: "Entrenamiento"} · ${completedExercises}/${workout.exercises.size} ejercicios",
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
                                )
                            } else {
                                Text(exercise.reps, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h2, color = WildforceThemeTokens.accentGold)
                                Text("REPETICIONES OBJETIVO", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    } else {
                        if (exercise.blockType == WorkoutBlockType.Superset && currentPathBlock != null) {
                            SupersetRoundMap(currentPathBlock, exerciseIndex, exercise.blockRound)
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(WildforceThemeTokens.textPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(exercise.setStyle.glyph, color = WildforceThemeTokens.backgroundSecondary, fontFamily = Exo2FontFamily, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(exercise.setStyle.label, Modifier.padding(start = 10.dp), fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
                                Text(
                                    setStyleScopeLabel(exercise.setStyle, effectiveSets, exercise.setStyleParameters.appliesToFinalSetOnly),
                                    Modifier.padding(start = 10.dp),
                                    color = WildforceThemeTokens.textSecondary,
                                    fontFamily = Exo2FontFamily,
                                    fontSize = 14.sp,
                                )
                            }
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.12f)).clickable { showsSetStyleInfo = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = "Información de la serie", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            styleParameterLabels(exercise.setStyle, exercise.setStyleParameters).forEach { label ->
                                Text(
                                    label,
                                    Modifier.clip(RoundedCornerShape(14.dp))
                                        .background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    color = WildforceThemeTokens.textPrimary,
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
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
                            )
                        }
                        Column(Modifier.fillMaxWidth()) {
                            SetTrackingRows(exerciseIndex, effectiveExercise ?: exercise, completedForExercise, completedSetRecords, useImperial, dense = effectiveSets >= 4, currentEditor = if (timerMode) null else {
                                {
                                    ActiveSetEditor(
                                        targetReps = exercise.targetRepsPerSet?.getOrNull(completedForExercise)?.toString() ?: exercise.reps,
                                        targetWeightKg = exercise.targetWeightsKg?.getOrNull(completedForExercise) ?: exercise.targetWeightKg ?: weightKg,
                                        reps = reps,
                                        weightKg = weightKg,
                                        useImperial = useImperial,
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
                            Spacer(Modifier.height(8.dp))
                        }
                        if (exercise.blockType == WorkoutBlockType.Standard && exercise.resolvedTargetDurationSeconds() == null && effectiveSets < 20) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("＋  AÑADIR SERIE", Modifier
                                    .clip(CircleShape)
                                    .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f))
                                    .clickable { addedSetsByExercise = addedSetsByExercise + (exerciseIndex to ((addedSetsByExercise[exerciseIndex] ?: 0) + 1)) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontFamily = Exo2FontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            }
                        }
                    }
                    // Match iOS vertical rhythm: the add-set capsule is a
                    // secondary action and must not touch the completion CTA.
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = ::completeCurrentStep,
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                    ) {
                        val completionLabel = when {
                            isWarmupOrCooldown || completedForExercise + 1 < effectiveSets ->
                                if (isWarmupOrCooldown) "COMPLETAR EJERCICIO" else "COMPLETAR SERIE ${completedForExercise + 1}"
                            exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock ->
                                "COMPLETAR ${exercise.blockLabel.orEmpty()}"
                            exercise.blockType == WorkoutBlockType.Superset && exercise.blockRound < exercise.blockRounds ->
                                "FINALIZAR RONDA"
                            else -> "COMPLETAR EJERCICIO"
                        }
                        Text(completionLabel, fontWeight = FontWeight.Bold)
                    }
                    workout.exercises.getOrNull(exerciseIndex + 1)?.let { next ->
                        val prefix = if (exercise.blockType == WorkoutBlockType.Superset && !exercise.isLastInBlock) {
                            "SIGUIENTE EN EL BLOQUE"
                        } else {
                            "SIGUIENTE EJERCICIO"
                        }
                        Text("$prefix: ${next.name}", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
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
                Text("⛓", color = WildforceThemeTokens.backgroundSecondary, style = MaterialTheme.typography.caption)
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
                    "${item.label.orEmpty()} ${item.exercise.name}",
                    Modifier.clip(CircleShape)
                        .background(if (current) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (current) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textSecondary,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (index < block.exercises.lastIndex) {
                    Text("→", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
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
    currentEditor: (@Composable () -> Unit)? = null,
    onRecordChanged: (CompletedSetRecord, CompletedSetRecord) -> Unit,
) {
    var editingSetNumber by remember(exerciseIndex) { mutableStateOf<Int?>(null) }
    var editReps by remember(exerciseIndex) { mutableStateOf(0) }
    var editWeightKg by remember(exerciseIndex) { mutableStateOf(0.0) }
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
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
            ) { currentEditor() }
        } else if (!current) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
            ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = if (dense) 2.dp else 4.dp)
                    .background(if (completed) Brush.horizontalGradient(listOf(Color(0x162EAF68), Color(0x082EAF68))) else Brush.horizontalGradient(listOf(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), WildforceThemeTokens.textSecondary.copy(alpha = 0.07f))), RoundedCornerShape(if (dense) 13.dp else 16.dp))
                    .clickable(enabled = record != null) {
                        record?.let { editingSetNumber = setNumber; editReps = it.reps; editWeightKg = it.weightKg }
                    }
                    .padding(horizontal = if (dense) 10.dp else 12.dp, vertical = if (dense) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (completed) "✓" else setNumber.toString(),
                    Modifier.width(34.dp).graphicsLayer { scaleX = completionScale; scaleY = completionScale },
                    color = if (completed) Color(0xFF2EAF68) else WildforceThemeTokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reps", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(if (record != null) record.reps.toString() else exercise.targetRepsPerSet?.getOrNull(setIndex)?.toString() ?: exercise.reps, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.weight(1.25f), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (exercise.isPerSideLoad) "Peso/lado" else "Peso", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(record?.let { formatTrainingWeight(it.weightKg, useImperial) } ?: exercise.targetWeightsKg?.getOrNull(setIndex)?.let { formatTrainingWeight(it, useImperial) } ?: exercise.targetWeightKg?.let { formatTrainingWeight(it, useImperial) }.orEmpty().ifBlank { "—" }, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            }
        }
        if (!current && editingSetNumber == setNumber && record != null) {
            Column(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
                Text("CORREGIR SERIE $setNumber", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                ActiveSetEditor(
                    targetReps = exercise.targetRepsPerSet?.getOrNull(setIndex)?.toString() ?: exercise.reps,
                    targetWeightKg = exercise.targetWeightsKg?.getOrNull(setIndex) ?: exercise.targetWeightKg ?: editWeightKg,
                    reps = editReps,
                    weightKg = editWeightKg,
                    useImperial = useImperial,
                    isPerSideLoad = exercise.isPerSideLoad,
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
        ActiveSetMetricRow(
            label = (if (isPerSideLoad) "Peso/lado" else "Peso") + if (useImperial) " (lb)" else " (kg)",
            target = "Objetivo ${formatTrainingWeight(targetWeightKg, useImperial)}",
            value = if (useImperial) "%.1f".format(Locale.US, weightKg * KG_TO_LB) else "%.1f".format(Locale.US, weightKg),
            onMinus = { onWeightChanged((weightKg - weightStep).coerceAtLeast(0.0)) },
            onPlus = { onWeightChanged(weightKg + weightStep) },
            onEdit = { editingWeight = true },
        )
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
                Text("LISTO", color = WildforceThemeTokens.accentGold)
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
    onSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDistanceChanged: (Double) -> Unit,
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
        Text("EJERCICIO COMPLETADO", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(exerciseName, color = WildforceThemeTokens.textSecondary)
        Text("¿CÓMO HA IDO?", Modifier.fillMaxWidth().padding(top = 12.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("Esta información ayudará a personalizar las siguientes sesiones.", Modifier.fillMaxWidth().padding(top = 2.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { (emoji, title, _) ->
                Column(
                    Modifier.weight(1f)
                        .offset(y = if (selected == title) (-6).dp else 0.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected == title) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.12f))
                        .border(1.dp, if (selected == title) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .semantics {
                            contentDescription = "Esfuerzo $title"
                            stateDescription = if (selected == title) "Seleccionado" else "No seleccionado"
                        }
                        .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelected(title) }
                        .heightIn(min = 92.dp)
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    Text(emoji, style = MaterialTheme.typography.h6)
                    Text(title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = if (selected == title) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
        val description = choices.firstOrNull { it.second == selected }?.third ?: "Selecciona la sensación que mejor describa el ejercicio."
        Text(description, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        if (targetDistanceKm != null) {
            val displayedDistance = if (useImperial) completedDistanceKm * MILES_PER_KM else completedDistanceKm
            val distanceStepKm = if (useImperial) 0.1 / MILES_PER_KM else 0.1
            CompactMetricStepper(
                title = if (useImperial) "DISTANCIA (MI)" else "DISTANCIA (KM)",
                value = String.format(Locale.getDefault(), "%.1f", displayedDistance),
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
private fun RestTimerContent(
    seconds: Int,
    totalSeconds: Int,
    context: WorkoutRestContext,
    nextExercise: ExerciseSummary?,
    nextRoundExercises: List<ExerciseSummary>,
    gender: String,
    useImperial: Boolean,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
) {
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
            RestActionButton("+", "+30s", primary = false, onClick = onAddTime)
            Box(
                Modifier.padding(horizontal = 8.dp).size(148.dp).semantics {
                    contentDescription = "Temporizador de descanso, $seconds segundos restantes"
                    stateDescription = when (context) {
                        WorkoutRestContext.BetweenSets -> "Antes de la siguiente serie"
                        WorkoutRestContext.BetweenRounds -> "Antes de la siguiente ronda"
                        WorkoutRestContext.BeforeNextBlock -> "Antes del siguiente bloque"
                    }
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(animatedProgress, 0f..1f)
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
            RestActionButton("»", "OMITIR", primary = true, onClick = onSkip)
        }
        if (context == WorkoutRestContext.BetweenRounds && nextRoundExercises.isNotEmpty()) {
            Text("SIGUIENTE RONDA", Modifier.fillMaxWidth().padding(start = 10.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nextRoundExercises.forEachIndexed { index, roundExercise ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A${index + 1}", Modifier.background(WildforceThemeTokens.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)).padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        RemoteTrainingImage(exerciseImageUrl(roundExercise.imageKey, gender), null, Modifier.padding(start = 8.dp).size(42.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(roundExercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text(nextExercise.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun RestActionButton(glyph: String, label: String, primary: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
            .background(if (primary) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f))
            .semantics { contentDescription = label }
            .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }.padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(glyph, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = if (primary) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = if (primary) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}
@Composable
private fun CompactMetricStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, inputValue: String = value, decimalInput: Boolean = false, onValueEntered: ((String) -> Unit)? = null) {
    var showManualInput by remember { mutableStateOf(false) }
    var manualInput by remember(inputValue) { mutableStateOf(TextFieldValue(inputValue)) }
    val manualInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CenteredRoundControl("−", 40.dp, onMinus)
            Text(value, Modifier.clickable(enabled = onValueEntered != null) { manualInput = TextFieldValue(inputValue); showManualInput = true }.padding(horizontal = 3.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
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
            confirmButton = { TextButton(onClick = { onValueEntered(manualInput.text); showManualInput = false }) { Text("GUARDAR", color = WildforceThemeTokens.accentGold) } },
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
