package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.codepassion.doubletriangle.AsyncOperationClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal object NutritionAIPlanner {
    suspend fun generate(context: Context): List<NutritionDayPlan> = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para generar el plan nutricional")
        val idempotencyKey = UUID.randomUUID().toString()
        val dailyEnergy = recentDailyEnergy(context)
        val requestBody = JSONObject().apply {
            if (dailyEnergy.length() > 0) put("daily_energy", dailyEnergy)
        }.toString()
        val plan = AsyncOperationClient.execute(
            startPath = "nutrition/plans/generate",
            token = token,
            idempotencyKey = idempotencyKey,
            requestBody = requestBody,
        ) { it }
        val generated = remoteDays(plan.optJSONArray("days") ?: JSONArray())
        NutritionStore.rememberRemotePlanId(context, plan.optString("id"))
        check(generated.isNotEmpty()) { "El generador no devolvió ningún día del plan." }
        generated
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
            // The sync endpoint serializes dates as ISO-8601 instants (for
            // example, "2026-10-05T00:00:00.000000Z"). iOS decodes these as
            // Date values, so Android must retain the date component too.
            val date = parseRemoteNutritionDate(day.optString("date")) ?: continue
            val meals = day.optJSONArray("meals")?.let { values -> buildList {
                for (mealIndex in 0 until values.length()) {
                    val meal = values.optJSONObject(mealIndex) ?: continue
                    add(PlannedMeal(meal.cleanString("title", "Comida ${mealIndex + 1}"), mealType(meal.optString("meal_type")),
                        NutritionTargets(meal.optDouble("target_calories").toInt(), meal.optDouble("target_protein_grams").toInt(), meal.optDouble("target_carbs_grams").toInt(), meal.optDouble("target_fat_grams").toInt()),
                        meal.cleanString("guidance"), meal.optJSONArray("example_foods").toFoods()))
                }
            } }.orEmpty()
            add(NutritionDayPlan(date, nutritionDayType(day.optString("day_type")), energyDemand(day.optString("energy_demand")),
                NutritionTargets(day.optDouble("target_calories").toInt(), day.optDouble("target_protein_grams").toInt(), day.optDouble("target_carbs_grams").toInt(), day.optDouble("target_fat_grams").toInt()),
                day.cleanString("planned_workout_title"), day.cleanString("notes"), day.cleanString("pre_workout_guidance").takeIf(String::isNotBlank), day.cleanString("post_workout_guidance").takeIf(String::isNotBlank), meals))
        }
    }

    private fun JSONArray?.toFoods(): List<Pair<String, Int>> = this?.let { array ->
        buildList {
            for (index in 0 until array.length()) {
                val food = array.optJSONObject(index) ?: continue
                val name = food.optString("name").trim()
                if (name.isNotBlank()) {
                    add(name to food.optInt("amountGrams", food.optInt("grams")).coerceAtLeast(0))
                }
            }
        }
    } ?: emptyList()

    private fun mealType(value: String): MealType = when (value.trim().lowercase()) {
        "breakfast", "desayuno" -> MealType.Breakfast
        "lunch", "comida" -> MealType.Lunch
        "dinner", "cena" -> MealType.Dinner
        "preworkout", "pre_workout", "pre-workout", "pre workout", "pre-entreno" -> MealType.PreWorkout
        "postworkout", "post_workout", "post-workout", "post workout", "post-entreno" -> MealType.PostWorkout
        else -> MealType.Snack
    }
    private fun nutritionDayType(value: String): NutritionDayType = when (value.trim().lowercase()) { "training" -> NutritionDayType.Training; "recovery" -> NutritionDayType.Recovery; else -> NutritionDayType.Rest }
    private fun energyDemand(value: String): EnergyDemand = when (value.trim().lowercase()) { "high" -> EnergyDemand.High; "medium" -> EnergyDemand.Medium; else -> EnergyDemand.Low }
}

/** Mirrors iOS's ISO-8601 date decoding for nutrition sync payloads. */
internal fun parseRemoteNutritionDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim().substringBefore('T')) }.getOrNull()

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
