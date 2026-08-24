package io.codepassion.doubletriangle.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.util.Locale
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun NutritionScreen(contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(), profile: io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val canEdit = selectedDate == LocalDate.now()
    var meals by remember(selectedDate) { mutableStateOf(NutritionStore.load(context, selectedDate)) }
    var addingMeal by remember { mutableStateOf(false) }
    var mealName by remember { mutableStateOf("") }
    var mealCalories by remember { mutableStateOf("") }
    var mealProtein by remember { mutableStateOf("") }
    var mealCarbs by remember { mutableStateOf("") }
    var mealFat by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.Snack) }
    var editingMeal by remember { mutableStateOf<MealLog?>(null) }
    var mealToDelete by remember { mutableStateOf<MealLog?>(null) }
    var targets by remember(profile) { mutableStateOf(NutritionStore.loadTargets(context, profile)) }
    var editingTargets by remember { mutableStateOf(false) }
    val calories = meals.sumOf { it.calories }
    val protein = meals.sumOf { it.protein }
    val carbs = meals.sumOf { it.carbs }
    val fat = meals.sumOf { it.fat }
    val macroCalories = protein * 4 + carbs * 4 + fat * 9
    val recentCalories = remember(selectedDate, meals) { (0..6).map { offset -> NutritionStore.load(context, selectedDate.minusDays(offset.toLong())).sumOf { it.calories } }.reversed() }
    Box(Modifier.fillMaxSize().padding(contentPadding).liquidGlassBackground()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("NUTRICIÓN", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", Modifier.clickable { selectedDate = selectedDate.minusDays(1) }.padding(end = 14.dp), style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
                    Text("${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).uppercase()}${if (canEdit) " · HOY" else " · ${selectedDate.dayOfMonth}/${selectedDate.monthValue}"}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("›", Modifier.clickable(enabled = !canEdit) { selectedDate = selectedDate.plusDays(1) }.padding(horizontal = 10.dp), style = MaterialTheme.typography.h5, color = if (canEdit) WildforceThemeTokens.textSecondary.copy(alpha = .35f) else WildforceThemeTokens.accentGold)
                    if (!canEdit) Text("HOY", Modifier.clickable { selectedDate = LocalDate.now() }.padding(8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(126.dp).clip(CircleShape).background(WildforceThemeTokens.accentGold.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$calories", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                                Text("kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                        Column(Modifier.padding(start = 18.dp)) {
                            Text("OBJETIVO DIARIO", Modifier.clickable { editingTargets = true }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                            Text("${targets.calories} kcal", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                            Text(if (calories <= targets.calories) "Te quedan ${targets.calories - calories} kcal" else "Has superado el objetivo", style = MaterialTheme.typography.caption, color = if (calories <= targets.calories) WildforceThemeTokens.accentGold else Color(0xFFC62828))
                            Text("${macroCalories} kcal de macros", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    MacroRow("PROTEÍNA", protein, targets.protein, Color(0xFFE2B93B))
                    MacroRow("CARBOHIDRATOS", carbs, targets.carbs, Color(0xFF70A8DA))
                    MacroRow("GRASAS", fat, targets.fat, Color(0xFFB88BD8))
                }
            }
            item { NutritionWeekSummary(recentCalories, targets.calories) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("COMIDAS DE HOY", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                    Spacer(Modifier.weight(1f))
                    if (canEdit) Text("+ AÑADIR", Modifier.clickable { editingMeal = null; addingMeal = true }.padding(8.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                }
            }
            if (meals.isEmpty()) item { EmptyMealsCard(canEdit) { editingMeal = null; addingMeal = true } }
            MealType.entries.forEach { type ->
                val grouped = meals.filter { it.type == type }
                if (grouped.isNotEmpty()) {
                    item { Text(type.title.uppercase(), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary) }
                    items(grouped, key = { it.id }) { meal ->
                        MealRow(meal, canEdit, onDelete = { mealToDelete = meal }, onEdit = {
                            editingMeal = meal
                            mealName = meal.name; mealCalories = meal.calories.toString(); mealProtein = meal.protein.toString(); mealCarbs = meal.carbs.toString(); mealFat = meal.fat.toString(); mealType = meal.type
                            addingMeal = true
                        })
                    }
                }
            }
        }
    }
    if (addingMeal) {
        AlertDialog(
            onDismissRequest = { addingMeal = false; editingMeal = null },
            title = { Text(if (editingMeal == null) "AÑADIR COMIDA" else "EDITAR COMIDA", fontFamily = AntonFontFamily) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(mealName, { mealName = it.take(60) }, label = { Text("Descripción") }, singleLine = true, colors = TextFieldDefaults.textFieldColors(focusedIndicatorColor = WildforceThemeTokens.accentGold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionNumberField("kcal", mealCalories, { mealCalories = it.filter(Char::isDigit) }, Modifier.weight(1f))
                        NutritionNumberField("Proteína", mealProtein, { mealProtein = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    }
                    Text("MOMENTO DEL DÍA", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        MealType.entries.forEach { type -> Text(type.title, Modifier.weight(1f).liquidGlass(RoundedCornerShape(10.dp), emphasized = mealType == type).clickable { mealType = type }.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, color = if (mealType == type) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionNumberField("Carbos", mealCarbs, { mealCarbs = it.filter(Char::isDigit) }, Modifier.weight(1f))
                        NutritionNumberField("Grasas", mealFat, { mealFat = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (mealName.isNotBlank()) {
                        val draft = MealLog(editingMeal?.id ?: System.currentTimeMillis(), mealName.trim(), mealCalories.toIntOrNull()?.coerceIn(0, 8000) ?: 0, mealProtein.toIntOrNull()?.coerceIn(0, 500) ?: 0, mealCarbs.toIntOrNull()?.coerceIn(0, 1000) ?: 0, mealFat.toIntOrNull()?.coerceIn(0, 500) ?: 0, mealType)
                        val updated = if (editingMeal == null) meals + draft else meals.map { if (it.id == editingMeal?.id) draft else it }
                        meals = updated; NutritionStore.save(context, updated); mealName = ""; mealCalories = ""; mealProtein = ""; mealCarbs = ""; mealFat = ""; mealType = MealType.Snack; editingMeal = null; addingMeal = false
                    }
                }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text("AÑADIR") }
            },
            dismissButton = { Button(onClick = { addingMeal = false; editingMeal = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = WildforceThemeTokens.textSecondary), elevation = ButtonDefaults.elevation(0.dp)) { Text("CANCELAR") } },
        )
    }
    if (editingTargets) NutritionTargetsDialog(targets, onDismiss = { editingTargets = false }) { updated -> targets = updated; NutritionStore.saveTargets(context, updated); editingTargets = false }
    mealToDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("¿ELIMINAR COMIDA?", fontFamily = AntonFontFamily) },
            text = { Text("Se eliminará «${meal.name}» del registro de hoy.") },
            confirmButton = { androidx.compose.material.TextButton(onClick = { meals = meals.filterNot { it.id == meal.id }; NutritionStore.save(context, meals); mealToDelete = null }) { Text("ELIMINAR", color = Color(0xFFC62828)) } },
            dismissButton = { androidx.compose.material.TextButton(onClick = { mealToDelete = null }) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
        )
    }
}

@Composable
private fun NutritionWeekSummary(calories: List<Int>, target: Int) {
    val loggedDays = calories.count { it > 0 }
    val average = if (loggedDays > 0) calories.filter { it > 0 }.average().toInt() else 0
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ÚLTIMOS 7 DÍAS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
            Text("$loggedDays/7 registrados", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        if (loggedDays == 0) Text("Registra comidas para ver tu media semanal.", color = WildforceThemeTokens.textSecondary)
        else {
            Text("Media: $average kcal", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            val progress = if (target > 0) (average.toFloat() / target).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .14f))) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).background(WildforceThemeTokens.accentGold))
            }
        }
    }
}

@Composable
private fun NutritionTargetsDialog(initial: NutritionTargets, onDismiss: () -> Unit, onSave: (NutritionTargets) -> Unit) {
    var calories by remember { mutableStateOf(initial.calories.toString()) }
    var protein by remember { mutableStateOf(initial.protein.toString()) }
    var carbs by remember { mutableStateOf(initial.carbs.toString()) }
    var fat by remember { mutableStateOf(initial.fat.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("OBJETIVOS DIARIOS", fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { NutritionNumberField("Calorías", calories, { calories = it.filter(Char::isDigit) }, Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NutritionNumberField("Proteína", protein, { protein = it.filter(Char::isDigit) }, Modifier.weight(1f)); NutritionNumberField("Carbos", carbs, { carbs = it.filter(Char::isDigit) }, Modifier.weight(1f)); NutritionNumberField("Grasas", fat, { fat = it.filter(Char::isDigit) }, Modifier.weight(1f)) } } }, confirmButton = { Button(onClick = { onSave(NutritionTargets(calories.toIntOrNull()?.coerceIn(500, 8000) ?: initial.calories, protein.toIntOrNull()?.coerceIn(0, 500) ?: initial.protein, carbs.toIntOrNull()?.coerceIn(0, 1000) ?: initial.carbs, fat.toIntOrNull()?.coerceIn(0, 500) ?: initial.fat)) }) { Text("GUARDAR") } }, dismissButton = { Text("CANCELAR", Modifier.clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) })
}

@Composable
private fun MealRow(meal: MealLog, canEdit: Boolean, onDelete: () -> Unit, onEdit: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).clickable(enabled = canEdit, onClick = onEdit).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("◉", color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.h6)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(meal.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text("${meal.protein}g proteína · ${meal.carbs}g carbos · ${meal.fat}g grasa", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${meal.calories} kcal", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            if (canEdit) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("EDITAR", Modifier.padding(top = 3.dp), style = MaterialTheme.typography.overline, color = WildforceThemeTokens.accentGold)
                Text("ELIMINAR", Modifier.clickable(onClick = onDelete).padding(top = 3.dp), style = MaterialTheme.typography.overline, color = Color(0xFFC62828))
            }
        }
    }
}

@Composable
private fun NutritionNumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    TextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = TextFieldDefaults.textFieldColors(focusedIndicatorColor = WildforceThemeTokens.accentGold))
}

@Composable
private fun MacroRow(label: String, value: Int, target: Int, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row { Text(label, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary); Spacer(Modifier.weight(1f)); Text("${value} / ${target} g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) }
        val progress = if (target > 0) (value.toFloat() / target).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.16f))) { Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(RoundedCornerShape(8.dp)).background(color)) }
    }
}

@Composable private fun EmptyMealsCard(canAdd: Boolean, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AÚN NO HAS REGISTRADO COMIDAS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(if (canAdd) "Añade una comida para empezar a ver tu progreso diario." else "No hay comidas registradas para este día.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        if (canAdd) Text("AÑADIR COMIDA", Modifier.clickable(onClick = onAdd).padding(top = 12.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable private fun NutritionPreview() = WildforceTheme { NutritionScreen() }
