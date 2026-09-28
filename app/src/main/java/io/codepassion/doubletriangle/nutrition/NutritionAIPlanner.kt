package io.codepassion.doubletriangle.nutrition

import android.content.Context
import io.codepassion.doubletriangle.BuildConfig
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

internal object NutritionAIPlanner {
    suspend fun generate(context: Context, profile: OnboardingProfile?, targets: NutritionTargets, preferences: NutritionPreferences, start: LocalDate): List<NutritionDayPlan> = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Configura OPENAI_API_KEY para generar el plan nutricional" }
        val prompt = """
            Genera un plan nutricional de 7 días para ${profile?.name?.ifBlank { "la persona" } ?: "la persona"}.
            Fecha inicial: $start. Objetivo: ${profile?.goal?.title ?: "fitness general"}. Peso: ${profile?.weightKg ?: 70} kg.
            Días de entrenamiento: ${profile?.workoutDays?.joinToString { it.storedValue } ?: "ninguno"}.
            Objetivos base: ${targets.calories} kcal, proteína ${targets.protein} g, carbohidratos ${targets.carbs} g, grasa ${targets.fat} g.
            Estilo alimentario: ${preferences.dietaryStyle}; comidas al día: ${preferences.mealsPerDay}; sugerencias: ${preferences.wantsSuggestions}.
            Excluidos/alergias: ${(preferences.excludedFoods + preferences.allergiesAndIntolerances + preferences.dislikes).joinToString().ifBlank { "ninguno" }}.
            Devuelve exclusivamente JSON válido con exactamente este formato:
            {"days":[{"date":"YYYY-MM-DD","dayType":"training|rest|recovery","energyDemand":"high|medium|low","workoutTitle":"","notes":"","preWorkoutGuidance":"","postWorkoutGuidance":"","targets":{"calories":0,"protein":0,"carbs":0,"fat":0},"meals":[{"title":"","type":"breakfast|lunch|dinner|snack","calories":0,"protein":0,"carbs":0,"fat":0,"guidance":"","foods":[{"name":"","grams":0}]}]}]}
            Todas las cadenas deben estar en español. Usa ${preferences.mealsPerDay.coerceIn(1, 4)} comidas por día cuando se soliciten sugerencias; si no, devuelve meals vacío. No incluyas alimentos excluidos. Los objetivos de cada día son restricciones deterministas de la aplicación: no los modifiques.
        """.trimIndent()
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_MODEL)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", "Eres un nutricionista experto. Responde solo JSON válido." )).put(JSONObject().put("role", "user").put("content", prompt)))
        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) { JSONObject(response).optJSONObject("error")?.optString("message") ?: "OpenAI devolvió HTTP ${connection.responseCode}" }
            val json = JSONObject(JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"))
            val deterministicPlan = NutritionStore.weekPlan(profile, targets, start, preferences, NutritionStore.loadPlanVersion(context))
            parse(json, deterministicPlan, preferences).also { NutritionStore.saveGeneratedPlan(context, it) }
        } finally { connection.disconnect() }
    }

    /**
     * The model supplies language and meal ideas only.  Daily macros, workout context and
     * demand always remain the locally computed values, as they do on iOS.
     */
    private fun parse(root: JSONObject, deterministicPlan: List<NutritionDayPlan>, preferences: NutritionPreferences): List<NutritionDayPlan> {
        val days = root.optJSONArray("days") ?: root.optJSONArray("week") ?: error("La IA no devolvió días")
        val parsed = buildMap {
            for (index in 0 until days.length()) {
                val value = days.optJSONObject(index) ?: continue
                val date = runCatching { LocalDate.parse(value.optString("date")) }.getOrNull()
                    ?: deterministicPlan.getOrNull(index)?.date
                    ?: continue
                val meals = if (preferences.wantsSuggestions) value.optJSONArray("meals")?.let { array -> buildList { for (mealIndex in 0 until array.length()) { val meal = array.optJSONObject(mealIndex) ?: continue; add(PlannedMeal(meal.optString("title").ifBlank { "Comida ${mealIndex + 1}" }, mealType(meal.optString("type")), NutritionTargets(meal.optInt("calories"), meal.optInt("protein"), meal.optInt("carbs"), meal.optInt("fat")), meal.optString("guidance"), meal.optJSONArray("foods").toFoods())) } } }.orEmpty().take(preferences.mealsPerDay.coerceIn(1, 4)) else emptyList()
                put(date, value to meals)
            }
        }
        return deterministicPlan.map { baseline ->
            val (aiDay, meals) = parsed[baseline.date] ?: return@map baseline
            baseline.copy(
                notes = aiDay.optString("notes").takeIf(String::isNotBlank) ?: baseline.notes,
                preWorkoutGuidance = aiDay.optString("preWorkoutGuidance").takeIf(String::isNotBlank) ?: baseline.preWorkoutGuidance,
                postWorkoutGuidance = aiDay.optString("postWorkoutGuidance").takeIf(String::isNotBlank) ?: baseline.postWorkoutGuidance,
                meals = meals,
            )
        }
    }
    private fun JSONArray?.toFoods(): List<Pair<String, Int>> = this?.let { array -> buildList { for (index in 0 until array.length()) { val food = array.optJSONObject(index) ?: continue; val name = food.optString("name").trim(); if (name.isNotBlank()) add(name to food.optInt("grams").coerceAtLeast(0)) } } } ?: emptyList()
    private fun mealType(value: String): MealType = when (value.trim().lowercase()) { "breakfast", "desayuno" -> MealType.Breakfast; "lunch", "comida" -> MealType.Lunch; "dinner", "cena" -> MealType.Dinner; else -> MealType.Snack }
}
