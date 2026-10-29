package io.codepassion.doubletriangle.nutrition

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.PlanGenerationAnimation
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.WorkoutRemoteSync
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.NutritionPlanProfileSnapshot
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionPaywall
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionTier
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionAccessStore
import io.codepassion.doubletriangle.feature.onboarding.subscriptionAccess
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** iOS AccentColor2 — the primary nutrition emphasis. */
internal val NutritionCoral = Color(0xFFD4AF37)
// iOS uses the primary black action color for controls; gold is reserved for
// nutrition progress/accent data, never for action buttons.
private val NutritionActionColor = Color(0xFF2B2B29)
// iOS NutritionEnergyBalanceCard uses AccentColor3 for both burned series.
// Keep this separate from the nutrition accent used by the rest of the hub.
private val EnergyBalanceBarColor = Color(0xFFF44545)
/** SwiftUI's system orange, used by the nutrition day gradients. */
internal val NutritionOrange = Color(0xFFFF9500)
/** iOS AccentColor3 — contrast only, never the default nutrition tint. */
internal val NutritionRed = Color(0xFFF44545)
/** SwiftUI's system green, used for recovery and a negative energy balance. */
internal val NutritionGreen = Color(0xFF34C759)
/** SwiftUI's system blue, used for Rest day badges in NutritionDayHeaderView. */
internal val NutritionBlue = Color(0xFF007AFF)
/** Icon.calories in WildforceShared resolves to SwiftUI's system orange. */
internal val NutritionCaloriesColor = NutritionOrange
// Match Icon.color in WildforceShared: protein=.teal, carbs=.green, fat=.yellow.
internal val ProteinColor = Color(0xFF00C7BE)
internal val CarbsColor = Color(0xFF34C759)
internal val FatColor = Color(0xFFFFCC00)

internal fun formatNutritionInteger(value: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

internal fun nutritionSelectionAfterDateChange(selected: LocalDate, previousToday: LocalDate, newToday: LocalDate): LocalDate =
    if (selected == previousToday) newToday else selected

internal fun nutritionSelectionAfterPlanChange(selected: LocalDate, today: LocalDate, planDates: Collection<LocalDate>): LocalDate =
    if (selected != today && selected !in planDates) today else selected

internal fun nutritionCalendarWindow(today: LocalDate, planDates: Collection<LocalDate>): Pair<LocalDate, LocalDate> {
    val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val defaultStart = currentMonday.minusDays(7)
    val defaultEnd = defaultStart.plusDays(20)
    return minOf(planDates.minOrNull() ?: defaultStart, defaultStart) to
        maxOf(planDates.maxOrNull() ?: defaultEnd, defaultEnd)
}

internal fun nutritionCalendarProgress(consumed: Int, target: Int): Float =
    if (target > 0) consumed.toFloat() / target else 0f

internal fun normalizeSavedMeal(entry: MealLog, date: LocalDate, replacementKey: String?, editing: MealLog?): MealLog =
    entry.copy(loggedDate = date, plannedMealKey = replacementKey ?: editing?.plannedMealKey)

/** Android counterpart of the iOS HealthKit onboarding completion callback. */
internal typealias HealthConnectRequest = (((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit)

@Composable
fun NutritionScreen(
    contentPadding: PaddingValues = PaddingValues(),
    profile: OnboardingProfile? = null,
    openLogging: Boolean = false,
    onLoggingOpened: () -> Unit = {},
    onRequestHealthConnect: HealthConnectRequest = { result -> result(false, null, null, null, null) },
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    var targets by remember(profile) { mutableStateOf(NutritionStore.loadTargets(context, profile)) }
    var preferences by remember { mutableStateOf(NutritionStore.loadPreferences(context)) }
    var hasNutritionProfile by remember { mutableStateOf(NutritionStore.hasNutritionProfile(context)) }
    var planVersion by remember { mutableStateOf(NutritionStore.loadPlanVersion(context)) }
    // iOS only renders planned days after a plan has actually been generated.
    // Keeping the local fallback here made Android show synthetic meals and a
    // misleading weekly calendar before the user created a plan.
    var generatedPlan by remember(profile) { mutableStateOf(NutritionStore.loadGeneratedPlan(context)) }
    // Keep plan-level metadata separate from the seven day rows, matching the
    // iOS NutritionPlan model when the profile changes after generation.
    var planMetadata by remember(profile) { mutableStateOf(NutritionStore.loadPlanMetadata(context)) }
    // A generated week is only a draft until the person explicitly accepts it.
    // This prevents an accidental regeneration from replacing the active plan.
    var generatedPlanPreview by remember { mutableStateOf<List<NutritionDayPlan>?>(null) }
    val plan = generatedPlan ?: emptyList()
    var meals by remember(selectedDate) { mutableStateOf(NutritionStore.load(context, selectedDate)) }
    var showLogFlow by remember { mutableStateOf(openLogging && hasNutritionProfile) }
    var showEntries by remember { mutableStateOf(false) }
    var detailDay by remember { mutableStateOf<NutritionDayPlan?>(null) }
    var showPlanOverview by remember { mutableStateOf(false) }
    var showTargets by remember { mutableStateOf(false) }
    // Mirrors iOS: entering Nutrition without a profile starts configuration
    // instead of silently treating the default omnivore value as a choice.
    var showPreferences by remember { mutableStateOf(!hasNutritionProfile) }
    var showIdeas by remember { mutableStateOf(false) }
    var fridgeTarget by remember { mutableStateOf<NutritionTargets?>(null) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showGrocery by remember { mutableStateOf(false) }
    var showFulfill by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<MealLog?>(null) }
    var plannedMethod by remember { mutableStateOf<LogMethod?>(null) }
    var plannedTitle by remember { mutableStateOf<String?>(null) }
    var plannedMealKey by remember { mutableStateOf<String?>(null) }
    var dayMealStateVersion by remember { mutableIntStateOf(0) }
    var generatingPlan by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var energyHistory by remember { mutableStateOf<Map<LocalDate, NutritionEnergyPoint>>(emptyMap()) }
    var aiNutritionAccess by remember { mutableStateOf(SubscriptionAccessStore.load(context)?.features?.aiNutritionAnalysis ?: true) }
    var showsAiCoach by remember {
        mutableStateOf(
            SubscriptionAccessStore.load(context)?.let { it.features.aiCoach || !it.hasCoach } ?: true,
        )
    }
    var showNutritionPaywall by remember { mutableStateOf(false) }
    val accountPreferences = remember { context.getSharedPreferences("wildforce_account", 0) }
    val accountToken = accountPreferences.getString("token", null)
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshToday() {
        val newToday = LocalDate.now()
        selectedDate = nutritionSelectionAfterDateChange(selectedDate, today, newToday)
        today = newToday
    }

    LaunchedEffect(openLogging, hasNutritionProfile) {
        if (openLogging) {
            if (hasNutritionProfile) showLogFlow = true else showPreferences = true
            // App-entry routes are one-shot events. Consuming this flag after
            // handling it prevents the log sheet from reopening whenever the
            // user leaves Nutrition and later returns to the tab.
            onLoggingOpened()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { refreshToday() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(plan) {
        selectedDate = nutritionSelectionAfterPlanChange(selectedDate, today, plan.map { it.date })
    }

    LaunchedEffect(profile) {
        energyHistory = runCatching { withContext(Dispatchers.IO) { NutritionEnergyBalance.read(context, { date -> NutritionStore.load(context, date).sumOf { it.loggedCalories() }.roundToInt() }) } }.getOrDefault(emptyMap())
        // Retry locally persisted nutrition records when the user returns
        // after granting Health Connect access or after a transient provider
        // failure. The local store remains the source of truth.
        if (HealthConnectNutritionSync.hasPendingSync(context)) {
            withContext(Dispatchers.IO) {
                HealthConnectNutritionSync.publish(context, NutritionStore.allForSync(context))
            }
        }
    }

    LaunchedEffect(accountToken) {
        if (accountToken.isNullOrBlank()) {
            val cachedAccess = SubscriptionAccessStore.load(context)
            aiNutritionAccess = cachedAccess?.features?.aiNutritionAnalysis ?: true
            showsAiCoach = cachedAccess?.let { it.features.aiCoach || !it.hasCoach } ?: true
        } else {
            subscriptionAccess(accountToken)
                .onSuccess {
                    SubscriptionAccessStore.update(context, it)
                    aiNutritionAccess = it.features.aiNutritionAnalysis
                    showsAiCoach = it.features.aiCoach || !it.hasCoach
                }
                // A transient access check failure should not make an already
                // usable nutrition flow disappear; the API remains authoritative
                // when an AI request is submitted.
                .onFailure {
                    val cachedAccess = SubscriptionAccessStore.load(context)
                    aiNutritionAccess = cachedAccess?.features?.aiNutritionAnalysis ?: true
                    showsAiCoach = cachedAccess?.let { it.features.aiCoach || !it.hasCoach } ?: true
                }
        }
    }

    fun refreshEnergyBalance() {
        scope.launch {
            energyHistory = runCatching {
                withContext(Dispatchers.IO) { NutritionEnergyBalance.read(context, { date -> NutritionStore.load(context, date).sumOf { it.loggedCalories() }.roundToInt() }) }
            }.getOrDefault(energyHistory)
        }
    }

    BackHandler(enabled = detailDay != null || showEntries || showPlanOverview) {
        when {
            detailDay != null -> detailDay = null
            showEntries -> showEntries = false
            showPlanOverview -> showPlanOverview = false
        }
    }

    fun reload() { meals = NutritionStore.load(context, selectedDate) }

    // Equivalent to iOS's scheduled sync after a SwiftData save. Keep the
    // pending marker if transport fails so the next foreground sync retries.
    fun queueRemoteSync() {
        val syncProfile = profile ?: return
        val syncPreferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        RemoteSyncChangeTracker.mark(
            context,
            "nutrition-profiles", "nutrition-plans", "nutrition-days", "nutrition-meals",
            "nutrition-log-media", "nutrition-log-entries", "nutrition-log-items",
        )
        scope.launch {
            if (WorkoutRemoteSync.synchronize(context, syncProfile, syncPreferences) is WorkoutRemoteSync.Outcome.Synchronized) {
                syncPreferences.edit().putBoolean("remote_sync_pending", false).apply()
            }
        }
    }

    fun updateHubBookmark(entry: MealLog, bookmarkName: String?) {
        val updated = applyNutritionBookmark(entry, bookmarkName)
        val dayEntries = NutritionStore.load(context, entry.loggedDate)
        NutritionStore.save(
            context,
            entry.loggedDate,
            dayEntries.map { if (it.id == entry.id) updated else it },
        )
        if (entry.loggedDate == selectedDate) meals = NutritionStore.load(context, selectedDate)
        RemoteSyncChangeTracker.mark(context, "nutrition-log-entries")
        queueRemoteSync()
    }

    fun requestPlanGeneration() {
        if (generatingPlan) return
        if (!hasNutritionProfile) {
            showPreferences = true
            return
        }
        generationError = null
        generatingPlan = true
        scope.launch {
            runCatching { NutritionAIPlanner.generate(context) }
                .onSuccess { plan ->
                    generatedPlanPreview = plan
                }
                .onFailure { error ->
                    generationError = error.message?.takeIf(String::isNotBlank)
                        ?: "No se pudo generar el plan nutricional. Inténtalo de nuevo."
                }
            generatingPlan = false
        }
    }

    // Keep a stable copy for the lifetime of this composition. A back event changes
    // detailDay immediately, while callbacks from the outgoing detail tree may still
    // run during that frame; reading detailDay!! there could therefore crash.
    fun handlePlannedMealAction(date: LocalDate, meal: PlannedMeal, method: LogMethod) {
        selectedDate = date
        // Keep the same paywall boundary as iOS before retaining a planned-meal
        // key or opening an AI child flow.
        if (method.requiresAiNutritionAccess() && !aiNutritionAccess) {
            showNutritionPaywall = true
            return
        }

        plannedMealKey = NutritionStore.plannedMealKey(date, meal)
        when (method) {
            LogMethod.Manual -> {
                val planned = meal.toLogDraft(date).copy(plannedMealKey = plannedMealKey)
                NutritionStore.save(context, date, NutritionStore.load(context, date) + planned)
                scope.launch { HealthConnectNutritionSync.publish(context, NutritionStore.allForSync(context)) }
                reload()
                refreshEnergyBalance()
                plannedMealKey = null
                dayMealStateVersion++
                queueRemoteSync()
            }
            LogMethod.Describe -> {
                fridgeTarget = meal.targets
                showIdeas = true
            }
            else -> {
                plannedMethod = method
                plannedTitle = meal.title
                showLogFlow = true
            }
        }
    }

    fun discardPlannedMeal(date: LocalDate, meal: PlannedMeal) {
        NutritionStore.dismissPlannedMeal(context, date, meal)
        dayMealStateVersion++
        queueRemoteSync()
    }

    fun openLogForSomethingElse(date: LocalDate, meal: PlannedMeal) {
        selectedDate = date
        plannedMethod = null
        plannedTitle = null
        plannedMealKey = NutritionStore.plannedMealKey(date, meal)
        showLogFlow = true
    }

    fun isPlannedMealHidden(date: LocalDate, meal: PlannedMeal): Boolean {
        // Reading this version makes discarding or fulfilling a suggestion redraw this day.
        dayMealStateVersion
        return NutritionStore.isPlannedMealDismissed(context, date, meal) ||
            NutritionStore.load(context, date).any { it.plannedMealKey == NutritionStore.plannedMealKey(date, meal) }
    }

    val activeDetailDay = detailDay
    if (showPlanOverview) {
        NutritionPlanOverview(
            plan = plan,
            profile = profile,
            metadata = planMetadata,
            onBack = { showPlanOverview = false },
            onDay = { day -> showPlanOverview = false; detailDay = day },
            onGrocery = { showGrocery = true },
            onRegenerate = ::requestPlanGeneration,
            modifier = Modifier.padding(contentPadding),
        )
    } else if (activeDetailDay != null) {
        NutritionDayDetail(
            day = activeDetailDay,
            entries = NutritionStore.load(context, activeDetailDay.date),
            onBack = { detailDay = null },
            onLog = { selectedDate = activeDetailDay.date; showLogFlow = true },
            onFulfill = { selectedDate = activeDetailDay.date; showFulfill = true },
            onGrocery = { selectedDate = activeDetailDay.date; showGrocery = true },
            // iOS shows the grocery-list action when the active plan contains
            // suggestions anywhere, not only when the selected day has one.
            hasMealSuggestions = plan.any { it.meals.isNotEmpty() },
            onPlannedMealAction = { meal, method -> handlePlannedMealAction(activeDetailDay.date, meal, method) },
            onDiscardPlannedMeal = { meal -> discardPlannedMeal(activeDetailDay.date, meal) },
            isSuggestionHidden = { meal -> isPlannedMealHidden(activeDetailDay.date, meal) },
            modifier = Modifier.padding(contentPadding),
        )
    } else if (showEntries) {
        NutritionEntriesScreen(
            date = selectedDate,
            target = plan.firstOrNull { it.date == selectedDate }?.targets ?: targets,
            entries = meals,
            onBack = { showEntries = false },
            onAdd = { showLogFlow = true },
            onEdit = { editingMeal = it; showLogFlow = true },
            onChanged = { updated -> NutritionStore.save(context, selectedDate, updated); meals = updated; refreshEnergyBalance(); queueRemoteSync() },
            modifier = Modifier.padding(contentPadding),
        )
    } else {
        NutritionHub(
            selectedDate = selectedDate,
            today = today,
            plan = plan,
            profile = profile,
            metadata = planMetadata,
            entries = meals,
            preferences = preferences,
            energyHistory = energyHistory,
            hasGeneratedPlan = generatedPlan != null,
            showsAiCoach = showsAiCoach,
            onSelectDate = { selectedDate = it },
            onToday = { selectedDate = today },
            onLog = { showLogFlow = true },
            onIdeas = { fridgeTarget = null; showMoreActions = true },
            onProgress = { showEntries = true },
            onDay = { detailDay = it },
            onOpenPlan = { showPlanOverview = true },
            onEditPlan = { showTargets = true },
            onEditPreferences = { showPreferences = true },
            onPlanGrocery = { showGrocery = true },
            onGeneratePlan = ::requestPlanGeneration,
            onPlannedMealAction = { meal, method -> handlePlannedMealAction(selectedDate, meal, method) },
            onDiscardPlannedMeal = { meal -> discardPlannedMeal(selectedDate, meal) },
            isSuggestionHidden = { meal -> isPlannedMealHidden(selectedDate, meal) },
            onLogSomethingElse = { meal -> openLogForSomethingElse(selectedDate, meal) },
            onBookmark = ::updateHubBookmark,
            modifier = Modifier.padding(contentPadding),
        )
    }

    if (showLogFlow) {
        NutritionLogFlow(
            selectedDate = selectedDate,
            initialType = editingMeal?.type ?: MealType.detect(),
            initialEntry = editingMeal,
            initialMethod = plannedMethod,
            initialTitle = plannedTitle,
            recent = NutritionStore.recent(context),
            aiNutritionAccess = aiNutritionAccess,
            onPremiumOffer = { showNutritionPaywall = true },
            onBookmarkChanged = { reload(); queueRemoteSync() },
            onDismiss = { showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null; plannedMealKey = null },
            onSave = { entry ->
                val normalized = normalizeSavedMeal(entry, selectedDate, plannedMealKey, editingMeal)
                val dateEntries = if (editingMeal == null) NutritionStore.load(context, selectedDate) + normalized else NutritionStore.load(context, selectedDate).map { if (it.id == editingMeal!!.id) normalized.copy(id = it.id) else it }
                NutritionStore.save(context, selectedDate, dateEntries)
                scope.launch { HealthConnectNutritionSync.publish(context, NutritionStore.allForSync(context)) }
                showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null; plannedMealKey = null; dayMealStateVersion++
                reload(); queueRemoteSync()
                refreshEnergyBalance()
            },
        )
    }
    if (showTargets) NutritionTargetsDialog(targets, { showTargets = false }) { value ->
        targets = value; NutritionStore.saveTargets(context, value); NutritionStore.clearGeneratedPlan(context); generatedPlan = null; planMetadata = null; showTargets = false; queueRemoteSync()
    }
    if (showPreferences) NutritionPreferencesDialog(
        preferences,
        { showPreferences = false },
        isInitialSetup = !hasNutritionProfile,
        isHealthConnectEnabled = profile?.isHealthConnectEnabled == true,
        onRequestHealthConnect = onRequestHealthConnect,
    ) { value ->
        NutritionPlanProfileSnapshot.ensureBaseline(context); preferences = value; NutritionStore.savePreferences(context, value); hasNutritionProfile = true; showPreferences = false; queueRemoteSync()
    }
    if (showIdeas) MealIdeasDialog(plan.firstOrNull { it.date == selectedDate }, fridgeTarget, { showIdeas = false; fridgeTarget = null }) { planned ->
        val entry = MealLog(System.currentTimeMillis(), planned.title, planned.targets.calories, planned.targets.protein, planned.targets.carbs, planned.targets.fat, planned.type, selectedDate, planned.guidance, "meal_idea", plannedMealKey = plannedMealKey)
        val updated = NutritionStore.load(context, selectedDate) + entry
        NutritionStore.save(context, selectedDate, updated); reload(); refreshEnergyBalance(); showIdeas = false; fridgeTarget = null; plannedMealKey = null; dayMealStateVersion++; queueRemoteSync()
    }
    if (showMoreActions) NutritionMoreActionsDialog(
        onDismiss = { showMoreActions = false },
        onIdeas = { showMoreActions = false; fridgeTarget = null; showIdeas = true },
        onFulfill = { showMoreActions = false; showFulfill = true },
    )
    if (showGrocery) NutritionGroceryDialog(plan, { showGrocery = false })
    if (showFulfill) NutritionFulfillDialog(
        current = meals.total(),
        target = plan.firstOrNull { it.date == selectedDate }?.targets ?: targets,
        onDismiss = { showFulfill = false },
        onSelect = { suggestion ->
            val updated = NutritionStore.load(context, selectedDate) + suggestion.copy(id = System.currentTimeMillis(), loggedDate = selectedDate, source = "fulfill_missing_macros")
            NutritionStore.save(context, selectedDate, updated); reload(); refreshEnergyBalance(); showFulfill = false; queueRemoteSync()
        },
    )
    generatedPlanPreview?.let { preview ->
        NutritionPlanPreviewDialog(
            plan = preview,
            onDismiss = { generatedPlanPreview = null },
            onSave = {
                val metadata = NutritionStore.metadataForPlan(profile, preview)
                NutritionStore.saveGeneratedPlan(context, preview, metadata)
                NutritionPlanProfileSnapshot.save(context)
                generatedPlan = preview
                planMetadata = metadata
                generatedPlanPreview = null
                planVersion = NutritionStore.regeneratePlan(context)
                queueRemoteSync()
            },
        )
    }
    if (generatingPlan) NutritionPlanGenerationSheet()
    generationError?.let { message -> AlertDialog(onDismissRequest = { generationError = null }, title = { Text("NO SE PUDO GENERAR EL PLAN", fontFamily = AntonFontFamily) }, text = { Text(message, color = WildforceThemeTokens.textSecondary) }, confirmButton = { TextButton(onClick = { generationError = null; showMoreActions = true }) { Text("REINTENTAR", color = NutritionCoral) } }, dismissButton = { TextButton(onClick = { generationError = null }) { Text("CERRAR") } }) }
    if (showNutritionPaywall) {
        SubscriptionPaywall(
            accountId = accountPreferences.getString("id", null),
            tiers = listOf(SubscriptionTier.Nutrition, SubscriptionTier.Premium),
            onDismiss = { showNutritionPaywall = false },
            onPurchaseCompleted = {
                showNutritionPaywall = false
                accountToken?.let { token -> scope.launch {
                    subscriptionAccess(token).onSuccess { access ->
                        SubscriptionAccessStore.update(context, access)
                        aiNutritionAccess = access.features.aiNutritionAnalysis
                        showsAiCoach = access.features.aiCoach || !access.hasCoach
                    }
                } }
            },
        )
    }
}

/** A planned meal is immediately reviewable with its target macros, like iOS's “log as suggested”. */
private fun PlannedMeal.toLogDraft(date: LocalDate) = MealLog(
    id = System.currentTimeMillis(),
    name = title,
    calories = targets.calories,
    protein = targets.protein,
    carbs = targets.carbs,
    fat = targets.fat,
    type = type,
    loggedDate = date,
    notes = guidance,
    source = "planned",
    items = listOf(MealItem(title, targets.calories, targets.protein, targets.carbs, targets.fat)),
)

@Composable
private fun NutritionPlanGenerationAnimation() {
    PlanGenerationAnimation(
        icon = "♧",
        // SwiftUI presents this in a fixed-height sheet. Fixing Android's
        // animation canvas prevents AlertDialog from remeasuring for each line.
        modifier = Modifier.width(320.dp).height(380.dp),
        steps = listOf(
            "Analizando tus objetivos nutricionales…",
            "Evaluando tus preferencias alimentarias…",
            "Calculando calorías y macronutrientes…",
            "Ajustando el plan a tus entrenamientos…",
            "Consultando al generador de IA…",
            "Personalizando tus comidas…",
            "Equilibrando proteínas, grasas y carbohidratos…",
            "Terminando tu semana nutricional…",
            "Ya casi está…",
        ),
    )
}

/** Matches iOS's fixed 450 pt, non-dismissible plan-generation sheet. */
@Composable
private fun NutritionPlanGenerationSheet() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(220)) + slideInVertically(tween(360)) { height -> height },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = WildforceThemeTokens.background,
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.padding(top = 10.dp, bottom = 12.dp)
                                .size(width = 34.dp, height = 4.dp)
                                .background(WildforceThemeTokens.textSecondary.copy(alpha = .28f), CircleShape),
                        )
                        Text(
                            "DISEÑANDO TU SEMANA",
                            fontFamily = AntonFontFamily,
                            color = WildforceThemeTokens.textPrimary,
                        )
                        NutritionPlanGenerationAnimation()
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionHub(
    selectedDate: LocalDate,
    today: LocalDate,
    plan: List<NutritionDayPlan>,
    profile: OnboardingProfile?,
    metadata: NutritionPlanMetadata?,
    entries: List<MealLog>,
    preferences: NutritionPreferences,
    energyHistory: Map<LocalDate, NutritionEnergyPoint>,
    hasGeneratedPlan: Boolean,
    showsAiCoach: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onLog: () -> Unit,
    onIdeas: () -> Unit,
    onProgress: () -> Unit,
    onDay: (NutritionDayPlan) -> Unit,
    onOpenPlan: () -> Unit,
    onEditPlan: () -> Unit,
    onEditPreferences: () -> Unit,
    onPlanGrocery: () -> Unit,
    onGeneratePlan: () -> Unit,
    onPlannedMealAction: (PlannedMeal, LogMethod) -> Unit,
    onDiscardPlannedMeal: (PlannedMeal) -> Unit,
    isSuggestionHidden: (PlannedMeal) -> Boolean,
    onLogSomethingElse: (PlannedMeal) -> Unit,
    onBookmark: (MealLog, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val selectedPlan = plan.firstOrNull { it.date == selectedDate }
    // iOS exposes the hero again when the active plan reaches its last day,
    // allowing the user to generate the next week without opening settings.
    val planReachesLastDay = nutritionPlanIsCompleteOrOnLastDay(plan.map { it.date }, today)
    Column(modifier.fillMaxSize().liquidGlassBackground()) {
        // iOS keeps the weekly calendar out of the empty state. It only becomes
        // available once a generated plan provides selectable nutrition days.
        if (plan.isNotEmpty()) {
            NutritionCalendar(
                plan = plan,
                selected = selectedDate,
                today = today,
                caloriesForDate = { date -> NutritionStore.load(context, date).sumOf { it.loggedCalories() }.roundToInt() },
                onSelect = onSelectDate,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // NutritionHubView places the date/intake actions before the hero;
            // keeping that order preserves the iOS reading hierarchy and puts
            // the primary logging action in the same location on Android.
            item { IntakeActions(selectedDate, today, onToday, onLog, onIdeas) }
            if (showsAiCoach && (!hasGeneratedPlan || planReachesLastDay)) item {
                NutritionPlanHero(onGeneratePlan, hasGeneratedPlan, planReachesLastDay)
            }
            // iOS only shows daily macro progress for a selected planned day;
            // an empty target card would otherwise suggest a zero-calorie plan.
            if (selectedPlan != null) item {
                DailyProgressCard(
                    date = selectedDate,
                    current = entries.total(),
                    target = selectedPlan.targets,
                    onClick = onProgress,
                )
            }
            // Match iOS: the balance card is meaningful only when at least one
            // day contains food intake or real burned-calorie data.
            if (energyHistory.values.any { it.hasEnergyData }) {
                item { WeeklyBalanceCard(energyHistory) }
            }
            item {
                DayMealsPreview(
                    selectedDate = selectedDate,
                    today = today,
                    entries = entries,
                    day = selectedPlan,
                    canLog = canLogNutritionDate(selectedDate, today),
                    onAdd = onLog,
                    onPlannedMealAction = onPlannedMealAction,
                    onDiscardPlannedMeal = onDiscardPlannedMeal,
                    isSuggestionHidden = isSuggestionHidden,
                    onLogSomethingElse = onLogSomethingElse,
                    onBookmark = onBookmark,
                )
            }
            if (plan.isNotEmpty()) item {
                NutritionPlanSummaryCard(
                    plan = plan,
                    profile = profile,
                    metadata = metadata,
                    onClick = onOpenPlan,
                )
            } else {
                item { NutritionEmptyPlanState(showsAiCoach = showsAiCoach) }
            }
        }
    }
}

/** Mirrors NutritionPlan.isCompleteOrOnLastDay on iOS; it is independent of the selected day. */
internal fun nutritionPlanIsCompleteOrOnLastDay(planDates: List<LocalDate>, today: LocalDate): Boolean =
    planDates.maxOrNull()?.let { !it.isAfter(today) } ?: true

@Composable
private fun NutritionEmptyPlanState(showsAiCoach: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Eco, null, tint = NutritionCoral, modifier = Modifier.size(42.dp))
        Text("Aún no hay plan nutricional", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        Text(
            if (showsAiCoach) "Genera un plan nutricional semanal para ver calorías, macros y orientación personalizada."
            else "Tu coach compartirá aquí tu plan nutricional.",
            style = MaterialTheme.typography.caption,
            color = WildforceThemeTokens.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NutritionPlanSummaryCard(plan: List<NutritionDayPlan>, profile: OnboardingProfile?, metadata: NutritionPlanMetadata?, onClick: () -> Unit) {
    val week = plan.take(7)
    val averageCalories = (metadata?.dailyCalorieAverage?.takeIf { it > 0 } ?: week.map { it.targets.calories }.average()).toInt()
    val effectiveGoal = metadata?.goal ?: profile?.goal
    val effectivePhase = metadata?.bodyCompositionPhase ?: profile?.bodyCompositionPhase
    val phase = effectivePhase?.title
    val goal = effectiveGoal?.title
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(WildforceThemeTokens.backgroundSecondary).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Plan de 7 días", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                goal?.let { CompactPlanMetric(nutritionGoalIcon(effectiveGoal), it) }
                effectivePhase?.let { CompactPlanMetric(bodyCompositionPhaseIcon(it), it.title) }
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(averageCalories.toString(), style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
            }
            Text("media diaria", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Icon(Icons.Filled.ChevronRight, "Abrir plan", tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CompactPlanMetric(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(12.dp))
        Text(label, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1)
    }
}

private fun nutritionGoalIcon(goal: FitnessGoal?): ImageVector = when (goal) {
    null -> Icons.Filled.FitnessCenter
    FitnessGoal.LoseWeight -> Icons.Filled.Sync
    FitnessGoal.BuildMuscle -> Icons.Filled.FitnessCenter
    FitnessGoal.GainStrength -> Icons.Filled.Bolt
    FitnessGoal.ImproveEndurance -> Icons.Filled.DirectionsRun
    FitnessGoal.ImproveMobility -> Icons.Filled.SelfImprovement
    FitnessGoal.BodyRecomposition -> Icons.Filled.AccessibilityNew
    FitnessGoal.GeneralFitness -> Icons.Filled.Favorite
}

private fun bodyCompositionPhaseIcon(phase: io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase): ImageVector = when (phase) {
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Automatic -> Icons.Filled.AutoAwesome
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Bulk -> Icons.Filled.ArrowUpward
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Cut -> Icons.Filled.ArrowDownward
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Maintain -> Icons.Filled.Equalizer
}

@Composable
private fun NutritionPlanTimeline(plan: List<NutritionDayPlan>, onDay: (NutritionDayPlan) -> Unit) {
    val context = LocalContext.current.applicationContext
    val week = plan.take(7)
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 8.dp)) {
        week.forEachIndexed { index, day ->
            val loggedCalories = NutritionStore.load(context, day.date).sumOf { it.loggedCalories() }.roundToInt()
            val isPastOrToday = day.date <= today
            Row(Modifier.fillMaxWidth().clickable { onDay(day) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                    if (index < week.lastIndex) Box(Modifier.width(1.dp).height(92.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .22f)))
                    Box(Modifier.size(26.dp).background(if (day.date == today) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textSecondary.copy(alpha = .18f), CircleShape).border(if (day.date == today) 2.dp else 0.dp, if (day.date == today) WildforceThemeTokens.accent else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                        if (day.date < today) Icon(Icons.Filled.Check, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(15.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.width(64.dp)) {
                            Text(if (day.date == today) "HOY" else day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, color = if (day.date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary)
                            Text(day.date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es"))), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                        }
                        val workoutTitle = day.safeWorkoutTitle()
                        Column(Modifier.weight(1f)) {
                            Text(workoutTitle.ifBlank { day.type.title }, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                            if (workoutTitle.isNotBlank() && day.type.title != workoutTitle) Text(day.type.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        Text("${day.targets.calories} kcal", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Icon(Icons.Filled.ChevronRight, null, tint = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp).size(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        NutritionMacroSymbol(NutritionMacroIcon.Protein, WildforceThemeTokens.textSecondary, Modifier.size(12.dp)); Text("${day.targets.protein} g", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                        NutritionMacroSymbol(NutritionMacroIcon.Carbs, WildforceThemeTokens.textSecondary, Modifier.size(12.dp)); Text("${day.targets.carbs} g", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                        NutritionMacroSymbol(NutritionMacroIcon.Fat, WildforceThemeTokens.textSecondary, Modifier.size(12.dp)); Text("${day.targets.fat} g", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                    }
                    if (isPastOrToday) {
                        LinearProgressIndicator((loggedCalories.toFloat() / day.targets.calories.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.fillMaxWidth().height(5.dp), color = if (day.date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .5f), backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = .16f))
                        Text("$loggedCalories / ${day.targets.calories} kcal", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionPlanOverview(
    plan: List<NutritionDayPlan>,
    profile: OnboardingProfile?,
    metadata: NutritionPlanMetadata?,
    onBack: () -> Unit,
    onDay: (NutritionDayPlan) -> Unit,
    onGrocery: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val averageCalories = (metadata?.dailyCalorieAverage?.takeIf { it > 0 }
        ?: plan.take(7).map { it.targets.calories }.average()).toInt()
    val startDate = metadata?.startsOn ?: plan.minOfOrNull { it.date }
    // Day guidance is not a plan note in iOS; only NutritionPlan.notes belongs
    // in the overview header and in the notes action.
    val notes = metadata?.notes?.takeIf(String::isNotBlank)
    val effectiveGoal = metadata?.goal ?: profile?.goal
    val effectivePhase = metadata?.bodyCompositionPhase ?: profile?.bodyCompositionPhase
    val phase = effectivePhase?.title
    val goal = effectiveGoal?.title
    var menuOpen by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showRedoConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .liquidGlassBackground()
            // The plan overview remains full-height beneath the floating
            // Resume/navigation overlay; its list keeps trailing scroll space.
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", tint = WildforceThemeTokens.textPrimary) }
            // The SwiftUI destination has no navigation title; only the
            // back affordance and trailing plan menu are visible.
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreHoriz, "Opciones del plan", tint = WildforceThemeTokens.textSecondary) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    // SwiftUI's Menu keeps regular labels in the primary text
                    // color and marks only the destructive redo action in red.
                    // NutritionCoral here made every plan action look like an
                    // accent CTA, unlike NutritionPlanOverviewView on iOS.
                    if (!notes.isNullOrBlank()) DropdownMenuItem(onClick = { menuOpen = false; showNotes = true }) { Icon(Icons.Filled.Notes, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp)); Text("Ver notas", Modifier.padding(start = 12.dp)) }
                    if (plan.any { it.meals.isNotEmpty() }) DropdownMenuItem(onClick = { menuOpen = false; onGrocery() }) { Icon(Icons.Filled.ShoppingBasket, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp)); Text("Lista de la compra", Modifier.padding(start = 12.dp)) }
                    DropdownMenuItem(onClick = { menuOpen = false; showRedoConfirmation = true }) { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colors.error, modifier = Modifier.size(18.dp)); Text("Rehacer plan", Modifier.padding(start = 12.dp), color = MaterialTheme.colors.error) }
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("PLAN ACTUAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Text(if (phase.isNullOrBlank()) "Plan de 7 días" else "Plan de 7 días • $phase", fontFamily = Exo2FontFamily, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$averageCalories kcal", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                            Text("media diaria", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        goal?.let { CompactPlanMetric(nutritionGoalIcon(effectiveGoal), it) }
                        effectivePhase?.let { CompactPlanMetric(bodyCompositionPhaseIcon(it), it.title) }
                        startDate?.let { CompactPlanMetric(Icons.Filled.CalendarToday, it.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))) }
                    }
                    // iOS keeps plan notes behind the header's "View notes"
                    // menu action; they are not rendered inline in the card.
                }
            }
            item { NutritionPlanTimeline(plan, onDay) }
        }
    }
    if (showNotes && !notes.isNullOrBlank()) NutritionPlanNotesDialog(
        notes = notes,
        onDismiss = { showNotes = false },
    )
    if (showRedoConfirmation) AlertDialog(
        onDismissRequest = { showRedoConfirmation = false },
        title = { Text("REHACER PLAN", fontFamily = AntonFontFamily) },
        text = { Text("El plan actual se sustituirá cuando guardes el nuevo plan.", color = WildforceThemeTokens.textSecondary) },
        confirmButton = {
            TextButton(onClick = { showRedoConfirmation = false; onRegenerate() }) {
                Text("REHACER PLAN", color = MaterialTheme.colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { showRedoConfirmation = false }) {
                Text("MANTENER PLAN ACTUAL", color = WildforceThemeTokens.textSecondary)
            }
        },
    )
}

/** Mirrors the iOS hub: the current day's meal timeline comes before the compact plan link. */
internal fun nutritionMealsSectionTitle(selectedDate: LocalDate, today: LocalDate, locale: Locale = Locale("es")): String =
    if (selectedDate == today) "Comidas de hoy"
    else "Comidas del ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM", locale))}"

/**
 * Mirrors iOS `NutritionHubViewModel.dayMeals`: dismissed suggestions are
 * removed before deciding whether the hub has anything to show. Otherwise a
 * day whose every planned meal was discarded renders a blank section instead
 * of the `ContentUnavailableView` empty state.
 */
internal fun visibleNutritionHubSuggestions(
    day: NutritionDayPlan?,
    entries: List<MealLog>,
    isSuggestionHidden: (PlannedMeal) -> Boolean,
): List<PlannedMeal> {
    val date = day?.date ?: return emptyList()
    return day.meals.filter { meal ->
        entries.none { it.plannedMealKey == NutritionStore.plannedMealKey(date, meal) } &&
            !isSuggestionHidden(meal)
    }
}

@Composable
private fun DayMealsPreview(
    selectedDate: LocalDate,
    today: LocalDate,
    entries: List<MealLog>,
    day: NutritionDayPlan?,
    canLog: Boolean,
    onAdd: () -> Unit,
    onPlannedMealAction: (PlannedMeal, LogMethod) -> Unit,
    onDiscardPlannedMeal: (PlannedMeal) -> Unit,
    isSuggestionHidden: (PlannedMeal) -> Boolean,
    onLogSomethingElse: (PlannedMeal) -> Unit,
    onBookmark: (MealLog, String?) -> Unit,
) {
    val visibleSuggestions = visibleNutritionHubSuggestions(day, entries, isSuggestionHidden)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                nutritionMealsSectionTitle(selectedDate, today),
                fontFamily = Exo2FontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = WildforceThemeTokens.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            if (canLog) {
                IconButton(onClick = onAdd, Modifier.semantics { contentDescription = "Registrar comida" }) {
                    Icon(Icons.Filled.Add, "Registrar comida", tint = NutritionCoral)
                }
            }
        }
        if (entries.isEmpty() && visibleSuggestions.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // iOS uses ContentUnavailableView here: the empty hub state is
                // centered and icon-led, unlike the custom text card used by
                // NutritionDayView for an empty planned day.
                Icon(
                    Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = WildforceThemeTokens.textSecondary,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    "Aún no hay comidas",
                    fontFamily = Exo2FontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = WildforceThemeTokens.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Aquí aparecerán las comidas registradas y las sugerencias de tu plan.",
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            entries.forEach { entry -> HubMealRow(entry, onBookmark) }
            visibleSuggestions.forEach { meal ->
                HubPlannedMealRow(
                    meal = meal,
                    canLog = canLog,
                    onAction = onPlannedMealAction,
                    onDiscard = onDiscardPlannedMeal,
                    onLogSomethingElse = { onLogSomethingElse(meal) },
                )
            }
        }
    }
}

@Composable
private fun HubPlannedMealRow(
    meal: PlannedMeal,
    canLog: Boolean,
    onAction: (PlannedMeal, LogMethod) -> Unit,
    onDiscard: (PlannedMeal) -> Unit,
    onLogSomethingElse: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val outlineColor = WildforceThemeTokens.textSecondary.copy(alpha = .4f)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // NutritionLogEntrySuggestionCard uses a dashed secondary outline
            // rather than a filled card or a navigable row.
            .drawBehind {
                drawRoundRect(
                    color = outlineColor,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                )
            }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val mealTint = mealTypeTint(meal.type)
        Box(Modifier.size(88.dp).background(mealTint.copy(alpha = .08f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(mealTypeIcon(meal.type), null, tint = mealTint, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(meal.title, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                    Text("Sugerida · ${meal.type.title}", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary, modifier = Modifier.background(mealTint.copy(alpha = .12f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                if (canLog) {
                    Box {
                        Button(
                            onClick = { menuOpen = true },
                            modifier = Modifier.heightIn(min = 40.dp),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        ) { Text("Registrar", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(onClick = { menuOpen = false; onAction(meal, LogMethod.Manual) }) {
                                Icon(Icons.Filled.CheckCircle, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp))
                                Text("Registrar como sugerida", Modifier.padding(start = 10.dp))
                            }
                            DropdownMenuItem(onClick = { menuOpen = false; onLogSomethingElse() }) {
                                Icon(Icons.Filled.Edit, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp))
                                Text("Registrar otra cosa", Modifier.padding(start = 10.dp))
                            }
                            DropdownMenuItem(onClick = { menuOpen = false; onDiscard(meal) }) {
                                Icon(Icons.Filled.Close, null, tint = NutritionRed, modifier = Modifier.size(18.dp))
                                Text("Descartar", Modifier.padding(start = 10.dp), color = NutritionRed)
                            }
                        }
                    }
                }
            }
            if (meal.foods.isNotEmpty()) {
                Text(meal.foods.joinToString(" · ") { it.first }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealMacro(NutritionMacroIcon.Calories, "${meal.targets.calories} kcal", NutritionCaloriesColor)
                MealMacro(NutritionMacroIcon.Protein, "${meal.targets.protein}g", ProteinColor)
                MealMacro(NutritionMacroIcon.Carbs, "${meal.targets.carbs}g", CarbsColor)
                MealMacro(NutritionMacroIcon.Fat, "${meal.targets.fat}g", FatColor)
            }
        }
    }
}

@Composable
private fun HubMealRow(entry: MealLog, onBookmark: (MealLog, String?) -> Unit) {
    val photo = remember(entry.photoPath) { entry.photoPath?.let(BitmapFactory::decodeFile) }
    val remoteImage = rememberNutritionRemoteImage(entry.mediaUrl, photo == null)
    val loggedTime = remember(entry.id) {
        Instant.ofEpochMilli(entry.id).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    var expanded by remember(entry.id) { mutableStateOf(false) }
    var confirmRemoveBookmark by remember(entry.id) { mutableStateOf(false) }
    var showBookmarkName by remember(entry.id) { mutableStateOf(false) }
    var bookmarkName by remember(entry.id) { mutableStateOf(entry.bookmarkName.ifBlank { entry.name }) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (photo != null) {
            androidx.compose.foundation.Image(photo.asImageBitmap(), "Foto de ${entry.name}", Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        } else if (remoteImage != null) {
            androidx.compose.foundation.Image(remoteImage.asImageBitmap(), "Imagen de ${entry.name}", Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        } else {
            NutritionMealPlaceholder(entry)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.name, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                    // iOS uses the general accent for the timestamp badge.
                    Text(loggedTime, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.background(WildforceThemeTokens.accent.copy(alpha = .10f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                IconButton(
                    onClick = { if (entry.isBookmarked) confirmRemoveBookmark = true else showBookmarkName = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        if (entry.isBookmarked) "Quitar de guardadas" else "Guardar",
                        tint = if (entry.isBookmarked) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .4f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MealMacro(NutritionMacroIcon.Calories, "${entry.loggedCalories().roundToInt()} kcal", NutritionCaloriesColor)
                MealMacro(NutritionMacroIcon.Protein, "${entry.loggedProtein().roundToInt()}g", ProteinColor)
                MealMacro(NutritionMacroIcon.Carbs, "${entry.loggedCarbs().roundToInt()}g", CarbsColor)
                MealMacro(NutritionMacroIcon.Fat, "${entry.loggedFat().roundToInt()}g", FatColor)
                if (entry.items.size > 1) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            if (expanded) "Contraer alimentos" else "Expandir alimentos",
                            tint = WildforceThemeTokens.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(tween(200)) + fadeIn(tween(150)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.items.forEach { food ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(food.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f))
                                if (food.quantity > 0) Text("${food.quantity} ${food.unit}".trim(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MealMacro(NutritionMacroIcon.Calories, "${food.calories.roundToInt()} kcal", NutritionCaloriesColor)
                                MealMacro(NutritionMacroIcon.Protein, "${food.protein.roundToInt()}g", ProteinColor)
                                MealMacro(NutritionMacroIcon.Carbs, "${food.carbs.roundToInt()}g", CarbsColor)
                                MealMacro(NutritionMacroIcon.Fat, "${food.fat.roundToInt()}g", FatColor)
                            }
                            if (food.notes.isNotBlank()) Text(food.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
            }
        }
    }
    if (confirmRemoveBookmark) {
        AlertDialog(
            onDismissRequest = { confirmRemoveBookmark = false },
            title = { Text("¿Quitar de guardadas?") },
            text = { Text("Esta comida dejará de aparecer en tus comidas guardadas.") },
            confirmButton = { TextButton(onClick = { confirmRemoveBookmark = false; onBookmark(entry, null) }) { Text("QUITAR", color = NutritionRed) } },
            dismissButton = { TextButton(onClick = { confirmRemoveBookmark = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
    if (showBookmarkName) {
        AlertDialog(
            onDismissRequest = { showBookmarkName = false },
            title = { Text("Guardar comida") },
            text = { OutlinedTextField(bookmarkName, { bookmarkName = it.take(80) }, label = { Text("Nombre") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { showBookmarkName = false; onBookmark(entry, bookmarkName.trim().ifBlank { entry.name }) }) { Text("GUARDAR", color = NutritionCoral) } },
            dismissButton = { TextButton(onClick = { showBookmarkName = false }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

internal fun mealTypeIcon(type: MealType): ImageVector = when (type) {
    MealType.Breakfast -> Icons.Filled.WbSunny       // iOS `sun.max.fill`
    MealType.Lunch -> Icons.Filled.WbCloudy          // iOS `sun.haze.fill`
    MealType.Dinner -> Icons.Filled.Nightlight      // iOS `moon.stars.fill`
    MealType.Snack -> Icons.Filled.LocalCafe        // iOS `takeoutbag.and.cup.and.straw.fill`
    MealType.PreWorkout -> Icons.Filled.DirectionsRun // iOS `figure.run`
    MealType.PostWorkout -> Icons.Filled.Favorite   // iOS `bolt.heart.fill`
}

internal fun mealTypeTint(type: MealType): Color = when (type) {
    MealType.Breakfast -> NutritionOrange           // iOS `.orange`
    MealType.Lunch -> Color(0xFFFFCC00)             // iOS `.yellow`
    MealType.Dinner -> Color(0xFF5856D6)            // iOS `.indigo`
    MealType.Snack -> Color(0xFF00C7BE)             // iOS `.mint`
    MealType.PreWorkout -> Color(0xFFFF2D55)        // iOS `.pink`
    MealType.PostWorkout -> Color(0xFFFF3B30)       // iOS `.red`
}

@Composable
private fun NutritionPlanHero(onGenerate: () -> Unit, hasGeneratedPlan: Boolean, planReachesLastDay: Boolean) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NutritionOrange, NutritionCoral), start = Offset.Zero, end = Offset(600f, 600f))),
    ) {
        NutritionHeroParticles(Modifier.matchParentSize())
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("ACTIVA TU SEMANA", fontFamily = AntonFontFamily, fontSize = 32.sp, color = Color.White)
                    Text("Crea un plan día a día adaptado a tus entrenamientos, objetivos y preferencias.", fontSize = 15.sp, color = Color.White.copy(alpha = .9f), modifier = Modifier.padding(top = 8.dp))
                }
                Box(Modifier.size(54.dp).background(Color.White.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                    NutritionCarrotIcon(Color.White, Modifier.size(28.dp))
                }
            }
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(50.dp), elevation = ButtonDefaults.elevation(defaultElevation = 5.dp, pressedElevation = 2.dp)) { Text(if (!hasGeneratedPlan) "GENERAR PLAN" else if (planReachesLastDay) "GENERAR NUEVA SEMANA" else "REGENERAR PLAN", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

/** Rising, lightly flickering equivalent of the SwiftUI DustParticlesEffectView in the iOS hero. */
@Composable
private fun NutritionHeroParticles(modifier: Modifier = Modifier) {
    val particlesProgress by rememberInfiniteTransition(label = "nutrition-hero-dust").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7_200, easing = LinearEasing), RepeatMode.Restart),
        label = "nutrition-hero-dust-progress",
    )
    Canvas(modifier) {
        val particles = listOf(
            Triple(.08f, .18f, 2.5f), Triple(.20f, .72f, 1.5f), Triple(.37f, .12f, 2f),
            Triple(.52f, .46f, 1.5f), Triple(.65f, .20f, 3f), Triple(.78f, .64f, 2f),
            Triple(.91f, .17f, 1.5f), Triple(.88f, .84f, 2.5f), Triple(.12f, .92f, 2f),
        )
        particles.forEachIndexed { index, (x, y, radius) ->
            val speed = .13f + (index % 4) * .035f
            val animatedY = (y - particlesProgress * speed + 1f) % 1f
            val baseAlpha = if (index % 2 == 0) .16f else .09f
            val flicker = .72f + ((particlesProgress + index * .17f) % 1f) * .28f
            drawCircle(
                Color.White.copy(alpha = baseAlpha * flicker),
                radius.dp.toPx(),
                Offset(size.width * x, size.height * animatedY),
            )
        }
    }
}

@Composable
private fun NutritionCalendar(
    plan: List<NutritionDayPlan>,
    selected: LocalDate,
    today: LocalDate,
    caloriesForDate: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
) {
    val planDates = plan.associateBy { it.date }
    // Three weeks starting on the previous Monday, extended to include the plan.
    val (firstVisibleDate, lastVisibleDate) = nutritionCalendarWindow(today, planDates.keys)
    val visibleDates = generateSequence(firstVisibleDate) { date -> date.plusDays(1).takeIf { !it.isAfter(lastVisibleDate) } }.toList()
    val selectedIndex = visibleDates.indexOf(selected).coerceAtLeast(0)
    val calendarState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0))
    LaunchedEffect(selected) { calendarState.animateScrollToItem((visibleDates.indexOf(selected).coerceAtLeast(0) - 2).coerceAtLeast(0)) }
    LazyRow(
        state = calendarState,
        modifier = Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visibleDates, key = { it }) { date ->
            val day = planDates[date]
            val isSelectable = day != null
            val isSelected = date == selected
            val isPast = date.isBefore(today)
            Column(
                Modifier.width(54.dp).height(84.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .08f))
                    .border(1.dp, if (date == today && !isSelected) WildforceThemeTokens.accent.copy(alpha = .15f) else Color.Transparent, RoundedCornerShape(12.dp))
                    .alpha(if (isSelected) 1f else if (!isSelectable) if (date == today) .72f else .55f else if (isPast) .9f else 1f)
                    .clickable(enabled = isSelectable) { onSelect(date) }.padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = if (isSelected) WildforceThemeTokens.primaryButtonText else if (date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.h6, color = if (isSelected) WildforceThemeTokens.primaryButtonText else if (isSelectable) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary)
                val consumed = caloriesForDate(date)
                if (day != null && shouldShowCalendarCalorieRing(date, today, consumed)) {
                    CalendarCalorieRing(
                        progress = nutritionCalendarProgress(consumed, day.targets.calories),
                        color = if (consumed > day.targets.calories) NutritionCoral else if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.accent,
                        isSelected = isSelected,
                    )
                } else if (day != null) {
                    Box(Modifier.padding(top = 4.dp).size(8.dp).background(if (isSelected) WildforceThemeTokens.primaryButtonText else demandColor(day.energyDemand), CircleShape))
                } else {
                    Box(Modifier.padding(top = 4.dp).size(8.dp).background(Color.Transparent, CircleShape).border(1.dp, WildforceThemeTokens.textSecondary.copy(alpha = if (isPast) .14f else .25f), CircleShape))
                }
            }
        }
    }
}

internal fun shouldShowCalendarCalorieRing(date: LocalDate, today: LocalDate, _consumedCalories: Int): Boolean =
    !date.isAfter(today)

@Composable
private fun CalendarCalorieRing(progress: Float, color: Color, isSelected: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtLeast(0f),
        animationSpec = tween(durationMillis = 560),
        label = "calendar-calorie-ring",
    )
    Box(Modifier.padding(top = 1.dp).size(18.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(16.dp)) {
            val background = if (isSelected) Color.White.copy(alpha = .3f) else Color.Gray.copy(alpha = .14f)
            drawArc(background, -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            if (animatedProgress > 0f) drawArc(color, -90f, 360f * animatedProgress.coerceIn(0f, 1f), false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        if (progress > 1f && (progress - 1f) % 1f > 0f) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun IntakeActions(date: LocalDate, today: LocalDate, onToday: () -> Unit, onLog: () -> Unit, onIdeas: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (date == today) "Ingesta de hoy" else "Ingesta del ${date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")))}", fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
        Spacer(Modifier.weight(1f))
        if (date != today) TextButton(onClick = onToday) { Text("Hoy", color = NutritionActionColor) }
        else {
            IconButton(onClick = onLog, Modifier.semantics { contentDescription = "Registrar ingesta" }) { Icon(Icons.Filled.AddCircle, "Registrar ingesta", tint = NutritionActionColor, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onIdeas) { Icon(Icons.Filled.MoreHoriz, "Más opciones", tint = WildforceThemeTokens.textSecondary) }
        }
    }
}

@Composable
private fun DailyProgressCard(date: LocalDate, current: NutritionTargets, target: NutritionTargets, onClick: () -> Unit) {
    val progress = if (target.calories > 0) current.calories.toFloat() / target.calories else 0f
    // iOS animates the nutrition ring when its totals change; use the same
    // 650 ms cadence here instead of Compose's default spring.
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(650), label = "calorie progress")
    val animatedCalories by animateIntAsState(current.calories, animationSpec = tween(650), label = "calorie-value")
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatNutritionInteger(animatedCalories), fontFamily = Exo2FontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(" / ${formatNutritionInteger(target.calories)} kcal", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${formatNutritionInteger(target.calories - current.calories)} kcal restantes", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
            ProgressRing(animatedProgress)
        }
        // NutritionDailyProgressCard on iOS has no second section title here;
        // the hub's `Ingesta de hoy` heading already labels this card. Its
        // calorie over-target state is represented by the ring, while only
        // individual macro overages show warning symbols below.
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroProgress("Proteína", current.protein, target.protein, ProteinColor, NutritionMacroIcon.Protein, Modifier.weight(1f))
            MacroProgress("Carbos", current.carbs, target.carbs, CarbsColor, NutritionMacroIcon.Carbs, Modifier.weight(1f))
            MacroProgress("Grasa", current.fat, target.fat, FatColor, NutritionMacroIcon.Fat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    // iOS gives the completed calorie ring a short spring lift; retain the
    // same state-driven cue on Android instead of leaving the threshold static.
    val scale by animateFloatAsState(
        targetValue = if (progress >= 1f) 1.03f else 1f,
        animationSpec = spring(dampingRatio = .6f),
        label = "nutrition-ring-complete",
    )
    Box(Modifier.size(92.dp).graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
        val track = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
        Canvas(Modifier.fillMaxSize()) {
            drawArc(track, -90f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawArc(NutritionCoral, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            if (progress > 1f) drawArc(NutritionCoral, -90f, 360f * (progress - 1f).coerceIn(0f, 1f), false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(verticalAlignment = Alignment.Bottom) { Text(formatNutritionInteger((progress * 100).roundToInt()), fontFamily = Exo2FontFamily, fontSize = 22.sp, color = WildforceThemeTokens.textPrimary); Text("%", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
private fun MacroProgress(title: String, current: Int, target: Int, color: Color, icon: NutritionMacroIcon, modifier: Modifier) {
    val fraction = if (target > 0) current.toFloat() / target else 0f
    val animatedFraction by animateFloatAsState(fraction, animationSpec = tween(650), label = "$title progress")
    val animatedCurrent by animateIntAsState(current, animationSpec = tween(650), label = "$title value")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NutritionMacroSymbol(icon, tint = color, modifier = Modifier.size(15.dp))
            Text(title, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (nutritionTargetExceeded(current, target)) {
                Icon(Icons.Filled.WarningAmber, null, tint = NutritionOrange, modifier = Modifier.size(14.dp))
            }
            Text("${formatNutritionInteger(animatedCurrent)} / ${formatNutritionInteger(target)} g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(start = 3.dp))
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) {
            Box(Modifier.fillMaxWidth(animatedFraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
        }
    }
}

internal fun nutritionTargetExceeded(current: Int, target: Int): Boolean = current > target

@Composable
private fun WeeklyBalanceCard(energyHistory: Map<LocalDate, NutritionEnergyPoint>) {
    val context = LocalContext.current.applicationContext
    // iOS exposes the previous 15 days, including both intake and the Health data.
    // The window is anchored to today, not to the currently inspected meal day.
    // Selecting an older day must not make the energy-balance chart jump in time.
    val today = LocalDate.now()
    val dates = remember(today) { (0L..14L).map { today.minusDays(it) }.reversed() }
    // Read intake on every recomposition so saving a meal immediately moves
    // today's bar and the displayed average, matching the iOS live update.
    val history = dates.map { date -> NutritionStore.load(context, date).sumOf { it.loggedCalories() }.roundToInt() }
    var inspectedDate by remember(today) { mutableStateOf<LocalDate?>(null) }
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Balance energético", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                Text(inspectedDate?.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("es"))) ?: "Últimos 15 días", style = MaterialTheme.typography.caption, color = if (inspectedDate != null) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary)
            }
        }
        val historicalIndexes = dates.indices.filter { dates[it] != LocalDate.now() }
        val averageEaten = historicalIndexes.map { history[it] }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val burnedValues = historicalIndexes.mapNotNull { energyHistory[dates[it]]?.let(::burnedCaloriesOrNull) }
        val averageBurned = burnedValues.average().takeIf { !it.isNaN() }?.toInt()
        val averageNet = historicalIndexes.mapNotNull { index ->
            val burned = energyHistory[dates[index]]?.let(::burnedCaloriesOrNull)
            burned?.takeIf { history[index] > 0 }?.let { history[index] - it }
        }.average().takeIf { !it.isNaN() }?.toInt()
        // SwiftUI only renders the summary row when at least one historical
        // metric exists. A meal logged today alone should still render the
        // chart, but must not produce a row containing only placeholders.
        val hasAverageSummary = averageEaten > 0 || averageBurned != null || averageNet != null
        val inspectedIndex = inspectedDate?.let(dates::indexOf)?.takeIf { it >= 0 }
        val displayedEaten = inspectedIndex?.let { history[it] } ?: averageEaten
        val displayedBurned = inspectedIndex?.let { index -> energyHistory[dates[index]]?.let(::burnedCaloriesOrNull)?.toInt() } ?: averageBurned
        val displayedNet = inspectedIndex?.let { index ->
            val eaten = history[index]
            val burned = energyHistory[dates[index]]?.let(::burnedCaloriesOrNull)?.toInt()
            burned?.takeIf { eaten > 0 }?.let { eaten - it }
        } ?: averageNet
        val netColor = when {
            displayedNet == null -> WildforceThemeTokens.textSecondary
            displayedNet > 0 -> NutritionOrange
            displayedNet < 0 -> NutritionGreen
            else -> WildforceThemeTokens.textSecondary
        }
        if (hasAverageSummary) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BalanceMetric(if (inspectedIndex == null) "Media ingerida" else "Ingerida", if (displayedEaten > 0) "$displayedEaten kcal" else "—", NutritionCoral)
                BalanceMetric(if (inspectedIndex == null) "Media quemada" else "Quemada", displayedBurned?.let { "$it kcal" } ?: "—", WildforceThemeTokens.accent)
                BalanceMetric("Balance", displayedNet?.let { "${if (it > 0) "+" else ""}$it kcal" } ?: "—", netColor)
            }
        }
        val chartBurnedValues = dates.mapNotNull { energyHistory[it]?.totalCalories ?: energyHistory[it]?.activeCalories }.map { it.toInt() }
          // Swift Charts derives the domain from the balance series themselves;
          // do not anchor the chart to a nutrition-plan target that is not part
          // of the energy-balance history.
          val maximum = (history + chartBurnedValues).maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
          Column(
              // iOS's 170pt chart includes the x-axis labels. Android keeps
              // the labels in a separate row, so the plot itself is 145dp;
              // together they occupy the same total height.
              Modifier.width(34.dp).height(145.dp),
              verticalArrangement = Arrangement.SpaceBetween,
          ) {
              Text("${maximum}", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
              Text("${maximum / 2}", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
              Text("0", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
          }
          Box(Modifier.weight(1f).height(145.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                dates.forEachIndexed { index, date ->
                    val eaten = history[index]
                    val point = energyHistory[date]
                    val segments = point?.let(::energyBalanceBarSegments)
                        ?: EnergyBalanceBarSegments(0, 0, 0, 0)
                    val burned = segments.totalCalories
                    val standard = segments.standardCalories
                    // Swift Charts starts the active series 40 kcal above the
                    // standard series. The gap is data-space, not a fixed dp gap:
                    // converting it with the same scale preserves both segment
                    // heights and the chart proportions on every density/scale.
                    val chartScale = 130f / maximum.toFloat()
                    val animatedBurnedHeight by animateDpAsState(
                        targetValue = (130 * burned / maximum).dp,
                        animationSpec = tween(420),
                        label = "energy-balance-bar-$date",
                    )
                    val animatedStandardHeight by animateDpAsState(
                        targetValue = (standard * chartScale).dp,
                        animationSpec = tween(420),
                        label = "energy-balance-standard-$date",
                    )
                    val animatedGapHeight by animateDpAsState(
                        targetValue = (segments.gapCalories * chartScale).dp,
                        animationSpec = tween(420),
                        label = "energy-balance-gap-$date",
                    )
                    val animatedActiveHeight by animateDpAsState(
                        targetValue = (segments.activeCalories * chartScale).dp,
                        animationSpec = tween(420),
                        label = "energy-balance-active-$date",
                    )
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable { inspectedDate = if (inspectedDate == date) null else date },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (burned > 0) {
                            Column(
                                Modifier.fillMaxWidth(.62f).height(animatedBurnedHeight),
                                verticalArrangement = Arrangement.Top,
                            ) {
                                // Compose lays a Column out from top to bottom.
                                // Put active first so the visual stack reads
                                // bottom-to-top as standard, gap, active, like
                                // the two Swift Charts BarMarks on iOS.
                                if (segments.activeCalories > 0) {
                                    Box(
                                        Modifier.fillMaxWidth().height(animatedActiveHeight).background(
                                            Brush.verticalGradient(listOf(EnergyBalanceBarColor, EnergyBalanceBarColor)),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                        ),
                                    )
                                    Spacer(Modifier.height(animatedGapHeight))
                                }
                                if (standard > 0) {
                                    Box(
                                        Modifier.fillMaxWidth().height(animatedStandardHeight).background(
                                            Brush.verticalGradient(
                                                listOf(EnergyBalanceBarColor.copy(alpha = .5f), EnergyBalanceBarColor.copy(alpha = .5f)),
                                            ),
                                            if (segments.activeCalories > 0) RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp) else RoundedCornerShape(4.dp),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
          val eatenLineColor = WildforceThemeTokens.textPrimary
          val eatenPointColor = WildforceThemeTokens.backgroundSecondary
          val chartGridColor = WildforceThemeTokens.textSecondary.copy(alpha = .15f)
          Canvas(Modifier.fillMaxSize()) {
              // Canvas coordinates are pixels, whereas the bars above use the
              // full 170.dp plot height. Use the actual pixel height here;
              // using a hard-coded 154px made small meals (e.g. a banana)
              // collapse against the baseline on xxhdpi devices.
              val lineWidth = 2.dp.toPx()
              val pointRadius = 4.dp.toPx()
              // Match Swift Charts' subtle horizontal grid marks.
              (0..3).forEach { mark ->
                  val y = size.height * mark / 3f
                  drawLine(chartGridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
              }
              val points = history.mapIndexedNotNull { index, eaten ->
                  if (eaten <= 0) null else Offset(
                      x = if (dates.lastIndex == 0) size.width / 2f else size.width * index / dates.lastIndex,
                      y = size.height - (size.height * eaten / maximum),
                  )
              }
              points.zipWithNext().forEach { (from, to) ->
                  drawLine(eatenLineColor, from, to, strokeWidth = lineWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
              }
              points.forEach { point ->
                  drawCircle(eatenPointColor, radius = pointRadius, center = point)
                  drawCircle(eatenLineColor, radius = pointRadius, center = point, style = Stroke(width = lineWidth))
              }
          }
          // Match iOS's ChartSelectionOverlay: selecting a day exposes a
          // vertical guide, a highlighted point at the burned value, and a
          // compact date/value tooltip instead of only outlining the column.
          inspectedIndex?.let { index ->
              val selectedDateValue = dates[index]
              val selectedPoint = energyHistory[selectedDateValue]
              val selectedY = selectedPoint?.totalCalories ?: selectedPoint?.activeCalories ?: 0.0
              val selectedXFraction = if (dates.lastIndex == 0) .5f else index.toFloat() / dates.lastIndex
              val selectedYFraction = (selectedY / maximum).toFloat().coerceIn(0f, 1f)
              BoxWithConstraints(Modifier.fillMaxSize()) {
                  val plotWidth = maxWidth
                  val plotHeight = maxHeight
                  val xOffset = plotWidth * selectedXFraction
                  val yOffset = plotHeight * (1f - selectedYFraction)
                  Box(
                      Modifier
                          .offset(x = xOffset - 1.dp, y = 0.dp)
                          .width(2.dp)
                          .fillMaxHeight()
                          .background(WildforceThemeTokens.accent.copy(alpha = .3f)),
                  )
                  Box(
                      Modifier
                          .offset(x = xOffset - 5.dp, y = yOffset - 5.dp)
                          .size(10.dp)
                          .background(WildforceThemeTokens.accent, CircleShape)
                          .border(2.dp, Color.White, CircleShape),
                  )
                  Column(
                      Modifier
                          .align(Alignment.TopStart)
                          .offset(
                              x = (xOffset - 56.dp).coerceIn(4.dp, (plotWidth - 116.dp).coerceAtLeast(4.dp)),
                              y = (yOffset - 40.dp).coerceAtLeast(4.dp),
                          )
                          .shadow(2.dp, RoundedCornerShape(8.dp))
                          .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(8.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                      Text("${selectedY.toInt()} kcal", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                      Text(selectedDateValue.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("es"))), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1)
                  }
              }
          }
          }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dates.filterIndexed { index, _ -> index % 4 == 0 || index == dates.lastIndex }.forEach { date ->
                Text(date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es"))), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { BalanceLegend(EnergyBalanceBarColor.copy(alpha = .5f), "Estándar"); BalanceLegend(EnergyBalanceBarColor, "Activa"); BalanceLegend(WildforceThemeTokens.textPrimary, "Ingerida") }
    }
}

@Composable private fun BalanceMetric(label: String, value: String, color: Color) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Medium, color = WildforceThemeTokens.textSecondary); Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text(value.removeSuffix(" kcal"), style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold, color = color); Text("kcal", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) } } }
@Composable private fun BalanceLegend(color: Color, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(color, CircleShape)); Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Medium, color = WildforceThemeTokens.textSecondary) } }

@Composable
private fun PlanHeader(
    plan: List<NutritionDayPlan>,
    preferences: NutritionPreferences,
    onEditPlan: () -> Unit,
    onEditPreferences: () -> Unit,
    onGrocery: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val average = plan.take(7).map { it.targets.calories }.average().toInt()
    val notes = plan.mapNotNull { it.notes.takeIf(String::isNotBlank) }.firstOrNull()
    var menuOpen by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) { Text("PLAN ACTUAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("Plan nutricional semanal", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 2) }
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.End) { Text(average.toString(), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1); Text("kcal/día", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1) }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreHoriz, "Opciones del plan", tint = WildforceThemeTokens.textSecondary) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (notes != null) DropdownMenuItem(onClick = { menuOpen = false; showNotes = true }) { Icon(Icons.Filled.Notes, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text("Ver notas", Modifier.padding(start = 12.dp)) }
                    DropdownMenuItem(onClick = { menuOpen = false; onEditPlan() }) { Icon(Icons.Filled.Tune, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text("Editar objetivos", Modifier.padding(start = 12.dp)) }
                    DropdownMenuItem(onClick = { menuOpen = false; onGrocery() }) { Icon(Icons.Filled.ShoppingBasket, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text("Lista de la compra", Modifier.padding(start = 12.dp)) }
                    DropdownMenuItem(onClick = { menuOpen = false; onRegenerate() }) { Icon(Icons.Filled.Refresh, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text("Regenerar plan", Modifier.padding(start = 12.dp)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanTag(Icons.Filled.CalendarToday, "7 días")
            PlanTag(Icons.Filled.LocalFireDepartment, "Objetivo diario")
            PlanTag(Icons.Filled.FitnessCenter, "Adaptado al entreno")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            PlanMetric(Icons.Filled.Restaurant, "Estilo", preferences.dietaryStyle, onEditPreferences)
            PlanMetric(Icons.Filled.RestaurantMenu, "Comidas al día", "${preferences.mealsPerDay} comidas", onEditPreferences)
            PlanMetric(Icons.Filled.Lightbulb, "Sugerencias", if (preferences.wantsSuggestions) "Activadas" else "Desactivadas", onEditPreferences)
        }
        Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .12f))
        Text("Las calorías y macros cambian según la demanda de cada entrenamiento.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
    if (showNotes && notes != null) NutritionPlanNotesDialog(
        notes = notes,
        onDismiss = { showNotes = false },
    )
}

@Composable
private fun NutritionPlanNotesDialog(notes: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .height(320.dp),
            shape = RoundedCornerShape(24.dp),
            color = WildforceThemeTokens.backgroundSecondary,
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Notas del plan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WildforceThemeTokens.textPrimary,
                )
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = notes,
                        color = WildforceThemeTokens.textPrimary,
                        style = MaterialTheme.typography.body1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTag(icon: ImageVector, text: String) {
    Row(Modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(13.dp))
        Text(text, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun PlanMetric(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(19.dp)); Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text(value, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary) }
}

@Composable
internal fun NutritionDayHeader(day: NutritionDayPlan, logged: NutritionTargets, isToday: Boolean, onClick: () -> Unit) {
    val gradient = when (day.energyDemand) { EnergyDemand.High -> listOf(NutritionOrange, NutritionRed); EnergyDemand.Medium -> listOf(NutritionCoral, NutritionOrange); EnergyDemand.Low -> listOf(NutritionGreen, NutritionCoral) }
    val dayTypeColor = nutritionDayTypeColor(day.type)
    var typeBadgeAppeared by remember(day.date, day.type) { mutableStateOf(false) }
    LaunchedEffect(day.date, day.type) { typeBadgeAppeared = true }
    val typeBadgeScale by animateFloatAsState(
        targetValue = if (typeBadgeAppeared) 1f else .8f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 560f),
        label = "nutrition-day-type-${day.date}",
    )
    Column(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradient)).clickable(onClick = onClick).clearAndSetSemantics {
            contentDescription = nutritionDayAccessibilityLabel(day, logged, isToday)
        }.padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Column { Text(day.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("es"))), style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = .82f)); Text(if (isToday) "HOY" else day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).uppercase(), fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Text(day.type.title.uppercase(), style = MaterialTheme.typography.caption, color = Color.White, modifier = Modifier.graphicsLayer { scaleX = typeBadgeScale; scaleY = typeBadgeScale; alpha = if (typeBadgeAppeared) 1f else 0f }.background(dayTypeColor.copy(alpha = .62f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp)); Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(NutritionMacroIcon.Calories, Color.White, Modifier.size(15.dp)); Text("${day.targets.calories} kcal", Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold, color = Color.White) } }
        }
        Row(Modifier.background(Color.White.copy(alpha = .12f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.FitnessCenter, null, tint = Color.White, modifier = Modifier.size(14.dp)); Text(day.safeWorkoutTitle().ifBlank { day.type.title }.uppercase(), Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White) }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HeaderMacroPill(NutritionMacroIcon.Protein, logged.protein, day.targets.protein); HeaderMacroPill(NutritionMacroIcon.Carbs, logged.carbs, day.targets.carbs); HeaderMacroPill(NutritionMacroIcon.Fat, logged.fat, day.targets.fat); HeaderPill(Icons.Filled.Bolt, day.energyDemand.title)
        }
    }
}

private fun nutritionDayAccessibilityLabel(day: NutritionDayPlan, logged: NutritionTargets, isToday: Boolean): String {
    val dayName = if (isToday) "Hoy" else day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
    val date = day.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("es")))
    return listOf(
        "$dayName, $date",
        "${day.type.title}: ${day.safeWorkoutTitle().ifBlank { day.type.title }}",
        "${day.targets.calories} calorías",
        "${day.targets.protein} gramos de proteína (${logged.protein} registrados)",
        "${day.targets.carbs} gramos de carbohidratos (${logged.carbs} registrados)",
        "${day.targets.fat} gramos de grasa (${logged.fat} registrados)",
        "Demanda energética ${day.energyDemand.title}",
    ).joinToString(", ")
}

@Composable private fun HeaderPill(icon: ImageVector, text: String) { Row(Modifier.background(Color.White.copy(alpha = .10f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp)); Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White) } }
@Composable
private fun HeaderMacroPill(icon: NutritionMacroIcon, logged: Int, target: Int) {
    val progress = nutritionHeaderMacroProgress(logged, target)
    Row(
        Modifier.background(Color.White.copy(alpha = .10f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 1.5.dp.toPx()
                drawArc(Color.White.copy(alpha = .35f), -90f, 360f, false, style = Stroke(stroke))
                drawArc(Color.White, -90f, 360f * progress, false, style = Stroke(stroke))
            }
            NutritionMacroSymbol(icon, Color.White, Modifier.size(8.dp))
        }
        Text("$target g", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

internal fun nutritionHeaderMacroProgress(logged: Int, target: Int): Float =
    if (target > 0) (logged.toFloat() / target).coerceIn(0f, 1f) else 0f

internal fun nutritionDayTypeColor(type: NutritionDayType): Color = when (type) {
    NutritionDayType.Training -> NutritionOrange
    NutritionDayType.Rest -> NutritionBlue
    NutritionDayType.Recovery -> NutritionGreen
}

private fun demandColor(demand: EnergyDemand) = when (demand) { EnergyDemand.High -> NutritionRed; EnergyDemand.Medium -> NutritionOrange; EnergyDemand.Low -> NutritionGreen }
internal fun List<MealLog>.total() = NutritionTargets(
    sumOf { it.loggedCalories() }.roundToInt(),
    sumOf { it.loggedProtein() }.roundToInt(),
    sumOf { it.loggedCarbs() }.roundToInt(),
    sumOf { it.loggedFat() }.roundToInt(),
)
