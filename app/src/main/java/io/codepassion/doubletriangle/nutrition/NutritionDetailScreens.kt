package io.codepassion.doubletriangle.nutrition

import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        Text("REVISA TU PLAN", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                        Text("No se guardará hasta que lo confirmes.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar", tint = WildforceThemeTokens.textPrimary) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(plan, key = { it.date }) { day ->
                        Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale("es"))).replaceFirstChar { it.uppercase() }, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                Text("${day.targets.calories} kcal", fontWeight = FontWeight.Bold, color = NutritionCoral)
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("DESCARTAR") }
                    Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = NutritionCoral)) { Text("GUARDAR PLAN", color = Color.White) }
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
    onPlannedMealAction: (PlannedMeal, LogMethod) -> Unit = { _, _ -> onLog() },
    onDiscardPlannedMeal: (PlannedMeal) -> Unit = {},
    isSuggestionHidden: (PlannedMeal) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().liquidGlassBackground()) {
        TopBar(day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }, onBack, onLog)
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { NutritionDayHeader(day, entries.total(), day.date == LocalDate.now()) {} }
            item { MacroSummary(day.targets) }
            if (day.notes.isNotBlank() || day.workoutTitle.isNotBlank() || day.preWorkoutGuidance != null || day.postWorkoutGuidance != null) {
                item {
                    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("COMBUSTIBLE PARA EL ENTRENAMIENTO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                        if (day.notes.isNotBlank()) {
                            Text("NOTAS", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                            Text(day.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        if (day.workoutTitle.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FitnessCenter, null, tint = NutritionCoral, modifier = Modifier.size(18.dp))
                                Text(day.workoutTitle, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                            }
                        }
                        day.preWorkoutGuidance?.let { Guidance("Antes de entrenar", it, NutritionOrange) }
                        day.postWorkoutGuidance?.let { Guidance("Después de entrenar", it, NutritionGreen) }
                    }
                }
            }
            item { Text("COMIDAS DEL DÍA", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary) }
            if (entries.isEmpty() && day.meals.isEmpty()) {
                item { EmptyState("Aún no hay comidas", "Añade una ingesta o activa las sugerencias en tu perfil nutricional.") }
            }
            MealType.entries.forEach { type ->
                val logged = entries.filter { it.type == type }
                val suggested = day.meals.filter { it.type == type && !isSuggestionHidden(it) }
                if (logged.isNotEmpty() || suggested.isNotEmpty()) {
                    item { Text(type.title.uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) }
                    items(logged, key = { "logged-${it.id}" }) { entry -> LoggedMealDayCard(entry) }
                    items(suggested, key = { "suggested-${it.title}-${it.type}" }) { meal -> PlannedMealCard(meal, onPlannedMealAction, onDiscardPlannedMeal) }
                }
            }
        }
    }
}

@Composable private fun TopBar(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }; Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Filled.AddCircle, "Registrar", tint = NutritionCoral) } else Spacer(Modifier.width(48.dp)) } }

@Composable private fun MacroSummary(target: NutritionTargets) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedMacroBadge("Calorías", target.calories, "kcal", NutritionCoral, NutritionMacroIcon.Calories, Modifier.fillMaxWidth())
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
    Row(modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            NutritionMacroSymbol(icon, color, Modifier.size(20.dp).graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value })
        }
        Column(Modifier.padding(start = 7.dp)) {
            Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Text("$value $unit", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        }
    }
}

@Composable private fun Guidance(title: String, body: String, color: Color) { Column(Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = color); Text(body, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }

@Composable private fun PlannedMealCard(meal: PlannedMeal, onAction: (PlannedMeal, LogMethod) -> Unit, onDiscard: (PlannedMeal) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) { Text(meal.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(meal.type.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
            Text("${meal.targets.calories} kcal", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Más opciones", tint = WildforceThemeTokens.textSecondary) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MealMenuItem(Icons.Filled.CheckCircle, "Registrar esta comida") { menuOpen = false; onAction(meal, LogMethod.Manual) }
                    MealMenuItem(Icons.Filled.Kitchen, "Obtener ideas de comida") { menuOpen = false; onAction(meal, LogMethod.Describe) }
                    MealMenuItem(Icons.Filled.CameraAlt, "Analizar foto") { menuOpen = false; onAction(meal, LogMethod.Photo) }
                    MealMenuItem(Icons.Filled.QrCodeScanner, "Escanear código de barras") { menuOpen = false; onAction(meal, LogMethod.Barcode) }
                    Divider()
                    MealMenuItem(Icons.Filled.DeleteOutline, "Descartar sugerencia") { menuOpen = false; onDiscard(meal) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Protein, "${meal.targets.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${meal.targets.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${meal.targets.fat}g", FatColor) }
        Text(meal.guidance, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { meal.foods.forEach { (food, grams) -> Text("$food (${grams}g)", Modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) } }
    }
}

/** Matches iOS's combined day timeline: registered food and plan suggestions share one meal-type section. */
@Composable
private fun LoggedMealDayCard(entry: MealLog) {
    val photo = remember(entry.photoPath) { entry.photoPath?.let(BitmapFactory::decodeFile) }
    Column(
        Modifier.fillMaxWidth()
            .background(NutritionGreen.copy(alpha = .08f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) Image(photo.asImageBitmap(), "Foto de ${entry.name}", Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(42.dp).background(NutritionGreen.copy(alpha = .15f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, "Registrada", tint = NutritionGreen) }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(entry.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("REGISTRADA · ${entry.calories} kcal", style = MaterialTheme.typography.caption, color = NutritionGreen)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MealMacro(NutritionMacroIcon.Protein, "${entry.protein}g", ProteinColor)
            MealMacro(NutritionMacroIcon.Carbs, "${entry.carbs}g", CarbsColor)
            MealMacro(NutritionMacroIcon.Fat, "${entry.fat}g", FatColor)
        }
        if (entry.items.size > 1) Text(entry.items.joinToString(" · ") { it.name }, maxLines = 1, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable private fun MealMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) { DropdownMenuItem(onClick = onClick) { Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(18.dp)); Text(text, Modifier.padding(start = 12.dp)) } }
@Composable private fun MealMacro(icon: NutritionMacroIcon, value: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { NutritionMacroSymbol(icon, color, Modifier.size(14.dp)); Text(value, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) } }

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
    Column(modifier.fillMaxSize().liquidGlassBackground()) {
        TopBar("Ingesta", onBack, onAdd)
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es"))).uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary) }
            item { EntrySummary(entries.total(), target) }
            if (entries.isEmpty()) item { EmptyState("Aún no hay registros", "Usa el botón + para añadir tu primera comida del día.") }
            MealType.entries.forEach { type ->
                val group = entries.filter { it.type == type }
                if (group.isNotEmpty()) {
                    item { Text(type.title.uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) }
                    items(group, key = { it.id }) { entry ->
                        EntryCard(entry, onBookmark = { onChanged(entries.map { if (it.id == entry.id) it.copy(isBookmarked = !it.isBookmarked) else it }) }, onDelete = { deleting = entry }, onEdit = { onEdit(entry) })
                    }
                }
            }
        }
    }
    deleting?.let { entry -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("¿Eliminar registro?") }, text = { Text("Se eliminará «${entry.name}» de este día.") }, confirmButton = { TextButton(onClick = { onChanged(entries.filterNot { it.id == entry.id }); deleting = null }) { Text("ELIMINAR", color = NutritionRed) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("CANCELAR") } }) }
}

@Composable private fun EntrySummary(current: NutritionTargets, target: NutritionTargets) { Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), emphasized = true).padding(16.dp)) { Row(verticalAlignment = Alignment.Bottom) { NutritionMacroSymbol(NutritionMacroIcon.Calories, NutritionCoral, Modifier.size(20.dp)); Text(current.calories.toString(), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(" / ${target.calories} kcal", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(bottom = 5.dp)) }; Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { MealMacro(NutritionMacroIcon.Protein, "${current.protein}/${target.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${current.carbs}/${target.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${current.fat}/${target.fat}g", FatColor) } } }

@Composable
private fun EntryCard(entry: MealLog, onBookmark: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val photo = remember(entry.photoPath) { entry.photoPath?.let(BitmapFactory::decodeFile) }
    var expanded by remember(entry.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable(onClick = onEdit).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) Image(photo.asImageBitmap(), "Foto de ${entry.name}", Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            else Icon(Icons.Filled.Restaurant, null, tint = NutritionCoral)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${entry.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${entry.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${entry.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${entry.fat}g", FatColor) }
            }
            IconButton(onClick = onBookmark) { Icon(if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Favorito", tint = if (entry.isBookmarked) NutritionCoral else WildforceThemeTokens.textSecondary) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Eliminar", tint = WildforceThemeTokens.textSecondary) }
        }
        if (entry.items.size > 1) {
            Text(
                "${entry.items.size} alimentos ${if (expanded) "⌃" else "⌄"}",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = NutritionCoral,
                modifier = Modifier.padding(top = 7.dp).clickable { expanded = !expanded },
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(150)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
            ) {
                Column {
                    entry.items.forEach { food -> Text("• ${food.name} · ${food.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            }
        }
        if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
internal fun NutritionTargetsDialog(initial: NutritionTargets, onDismiss: () -> Unit, onSave: (NutritionTargets) -> Unit) {
    var calories by remember { mutableStateOf(initial.calories.toString()) }; var protein by remember { mutableStateOf(initial.protein.toString()) }; var carbs by remember { mutableStateOf(initial.carbs.toString()) }; var fat by remember { mutableStateOf(initial.fat.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("OBJETIVOS DIARIOS", fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Calorías", calories, { calories = it.filter(Char::isDigit) }, Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Proteína", protein, { protein = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Carbos", carbs, { carbs = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Grasa", fat, { fat = it.filter(Char::isDigit) }, Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { onSave(NutritionTargets(calories.toIntOrNull()?.coerceIn(500, 8000) ?: initial.calories, protein.toIntOrNull()?.coerceIn(0, 500) ?: initial.protein, carbs.toIntOrNull()?.coerceIn(0, 1000) ?: initial.carbs, fat.toIntOrNull()?.coerceIn(0, 500) ?: initial.fat)) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary)) { Text("GUARDAR", color = WildforceThemeTokens.backgroundSecondary) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } })
}

@Composable
internal fun NutritionPreferencesDialog(initial: NutritionPreferences, onDismiss: () -> Unit, isInitialSetup: Boolean = false, onSave: (NutritionPreferences) -> Unit) {
    if (isInitialSetup) {
        NutritionOnboardingDialog(initial, onDismiss, onSave)
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
        title = { Text("PERFIL NUTRICIONAL", fontFamily = AntonFontFamily) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("PLANIFICACIÓN", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary) }
                item { Text("¿Cómo comes?", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { styles.forEach { item -> Text(item, Modifier.background(if (style == item) NutritionCoral else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).clickable { style = item }.padding(horizontal = 10.dp, vertical = 7.dp), color = if (style == item) Color.White else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) } } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Comidas al día", Modifier.weight(1f)); IconButton(onClick = { meals = (meals - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.Remove, null) }; Text(meals.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { meals = (meals + 1).coerceAtMost(10) }) { Icon(Icons.Filled.Add, null) } } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Inicio (0–23)", startHour, { startHour = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f)); NumberField("Fin (0–23)", endHour, { endHour = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f)) } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Sugerencias de comidas", Modifier.weight(1f)); Switch(suggestions, { suggestions = it }, colors = SwitchDefaults.colors(checkedThumbColor = NutritionCoral, checkedTrackColor = NutritionCoral)) } }
                item { Text("PREFERENCIAS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp)) }
                item { PreferenceChoice("Esfuerzo al cocinar", cookingEffort, listOf("Bajo", "Medio", "Alto")) { cookingEffort = it } }
                item { PreferenceChoice("Sensibilidad al presupuesto", budgetSensitivity, listOf("Baja", "Media", "Alta")) { budgetSensitivity = it } }
                item { OutlinedTextField(proteinSources, { proteinSources = it }, Modifier.fillMaxWidth(), label = { Text("Fuentes de proteína") }, placeholder = { Text("Ej.: huevos, tofu, salmón") }, maxLines = 3) }
                item { OutlinedTextField(dislikes, { dislikes = it }, Modifier.fillMaxWidth(), label = { Text("Alimentos que no te gustan") }, placeholder = { Text("Ej.: aceitunas, hígado") }, maxLines = 3) }
                item { Text("RESTRICCIONES", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp)) }
                item { OutlinedTextField(excludedFoods, { excludedFoods = it }, Modifier.fillMaxWidth(), label = { Text("Alimentos excluidos") }, placeholder = { Text("Ej.: setas, aceitunas") }, maxLines = 3) }
                item { OutlinedTextField(allergies, { allergies = it }, Modifier.fillMaxWidth(), label = { Text("Alergias o intolerancias") }, placeholder = { Text("Ej.: cacahuetes, lactosa") }, maxLines = 3) }
                item { OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Notas") }, placeholder = { Text("Ej.: comidas rápidas entre semana") }, minLines = 3) }
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
            }, colors = ButtonDefaults.buttonColors(backgroundColor = NutritionCoral)) { Text("GUARDAR", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

/** Mirrors iOS's first-run nutrition setup instead of dropping a new user into the long edit form. */
@Composable
private fun NutritionOnboardingDialog(initial: NutritionPreferences, onDismiss: () -> Unit, onSave: (NutritionPreferences) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var style by remember { mutableStateOf(initial.dietaryStyle) }
    var meals by remember { mutableIntStateOf(initial.mealsPerDay.coerceIn(1, 8)) }
    var excluded by remember { mutableStateOf(initial.excludedFoods.joinToString(", ")) }
    var allergies by remember { mutableStateOf(initial.allergiesAndIntolerances.joinToString(", ")) }
    var suggestions by remember { mutableStateOf(initial.wantsSuggestions) }
    val titles = listOf("¿CÓMO COMES?", "¿CUÁNTAS COMIDAS TE ENCAJAN?", "¿HAY ALGO QUE EVITAR?", "¿QUIERES IDEAS DE COMIDAS?")
    val subtitles = listOf(
        "Usaremos esto para adaptar las sugerencias a tus preferencias.",
        "Distribuiremos calorías y macros de una forma realista para tu día.",
        "Añade alimentos excluidos y alergias o intolerancias.",
        "Puedes recibir opciones sencillas junto a tus objetivos de macros.",
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 8.dp), shape = RoundedCornerShape(28.dp), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (step == 0) onDismiss() else step-- }) { Icon(if (step == 0) Icons.Filled.Close else Icons.Filled.ArrowBack, if (step == 0) "Cerrar" else "Atrás") }
                    Text("NUTRICIÓN", Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                    Text("${step + 1}/4", color = NutritionCoral, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator((step + 1) / 4f, Modifier.fillMaxWidth().padding(top = 14.dp), color = NutritionCoral, backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = .15f))
                Text(titles[step], fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 32.dp))
                Text(subtitles[step], color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 8.dp))
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f).padding(top = 24.dp),
                    transitionSpec = {
                        (fadeIn(tween(190)) + slideInHorizontally(tween(240)) { it / 10 }) togetherWith
                            (fadeOut(tween(130)) + slideOutHorizontally(tween(180)) { -it / 12 })
                    },
                    label = "nutrition-onboarding-step",
                ) { displayedStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (displayedStep) {
                        0 -> listOf("Omnívora", "Vegetariana", "Vegana", "Pescetariana").forEach { option -> SetupChoice(option, style == option) { style = option } }
                        1 -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { meals = (meals - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.RemoveCircleOutline, "Menos") }
                                Text("$meals", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = NutritionCoral, modifier = Modifier.padding(horizontal = 24.dp))
                                IconButton(onClick = { meals = (meals + 1).coerceAtMost(8) }) { Icon(Icons.Filled.AddCircleOutline, "Más") }
                            }
                            Text("COMIDAS AL DÍA", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        2 -> {
                            OutlinedTextField(excluded, { excluded = it.take(400) }, Modifier.fillMaxWidth(), label = { Text("Alimentos excluidos") }, placeholder = { Text("Ej.: setas, aceitunas") }, minLines = 3)
                            OutlinedTextField(allergies, { allergies = it.take(400) }, Modifier.fillMaxWidth(), label = { Text("Alergias o intolerancias") }, placeholder = { Text("Ej.: cacahuetes, lactosa") }, minLines = 3)
                        }
                        else -> {
                            SetupChoice("Sí, incluir sugerencias", suggestions, "Ideas sencillas adaptadas a tus macros.") { suggestions = true }
                            SetupChoice("No, solo objetivos", !suggestions, "Plan centrado en calorías, macros y timing.") { suggestions = false }
                        }
                    }
                    }
                }
                Button(onClick = {
                    if (step < 3) step++ else onSave(initial.copy(dietaryStyle = style, mealsPerDay = meals, wantsSuggestions = suggestions, excludedFoods = excluded.toNutritionList(), allergiesAndIntolerances = allergies.toNutritionList()))
                }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), shape = RoundedCornerShape(14.dp)) { Text(if (step == 3) "GUARDAR CONFIGURACIÓN" else "CONTINUAR", fontWeight = FontWeight.Bold) }
            }
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
            options.forEach { option -> Text(option, Modifier.background(if (selected.equals(option, true)) NutritionCoral else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).clickable { onSelect(option) }.padding(horizontal = 10.dp, vertical = 7.dp), color = if (selected.equals(option, true)) Color.White else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) }
        }
    }
}

@Composable
internal fun MealIdeasDialog(day: NutritionDayPlan?, targetOverride: NutritionTargets? = null, onDismiss: () -> Unit, onSelect: (PlannedMeal) -> Unit) {
    if (day == null) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text("IDEAS DE COMIDAS", fontFamily = AntonFontFamily) }, text = { Text("Selecciona un día del plan para crear una idea de comida.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } })
        return
    }
    val scope = rememberCoroutineScope()
    var ingredients by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val target = targetOverride ?: day.meals.firstOrNull()?.targets ?: day.targets
    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("IDEAS DESDE TU NEVERA", fontFamily = AntonFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Escribe los ingredientes que tienes y crearé una comida ajustada a los macros del día.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MealMacro(NutritionMacroIcon.Calories, "${target.calories} kcal", NutritionCoral); MealMacro(NutritionMacroIcon.Protein, "${target.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${target.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${target.fat}g", FatColor) }
                OutlinedTextField(ingredients, { ingredients = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Ingredientes disponibles") }, placeholder = { Text("Ej.: arroz, pollo, tomate y yogur") }, minLines = 3, enabled = !creating)
                AnimatedVisibility(
                    visible = creating,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = NutritionCoral, strokeWidth = 2.dp)
                        Text("Creando idea…", Modifier.padding(start = 9.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                }
                error?.let { Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                creating = true; error = null
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.fridgeIdea(ingredients, target) } }
                        .onSuccess { idea ->
                            onSelect(PlannedMeal(idea.name, MealType.Snack, NutritionTargets(idea.calories, idea.protein, idea.carbs, idea.fat), idea.notes, ingredients.split(",").mapNotNull { ingredient -> ingredient.trim().takeIf(String::isNotBlank)?.let { it to 0 } }))
                        }
                        .onFailure { error = it.message ?: "No se pudo crear la idea" }
                    creating = false
                }
            }, enabled = ingredients.isNotBlank() && !creating, colors = ButtonDefaults.buttonColors(backgroundColor = NutritionCoral)) { Icon(Icons.Filled.Kitchen, null, modifier = Modifier.size(17.dp)); Text("CREAR IDEA", Modifier.padding(start = 7.dp), color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !creating) { Text("CANCELAR") } },
    )
}

@Composable
internal fun NutritionMoreActionsDialog(onDismiss: () -> Unit, onIdeas: () -> Unit, onFulfill: () -> Unit, onGrocery: () -> Unit, onRegenerate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ACCIONES DE NUTRICIÓN", fontFamily = AntonFontFamily) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionRow(Icons.Filled.Lightbulb, "Ideas de comidas", "Sugerencias ajustadas a tu plan", onIdeas)
            ActionRow(Icons.Filled.AutoAwesome, "Completar macros", "Encuentra opciones para cubrir lo que falta", onFulfill)
            ActionRow(Icons.Filled.ShoppingBasket, "Lista de la compra", "Agrupa los alimentos del plan semanal", onGrocery)
            ActionRow(Icons.Filled.Refresh, "Regenerar plan", "Vuelve a distribuir la semana con tus objetivos", onRegenerate)
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } },
    )
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(30.dp))
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text(description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
internal fun NutritionGroceryDialog(plan: List<NutritionDayPlan>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var persons by remember { mutableStateOf(1) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    val groceries = remember(plan) {
        plan.flatMap { it.meals }.flatMap { it.foods }.groupBy { it.first.lowercase() }.map { (_, values) -> values.first().first to values.sumOf { it.second } }.sortedBy { it.first }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 8.dp), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("LISTA DE LA COMPRA", Modifier.weight(1f), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary); TextButton(onClick = { val text = groceries.joinToString("\n") { "- ${it.first}: ${it.second * persons}g" }; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Lista de la compra\n\n$text") }, "Compartir lista")) }) { Text("COMPARTIR", color = NutritionCoral) }; IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.People, null, tint = NutritionCoral); Text("Personas", Modifier.padding(start = 8.dp).weight(1f), color = WildforceThemeTokens.textPrimary); IconButton(onClick = { persons = (persons - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.Remove, null) }; Text(persons.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { persons = (persons + 1).coerceAtMost(20) }) { Icon(Icons.Filled.Add, null) } }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (groceries.isEmpty()) item { EmptyState("Sin alimentos", "No hay sugerencias en este plan.") }
                    items(groceries, key = { it.first }) { (name, amount) ->
                        val isChecked = name.lowercase() in checked
                        val checkScale by animateFloatAsState(
                            targetValue = if (isChecked) 1.16f else 1f,
                            animationSpec = tween(180),
                            label = "grocery-check-$name",
                        )
                        Row(Modifier.fillMaxWidth().clickable { checked = if (isChecked) checked - name.lowercase() else checked + name.lowercase() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, modifier = Modifier.graphicsLayer { scaleX = checkScale; scaleY = checkScale }, tint = if (isChecked) NutritionGreen else WildforceThemeTokens.textSecondary); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text("Cantidad total", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }; Text("${amount * persons}g", Modifier.background(NutritionCoral.copy(alpha = .1f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = NutritionCoral) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutritionFulfillDialog(current: NutritionTargets, target: NutritionTargets, onDismiss: () -> Unit, onSelect: (MealLog) -> Unit) {
    val missing = NutritionTargets((target.calories - current.calories).coerceAtLeast(0), (target.protein - current.protein).coerceAtLeast(0), (target.carbs - current.carbs).coerceAtLeast(0), (target.fat - current.fat).coerceAtLeast(0))
    val met = missing.calories <= 10 && missing.protein <= 1 && missing.carbs <= 1 && missing.fat <= 1
    val suggestions = listOf(
        MealLog(0, "Yogur griego con fruta", (missing.calories * .75).toInt(), (missing.protein * .95).toInt(), (missing.carbs * .55).toInt(), (missing.fat * .5).toInt(), MealType.Snack),
        MealLog(0, "Arroz con pollo", (missing.calories * .9).toInt(), (missing.protein * .85).toInt(), (missing.carbs * .9).toInt(), (missing.fat * .45).toInt(), MealType.Lunch),
        MealLog(0, "Tostada con huevo y aguacate", (missing.calories * .8).toInt(), (missing.protein * .55).toInt(), (missing.carbs * .65).toInt(), (missing.fat * .85).toInt(), MealType.Snack),
    )
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("MACROS QUE FALTAN", fontFamily = AntonFontFamily) }, text = { if (met) EmptyState("¡Objetivos cumplidos!", "Ya has cubierto los macros del día.") else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Calories, "${missing.calories} kcal", NutritionCoral); MealMacro(NutritionMacroIcon.Protein, "${missing.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${missing.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${missing.fat}g", FatColor) }; suggestions.forEachIndexed { index, suggestion ->
        val reveal by animateFloatAsState(if (appeared) 1f else 0f, tween(260, delayMillis = 90 + index * 55), label = "macro-idea-$index")
        Row(Modifier.fillMaxWidth().graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 18.dp.toPx() }.background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).clickable { onSelect(suggestion) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(suggestion.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${suggestion.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${suggestion.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${suggestion.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${suggestion.fat}g", FatColor) } }; Icon(Icons.Filled.AddCircle, "Registrar", tint = NutritionCoral) }
    } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } })
}
