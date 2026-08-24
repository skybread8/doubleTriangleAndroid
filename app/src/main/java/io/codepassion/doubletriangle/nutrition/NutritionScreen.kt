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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.util.Locale

private const val DAILY_CALORIES = 2_350
private const val DAILY_PROTEIN = 165
private const val DAILY_CARBS = 250
private const val DAILY_FAT = 75

@Composable
fun NutritionScreen(contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var meals by remember { mutableStateOf(NutritionStore.load(context)) }
    var addingMeal by remember { mutableStateOf(false) }
    var mealName by remember { mutableStateOf("") }
    val calories = meals.sumOf { it.calories }
    val protein = meals.sumOf { it.protein }
    val carbs = meals.sumOf { it.carbs }
    val fat = meals.sumOf { it.fat }
    Box(Modifier.fillMaxSize().padding(contentPadding).liquidGlassBackground()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("NUTRICIÓN", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text("LUNES · HOY", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
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
                            Text("OBJETIVO DIARIO", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                            Text("$DAILY_CALORIES kcal", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                            Text(if (calories <= DAILY_CALORIES) "Te quedan ${DAILY_CALORIES - calories} kcal" else "Has superado el objetivo", style = MaterialTheme.typography.caption, color = if (calories <= DAILY_CALORIES) WildforceThemeTokens.accentGold else Color(0xFFC62828))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    MacroRow("PROTEÍNA", protein, DAILY_PROTEIN, Color(0xFFE2B93B))
                    MacroRow("CARBOHIDRATOS", carbs, DAILY_CARBS, Color(0xFF70A8DA))
                    MacroRow("GRASAS", fat, DAILY_FAT, Color(0xFFB88BD8))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("COMIDAS DE HOY", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Text("+ AÑADIR", Modifier.clickable { addingMeal = true }.padding(8.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                }
            }
            if (meals.isEmpty()) item { EmptyMealsCard { addingMeal = true } }
            items(meals, key = { it.id }) { meal ->
                Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◉", color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.h6)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(meal.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Text("${meal.protein}g proteína · ${meal.carbs}g carbos · ${meal.fat}g grasa", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${meal.calories} kcal", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Text("ELIMINAR", Modifier.clickable { meals = meals.filterNot { it.id == meal.id }; NutritionStore.save(context, meals) }.padding(top = 3.dp), style = MaterialTheme.typography.overline, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
    if (addingMeal) {
        AlertDialog(
            onDismissRequest = { addingMeal = false },
            title = { Text("AÑADIR COMIDA", fontFamily = AntonFontFamily) },
            text = { TextField(mealName, { mealName = it.take(60) }, label = { Text("Descripción") }, singleLine = true, colors = TextFieldDefaults.textFieldColors(focusedIndicatorColor = WildforceThemeTokens.accentGold)) },
            confirmButton = {
                Button(onClick = {
                    if (mealName.isNotBlank()) {
                        val updated = meals + MealLog(System.currentTimeMillis(), mealName.trim(), 500, 30, 55, 18)
                        meals = updated; NutritionStore.save(context, updated); mealName = ""; addingMeal = false
                    }
                }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text("AÑADIR") }
            },
            dismissButton = { Button(onClick = { addingMeal = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = WildforceThemeTokens.textSecondary), elevation = ButtonDefaults.elevation(0.dp)) { Text("CANCELAR") } },
        )
    }
}

@Composable
private fun MacroRow(label: String, value: Int, target: Int, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row { Text(label, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary); Spacer(Modifier.weight(1f)); Text("${value} / ${target} g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.16f))) { Box(Modifier.fillMaxWidth((value.toFloat() / target).coerceIn(0f, 1f)).height(7.dp).clip(RoundedCornerShape(8.dp)).background(color)) }
    }
}

@Composable private fun EmptyMealsCard(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AÚN NO HAS REGISTRADO COMIDAS", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text("Añade una comida para empezar a ver tu progreso diario.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        Text("AÑADIR COMIDA", Modifier.clickable(onClick = onAdd).padding(top = 12.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable private fun NutritionPreview() = WildforceTheme { NutritionScreen() }
