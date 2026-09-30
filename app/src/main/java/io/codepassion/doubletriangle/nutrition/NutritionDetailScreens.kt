package io.codepassion.doubletriangle.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun NutritionDayDetail(
    day: NutritionDayPlan,
    entries: List<MealLog>,
    onBack: () -> Unit,
    onLog: () -> Unit,
    onPlannedMealAction: (PlannedMeal, LogMethod) -> Unit = { _, _ -> onLog() },
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
            item { Text("COMIDAS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary) }
            if (day.meals.isEmpty()) item { EmptyState("Sin sugerencias de comidas", "Activa las sugerencias en tu perfil nutricional para ver opciones para este día.") }
            else items(day.meals) { meal -> PlannedMealCard(meal, onPlannedMealAction) }
        }
    }
}

@Composable private fun TopBar(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp)) }; Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Filled.AddCircle, "Registrar", tint = NutritionCoral) } else Spacer(Modifier.width(48.dp)) } }

@Composable private fun MacroSummary(target: NutritionTargets) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroBadge("Calorías", target.calories, "kcal", NutritionCoral, NutritionMacroIcon.Calories, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MacroBadge("Proteína", target.protein, "g", ProteinColor, NutritionMacroIcon.Protein, Modifier.weight(1f)); MacroBadge("Carbos", target.carbs, "g", CarbsColor, NutritionMacroIcon.Carbs, Modifier.weight(1f)); MacroBadge("Grasa", target.fat, "g", FatColor, NutritionMacroIcon.Fat, Modifier.weight(1f)) }
    }
}

@Composable private fun MacroBadge(label: String, value: Int, unit: String, color: Color, icon: NutritionMacroIcon, modifier: Modifier) { Row(modifier.background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(36.dp).background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { NutritionMacroSymbol(icon, color, Modifier.size(20.dp)) }; Column(Modifier.padding(start = 7.dp)) { Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1); Text("$value $unit", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1) } } }

@Composable private fun Guidance(title: String, body: String, color: Color) { Column(Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = color); Text(body, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }

@Composable private fun PlannedMealCard(meal: PlannedMeal, onAction: (PlannedMeal, LogMethod) -> Unit) {
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
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Protein, "${meal.targets.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${meal.targets.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${meal.targets.fat}g", FatColor) }
        Text(meal.guidance, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { meal.foods.forEach { (food, grams) -> Text("$food (${grams}g)", Modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) } }
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

@Composable private fun EntryCard(entry: MealLog, onBookmark: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) { Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable(onClick = onEdit).padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Restaurant, null, tint = NutritionCoral); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${entry.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${entry.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${entry.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${entry.fat}g", FatColor) } }; IconButton(onClick = onBookmark) { Icon(if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Favorito", tint = if (entry.isBookmarked) NutritionCoral else WildforceThemeTokens.textSecondary) }; IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Eliminar", tint = WildforceThemeTokens.textSecondary) } }; if (entry.items.size > 1) { Text("${entry.items.size} alimentos", style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = NutritionCoral, modifier = Modifier.padding(top = 7.dp)); entry.items.forEach { food -> Text("• ${food.name} · ${food.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }; if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 7.dp)) } }

@Composable
internal fun NutritionTargetsDialog(initial: NutritionTargets, onDismiss: () -> Unit, onSave: (NutritionTargets) -> Unit) {
    var calories by remember { mutableStateOf(initial.calories.toString()) }; var protein by remember { mutableStateOf(initial.protein.toString()) }; var carbs by remember { mutableStateOf(initial.carbs.toString()) }; var fat by remember { mutableStateOf(initial.fat.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("OBJETIVOS DIARIOS", fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Calorías", calories, { calories = it.filter(Char::isDigit) }, Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Proteína", protein, { protein = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Carbos", carbs, { carbs = it.filter(Char::isDigit) }, Modifier.weight(1f)); NumberField("Grasa", fat, { fat = it.filter(Char::isDigit) }, Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { onSave(NutritionTargets(calories.toIntOrNull()?.coerceIn(500, 8000) ?: initial.calories, protein.toIntOrNull()?.coerceIn(0, 500) ?: initial.protein, carbs.toIntOrNull()?.coerceIn(0, 1000) ?: initial.carbs, fat.toIntOrNull()?.coerceIn(0, 500) ?: initial.fat)) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary)) { Text("GUARDAR", color = WildforceThemeTokens.backgroundSecondary) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } })
}

@Composable
internal fun NutritionPreferencesDialog(initial: NutritionPreferences, onDismiss: () -> Unit, onSave: (NutritionPreferences) -> Unit) {
    var style by remember { mutableStateOf(initial.dietaryStyle) }; var meals by remember { mutableStateOf(initial.mealsPerDay) }; var suggestions by remember { mutableStateOf(initial.wantsSuggestions) }
    val styles = listOf("Omnívora", "Vegetariana", "Vegana", "Pescetariana", "Mediterránea")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("PERFIL NUTRICIONAL", fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Estilo alimentario", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { styles.forEach { item -> Text(item, Modifier.background(if (style == item) NutritionCoral else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).clickable { style = item }.padding(horizontal = 10.dp, vertical = 7.dp), color = if (style == item) Color.White else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) } }; Row(verticalAlignment = Alignment.CenterVertically) { Text("Comidas al día", Modifier.weight(1f)); IconButton(onClick = { meals = (meals - 1).coerceAtLeast(2) }) { Icon(Icons.Filled.Remove, null) }; Text(meals.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { meals = (meals + 1).coerceAtMost(6) }) { Icon(Icons.Filled.Add, null) } }; Row(verticalAlignment = Alignment.CenterVertically) { Text("Sugerencias de comidas", Modifier.weight(1f)); Switch(suggestions, { suggestions = it }, colors = SwitchDefaults.colors(checkedThumbColor = NutritionCoral, checkedTrackColor = NutritionCoral)) } } }, confirmButton = { Button(onClick = { onSave(NutritionPreferences(style, meals, suggestions)) }, colors = ButtonDefaults.buttonColors(backgroundColor = NutritionCoral)) { Text("GUARDAR", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } })
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
                if (creating) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = NutritionCoral, strokeWidth = 2.dp); Text("Creando idea…", Modifier.padding(start = 9.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
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
                        Row(Modifier.fillMaxWidth().clickable { checked = if (isChecked) checked - name.lowercase() else checked + name.lowercase() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (isChecked) NutritionGreen else WildforceThemeTokens.textSecondary); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text("Cantidad total", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }; Text("${amount * persons}g", Modifier.background(NutritionCoral.copy(alpha = .1f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = NutritionCoral) }
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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("MACROS QUE FALTAN", fontFamily = AntonFontFamily) }, text = { if (met) EmptyState("¡Objetivos cumplidos!", "Ya has cubierto los macros del día.") else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MealMacro(NutritionMacroIcon.Calories, "${missing.calories} kcal", NutritionCoral); MealMacro(NutritionMacroIcon.Protein, "${missing.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${missing.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${missing.fat}g", FatColor) }; suggestions.forEach { suggestion -> Row(Modifier.fillMaxWidth().background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).clickable { onSelect(suggestion) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(suggestion.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${suggestion.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); MealMacro(NutritionMacroIcon.Protein, "${suggestion.protein}g", ProteinColor); MealMacro(NutritionMacroIcon.Carbs, "${suggestion.carbs}g", CarbsColor); MealMacro(NutritionMacroIcon.Fat, "${suggestion.fat}g", FatColor) } }; Icon(Icons.Filled.AddCircle, "Registrar", tint = NutritionCoral) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } })
}
