package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType as HealthMealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.LocalTime
import java.time.ZoneId

/** Publishes the app's logged meals using stable client IDs, so edits replace the previous record. */
internal object HealthConnectNutritionSync {
    private const val PREFS = "wildforce_nutrition_health"
    private const val PENDING_KEY = "nutrition_sync_pending"

    suspend fun publish(context: Context, entries: List<MealLog>): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val result = runCatching {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return@runCatching false
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (HealthPermission.getWritePermission(NutritionRecord::class) !in granted) return@runCatching false
            val canReadNutrition = HealthPermission.getReadPermission(NutritionRecord::class) in granted
            val zone = ZoneId.systemDefault()
            val records = entries.map { entry ->
                val start = entry.loggedDate.atTime(timeFor(entry.type)).atZone(zone).toInstant()
                NutritionRecord(
                    startTime = start,
                    startZoneOffset = zone.rules.getOffset(start),
                    endTime = start.plusSeconds(60),
                    endZoneOffset = zone.rules.getOffset(start),
                    metadata = Metadata.manualEntry("wildforce-nutrition-${entry.id}"),
                    name = entry.name.take(100),
                    energy = Energy.kilocalories(entry.calories.toDouble().coerceAtLeast(0.0)),
                    protein = Mass.grams(entry.protein.toDouble().coerceAtLeast(0.0)),
                    totalCarbohydrate = Mass.grams(entry.carbs.toDouble().coerceAtLeast(0.0)),
                    totalFat = Mass.grams(entry.fat.toDouble().coerceAtLeast(0.0)),
                    mealType = entry.type.toHealthConnectMealType(),
                )
            }
            // Replace records previously written by this app before inserting
            // the new values. This keeps edits idempotent instead of creating
            // duplicate meals in Health Connect.
            if (canReadNutrition) {
                // Read the complete local sync window and remove every record
                // previously owned by Wildforce, including meals deleted locally.
                // Filtering only by current entry IDs would leave deleted meals
                // orphaned in Health Connect.
                // Keep an empty publish useful: deleting the last local meal
                // must also remove its Health Connect counterpart. A rolling
                // window covers records from previous app sessions safely.
                // Always include a rolling window around today. If an old meal is
                // deleted locally, it will not be present in `entries`; anchoring
                // the query only to the remaining rows would leave its Health
                // Connect record orphaned forever.
                val today = java.time.LocalDate.now()
                val firstDate = minOf(entries.minOfOrNull { it.loggedDate } ?: today, today.minusDays(365))
                val lastDate = maxOf(entries.maxOfOrNull { it.loggedDate } ?: today, today.plusDays(1))
                val startOfWindow = firstDate.atStartOfDay(zone).toInstant()
                val endOfWindow = lastDate.plusDays(1).atStartOfDay(zone).toInstant()
                val existing = client.readRecords(
                    ReadRecordsRequest(
                        NutritionRecord::class,
                        TimeRangeFilter.between(startOfWindow, endOfWindow),
                    ),
                ).records
                val ids = existing.filter { record ->
                    record.metadata.clientRecordId?.startsWith("wildforce-nutrition-") == true
                }.map { it.metadata.id }
                if (ids.isNotEmpty()) client.deleteRecords(NutritionRecord::class, ids, emptyList())
            }
            if (records.isNotEmpty()) client.insertRecords(records)
            true
        }.getOrDefault(false)
        prefs.edit().putBoolean(PENDING_KEY, !result).apply()
        return result
    }

    fun hasPendingSync(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PENDING_KEY, false)

    private fun timeFor(type: io.codepassion.doubletriangle.nutrition.MealType): LocalTime = when (type) {
        io.codepassion.doubletriangle.nutrition.MealType.Breakfast -> LocalTime.of(8, 0)
        io.codepassion.doubletriangle.nutrition.MealType.Lunch -> LocalTime.of(13, 0)
        io.codepassion.doubletriangle.nutrition.MealType.Dinner -> LocalTime.of(20, 0)
        else -> LocalTime.of(16, 0)
    }

    private fun io.codepassion.doubletriangle.nutrition.MealType.toHealthConnectMealType(): Int = when (this) {
        io.codepassion.doubletriangle.nutrition.MealType.Breakfast -> HealthMealType.MEAL_TYPE_BREAKFAST
        io.codepassion.doubletriangle.nutrition.MealType.Lunch -> HealthMealType.MEAL_TYPE_LUNCH
        io.codepassion.doubletriangle.nutrition.MealType.Dinner -> HealthMealType.MEAL_TYPE_DINNER
        io.codepassion.doubletriangle.nutrition.MealType.Snack -> HealthMealType.MEAL_TYPE_SNACK
    }
}
