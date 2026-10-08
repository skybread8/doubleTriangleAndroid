package io.codepassion.doubletriangle.nutrition

import android.content.Context
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.WorkoutRemoteSync
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** iOS AccentColor2 — the primary nutrition emphasis. */
internal val NutritionCoral = Color(0xFFD4AF37)
/** SwiftUI's system orange, used by the nutrition day gradients. */
internal val NutritionOrange = Color(0xFFFF9500)
/** iOS AccentColor3 — contrast only, never the default nutrition tint. */
internal val NutritionRed = Color(0xFFF44545)
/** SwiftUI's system green, used for recovery and a negative energy balance. */
internal val NutritionGreen = Color(0xFF34C759)
internal val ProteinColor = Color(0xFFE2B93B)
internal val CarbsColor = Color(0xFF70A8DA)
internal val FatColor = Color(0xFFB88BD8)

@Composable
fun NutritionScreen(
    contentPadding: PaddingValues = PaddingValues(),
    profile: OnboardingProfile? = null,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var targets by remember(profile) { mutableStateOf(NutritionStore.loadTargets(context, profile)) }
    var preferences by remember { mutableStateOf(NutritionStore.loadPreferences(context)) }
    var hasNutritionProfile by remember { mutableStateOf(NutritionStore.hasNutritionProfile(context)) }
    var planVersion by remember { mutableStateOf(NutritionStore.loadPlanVersion(context)) }
    val fallbackPlan = remember(profile, targets, preferences, today, planVersion) { NutritionStore.weekPlan(profile, targets, today, preferences, planVersion) }
    var generatedPlan by remember(profile) { mutableStateOf(NutritionStore.loadGeneratedPlan(context)) }
    // A generated week is only a draft until the person explicitly accepts it.
    // This prevents an accidental regeneration from replacing the active plan.
    var generatedPlanPreview by remember { mutableStateOf<List<NutritionDayPlan>?>(null) }
    val plan = generatedPlan ?: fallbackPlan
    var meals by remember(selectedDate) { mutableStateOf(NutritionStore.load(context, selectedDate)) }
    var showLogFlow by remember { mutableStateOf(false) }
    var showEntries by remember { mutableStateOf(false) }
    var detailDay by remember { mutableStateOf<NutritionDayPlan?>(null) }
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
    val planGenerationPreferences = remember { context.getSharedPreferences("wildforce_nutrition_generation", Context.MODE_PRIVATE) }
    var energyHistory by remember { mutableStateOf<Map<LocalDate, NutritionEnergyPoint>>(emptyMap()) }

    LaunchedEffect(profile) {
        energyHistory = runCatching {
            withContext(Dispatchers.IO) { NutritionEnergyBalance.read(context, { date -> NutritionStore.load(context, date).sumOf { it.calories } }) }
        }.getOrDefault(emptyMap())
    }

    BackHandler(enabled = detailDay != null || showEntries) {
        when {
            detailDay != null -> detailDay = null
            showEntries -> showEntries = false
        }
    }

    fun reload() { meals = NutritionStore.load(context, selectedDate) }

    // Equivalent to iOS's scheduled sync after a SwiftData save. Keep the
    // pending marker if transport fails so the next foreground sync retries.
    fun queueRemoteSync() {
        val syncProfile = profile ?: return
        val syncPreferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        syncPreferences.edit().putBoolean("remote_sync_pending", true).apply()
        scope.launch {
            if (WorkoutRemoteSync.synchronize(context, syncProfile, syncPreferences) is WorkoutRemoteSync.Outcome.Synchronized) {
                syncPreferences.edit().putBoolean("remote_sync_pending", false).apply()
            }
        }
    }

    fun requestPlanGeneration() {
        if (generatingPlan) return
        if (!hasNutritionProfile) {
            showPreferences = true
            return
        }
        val now = System.currentTimeMillis()
        val lastAttempt = planGenerationPreferences.getLong("last_remote_plan_attempt", 0)
        val remainingMillis = 60_000 - (now - lastAttempt)
        if (remainingMillis > 0) {
            generationError = "Espera ${((remainingMillis + 999) / 1_000)} segundos antes de volver a generar el plan."
            return
        }
        generationError = null
        generatingPlan = true
        planGenerationPreferences.edit().putLong("last_remote_plan_attempt", now).apply()
        scope.launch {
            runCatching { NutritionAIPlanner.generate(context, profile, targets, preferences, today) }
                .onSuccess { generatedPlanPreview = it }
                .onFailure { generationError = it.message ?: "No se pudo generar el plan" }
            generatingPlan = false
        }
    }

    if (detailDay != null) {
        NutritionDayDetail(
            day = detailDay!!,
            entries = NutritionStore.load(context, detailDay!!.date),
            onBack = { detailDay = null },
            onLog = { selectedDate = detailDay!!.date; showLogFlow = true },
            onPlannedMealAction = { meal, method ->
                selectedDate = detailDay!!.date
                plannedMealKey = NutritionStore.plannedMealKey(detailDay!!.date, meal)
                if (method == LogMethod.Manual) {
                    val planned = meal.toLogDraft(detailDay!!.date).copy(plannedMealKey = plannedMealKey)
                    NutritionStore.save(context, detailDay!!.date, NutritionStore.load(context, detailDay!!.date) + planned)
                    scope.launch { HealthConnectNutritionSync.publish(context, NutritionStore.load(context, detailDay!!.date)) }
                    reload()
                    plannedMealKey = null
                    dayMealStateVersion++
                    queueRemoteSync()
                } else if (method == LogMethod.Describe) {
                    fridgeTarget = meal.targets
                    showIdeas = true
                } else {
                    plannedMethod = method
                    plannedTitle = meal.title
                    showLogFlow = true
                }
            },
            onDiscardPlannedMeal = { meal ->
                NutritionStore.dismissPlannedMeal(context, detailDay!!.date, meal)
                dayMealStateVersion++
            },
            isSuggestionHidden = { meal ->
                // Reading this version makes discarding or fulfilling a suggestion redraw this day.
                dayMealStateVersion
                NutritionStore.isPlannedMealDismissed(context, detailDay!!.date, meal) ||
                    NutritionStore.load(context, detailDay!!.date).any { it.plannedMealKey == NutritionStore.plannedMealKey(detailDay!!.date, meal) }
            },
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
            onChanged = { updated -> NutritionStore.save(context, selectedDate, updated); meals = updated; queueRemoteSync() },
            modifier = Modifier.padding(contentPadding),
        )
    } else {
        NutritionHub(
            selectedDate = selectedDate,
            today = today,
            plan = plan,
            entries = meals,
            preferences = preferences,
            energyHistory = energyHistory,
            hasGeneratedPlan = generatedPlan != null,
            onSelectDate = { selectedDate = it },
            onToday = { selectedDate = today },
            onLog = { showLogFlow = true },
            onIdeas = { fridgeTarget = null; showMoreActions = true },
            onProgress = { showEntries = true },
            onDay = { detailDay = it },
            onEditPlan = { showTargets = true },
            onEditPreferences = { showPreferences = true },
            onPlanGrocery = { showGrocery = true },
            onPlanRegenerate = ::requestPlanGeneration,
            onGeneratePlan = ::requestPlanGeneration,
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
            onDismiss = { showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null; plannedMealKey = null },
            onSave = { entry ->
                val normalized = entry.copy(loggedDate = selectedDate, plannedMealKey = plannedMealKey)
                val dateEntries = if (editingMeal == null) NutritionStore.load(context, selectedDate) + normalized else NutritionStore.load(context, selectedDate).map { if (it.id == editingMeal!!.id) normalized.copy(id = it.id) else it }
                NutritionStore.save(context, selectedDate, dateEntries)
                scope.launch { NutritionStore.load(context, selectedDate).let { HealthConnectNutritionSync.publish(context, it) } }
                showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null; plannedMealKey = null; dayMealStateVersion++
                reload(); queueRemoteSync()
            },
        )
    }
    if (showTargets) NutritionTargetsDialog(targets, { showTargets = false }) { value ->
        targets = value; NutritionStore.saveTargets(context, value); NutritionStore.clearGeneratedPlan(context); generatedPlan = null; showTargets = false; queueRemoteSync()
    }
    if (showPreferences) NutritionPreferencesDialog(preferences, { showPreferences = false }, isInitialSetup = !hasNutritionProfile) { value ->
        preferences = value; NutritionStore.savePreferences(context, value); hasNutritionProfile = true; NutritionStore.clearGeneratedPlan(context); generatedPlan = null; showPreferences = false; queueRemoteSync()
    }
    if (showIdeas) MealIdeasDialog(plan.firstOrNull { it.date == selectedDate }, fridgeTarget, { showIdeas = false; fridgeTarget = null }) { planned ->
        val entry = MealLog(System.currentTimeMillis(), planned.title, planned.targets.calories, planned.targets.protein, planned.targets.carbs, planned.targets.fat, planned.type, selectedDate, planned.guidance, "meal_idea", plannedMealKey = plannedMealKey)
        val updated = NutritionStore.load(context, selectedDate) + entry
        NutritionStore.save(context, selectedDate, updated); reload(); showIdeas = false; fridgeTarget = null; plannedMealKey = null; dayMealStateVersion++; queueRemoteSync()
    }
    if (showMoreActions) NutritionMoreActionsDialog(
        onDismiss = { showMoreActions = false },
        onIdeas = { showMoreActions = false; fridgeTarget = null; showIdeas = true },
        onFulfill = { showMoreActions = false; showFulfill = true },
        onGrocery = { showMoreActions = false; showGrocery = true },
        onRegenerate = {
            showMoreActions = false
            requestPlanGeneration()
        },
    )
    if (showGrocery) NutritionGroceryDialog(plan, { showGrocery = false })
    if (showFulfill) NutritionFulfillDialog(
        current = meals.total(),
        target = plan.firstOrNull { it.date == selectedDate }?.targets ?: targets,
        onDismiss = { showFulfill = false },
        onSelect = { suggestion ->
            val updated = NutritionStore.load(context, selectedDate) + suggestion.copy(id = System.currentTimeMillis(), loggedDate = selectedDate, source = "fulfill_missing_macros")
            NutritionStore.save(context, selectedDate, updated); reload(); showFulfill = false
        },
    )
    generatedPlanPreview?.let { preview ->
        NutritionPlanPreviewDialog(
            plan = preview,
            onDismiss = { generatedPlanPreview = null },
            onSave = {
                NutritionStore.saveGeneratedPlan(context, preview)
                generatedPlan = preview
                generatedPlanPreview = null
                planVersion = NutritionStore.regeneratePlan(context)
                queueRemoteSync()
            },
        )
    }
    if (generatingPlan) AlertDialog(onDismissRequest = {}, title = { Text("DISEÑANDO TU SEMANA", fontFamily = AntonFontFamily) }, text = { NutritionPlanGenerationAnimation() }, confirmButton = {})
    generationError?.let { message -> AlertDialog(onDismissRequest = { generationError = null }, title = { Text("NO SE PUDO GENERAR EL PLAN", fontFamily = AntonFontFamily) }, text = { Text(message, color = WildforceThemeTokens.textSecondary) }, confirmButton = { TextButton(onClick = { generationError = null; showMoreActions = true }) { Text("REINTENTAR", color = NutritionCoral) } }, dismissButton = { TextButton(onClick = { generationError = null }) { Text("CERRAR") } }) }
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
    val transition = rememberInfiniteTransition(label = "nutrition-plan-generation")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1_800, easing = LinearEasing)), label = "nutrition-plan-ring")
    val opacity by transition.animateFloat(1f, .58f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "nutrition-plan-copy")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(1f, Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }, NutritionCoral, strokeWidth = 4.dp)
            Icon(Icons.Filled.Restaurant, null, tint = NutritionCoral, modifier = Modifier.size(28.dp))
        }
        Text("Equilibrando tus macros…", color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alpha(opacity))
        Text("La IA está adaptando calorías, macros, entrenamientos y preferencias alimentarias.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NutritionHub(
    selectedDate: LocalDate,
    today: LocalDate,
    plan: List<NutritionDayPlan>,
    entries: List<MealLog>,
    preferences: NutritionPreferences,
    energyHistory: Map<LocalDate, NutritionEnergyPoint>,
    hasGeneratedPlan: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onLog: () -> Unit,
    onIdeas: () -> Unit,
    onProgress: () -> Unit,
    onDay: (NutritionDayPlan) -> Unit,
    onEditPlan: () -> Unit,
    onEditPreferences: () -> Unit,
    onPlanGrocery: () -> Unit,
    onPlanRegenerate: () -> Unit,
    onGeneratePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val selectedPlan = plan.firstOrNull { it.date == selectedDate }
    Column(modifier.fillMaxSize().liquidGlassBackground()) {
        NutritionCalendar(
            plan = plan,
            selected = selectedDate,
            today = today,
            caloriesForDate = { date -> NutritionStore.load(context, date).sumOf { it.calories } },
            onSelect = onSelectDate,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasGeneratedPlan) item { NutritionPlanHero(onGeneratePlan) }
            item { IntakeActions(selectedDate, today, onToday, onLog, onIdeas) }
            item {
                DailyProgressCard(
                    date = selectedDate,
                    current = entries.total(),
                    target = selectedPlan?.targets ?: NutritionTargets(),
                    onClick = onProgress,
                )
            }
            if (energyHistory.isNotEmpty()) item { WeeklyBalanceCard(plan, selectedDate, energyHistory) }
            item { DayMealsPreview(selectedDate, today, entries, selectedPlan, onLog) { selectedPlan?.let(onDay) } }
            if (plan.isNotEmpty()) item {
                NutritionPlanSummaryCard(
                    plan = plan,
                    preferences = preferences,
                    onClick = { selectedPlan?.let(onDay) ?: onEditPlan() },
                )
            }
        }
    }
}

@Composable
private fun NutritionPlanSummaryCard(plan: List<NutritionDayPlan>, preferences: NutritionPreferences, onClick: () -> Unit) {
    val week = plan.take(7)
    val averageCalories = week.map { it.targets.calories }.average().toInt()
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(WildforceThemeTokens.backgroundSecondary).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Plan de 7 días", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactPlanMetric(Icons.Filled.Restaurant, preferences.dietaryStyle)
                CompactPlanMetric(Icons.Filled.FitnessCenter, "Adaptado al entreno")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(averageCalories.toString(), fontFamily = Exo2FontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text(" kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
            }
            Text("media diaria", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Icon(Icons.Filled.ChevronRight, "Abrir plan", tint = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(start = 12.dp).size(20.dp))
    }
}

@Composable
private fun CompactPlanMetric(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(12.dp))
        Text(label, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1)
    }
}

/** Mirrors the iOS hub: the current day's meal timeline comes before the compact plan link. */
@Composable
private fun DayMealsPreview(
    selectedDate: LocalDate,
    today: LocalDate,
    entries: List<MealLog>,
    day: NutritionDayPlan?,
    onAdd: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val suggestions = day?.meals.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (selectedDate == today) "COMIDAS DE HOY" else "COMIDAS DEL ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale("es"))).uppercase()}", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd, Modifier.semantics { contentDescription = "Registrar comida" }) {
                Icon(Icons.Filled.Add, "Registrar comida", tint = NutritionCoral)
            }
        }
        if (entries.isEmpty() && suggestions.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Aún no hay comidas", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                Text("Aquí aparecerán las comidas registradas y las sugerencias de tu plan.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        } else {
            entries.forEach { entry -> HubMealRow(entry.name, entry.type.title, entry.calories, NutritionGreen, true, onOpenDay) }
            suggestions.filter { suggestion ->
                entries.none { it.plannedMealKey == NutritionStore.plannedMealKey(day!!.date, suggestion) } &&
                    !NutritionStore.isPlannedMealDismissed(context, day!!.date, suggestion)
            }
                .forEach { meal -> HubMealRow(meal.title, meal.type.title, meal.targets.calories, NutritionCoral, false, onOpenDay) }
        }
    }
}

@Composable
private fun HubMealRow(title: String, mealType: String, calories: Int, tint: Color, logged: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.backgroundSecondary).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(tint.copy(alpha = .13f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(if (logged) Icons.Filled.Check else Icons.Filled.Restaurant, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 11.dp)) {
            Text(title, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            Text(if (logged) "REGISTRADA · ${mealType.uppercase()}" else mealType.uppercase(), style = MaterialTheme.typography.caption, color = if (logged) NutritionGreen else WildforceThemeTokens.textSecondary)
        }
        Text("$calories kcal", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
        Icon(Icons.Filled.ChevronRight, null, tint = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(start = 6.dp).size(18.dp))
    }
}

@Composable
private fun NutritionPlanHero(onGenerate: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NutritionOrange, NutritionCoral), start = Offset.Zero, end = Offset(600f, 600f))),
    ) {
        NutritionHeroParticles(Modifier.matchParentSize())
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("ACTIVA TU SEMANA", fontFamily = AntonFontFamily, fontSize = 30.sp, color = Color.White)
                    Text("Crea un plan día a día adaptado a tus entrenamientos, objetivos y preferencias.", color = Color.White.copy(alpha = .9f), modifier = Modifier.padding(top = 8.dp))
                }
                Box(Modifier.size(54.dp).background(Color.White.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Restaurant, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), shape = RoundedCornerShape(14.dp)) { Text("GENERAR PLAN", modifier = Modifier.padding(vertical = 5.dp)) }
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
    // Same three-week window as iOS: previous Monday through 20 days after the current Monday.
    val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val firstVisibleDate = minOf(plan.minOfOrNull { it.date } ?: today, currentMonday.minusDays(7))
    val lastVisibleDate = maxOf(plan.maxOfOrNull { it.date } ?: today, currentMonday.plusDays(20))
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
            val isSelected = isSelectable && date == selected
            val isPast = date.isBefore(today)
            // iOS gives the selected day a small spring response rather than a
            // hard color swap. Compose keeps that cue local to each day cell.
            val selectionScale by animateFloatAsState(
                targetValue = if (isSelected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = .7f, stiffness = 650f),
                label = "nutrition-calendar-${date}",
            )
            Column(
                Modifier.width(54.dp).height(84.dp).graphicsLayer { scaleX = selectionScale; scaleY = selectionScale }.clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .08f))
                    .border(1.dp, if (date == today && !isSelected) WildforceThemeTokens.accent.copy(alpha = .15f) else Color.Transparent, RoundedCornerShape(12.dp))
                    .alpha(if (!isSelectable) if (date == today) .72f else .55f else if (isPast) .9f else 1f)
                    .clickable(enabled = isSelectable) { onSelect(date) }.padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = if (isSelected) WildforceThemeTokens.primaryButtonText else if (date == today) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.h6, color = if (isSelected) WildforceThemeTokens.primaryButtonText else if (isSelectable) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary)
                val consumed = caloriesForDate(date)
                if (day != null && consumed > 0) {
                    CalendarCalorieRing(
                        progress = consumed.toFloat() / day.targets.calories.coerceAtLeast(1),
                        color = if (isSelected) WildforceThemeTokens.primaryButtonText else NutritionCoral,
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

@Composable
private fun CalendarCalorieRing(progress: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 2f),
        animationSpec = tween(durationMillis = 560),
        label = "calendar-calorie-ring",
    )
    Canvas(Modifier.padding(top = 1.dp).size(18.dp)) {
        drawArc(color.copy(alpha = .24f), -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * animatedProgress.coerceIn(0f, 1f), false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        if (animatedProgress > 1f) drawArc(color, -90f, 360f * (animatedProgress - 1f).coerceIn(0f, 1f), false, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun IntakeActions(date: LocalDate, today: LocalDate, onToday: () -> Unit, onLog: () -> Unit, onIdeas: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (date == today) "Ingesta de hoy" else "Ingesta del ${date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")))}", style = MaterialTheme.typography.subtitle1, color = WildforceThemeTokens.textPrimary)
        Spacer(Modifier.weight(1f))
        if (date != today) TextButton(onClick = onToday) { Text("Hoy", color = NutritionCoral) }
        else {
            IconButton(onClick = onLog, Modifier.semantics { contentDescription = "Registrar ingesta" }) { Icon(Icons.Filled.AddCircle, "Registrar ingesta", tint = NutritionCoral, modifier = Modifier.size(30.dp)) }
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
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).clickable(onClick = onClick).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(animatedCalories.toString(), fontFamily = Exo2FontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(" / ${target.calories} kcal", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress > 1f) Icon(Icons.Filled.WarningAmber, null, tint = NutritionOrange, modifier = Modifier.size(16.dp).padding(end = 3.dp))
                    Text(if (progress > 1f) "${current.calories - target.calories} kcal por encima" else "${target.calories - current.calories} kcal restantes", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
            ProgressRing(animatedProgress)
        }
        Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(verticalAlignment = Alignment.Bottom) { Text("${(progress * 100).toInt()}", fontFamily = Exo2FontFamily, fontSize = 22.sp, color = WildforceThemeTokens.textPrimary); Text("%", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
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
        Text("$animatedCurrent / $target g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) {
            Box(Modifier.fillMaxWidth(animatedFraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun WeeklyBalanceCard(plan: List<NutritionDayPlan>, selected: LocalDate, energyHistory: Map<LocalDate, NutritionEnergyPoint>) {
    val context = LocalContext.current.applicationContext
    // iOS exposes the previous 15 days, including both intake and the Health data.
    val dates = remember(selected) { (0L..14L).map { selected.minusDays(it) }.reversed() }
    val history = remember(selected) { dates.map { date -> NutritionStore.load(context, date).sumOf { it.calories } } }
    var inspectedDate by remember(selected) { mutableStateOf<LocalDate?>(null) }
    if (energyHistory.isEmpty()) return
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("BALANCE ENERGÉTICO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary); Text(inspectedDate?.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")))?.uppercase() ?: "ÚLTIMOS 15 DÍAS", style = MaterialTheme.typography.caption, color = NutritionCoral) }
        val historicalIndexes = dates.indices.filter { dates[it] != LocalDate.now() }
        val averageEaten = historicalIndexes.map { history[it] }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val burnedValues = historicalIndexes.mapNotNull { energyHistory[dates[it]]?.totalCalories }
        val averageBurned = burnedValues.average().takeIf { !it.isNaN() }?.toInt()
        val inspectedIndex = inspectedDate?.let(dates::indexOf)?.takeIf { it >= 0 }
        val displayedEaten = inspectedIndex?.let { history[it] } ?: averageEaten
        val displayedBurned = inspectedIndex?.let { index -> energyHistory[dates[index]]?.totalCalories?.toInt() } ?: averageBurned
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BalanceMetric(if (inspectedIndex == null) "MEDIA INGESTA" else "INGERIDA", if (displayedEaten > 0) "$displayedEaten kcal" else "—", NutritionCoral)
            BalanceMetric(if (inspectedIndex == null) "MEDIA QUEMADA" else "QUEMADA", displayedBurned?.let { "$it kcal" } ?: "—", NutritionRed)
            BalanceMetric("BALANCE", displayedBurned?.let { "${if (displayedEaten - it > 0) "+" else ""}${displayedEaten - it} kcal" } ?: "—", if ((displayedBurned ?: displayedEaten) > displayedEaten) NutritionGreen else NutritionOrange)
        }
        val chartBurnedValues = dates.mapNotNull { energyHistory[it]?.totalCalories ?: energyHistory[it]?.activeCalories }.map { it.toInt() }
        val maximum = (history + chartBurnedValues).maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            dates.forEachIndexed { index, date ->
                val eaten = history[index]
                val point = energyHistory[date]
                val burned = (point?.totalCalories ?: point?.activeCalories ?: 0.0).toInt()
                val active = point?.activeCalories?.toInt()?.coerceAtMost(burned) ?: 0
                val animatedBurnedHeight by animateDpAsState(
                    targetValue = (118 * burned / maximum).dp,
                    animationSpec = tween(420),
                    label = "energy-balance-bar-$date",
                )
                Box(Modifier.weight(1f).fillMaxHeight().clickable { inspectedDate = if (inspectedDate == date) null else date }, contentAlignment = Alignment.BottomCenter) {
                    if (burned > 0) Column(Modifier.fillMaxWidth(.62f).height(animatedBurnedHeight), verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.fillMaxWidth().weight((burned - active).coerceAtLeast(1).toFloat()).background(NutritionRed.copy(alpha = .5f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        if (active > 0) Box(Modifier.fillMaxWidth().weight(active.toFloat()).background(NutritionRed, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    }
                    if (eaten > 0) Box(Modifier.fillMaxWidth(.9f).height(3.dp).offset(y = -((118 * eaten / maximum).dp)).background(WildforceThemeTokens.textPrimary, CircleShape))
                    if (inspectedDate == date) Box(Modifier.fillMaxWidth().fillMaxHeight().border(1.dp, NutritionCoral, RoundedCornerShape(6.dp)))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dates.filterIndexed { index, _ -> index % 4 == 0 || index == dates.lastIndex }.forEach { date ->
                Text(date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es"))), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { BalanceLegend(NutritionRed.copy(alpha = .5f), "Base"); BalanceLegend(NutritionRed, "Activa"); BalanceLegend(WildforceThemeTokens.textPrimary, "Ingerida") }
    }
}

@Composable private fun BalanceMetric(label: String, value: String, color: Color) { Column { Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text(value, fontWeight = FontWeight.Bold, color = color) } }
@Composable private fun BalanceLegend(color: Color, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).background(color, CircleShape)); Text(label, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }

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
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
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
    if (showNotes && notes != null) AlertDialog(onDismissRequest = { showNotes = false }, title = { Text("NOTAS DEL PLAN", fontFamily = AntonFontFamily) }, text = { Text(notes, color = WildforceThemeTokens.textSecondary) }, confirmButton = { TextButton(onClick = { showNotes = false }) { Text("CERRAR", color = NutritionCoral) } })
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
    val dayTypeColor = when (day.type) { NutritionDayType.Training -> NutritionOrange; NutritionDayType.Rest -> CarbsColor; NutritionDayType.Recovery -> NutritionGreen }
    var typeBadgeAppeared by remember(day.date, day.type) { mutableStateOf(false) }
    LaunchedEffect(day.date, day.type) { typeBadgeAppeared = true }
    val typeBadgeScale by animateFloatAsState(
        targetValue = if (typeBadgeAppeared) 1f else .8f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 560f),
        label = "nutrition-day-type-${day.date}",
    )
    Column(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradient)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Column { Text(day.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("es"))), style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = .82f)); Text(if (isToday) "HOY" else day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).uppercase(), fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Text(day.type.title.uppercase(), style = MaterialTheme.typography.caption, color = Color.White, modifier = Modifier.graphicsLayer { scaleX = typeBadgeScale; scaleY = typeBadgeScale; alpha = if (typeBadgeAppeared) 1f else 0f }.background(dayTypeColor.copy(alpha = .62f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp)); Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(NutritionMacroIcon.Calories, Color.White, Modifier.size(15.dp)); Text("${day.targets.calories} kcal", Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold, color = Color.White) } }
        }
        Row(Modifier.background(Color.White.copy(alpha = .12f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.FitnessCenter, null, tint = Color.White, modifier = Modifier.size(14.dp)); Text(day.workoutTitle.uppercase(), Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White) }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HeaderMacroPill(NutritionMacroIcon.Protein, "${logged.protein}/${day.targets.protein} g"); HeaderMacroPill(NutritionMacroIcon.Carbs, "${logged.carbs}/${day.targets.carbs} g"); HeaderMacroPill(NutritionMacroIcon.Fat, "${logged.fat}/${day.targets.fat} g"); HeaderPill(Icons.Filled.Bolt, day.energyDemand.title)
        }
    }
}

@Composable private fun HeaderPill(icon: ImageVector, text: String) { Row(Modifier.background(Color.White.copy(alpha = .10f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp)); Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White) } }
@Composable private fun HeaderMacroPill(icon: NutritionMacroIcon, text: String) { Row(Modifier.background(Color.White.copy(alpha = .10f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(icon, Color.White, Modifier.size(13.dp)); Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color.White) } }
private fun demandColor(demand: EnergyDemand) = when (demand) { EnergyDemand.High -> NutritionRed; EnergyDemand.Medium -> NutritionOrange; EnergyDemand.Low -> NutritionGreen }
internal fun List<MealLog>.total() = NutritionTargets(sumOf { it.calories }, sumOf { it.protein }, sumOf { it.carbs }, sumOf { it.fat })
