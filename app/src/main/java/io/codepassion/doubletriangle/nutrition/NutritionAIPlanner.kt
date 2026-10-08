package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.codepassion.wildforce.android.BuildConfig
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal object NutritionAIPlanner {
    suspend fun generate(context: Context, profile: OnboardingProfile?, targets: NutritionTargets, preferences: NutritionPreferences, start: LocalDate): List<NutritionDayPlan> = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para generar el plan nutricional")
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition-plans/generate")).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 180_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Idempotency-Key", UUID.randomUUID().toString())
            val dailyEnergy = recentDailyEnergy(context)
            val requestBody = JSONObject().apply {
                if (dailyEnergy.length() > 0) put("daily_energy", dailyEnergy)
            }
            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode in 500..599) {
                return@withContext generateLocally(context, profile, targets, preferences, start)
            }
            check(connection.responseCode in 200..299) {
                if (connection.responseCode == 429) "Demasiados intentos. Espera un minuto antes de volver a generar el plan."
                else JSONObject(response).optString("message").ifBlank { "El servidor devolvió HTTP ${connection.responseCode}" }
            }
            val plan = JSONObject(response).getJSONObject("data")
            val generated = remoteDays(plan.optJSONArray("days") ?: JSONArray())
            NutritionStore.rememberRemotePlanId(context, plan.optString("id"))
            // The endpoint can successfully persist the plan shell while the
            // upstream AI returns no days. iOS tolerates that response, but an
            // empty plan is not useful in Android. Fill that exact remote plan
            // with the local parity fallback on the next queued sync instead
            // of showing an error or creating a second plan.
            if (generated.isEmpty()) {
                return@withContext generateLocally(context, profile, targets, preferences, start)
            }
            generated
        } finally { connection.disconnect() }
    }

    /**
     * Mirrors iOS's 21-day Apple Health payload.  Health Connect exposes total
     * energy rather than basal energy, so basal is derived only when both
     * record types are available.  Missing Health permissions deliberately
     * result in no payload and let the backend use its formula estimate.
     */
    private suspend fun recentDailyEnergy(context: Context): JSONArray {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return JSONArray()
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val canReadActive = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        val canReadTotal = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted
        if (!canReadActive && !canReadTotal) return JSONArray()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.minusDays(20).atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val active = if (canReadActive) client.readRecords(
            ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end)),
        ).records else emptyList()
        val total = if (canReadTotal) client.readRecords(
            ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end)),
        ).records else emptyList()

        return JSONArray().apply {
            for (offset in 20 downTo 0) {
                val date = today.minusDays(offset.toLong())
                val activeCalories = active.filter { it.startTime.atZone(zone).toLocalDate() == date }
                    .sumOf { it.energy.inKilocalories }
                val totalCalories = total.filter { it.startTime.atZone(zone).toLocalDate() == date }
                    .sumOf { it.energy.inKilocalories }
                // Do not invent zeroes for a day with no measured data.
                if (activeCalories > 0.0 || totalCalories > 0.0) {
                    put(JSONObject().put("date", date.toString()).put("active_calories", activeCalories).apply {
                        if (totalCalories > 0.0) put("basal_calories", (totalCalories - activeCalories).coerceAtLeast(0.0))
                    })
                }
            }
        }
    }

    /** Temporary parity fallback: iOS keeps a local OpenAI planner for server outages. */
    private fun generateLocally(context: Context, profile: OnboardingProfile?, targets: NutritionTargets, preferences: NutritionPreferences, start: LocalDate): List<NutritionDayPlan> {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "No hay una clave local de IA configurada." }
        val prompt = """
            Genera un plan nutricional de 7 días para ${profile?.name?.ifBlank { "la persona" } ?: "la persona"}.
            Fecha inicial: $start. Objetivo: ${profile?.goal?.title ?: "fitness general"}. Peso: ${profile?.weightKg ?: 70} kg.
            Días de entrenamiento: ${profile?.workoutDays?.joinToString { it.storedValue } ?: "ninguno"}.
            Objetivos base: ${targets.calories} kcal, proteína ${targets.protein} g, carbohidratos ${targets.carbs} g, grasa ${targets.fat} g.
            Estilo alimentario: ${preferences.dietaryStyle}; comidas al día: ${preferences.mealsPerDay}; sugerencias: ${preferences.wantsSuggestions}.
            Excluidos/alergias: ${(preferences.excludedFoods + preferences.allergiesAndIntolerances + preferences.dislikes).joinToString().ifBlank { "ninguno" }}.
            Devuelve exclusivamente JSON válido con exactamente este formato:
            {"days":[{"date":"YYYY-MM-DD","notes":"","preWorkoutGuidance":"","postWorkoutGuidance":"","meals":[{"title":"","type":"breakfast|lunch|dinner|snack","calories":0,"protein":0,"carbs":0,"fat":0,"guidance":"","foods":[{"name":"","grams":0}]}]}]}
            Todas las cadenas deben estar en español. No incluyas alimentos excluidos.
        """.trimIndent()
        val body = JSONObject().put("model", BuildConfig.OPENAI_MODEL).put("response_format", JSONObject().put("type", "json_object"))
            .put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", "Eres un nutricionista experto. Responde solo JSON válido.")).put(JSONObject().put("role", "user").put("content", prompt)))
        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.doOutput = true; connection.connectTimeout = 30_000; connection.readTimeout = 120_000
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) { JSONObject(response).optJSONObject("error")?.optString("message") ?: "La IA devolvió HTTP ${connection.responseCode}" }
            val json = JSONObject(JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"))
            val deterministicPlan = NutritionStore.weekPlan(profile, targets, start, preferences, NutritionStore.loadPlanVersion(context))
            return parse(json, deterministicPlan, preferences)
        } finally { connection.disconnect() }
    }

    private fun remoteDays(days: JSONArray): List<NutritionDayPlan> = buildList {
        for (index in 0 until days.length()) {
            val day = days.optJSONObject(index) ?: continue
            val date = runCatching { LocalDate.parse(day.optString("date")) }.getOrNull() ?: continue
            val meals = day.optJSONArray("meals")?.let { values -> buildList {
                for (mealIndex in 0 until values.length()) {
                    val meal = values.optJSONObject(mealIndex) ?: continue
                    add(PlannedMeal(meal.optString("title", "Comida ${mealIndex + 1}"), mealType(meal.optString("meal_type")),
                        NutritionTargets(meal.optDouble("target_calories").toInt(), meal.optDouble("target_protein_grams").toInt(), meal.optDouble("target_carbs_grams").toInt(), meal.optDouble("target_fat_grams").toInt()),
                        meal.optString("guidance"), meal.optJSONArray("example_foods").toFoods()))
                }
            } }.orEmpty()
            add(NutritionDayPlan(date, nutritionDayType(day.optString("day_type")), energyDemand(day.optString("energy_demand")),
                NutritionTargets(day.optDouble("target_calories").toInt(), day.optDouble("target_protein_grams").toInt(), day.optDouble("target_carbs_grams").toInt(), day.optDouble("target_fat_grams").toInt()),
                day.optString("planned_workout_title"), day.optString("notes"), day.optString("pre_workout_guidance").takeIf(String::isNotBlank), day.optString("post_workout_guidance").takeIf(String::isNotBlank), meals))
        }
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
    private fun nutritionDayType(value: String): NutritionDayType = when (value.trim().lowercase()) { "training" -> NutritionDayType.Training; "recovery" -> NutritionDayType.Recovery; else -> NutritionDayType.Rest }
    private fun energyDemand(value: String): EnergyDemand = when (value.trim().lowercase()) { "high" -> EnergyDemand.High; "medium" -> EnergyDemand.Medium; else -> EnergyDemand.Low }
}
