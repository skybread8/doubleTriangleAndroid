package io.codepassion.doubletriangle.nutrition

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
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
    var planVersion by remember { mutableStateOf(NutritionStore.loadPlanVersion(context)) }
    val fallbackPlan = remember(profile, targets, preferences, today, planVersion) { NutritionStore.weekPlan(profile, targets, today, preferences, planVersion) }
    var generatedPlan by remember(profile) { mutableStateOf(NutritionStore.loadGeneratedPlan(context)) }
    val plan = generatedPlan ?: fallbackPlan
    var meals by remember(selectedDate) { mutableStateOf(NutritionStore.load(context, selectedDate)) }
    var showLogFlow by remember { mutableStateOf(false) }
    var showEntries by remember { mutableStateOf(false) }
    var detailDay by remember { mutableStateOf<NutritionDayPlan?>(null) }
    var showTargets by remember { mutableStateOf(false) }
    var showPreferences by remember { mutableStateOf(false) }
    var showIdeas by remember { mutableStateOf(false) }
    var fridgeTarget by remember { mutableStateOf<NutritionTargets?>(null) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showGrocery by remember { mutableStateOf(false) }
    var showFulfill by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<MealLog?>(null) }
    var plannedMethod by remember { mutableStateOf<LogMethod?>(null) }
    var plannedTitle by remember { mutableStateOf<String?>(null) }
    var generatingPlan by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
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

    fun requestPlanGeneration() {
        generationError = null
        generatingPlan = true
        scope.launch {
            runCatching { NutritionAIPlanner.generate(context, profile, targets, preferences, today) }
                .onSuccess { generatedPlan = it; planVersion = NutritionStore.regeneratePlan(context) }
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
                if (method == LogMethod.Describe) {
                    fridgeTarget = meal.targets
                    showIdeas = true
                } else {
                    plannedMethod = method
                    plannedTitle = meal.title
                    showLogFlow = true
                }
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
            onChanged = { updated -> NutritionStore.save(context, selectedDate, updated); meals = updated },
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
            onDismiss = { showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null },
            onSave = { entry ->
                val normalized = entry.copy(loggedDate = selectedDate)
                val dateEntries = if (editingMeal == null) NutritionStore.load(context, selectedDate) + normalized else NutritionStore.load(context, selectedDate).map { if (it.id == editingMeal!!.id) normalized.copy(id = it.id) else it }
                NutritionStore.save(context, selectedDate, dateEntries)
                scope.launch { NutritionStore.load(context, selectedDate).let { HealthConnectNutritionSync.publish(context, it) } }
                showLogFlow = false; editingMeal = null; plannedMethod = null; plannedTitle = null
                reload()
            },
        )
    }
    if (showTargets) NutritionTargetsDialog(targets, { showTargets = false }) { value ->
        targets = value; NutritionStore.saveTargets(context, value); NutritionStore.clearGeneratedPlan(context); generatedPlan = null; showTargets = false
    }
    if (showPreferences) NutritionPreferencesDialog(preferences, { showPreferences = false }) { value ->
        preferences = value; NutritionStore.savePreferences(context, value); NutritionStore.clearGeneratedPlan(context); generatedPlan = null; showPreferences = false
    }
    if (showIdeas) MealIdeasDialog(plan.firstOrNull { it.date == selectedDate }, fridgeTarget, { showIdeas = false; fridgeTarget = null }) { planned ->
        val entry = MealLog(System.currentTimeMillis(), planned.title, planned.targets.calories, planned.targets.protein, planned.targets.carbs, planned.targets.fat, planned.type, selectedDate, planned.guidance, "meal_idea")
        val updated = NutritionStore.load(context, selectedDate) + entry
        NutritionStore.save(context, selectedDate, updated); reload(); showIdeas = false; fridgeTarget = null
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
    if (generatingPlan) AlertDialog(onDismissRequest = {}, title = { Text("DISEÑANDO TU SEMANA", fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(color = NutritionCoral); Text("La IA está adaptando calorías, macros, entrenamientos y preferencias alimentarias.", color = WildforceThemeTokens.textSecondary) } }, confirmButton = {})
    generationError?.let { message -> AlertDialog(onDismissRequest = { generationError = null }, title = { Text("NO SE PUDO GENERAR EL PLAN", fontFamily = AntonFontFamily) }, text = { Text(message, color = WildforceThemeTokens.textSecondary) }, confirmButton = { TextButton(onClick = { generationError = null; showMoreActions = true }) { Text("REINTENTAR", color = NutritionCoral) } }, dismissButton = { TextButton(onClick = { generationError = null }) { Text("CERRAR") } }) }
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
    var historyExpanded by remember { mutableStateOf(false) }
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
            item { WeeklyBalanceCard(plan, selectedDate, energyHistory) }
            item { PlanHeader(plan, preferences, onEditPlan, onEditPreferences, onPlanGrocery, onPlanRegenerate) }
            items(plan.filter { !it.date.isBefore(today) }.take(7), key = { it.date }) { day ->
                NutritionDayHeader(day, NutritionStore.load(context, day.date).total(), day.date == today) { onDay(day) }
            }
            val history = plan.filter { it.date.isBefore(today) }.sortedByDescending { it.date }
            if (history.isNotEmpty()) {
                item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("HISTORIAL", Modifier.weight(1f), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary); if (history.size > 3) TextButton(onClick = { historyExpanded = !historyExpanded }) { Text(if (historyExpanded) "OCULTAR" else "VER TODO", color = NutritionCoral) } } }
                items(if (historyExpanded) history else history.take(3), key = { it.date }) { day ->
                    NutritionDayHeader(day, NutritionStore.load(context, day.date).total(), false) { onDay(day) }
                }
            }
        }
    }
}

@Composable
private fun NutritionPlanHero(onGenerate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NutritionOrange, NutritionCoral), start = Offset.Zero, end = Offset(600f, 600f)))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

@Composable
private fun NutritionCalendar(
    plan: List<NutritionDayPlan>,
    selected: LocalDate,
    today: LocalDate,
    caloriesForDate: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
) {
    val planDates = plan.associateBy { it.date }
    val firstVisibleDate = minOf(plan.minOfOrNull { it.date } ?: today, today.minusDays(7))
    val lastVisibleDate = maxOf(plan.maxOfOrNull { it.date } ?: today, today.plusDays(14))
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
            Column(
                Modifier.width(54.dp).height(84.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .08f))
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
    Canvas(Modifier.padding(top = 1.dp).size(18.dp)) {
        drawArc(color.copy(alpha = .24f), -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        if (progress > 1f) drawArc(color, -90f, 360f * (progress - 1f).coerceIn(0f, 1f), false, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
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
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).clickable(onClick = onClick).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(current.calories.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
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
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        val track = WildforceThemeTokens.textSecondary.copy(alpha = .14f)
        Canvas(Modifier.fillMaxSize()) {
            drawArc(track, -90f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawArc(NutritionCoral, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            if (progress > 1f) drawArc(NutritionCoral, -90f, 360f * (progress - 1f).coerceIn(0f, 1f), false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(verticalAlignment = Alignment.Bottom) { Text("${(progress * 100).toInt()}", fontSize = 22.sp, color = WildforceThemeTokens.textPrimary); Text("%", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
private fun MacroProgress(title: String, current: Int, target: Int, color: Color, icon: NutritionMacroIcon, modifier: Modifier) {
    val fraction = if (target > 0) current.toFloat() / target else 0f
    val animatedFraction by animateFloatAsState(fraction, animationSpec = tween(650), label = "$title progress")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NutritionMacroSymbol(icon, tint = color, modifier = Modifier.size(15.dp))
            Text(title, Modifier.padding(start = 5.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        }
        Text("$current / $target g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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
                Box(Modifier.weight(1f).fillMaxHeight().clickable { inspectedDate = if (inspectedDate == date) null else date }, contentAlignment = Alignment.BottomCenter) {
                    if (burned > 0) Column(Modifier.fillMaxWidth(.62f).height((118 * burned / maximum).dp), verticalArrangement = Arrangement.Bottom) {
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
    Column(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradient)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Column { Text(day.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("es"))), style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = .82f)); Text(if (isToday) "HOY" else day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Text(day.type.title.uppercase(), style = MaterialTheme.typography.caption, color = Color.White, modifier = Modifier.background(Color.White.copy(alpha = .14f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp)); Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(NutritionMacroIcon.Calories, Color.White, Modifier.size(15.dp)); Text("${day.targets.calories} kcal", Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold, color = Color.White) } }
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
