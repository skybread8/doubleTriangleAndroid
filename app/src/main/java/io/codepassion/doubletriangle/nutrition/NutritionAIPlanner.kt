package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.roundToLong

internal object NutritionAIPlanner {
    suspend fun generate(context: Context): List<NutritionDayPlan> = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para generar el plan nutricional")
        val idempotencyKey = UUID.randomUUID().toString()
        val dailyEnergy = recentDailyEnergy(context)
        val requestBody = JSONObject().apply {
            if (dailyEnergy.length() > 0) put("daily_energy", dailyEnergy)
        }.toString()
        while (true) {
            val connection = URL(WildforceApiEnvironment.apiUrl("nutrition-plans/generate")).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 180_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Idempotency-Key", idempotencyKey)
                connection.outputStream.use { it.write(requestBody.toByteArray()) }
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val retryAfter = connection.getHeaderField("Retry-After")?.toDoubleOrNull()
                if (status == 429 && retryAfter != null && retryAfter >= 0) {
                    delay((retryAfter * 1_000).roundToLong())
                    continue
                }
                check(status in 200..299) {
                    JSONObject(response).optString("message").ifBlank { "El servidor devolvió HTTP $status" }
                }
                val plan = JSONObject(response).getJSONObject("data")
                val generated = remoteDays(plan.optJSONArray("days") ?: JSONArray())
                NutritionStore.rememberRemotePlanId(context, plan.optString("id"))
                check(generated.isNotEmpty()) { "El generador no devolvió ningún día del plan." }
                return@withContext generated
            } finally { connection.disconnect() }
        }
        error("La generación nutricional terminó sin respuesta.")
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

    private fun JSONArray?.toFoods(): List<Pair<String, Int>> = this?.let { array ->
        buildList {
            for (index in 0 until array.length()) {
                val food = array.optJSONObject(index) ?: continue
                val name = food.optString("name").trim()
                if (name.isNotBlank()) add(name to food.optInt("grams").coerceAtLeast(0))
            }
        }
    } ?: emptyList()

    private fun mealType(value: String): MealType = when (value.trim().lowercase()) { "breakfast", "desayuno" -> MealType.Breakfast; "lunch", "comida" -> MealType.Lunch; "dinner", "cena" -> MealType.Dinner; else -> MealType.Snack }
    private fun nutritionDayType(value: String): NutritionDayType = when (value.trim().lowercase()) { "training" -> NutritionDayType.Training; "recovery" -> NutritionDayType.Recovery; else -> NutritionDayType.Rest }
    private fun energyDemand(value: String): EnergyDemand = when (value.trim().lowercase()) { "high" -> EnergyDemand.High; "medium" -> EnergyDemand.Medium; else -> EnergyDemand.Low }
}

private fun JSONArray?.toFoods(): List<Pair<String, Double>> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = opt(index)
            when (value) {
                is JSONObject -> add(value.optString("name").trim() to value.optDouble("grams", 0.0).coerceAtLeast(0.0))
                is String -> value.trim().takeIf { it.isNotBlank() }?.let { add(it to 0.0) }
            }
        }
    }
}
