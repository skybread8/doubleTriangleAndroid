package io.codepassion.doubletriangle.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.painterResource
import io.codepassion.wildforce.android.R

/** Full-screen review before replacing the active weekly plan, matching the iOS save step. */
@Composable
internal fun NutritionPlanPreviewDialog(
    plan: List<NutritionDayPlan>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = WildforceThemeTokens.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d87b542b930fc289), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d19904bc72fb8c70), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar", tint = WildforceThemeTokens.textPrimary) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(plan, key = { it.date }) { day ->
                        Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE d MMM", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])).replaceFirstChar { it.uppercase() }, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_84744cf2ee7e93cc, day.targets.calories), fontWeight = FontWeight.Bold, color = NutritionCoral)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MealMacro(NutritionMacroIcon.Protein, "${day.targets.protein}g", ProteinColor)
                                MealMacro(NutritionMacroIcon.Carbs, "${day.targets.carbs}g", CarbsColor)
                                MealMacro(NutritionMacroIcon.Fat, "${day.targets.fat}g", FatColor)
                            }
                            if (day.meals.isNotEmpty()) Text(day.meals.joinToString(" · ") { it.title }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            if (day.notes.isNotBlank()) Text(day.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.discard).uppercase()) }
                    Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.save_plan).uppercase(), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
                }
            }
        }
    }
}

@Composable
internal fun NutritionDayDetail(
    day: NutritionDayPlan,
    entries: List<MealLog>,
    onBack: () -> Unit,
    onLog: () -> Unit,
    onFulfill: () -> Unit = {},
    onGrocery: () -> Unit = {},
    hasMealSuggestions: Boolean = day.meals.isNotEmpty(),
    onPlannedMealAction: (PlannedMeal, LogMethod) -> Unit = { _, _ -> onLog() },
    onDiscardPlannedMeal: (PlannedMeal) -> Unit = {},
    isSuggestionHidden: (PlannedMeal) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val canLog = canLogNutritionDate(day.date, LocalDate.now())
    var showActions by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .liquidGlassBackground()
            // Keep the page background full-height beneath the floating
            // Resume/navigation overlay; the list supplies trailing space.
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }
            Text(day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).replaceFirstChar { it.uppercase() }, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Box {
                IconButton(onClick = { showActions = true }) { Icon(Icons.Filled.MoreVert, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_803e7245f990473b), tint = WildforceThemeTokens.textSecondary) }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(onClick = { showActions = false; onFulfill() }) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = NutritionCoral, modifier = Modifier.size(18.dp))
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.fulfill_missing_macros), Modifier.padding(start = 10.dp))
                    }
                    if (hasMealSuggestions) DropdownMenuItem(onClick = { showActions = false; onGrocery() }) {
                        Icon(Icons.Filled.ShoppingBasket, null, tint = NutritionCoral, modifier = Modifier.size(18.dp))
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.grocery_list), Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { NutritionDayHeader(day, entries.total(), day.date == LocalDate.now()) {} }
            item { MacroSummary(day.targets) }
            val workoutTitle = day.safeWorkoutTitle()
            // iOS only presents WorkoutFuelingSection when the day has a
            // planned workout or before/after-training guidance. General day
            // notes alone do not create that section in NutritionDayView.
            if (hasNutritionWorkoutFuelingContent(workoutTitle, day.preWorkoutGuidance, day.postWorkoutGuidance)) {
                item {
                    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (day.notes.isNotBlank()) {
                            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.notes), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            Text(day.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.workout_fueling), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                        if (workoutTitle.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FitnessCenter, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp))
                                Text(workoutTitle, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                            }
                        }
                        day.preWorkoutGuidance?.let { Guidance("Antes de entrenar", it, NutritionOrange) }
                        day.postWorkoutGuidance?.let { Guidance("Después de entrenar", it, NutritionGreen) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // iOS uses a regular headline titled "Meals" here; the
                    // day detail is not grouped by meal type or rendered as
                    // an Anton section banner.
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.meals), Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                    if (canLog) {
                        IconButton(onClick = onLog) {
                            Icon(Icons.Filled.Add, "Registrar ingesta", tint = NutritionCoral)
                        }
                    }
                }
            }
            val visibleMeals = day.meals.filterNot(isSuggestionHidden)
            if (visibleMeals.isEmpty()) {
                // iOS does not present an empty-action prompt here. A day
                // without planned meals is intentionally a targets-only day.
                item {
                    Text(
                        androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_day_focuses_on_calorie_and_macro_targets_only_),
                        style = MaterialTheme.typography.caption,
                        color = WildforceThemeTokens.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    )
                }
            }
            items(visibleMeals, key = { "planned-${it.title}-${it.type}" }) { meal ->
                PlannedMealCard(meal, canLog, onPlannedMealAction, onDiscardPlannedMeal, showDiscard = false)
            }
        }
    }
}

internal fun canLogNutritionDate(date: LocalDate, today: LocalDate): Boolean = !date.isAfter(today)

internal fun hasNutritionWorkoutFuelingContent(workoutTitle: String, preWorkoutGuidance: String?, postWorkoutGuidance: String?): Boolean =
    workoutTitle.isNotBlank() || !preWorkoutGuidance.isNullOrBlank() || !postWorkoutGuidance.isNullOrBlank()

@Composable private fun TopBar(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }; Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Filled.AddCircle, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.log), tint = NutritionCoral) } else Spacer(Modifier.width(48.dp)) } }

@Composable private fun MacroSummary(target: NutritionTargets) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedMacroBadge("Calorías", target.calories, "kcal", NutritionCaloriesColor, NutritionMacroIcon.Calories, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { AnimatedMacroBadge("Proteína", target.protein, "g", ProteinColor, NutritionMacroIcon.Protein, Modifier.weight(1f)); AnimatedMacroBadge("Carbos", target.carbs, "g", CarbsColor, NutritionMacroIcon.Carbs, Modifier.weight(1f)); AnimatedMacroBadge("Grasa", target.fat, "g", FatColor, NutritionMacroIcon.Fat, Modifier.weight(1f)) }
    }
}

@Composable
private fun AnimatedMacroBadge(label: String, target: Int, unit: String, color: Color, icon: NutritionMacroIcon, modifier: Modifier = Modifier) {
    var started by remember(target) { mutableStateOf(false) }
    LaunchedEffect(target) {
        delay(400)
        started = true
    }
    val value by animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(durationMillis = 520),
        label = "nutrition-day-macro-$label",
    )
    MacroBadge(label, value, unit, color, icon, modifier, animationKey = target)
}

@Composable
private fun MacroBadge(label: String, value: Int, unit: String, color: Color, icon: NutritionMacroIcon, modifier: Modifier, animationKey: Int = value) {
    val iconScale = remember(animationKey) { Animatable(.78f) }
    LaunchedEffect(animationKey) { iconScale.animateTo(1f, spring(dampingRatio = .52f, stiffness = 560f)) }
    Row(
        modifier
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(16.dp))
            .border(1.dp, WildforceThemeTokens.textSecondary.copy(alpha = .1f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            NutritionMacroSymbol(icon, color, Modifier.size(20.dp).graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value })
        }
        Column(Modifier.padding(start = 7.dp)) {
            Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Text("$value $unit", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        }
    }
}

@Composable private fun Guidance(title: String, body: String, color: Color) { Column(Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = color); Text(body, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }

@Composable private fun PlannedMealCard(meal: PlannedMeal, canLog: Boolean, onAction: (PlannedMeal, LogMethod) -> Unit, onDiscard: (PlannedMeal) -> Unit, showDiscard: Boolean = true) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) { Text(meal.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(mealTypeLabel(meal.type), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_84744cf2ee7e93cc, meal.targets.calories), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
            if (canLog) {
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.more_options), tint = WildforceThemeTokens.textSecondary) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MealMenuItem(Icons.Filled.CheckCircle, "Registrar esta comida") { menuOpen = false; onAction(meal, LogMethod.Manual) }
                        MealMenuItem(Icons.Filled.Kitchen, "Obtener ideas de comida") { menuOpen = false; onAction(meal, LogMethod.Describe) }
                        MealMenuItem(Icons.Filled.CameraAlt, "Analizar foto") { menuOpen = false; onAction(meal, LogMethod.Photo) }
                        MealMenuItem(Icons.Filled.QrCodeScanner, "Escanear código de barras") { menuOpen = false; onAction(meal, LogMethod.Barcode) }
                        if (showDiscard) {
                            Divider()
                            MealMenuItem(Icons.Filled.DeleteOutline, "Descartar sugerencia") { menuOpen = false; onDiscard(meal) }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Protein, "${meal.targets.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${meal.targets.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${meal.targets.fat}g", FatColor) }
        Text(meal.guidance, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { meal.foods.forEach { (food, grams) -> Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f607a4511eaa0bad, food, grams), Modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) } }
    }
}

/** Matches iOS's combined day timeline: registered food and plan suggestions share one meal-type section. */
@Composable
private fun LoggedMealDayCard(entry: MealLog) {
    val photo = remember(entry.photoPath) { entry.photoPath?.let(BitmapFactory::decodeFile) }
    val remoteImage = rememberNutritionRemoteImage(entry.mediaUrl, photo == null)
    Column(
        Modifier.fillMaxWidth()
            .background(NutritionGreen.copy(alpha = .08f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) Image(photo.asImageBitmap(), "Foto de ${entry.name}", Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)), contentScale = ContentScale.Crop)
            else if (remoteImage != null) Image(remoteImage.asImageBitmap(), "Imagen de ${entry.name}", Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(42.dp).background(NutritionGreen.copy(alpha = .15f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, "Registrada", tint = NutritionGreen) }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(entry.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_2e2d59534a6894d1, entry.loggedCalories().roundToInt()), style = MaterialTheme.typography.caption, color = NutritionGreen)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MealMacro(NutritionMacroIcon.Protein, "${entry.loggedProtein().roundToInt()}g", ProteinColor)
            MealMacro(NutritionMacroIcon.Carbs, "${entry.loggedCarbs().roundToInt()}g", CarbsColor)
            MealMacro(NutritionMacroIcon.Fat, "${entry.loggedFat().roundToInt()}g", FatColor)
        }
        if (entry.items.size > 1) Text(entry.items.joinToString(" · ") { it.name }, maxLines = 1, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable private fun MealMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) { DropdownMenuItem(onClick = onClick) { Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text(text, Modifier.padding(start = 12.dp)) } }
@Composable internal fun MealMacro(icon: NutritionMacroIcon, value: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(icon, color, Modifier.size(14.dp)); Text(value, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) } }

@Composable
internal fun NutritionEntriesScreen(
    date: LocalDate,
    target: NutritionTargets,
    entries: List<MealLog>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (MealLog) -> Unit,
    onChanged: (List<MealLog>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleting by remember { mutableStateOf<MealLog?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .liquidGlassBackground()
            // Keep the final intake entries above the floating Resume
            // accessory and root navigation, matching the iOS tab safe area.
            .padding(bottom = 180.dp),
    ) {
        TopBar("Ingesta", onBack, onAdd)
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary) }
            item { EntrySummary(entries.total(), target) }
            if (entries.isEmpty()) item { EmptyState("Aún no hay registros", "Usa el botón + para añadir tu primera comida del día.") }
            MealType.entries.forEach { type ->
                val group = entries.filter { it.type == type }
                if (group.isNotEmpty()) {
                    item { Text(mealTypeLabel(type).uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) }
                    items(group, key = { it.id }) { entry ->
                        EntryCard(entry, onBookmark = { name -> onChanged(entries.map { if (it.id == entry.id) applyNutritionBookmark(it, name) else it }) }, onDelete = { deleting = entry }, onEdit = { onEdit(entry) })
                    }
                }
            }
        }
    }
    deleting?.let { entry -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_c564319129b0cdb7)) }, text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_09c91686cd8d5922, entry.name)) }, confirmButton = { TextButton(onClick = { onChanged(entries.filterNot { it.id == entry.id }); deleting = null }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.delete).uppercase(), color = NutritionRed) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } }) }
}

@Composable private fun EntrySummary(current: NutritionTargets, target: NutritionTargets) { Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), emphasized = true).padding(16.dp)) { Row(verticalAlignment = Alignment.Bottom) { NutritionMacroSymbol(NutritionMacroIcon.Calories, NutritionCaloriesColor, Modifier.size(20.dp)); Text(current.calories.toString(), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(" / ${target.calories} kcal", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(bottom = 5.dp)) }; Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { MealMacro(NutritionMacroIcon.Protein, "${current.protein}/${target.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${current.carbs}/${target.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${current.fat}/${target.fat}g", FatColor) } } }

@Composable
private fun EntryCard(entry: MealLog, onBookmark: (String?) -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val photo = remember(entry.photoPath) { entry.photoPath?.let(BitmapFactory::decodeFile) }
    val remoteImage = rememberNutritionRemoteImage(entry.mediaUrl, photo == null)
    val loggedTime = remember(entry.id) {
        Instant.ofEpochMilli(entry.id).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    var expanded by remember(entry.id) { mutableStateOf(false) }
    var confirmRemoveBookmark by remember(entry.id) { mutableStateOf(false) }
    var showBookmarkName by remember(entry.id) { mutableStateOf(false) }
    var bookmarkName by remember(entry.id) { mutableStateOf(entry.bookmarkName.ifBlank { entry.name }) }
    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable(onClick = onEdit).padding(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            if (photo != null) Image(photo.asImageBitmap(), "Foto de ${entry.name}", Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            else if (remoteImage != null) Image(remoteImage.asImageBitmap(), "Imagen de ${entry.name}", Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            else NutritionMealPlaceholder(entry)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                // iOS uses the general accent for the timestamp badge, not
                // AccentColor2 (the nutrition/gold emphasis color).
                Text(loggedTime, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 3.dp).background(WildforceThemeTokens.accent.copy(alpha = .10f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_84744cf2ee7e93cc, entry.calories), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${entry.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${entry.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${entry.fat}g", FatColor) }
            }
            IconButton(onClick = { if (entry.isBookmarked) confirmRemoveBookmark = true else showBookmarkName = true }) { Icon(if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Favorito", tint = if (entry.isBookmarked) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Eliminar", tint = WildforceThemeTokens.textSecondary) }
        }
        if (entry.items.size > 1) {
            Row(
                modifier = Modifier.padding(top = 7.dp).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // iOS exposes only the disclosure chevron; the item count is
                // Android-only text and changes the card hierarchy.
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (expanded) "Contraer alimentos" else "Expandir alimentos", tint = WildforceThemeTokens.textSecondary)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
            ) {
                Column {
                    entry.items.forEach { food -> Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_b3122afa77bde834, food.name, food.calories.roundToInt()), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            }
        }
        if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 7.dp))
    }
    if (confirmRemoveBookmark) {
        AlertDialog(
            onDismissRequest = { confirmRemoveBookmark = false },
            title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.remove_bookmark_)) },
            text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_meal_will_no_longer_appear_in_your_bookmarks_)) },
            confirmButton = {
                TextButton(onClick = { confirmRemoveBookmark = false; onBookmark(null) }) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.remove).uppercase(), color = NutritionRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveBookmark = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } },
        )
    }
    if (showBookmarkName) {
        AlertDialog(
            onDismissRequest = { showBookmarkName = false },
            title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_fc0eb916b51fbc32)) },
            text = { OutlinedTextField(bookmarkName, { bookmarkName = it.take(80) }, label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.name)) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { showBookmarkName = false; onBookmark(bookmarkName.trim().ifBlank { entry.name }) }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.save).uppercase(), color = NutritionCoral) } },
            dismissButton = { TextButton(onClick = { showBookmarkName = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } },
        )
    }
}

/** Visual counterpart of iOS NutritionLogEntryThumbnailView's fallback. */
@Composable
internal fun NutritionMealPlaceholder(entry: MealLog) {
    val tint = mealTypeTint(entry.type)
    Box(
        Modifier
            .size(88.dp)
            .background(
                Brush.linearGradient(listOf(tint.copy(alpha = .95f), tint.copy(alpha = .58f))),
                RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(mealTypeIcon(entry.type), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White.copy(alpha = .95f))
            entry.name.trim().firstOrNull()?.let { initial ->
                Text(initial.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .92f))
            }
        }
    }
}

@Composable
internal fun rememberNutritionRemoteImage(url: String?, enabled: Boolean): Bitmap? {
    val image by produceState<Bitmap?>(initialValue = null, key1 = url, key2 = enabled) {
        value = if (enabled && !url.isNullOrBlank()) {
            withContext(Dispatchers.IO) { runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull() }
        } else null
    }
    return image
}

@Composable
internal fun NutritionTargetsDialog(initial: NutritionTargets, onDismiss: () -> Unit, onSave: (NutritionTargets) -> Unit) {
    var calories by remember { mutableStateOf(initial.calories.toString()) }; var protein by remember { mutableStateOf(initial.protein.toString()) }; var carbs by remember { mutableStateOf(initial.carbs.toString()) }; var fat by remember { mutableStateOf(initial.fat.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_960b584841df4abf), fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Calorías", calories, { calories = it.filter(Char::isDigit) }, Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Proteína", protein, { protein = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Carbos", carbs, { carbs = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Grasa", fat, { fat = it.filter(Char::isDigit) }, Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { onSave(NutritionTargets(calories.toIntOrNull()?.coerceIn(500, 8000) ?: initial.calories, protein.toIntOrNull()?.coerceIn(0, 500) ?: initial.protein, carbs.toIntOrNull()?.coerceIn(0, 1000) ?: initial.carbs, fat.toIntOrNull()?.coerceIn(0, 500) ?: initial.fat)) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.save).uppercase(), fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } })
}

@Composable
internal fun NutritionPreferencesDialog(
    initial: NutritionPreferences,
    onDismiss: () -> Unit,
    isInitialSetup: Boolean = false,
    isHealthConnectEnabled: Boolean = false,
    onRequestHealthConnect: HealthConnectRequest = { result -> result(false, null, null, null, null) },
    onSave: (NutritionPreferences) -> Unit,
) {
    if (isInitialSetup) {
        NutritionOnboardingDialog(
            initial = initial,
            isHealthConnectEnabled = isHealthConnectEnabled,
            onRequestHealthConnect = onRequestHealthConnect,
            onDismiss = onDismiss,
            onSave = onSave,
        )
        return
    }
    var style by remember { mutableStateOf(initial.dietaryStyle) }
    var meals by remember { mutableStateOf(initial.mealsPerDay) }
    var suggestions by remember { mutableStateOf(initial.wantsSuggestions) }
    var excludedFoods by remember { mutableStateOf(initial.excludedFoods.joinToString(", ")) }
    var allergies by remember { mutableStateOf(initial.allergiesAndIntolerances.joinToString(", ")) }
    var proteinSources by remember { mutableStateOf(initial.preferredProteinSources.joinToString(", ")) }
    var dislikes by remember { mutableStateOf(initial.dislikes.joinToString(", ")) }
    var notes by remember { mutableStateOf(initial.notes) }
    var startHour by remember { mutableStateOf(initial.eatingWindowStartHour?.toString().orEmpty()) }
    var endHour by remember { mutableStateOf(initial.eatingWindowEndHour?.toString().orEmpty()) }
    var cookingEffort by remember { mutableStateOf(initial.cookingEffort) }
    var budgetSensitivity by remember { mutableStateOf(initial.budgetSensitivity) }
    // These are exactly the four dietary styles offered by iOS.  "Omnívora"
    // is Android's localized presentation of iOS's standard style.
    val styles = listOf("Omnívora", "Vegetariana", "Vegana", "Pescetariana")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.nutrition_profile).uppercase(), fontFamily = AntonFontFamily) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.planning).uppercase(), fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) }
                item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_2f0b47dc21305702), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { styles.forEach { item -> NutritionOptionChip(item, style == item) { style = item } } } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.meals_per_day), Modifier.weight(1f)); IconButton(onClick = { meals = (meals - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.Remove, "Reducir comidas al día") }; Text(meals.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { meals = (meals + 1).coerceAtMost(10) }) { Icon(Icons.Filled.Add, "Aumentar comidas al día") } } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Inicio (0–23)", startHour, { startHour = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f)); NumberField("Fin (0–23)", endHour, { endHour = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f)) } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.meal_suggestions), Modifier.weight(1f)); Switch(suggestions, { suggestions = it }, colors = SwitchDefaults.colors(checkedThumbColor = WildforceThemeTokens.textPrimary, checkedTrackColor = WildforceThemeTokens.textPrimary)) } }
                item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.preferences).uppercase(), fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp)) }
                item { PreferenceChoice("Esfuerzo al cocinar", cookingEffort, listOf("Bajo", "Medio", "Alto")) { cookingEffort = it } }
                item { PreferenceChoice("Sensibilidad al presupuesto", budgetSensitivity, listOf("Baja", "Media", "Alta")) { budgetSensitivity = it } }
                item { OutlinedTextField(proteinSources, { proteinSources = it }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.protein_sources)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_51abb8184a95f0c0)) }, maxLines = 3) }
                item { OutlinedTextField(dislikes, { dislikes = it }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_0218e37495d4775d)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_966462e99a10529e)) }, maxLines = 3) }
                item { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.restrictions).uppercase(), fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp)) }
                item { OutlinedTextField(excludedFoods, { excludedFoods = it }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.excluded_foods)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d0d81f2dcf960609)) }, maxLines = 3) }
                item { OutlinedTextField(allergies, { allergies = it }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.allergies_or_intolerances)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.e_g__peanuts__lactose)) }, maxLines = 3) }
                item { OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.notes)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_cedac6b4faab8beb)) }, minLines = 3) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(initial.copy(
                    dietaryStyle = style,
                    mealsPerDay = meals,
                    wantsSuggestions = suggestions,
                    eatingWindowStartHour = startHour.toIntOrNull()?.takeIf { it in 0..23 },
                    eatingWindowEndHour = endHour.toIntOrNull()?.takeIf { it in 0..23 },
                    cookingEffort = cookingEffort,
                    budgetSensitivity = budgetSensitivity,
                    preferredProteinSources = proteinSources.toNutritionList(),
                    dislikes = dislikes.toNutritionList(),
                    excludedFoods = excludedFoods.toNutritionList(),
                    allergiesAndIntolerances = allergies.toNutritionList(),
                    notes = notes.trim(),
                ))
            }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.save).uppercase(), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } },
    )
}

/** Mirrors iOS's first-run nutrition setup instead of dropping a new user into the long edit form. */
@Composable
private fun NutritionOnboardingDialog(
    initial: NutritionPreferences,
    isHealthConnectEnabled: Boolean,
    onRequestHealthConnect: HealthConnectRequest,
    onDismiss: () -> Unit,
    onSave: (NutritionPreferences) -> Unit,
) {
    var intro by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    var style by remember { mutableStateOf(initial.dietaryStyle) }
    // iOS InlineStepper permits 1–10 meals per day.
    var meals by remember { mutableIntStateOf(initial.mealsPerDay.coerceIn(1, 10)) }
    var excluded by remember { mutableStateOf(initial.excludedFoods.joinToString(", ")) }
    var allergies by remember { mutableStateOf(initial.allergiesAndIntolerances.joinToString(", ")) }
    var suggestions by remember { mutableStateOf(initial.wantsSuggestions) }
    var healthConnecting by remember { mutableStateOf(false) }
    var healthError by remember { mutableStateOf<String?>(null) }
    val titles = listOf("¿CÓMO COMES?", "¿CUÁNTAS COMIDAS TE ENCAJAN?", "¿HAY ALGO QUE EVITAR?", "¿QUIERES IDEAS DE COMIDAS?")
    val subtitles = listOf(
        "Usaremos esto para adaptar las sugerencias a tus preferencias.",
        "Distribuiremos calorías y macros de una forma realista para tu día.",
        "Añade alimentos excluidos y alergias o intolerancias.",
        "Puedes recibir opciones sencillas junto a tus objetivos de macros.",
    )
    val needsHealthConnect = !isHealthConnectEnabled
    val healthStep = 4
    val totalSteps = 4 + if (needsHealthConnect) 1 else 0
    val isHealthStep = needsHealthConnect && step == healthStep
    fun saveSetup() {
        onSave(initial.copy(
            dietaryStyle = style,
            mealsPerDay = meals,
            wantsSuggestions = suggestions,
            excludedFoods = excluded.toNutritionList(),
            allergiesAndIntolerances = allergies.toNutritionList(),
        ))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 8.dp), shape = RoundedCornerShape(28.dp), color = WildforceThemeTokens.background) {
            if (intro) {
                NutritionWelcome(onContinue = { intro = false }, onDismiss = onDismiss)
            } else Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (step == 0) onDismiss() else step-- }) { Icon(if (step == 0) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, if (step == 0) "Cerrar" else "Atrás") }
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.nutrition).uppercase(), Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                    Text("${step + 1}/$totalSteps", color = NutritionCoral, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator((step + 1) / totalSteps.toFloat(), Modifier.fillMaxWidth().padding(top = 14.dp), color = NutritionCoral, backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = .15f))
                if (!isHealthStep) {
                    Text(titles[step], fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 32.dp))
                    Text(subtitles[step], color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 8.dp))
                }
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f).padding(top = if (isHealthStep) 0.dp else 24.dp),
                    transitionSpec = {
                        (fadeIn(tween(190)) + slideInHorizontally(tween(240)) { it / 10 }) togetherWith
                            (fadeOut(tween(130)) + slideOutHorizontally(tween(180)) { -it / 12 })
                    },
                    label = "nutrition-onboarding-step",
                ) { displayedStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (displayedStep == healthStep && needsHealthConnect) {
                            NutritionHealthConnectStep(
                                isConnecting = healthConnecting,
                                errorMessage = healthError,
                                onConnect = {
                                    if (!healthConnecting) {
                                        healthConnecting = true
                                        healthError = null
                                        onRequestHealthConnect { granted, _, _, _, _ ->
                                            healthConnecting = false
                                            if (granted) saveSetup()
                                            else healthError = "No se concedió el acceso a Health Connect."
                                        }
                                    }
                                },
                                onSkip = ::saveSetup,
                            )
                        } else when (displayedStep) {
                            0 -> listOf("Omnívora", "Vegetariana", "Vegana", "Pescetariana").forEach { option -> SetupChoice(option, style == option) { style = option } }
                            1 -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { meals = (meals - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.RemoveCircleOutline, "Menos") }
                                    Text("$meals", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = NutritionCoral, modifier = Modifier.padding(horizontal = 24.dp))
                                    IconButton(onClick = { meals = (meals + 1).coerceAtMost(10) }) { Icon(Icons.Filled.AddCircleOutline, "Más") }
                                }
                                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.meals_per_day).uppercase(), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            2 -> {
                                OutlinedTextField(excluded, { excluded = it.take(400) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.excluded_foods)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d0d81f2dcf960609)) }, minLines = 3)
                                OutlinedTextField(allergies, { allergies = it.take(400) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.allergies_or_intolerances)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.e_g__peanuts__lactose)) }, minLines = 3)
                            }
                            else -> {
                                SetupChoice("Sí, incluir sugerencias", suggestions, "Ideas sencillas adaptadas a tus macros.") { suggestions = true }
                                SetupChoice("No, solo objetivos", !suggestions, "Plan centrado en calorías, macros y timing.") { suggestions = false }
                            }
                        }
                    }
                }
                if (!isHealthStep) {
                    Button(onClick = {
                        if (step < totalSteps - 1) step++ else saveSetup()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(if (step == totalSteps - 1) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.save_setup).uppercase() else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.key_continue).uppercase(), fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
                }
            }
        }
    }
}

@Composable
private fun NutritionHealthConnectStep(
    isConnecting: Boolean,
    errorMessage: String?,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.wildforce_app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Icon(Icons.Filled.Sync, null, tint = NutritionCoral, modifier = Modifier.size(32.dp))
            Box(Modifier.size(92.dp).background(NutritionCoral.copy(alpha = .14f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, null, tint = NutritionCoral, modifier = Modifier.size(42.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_86aecc17fac663f8), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_141c18fe0c772367), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
            errorMessage?.let { Text(it, style = MaterialTheme.typography.caption, color = NutritionRed, textAlign = TextAlign.Center) }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onConnect, enabled = !isConnecting, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) {
                if (isConnecting) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp)
                else Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_79de068350f9e051), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.0.sp)
            }
            TextButton(onClick = onSkip, enabled = !isConnecting, modifier = Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.not_now).uppercase(), color = WildforceThemeTokens.textSecondary) }
        }
    }
}

@Composable
private fun NutritionWelcome(onContinue: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(80.dp).background(WildforceThemeTokens.textPrimary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Eco, null, tint = WildforceThemeTokens.primaryButtonText, modifier = Modifier.size(36.dp))
            }
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.welcome_to_nutrition).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.wildforce_helps_you_set_targets__plan_meals__and_keep_nutrition_simple_), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WelcomeFeature(Icons.Filled.PieChart, "Macros personalizados", "Calorías y objetivos de proteína, carbohidratos y grasa adaptados a ti.")
            WelcomeFeature(Icons.Filled.Event, "Planificación flexible", "Elige cuántas comidas haces y tu estilo alimentario.")
            WelcomeFeature(Icons.Filled.WarningAmber, "Exclusiones y alergias", "Evita alimentos para que las sugerencias sigan siendo realistas.")
        }
        Button(onClick = onContinue, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_276f6069beea5cd5), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, title: String, description: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Text(description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Composable
private fun SetupChoice(title: String, selected: Boolean, description: String? = null, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 560f),
        label = "nutrition-setup-choice-$title",
    )
    Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.background(if (selected) NutritionCoral.copy(alpha = .12f) else WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = { (fadeIn(tween(130)) + scaleIn(tween(170), initialScale = .7f)) togetherWith (fadeOut(tween(100)) + scaleOut(tween(120), targetScale = .8f)) },
            label = "nutrition-setup-check-$title",
        ) { isSelected ->
            Icon(if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (isSelected) NutritionCoral else WildforceThemeTokens.textSecondary)
        }
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); description?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 3.dp)) } }
    }
}

private fun String.toNutritionList(): List<String> = split(',').map(String::trim).filter(String::isNotBlank)

@Composable
private fun PreferenceChoice(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option -> NutritionOptionChip(option, selected.equals(option, true)) { onSelect(option) } }
        }
    }
}

@Composable
internal fun MealIdeasDialog(day: NutritionDayPlan?, targetOverride: NutritionTargets? = null, onDismiss: () -> Unit, onSelect: (PlannedMeal) -> Unit) {
    if (day == null) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.meal_ideas).uppercase(), fontFamily = AntonFontFamily) }, text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f859a18512113c62)) }, confirmButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.dismiss).uppercase()) } })
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ingredients by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoIngredients by remember { mutableStateOf<List<String>>(emptyList()) }
    var generatedIdeas by remember { mutableStateOf<List<NutritionMealIdea>>(emptyList()) }
    var selectedIdea by remember { mutableIntStateOf(0) }
    var showIdeaDetail by remember { mutableStateOf(false) }
    var showPhotoSource by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val target = targetOverride ?: day.meals.firstOrNull()?.targets ?: day.targets
    var mode by remember { mutableStateOf("Texto") }
    var cookingEffort by remember { mutableStateOf("Bajo") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            creating = true; error = null
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.ingredientsFromPhoto(context, uri) } }
                    .onSuccess { ingredients -> photoIngredients = ingredients }
                    .onFailure { error = photoAnalysisError(it) }
                creating = false
            }
        }
    }
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (saved && file != null) {
            runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.getOrNull()?.let { uri ->
                creating = true; error = null
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.ingredientsFromPhoto(context, uri) } }
                        .onSuccess { ingredients -> photoIngredients = ingredients }
                        .onFailure { error = photoAnalysisError(it) }
                    creating = false
                }
            }
        } else {
            file?.delete()
            error = "No se pudo abrir la cámara o no se recibió ninguna foto. Puedes intentarlo de nuevo o elegir una imagen de la galería."
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingCameraFile?.let { cameraPicker.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)) }
        else { pendingCameraFile?.delete(); pendingCameraFile = null; error = "Necesitamos acceso a la cámara para fotografiar la comida." }
    }
    fun launchCamera() {
        val file = File(context.cacheDir, "nutrition-ideas-${UUID.randomUUID()}.jpg")
        file.parentFile?.mkdirs(); pendingCameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraPicker.launch(uri)
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    fun plannedMealFor(idea: NutritionMealIdea): PlannedMeal = PlannedMeal(
        title = idea.title,
        type = MealType.Snack,
        targets = idea.targets,
        guidance = listOf(idea.description, idea.matchReason.orEmpty())
            .filter(String::isNotBlank)
            .joinToString("\n\n"),
        foods = idea.usesIngredients.map { it to 0 },
    )

    fun generateIdeas(regenerate: Boolean = false) {
        if (regenerate) {
            generatedIdeas = emptyList()
            selectedIdea = 0
        }
        creating = true
        error = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val request = if (mode == "Sorpréndeme") null else ingredients
                    NutritionAnalyzer.generateMealIdeas(
                        context,
                        request,
                        when (cookingEffort) { "Bajo" -> "low"; "Alto" -> "high"; else -> "medium" },
                        target,
                    )
                }
            }.onSuccess { ideas ->
                if (ideas.isEmpty()) error = "No se pudo crear ninguna idea. Comprueba tu conexión e inténtalo de nuevo."
                else {
                    generatedIdeas = ideas
                    selectedIdea = 0
                }
            }.onFailure { error = "No se pudo crear la idea. Comprueba tu conexión e inténtalo de nuevo." }
            creating = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_c20cc6b8b1c4e51a), fontFamily = AntonFontFamily) },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_655cdbd0ac1b3b55), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Texto", "Foto", "Sorpréndeme").forEach { option ->
                        NutritionOptionChip(option, mode == option) { mode = option }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MealMacro(NutritionMacroIcon.Calories, "${target.calories} kcal", NutritionCaloriesColor); MealMacro(NutritionMacroIcon.Protein, "${target.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${target.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${target.fat}g", FatColor) }
                if (mode == "Texto") OutlinedTextField(ingredients, { ingredients = it.take(500) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_61b41c62b0a376b9)) }, placeholder = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_6cb18128083479df)) }, minLines = 3, enabled = !creating)
                Text(
                    when (mode) {
                        "Foto" -> "Selecciona una foto de los ingredientes disponibles y revisaré lo que detecte antes de crear ideas."
                        "Texto" -> "Crearé ideas usando los ingredientes que escribas y los objetivos del día."
                        else -> "Crearé una idea sorpresa usando los objetivos del día y una combinación equilibrada de alimentos."
                    },
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
                if (mode == "Foto") Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Bajo", "Medio", "Alto").forEach { effort ->
                        NutritionOptionChip(effort, cookingEffort == effort) { cookingEffort = effort }
                    }
                }
                if (photoIngredients.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.review_ingredients).uppercase(), fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                        photoIngredients.forEachIndexed { index, ingredient ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(ingredient, { value -> photoIngredients = photoIngredients.mapIndexed { i, item -> if (i == index) value.take(80) else item } }, Modifier.weight(1f), label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ingredient)) }, singleLine = true)
                                IconButton(onClick = { photoIngredients = photoIngredients.filterIndexed { i, _ -> i != index } }, enabled = photoIngredients.size > 1) { Icon(Icons.Filled.DeleteOutline, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.delete_ingredient), tint = NutritionRed) }
                            }
                        }
                        TextButton(onClick = { photoIngredients = photoIngredients + "" }) { Icon(Icons.Filled.Add, null, tint = NutritionCoral); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.add_ingredient), Modifier.padding(start = 6.dp), color = NutritionCoral) }
                    }
                }
                if (generatedIdeas.isNotEmpty()) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f38cc5bbb28c6004), fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                    generatedIdeas.forEachIndexed { index, idea ->
                        Row(Modifier.fillMaxWidth().background(if (selectedIdea == index) WildforceThemeTokens.textPrimary.copy(alpha = .08f) else Color.Transparent, RoundedCornerShape(10.dp)).clickable { selectedIdea = index; showIdeaDetail = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(idea.title, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_ac0bf93ae7af8c28, idea.targets.calories, idea.targets.protein, idea.targets.carbs, idea.targets.fat), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                                if (idea.description.isNotBlank()) Text(idea.description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                                if (idea.usesIngredients.isNotEmpty()) Text("Usa: ${idea.usesIngredients.joinToString(", ")}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                                if (idea.extraIngredients.isNotEmpty()) Text("Necesita: ${idea.extraIngredients.joinToString(", ")}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            if (selectedIdea == index) Icon(Icons.Filled.CheckCircle, "Seleccionada", tint = NutritionCoral)
                        }
                    }
                    TextButton(onClick = { generateIdeas(regenerate = true) }, enabled = !creating) { Icon(Icons.Filled.Refresh, null, tint = NutritionCoral); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.generate_another), Modifier.padding(start = 6.dp), color = NutritionCoral) }
                }
                AnimatedVisibility(
                    visible = creating,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = NutritionCoral, strokeWidth = 2.dp)
                        Text(if (mode == "Foto") androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_56b4f7ccc60fba1d) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_85218e9164f8e4c6), Modifier.padding(start = 9.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                }
                error?.let { Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (mode == "Foto") {
                    if (photoIngredients.isNotEmpty()) {
                        ingredients = photoIngredients.filter(String::isNotBlank).joinToString(", ")
                        mode = "Texto"
                        generateIdeas()
                    } else showPhotoSource = true
                    return@Button
                }
                if (generatedIdeas.isNotEmpty()) { onSelect(plannedMealFor(generatedIdeas[selectedIdea.coerceIn(0, generatedIdeas.lastIndex)])); return@Button }
                generateIdeas()
            }, enabled = (mode != "Texto" || ingredients.isNotBlank() || photoIngredients.isNotEmpty() || generatedIdeas.isNotEmpty()) && !creating, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Icon(if (mode == "Foto") Icons.Filled.Photo else Icons.Filled.Kitchen, null, modifier = Modifier.size(17.dp)); Text(if (generatedIdeas.isNotEmpty()) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_2e89417900b71bf5).uppercase() else if (mode == "Foto" && photoIngredients.isNotEmpty()) "CREAR IDEAS" else if (mode == "Foto") "ELEGIR FOTO" else "CREAR IDEA", Modifier.padding(start = 7.dp), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !creating) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } },
    )
    val detailIdea = generatedIdeas.getOrNull(selectedIdea)
    if (showIdeaDetail && detailIdea != null) {
        AlertDialog(
            onDismissRequest = { showIdeaDetail = false },
            title = { Text(detailIdea.title, fontFamily = AntonFontFamily) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (detailIdea.description.isNotBlank()) Text(detailIdea.description, color = WildforceThemeTokens.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealMacro(NutritionMacroIcon.Calories, "${detailIdea.targets.calories} kcal", NutritionCaloriesColor)
                        MealMacro(NutritionMacroIcon.Protein, "${detailIdea.targets.protein}g", ProteinColor)
                        MealMacro(NutritionMacroIcon.Carbs, "${detailIdea.targets.carbs}g", CarbsColor)
                        MealMacro(NutritionMacroIcon.Fat, "${detailIdea.targets.fat}g", FatColor)
                    }
                    if (detailIdea.usesIngredients.isNotEmpty()) Text("Ingredientes usados\n${detailIdea.usesIngredients.joinToString("\n") { "• $it" }}", color = WildforceThemeTokens.textPrimary)
                    if (detailIdea.extraIngredients.isNotEmpty()) Text("Ingredientes adicionales\n${detailIdea.extraIngredients.joinToString("\n") { "• $it" }}", color = WildforceThemeTokens.textPrimary)
                    detailIdea.matchReason?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            },
            confirmButton = { Button(onClick = { onSelect(plannedMealFor(detailIdea)) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_2e89417900b71bf5)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { showIdeaDetail = false; generateIdeas(regenerate = true) }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.generate_another).uppercase(), color = NutritionCoral) }
                    TextButton(onClick = { showIdeaDetail = false; generatedIdeas = emptyList(); selectedIdea = 0 }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.edit).uppercase()) }
                }
            },
        )
    }
    if (showPhotoSource) AlertDialog(
        onDismissRequest = { showPhotoSource = false },
        title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.add_photo), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold) },
        text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_63cd02ffaabd142e)) },
        confirmButton = { TextButton(onClick = { showPhotoSource = false; launchCamera() }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_841b8ccb44fa8d7f), color = NutritionCoral) } },
        dismissButton = { TextButton(onClick = { showPhotoSource = false; photoPicker.launch("image/*") }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.gallery)) } },
    )
}

@Composable
internal fun NutritionMoreActionsDialog(onDismiss: () -> Unit, onIdeas: () -> Unit, onFulfill: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_803e7245f990473b), fontFamily = AntonFontFamily) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionRow(Icons.Filled.Lightbulb, "Ideas de comidas", "Sugerencias ajustadas a tu plan", onIdeas)
            ActionRow(Icons.Filled.AutoAwesome, "Completar macros", "Encuentra opciones para cubrir lo que falta", onFulfill)
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.dismiss).uppercase()) } },
    )
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(30.dp))
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text(description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
internal fun NutritionGroceryDialog(plan: List<NutritionDayPlan>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var persons by remember { mutableStateOf(1) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var showDiscard by remember { mutableStateOf(false) }
    val groceries = remember(plan) {
        plan.flatMap { it.meals }.flatMap { it.foods }.groupBy { it.first.lowercase() }.map { (_, values) -> values.first().first to values.sumOf { it.second } }.sortedBy { it.first }
    }
    val orderedGroceries = groceries.sortedWith(
        compareBy<Pair<String, Int>> { it.first.lowercase() in checked }
            .thenBy { it.first.lowercase() },
    )
    Dialog(
        onDismissRequest = {
            if (groceries.all { it.first.lowercase() in checked } || groceries.isEmpty()) onDismiss()
            else showDiscard = true
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 8.dp), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.grocery_list).uppercase(), Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary); TextButton(onClick = { val text = groceries.joinToString("\n") { "- ${it.first}: ${it.second * persons}g" }; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Lista de la compra\n\n$text") }, "Compartir lista")) }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_884f0d53f3e1d469), color = NutritionCoral) }; TextButton(onClick = { if (groceries.all { it.first.lowercase() in checked } || groceries.isEmpty()) onDismiss() else showDiscard = true }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ready).uppercase(), color = NutritionCoral) } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.People, "Personas", tint = NutritionCoral); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_fa2ea3fb081cf5c5), Modifier.padding(start = 8.dp).weight(1f), color = WildforceThemeTokens.textPrimary); IconButton(onClick = { persons = (persons - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.Remove, "Reducir personas") }; Text(persons.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { persons = (persons + 1).coerceAtMost(20) }) { Icon(Icons.Filled.Add, "Aumentar personas") } }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (groceries.isEmpty()) item { EmptyState("Sin alimentos", "No hay sugerencias en este plan.") }
                    items(orderedGroceries, key = { it.first }) { (name, amount) ->
                        val isChecked = name.lowercase() in checked
                        val checkScale by animateFloatAsState(
                            targetValue = if (isChecked) 1.16f else 1f,
                            animationSpec = tween(180),
                            label = "grocery-check-$name",
                        )
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (isChecked) .6f else 1f).clickable { checked = if (isChecked) checked - name.lowercase() else checked + name.lowercase() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, modifier = Modifier.graphicsLayer { scaleX = checkScale; scaleY = checkScale }, tint = if (isChecked) NutritionGreen else WildforceThemeTokens.textSecondary); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.total_amount), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null) }; Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_35b530b7e1763c2b, amount * persons), Modifier.background((if (isChecked) WildforceThemeTokens.textSecondary else NutritionCoral).copy(alpha = .1f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = if (isChecked) WildforceThemeTokens.textSecondary else NutritionCoral, textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null) }
                    }
                }
            }
        }
        if (showDiscard) {
            AlertDialog(
                onDismissRequest = { showDiscard = false },
                title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.discard_changes_)) },
                text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.some_items_are_still_unchecked__are_you_sure_you_want_to_close_the_list_)) },
                confirmButton = { TextButton(onClick = { showDiscard = false; onDismiss() }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.discard___close).uppercase(), color = NutritionRed) } },
                dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_shopping).uppercase()) } },
            )
        }
    }
}

@Composable
internal fun NutritionFulfillDialog(current: NutritionTargets, target: NutritionTargets, onDismiss: () -> Unit, onSelect: (MealLog) -> Unit) {
    val context = LocalContext.current
    val missing = NutritionTargets((target.calories - current.calories).coerceAtLeast(0), (target.protein - current.protein).coerceAtLeast(0), (target.carbs - current.carbs).coerceAtLeast(0), (target.fat - current.fat).coerceAtLeast(0))
    val met = missing.calories <= 10 && missing.protein <= 1 && missing.carbs <= 1 && missing.fat <= 1
    var suggestions by remember(missing) { mutableStateOf<List<NutritionMealIdea>>(emptyList()) }
    var loading by remember(missing) { mutableStateOf(true) }
    var error by remember(missing) { mutableStateOf<String?>(null) }
    LaunchedEffect(missing) {
        if (met) { loading = false; return@LaunchedEffect }
        runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.generateMissingMacroIdeas(context, missing) } }
            .onSuccess { suggestions = it }
            .onFailure { error = "No se pudieron cargar las ideas. Inténtalo de nuevo." }
        loading = false
    }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 8.dp), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_325b08a2f774093a), Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                    TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.dismiss).uppercase(), color = NutritionCoral) }
                }
                if (met) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("¡Objetivos cumplidos!", "Ya has cubierto los macros del día.") }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Calories, "${missing.calories} kcal", NutritionCaloriesColor); MealMacro(NutritionMacroIcon.Protein, "${missing.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${missing.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${missing.fat}g", FatColor) }
                        if (loading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), color = NutritionCoral, strokeWidth = 2.dp); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_1182ed312c77b345), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                        error?.let { message -> Text(message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption) }
                        suggestions.forEachIndexed { index, suggestion ->
        val reveal by animateFloatAsState(if (appeared) 1f else 0f, tween(260, delayMillis = 90 + index * 55), label = "macro-idea-$index")
                        Row(Modifier.fillMaxWidth().graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 18.dp.toPx() }.background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).clickable { onSelect(MealLog(0, suggestion.title, suggestion.targets.calories, suggestion.targets.protein, suggestion.targets.carbs, suggestion.targets.fat, MealType.Snack, source = "fulfill_missing_macros")) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(suggestion.title, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_84744cf2ee7e93cc, suggestion.targets.calories), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${suggestion.targets.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${suggestion.targets.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${suggestion.targets.fat}g", FatColor) } }; Icon(Icons.Filled.AddCircle, "Registrar", tint = NutritionCoral) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionOptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp)
            .background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
    }
}
