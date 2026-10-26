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
import java.time.LocalDate
import java.time.ZoneId

/** Publishes the same daily macro totals that iOS writes to HealthKit. */
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
            val records = aggregateNutritionByDay(entries).map { day ->
                val start = day.date.atStartOfDay(zone).toInstant()
                NutritionRecord(
                    startTime = start,
                    startZoneOffset = zone.rules.getOffset(start),
                    endTime = day.date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1),
                    endZoneOffset = zone.rules.getOffset(day.date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1)),
                    metadata = Metadata.manualEntry("wildforce-nutrition-${day.date}"),
                    name = "Wildforce",
                    energy = Energy.kilocalories(day.calories.toDouble().coerceAtLeast(0.0)),
                    protein = Mass.grams(day.protein.toDouble().coerceAtLeast(0.0)),
                    totalCarbohydrate = Mass.grams(day.carbs.toDouble().coerceAtLeast(0.0)),
                    totalFat = Mass.grams(day.fat.toDouble().coerceAtLeast(0.0)),
                    mealType = HealthMealType.MEAL_TYPE_UNKNOWN,
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
}

internal data class NutritionSyncDay(
    val date: LocalDate,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

/** The iOS HealthKit bridge publishes one aggregate sample for each calendar day. */
internal fun aggregateNutritionByDay(entries: List<MealLog>): List<NutritionSyncDay> =
    entries.groupBy { it.loggedDate }
        .map { (date, dayEntries) ->
            NutritionSyncDay(
                date = date,
                calories = dayEntries.sumOf { it.calories },
                protein = dayEntries.sumOf { it.protein },
                carbs = dayEntries.sumOf { it.carbs },
                fat = dayEntries.sumOf { it.fat },
            )
        }
        .sortedBy { it.date }
