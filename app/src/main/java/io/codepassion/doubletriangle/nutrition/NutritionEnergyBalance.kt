package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.codepassion.doubletriangle.WorkoutPlanGenerator
import io.codepassion.doubletriangle.WorkoutPlanStateStore
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutStore
import io.codepassion.doubletriangle.feature.workout.WorkoutCompletionMetricsStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class NutritionEnergyPoint(val activeCalories: Double?, val totalCalories: Double?, val eatenCalories: Int)

/**
 * Returns the best burned-calorie value available for a point.
 *
 * A Health Connect total includes basal energy, while a local workout
 * fallback only contains active energy. The chart already supports both
 * shapes; the headline metrics must use the same fallback or the card would
 * show bars while still displaying “—” above them.
 */
internal fun burnedCaloriesOrNull(point: NutritionEnergyPoint): Double? =
    (point.totalCalories ?: point.activeCalories)?.takeIf { it > 0.0 }

/**
 * Data-space segments used by the red burned-calorie bars.
 *
 * The active segment is intentionally available without a total-calorie
 * provider: a completed local workout can produce it even when Health Connect
 * is disabled, unavailable, or has no record for that day.
 */
internal data class EnergyBalanceBarSegments(
    val totalCalories: Int,
    val standardCalories: Int,
    val gapCalories: Int,
    val activeCalories: Int,
)

internal fun energyBalanceBarSegments(point: NutritionEnergyPoint): EnergyBalanceBarSegments {
    val total = (point.totalCalories ?: point.activeCalories ?: 0.0).toInt().coerceAtLeast(0)
    val active = (point.activeCalories ?: 0.0).toInt().coerceAtLeast(0).coerceAtMost(total)
    val standard = if (point.totalCalories != null) (total - active).coerceAtLeast(0) else 0
    return EnergyBalanceBarSegments(
        totalCalories = total,
        standardCalories = standard,
        gapCalories = if (active > 40) 40 else 0,
        activeCalories = (active - 40).coerceAtLeast(0),
    )
}

internal val NutritionEnergyPoint.hasEnergyData: Boolean
    get() = (activeCalories ?: 0.0) > 0.0 || (totalCalories ?: 0.0) > 0.0 || eatenCalories > 0

internal fun nutritionEnergyWindow(today: LocalDate, days: Int): List<LocalDate> {
    val count = days.coerceAtLeast(0)
    return (0 until count).map { today.minusDays((count - 1 - it).toLong()) }
}

internal fun eatenOnlyNutritionEnergyHistory(
    today: LocalDate,
    days: Int,
    eatenFor: (LocalDate) -> Int,
): Map<LocalDate, NutritionEnergyPoint> =
    nutritionEnergyWindow(today, days).associateWith { date ->
        NutritionEnergyPoint(activeCalories = null, totalCalories = null, eatenCalories = eatenFor(date))
    }

internal fun localWorkoutNutritionEnergyHistory(
    today: LocalDate,
    days: Int,
    eatenFor: (LocalDate) -> Int,
    activeFor: (LocalDate) -> Double?,
): Map<LocalDate, NutritionEnergyPoint> =
    nutritionEnergyWindow(today, days).associateWith { date ->
        NutritionEnergyPoint(
            activeCalories = activeFor(date)?.takeIf { it > 0.0 },
            totalCalories = null,
            eatenCalories = eatenFor(date),
        )
    }

internal fun energyCaloriesOrNull(values: Iterable<Double>): Double? {
    val collected = values.toList()
    return collected.takeIf { it.isNotEmpty() }?.sum()
}

internal fun preferredActiveCalories(healthConnectValues: Iterable<Double>, localEstimate: Double?): Double? =
    energyCaloriesOrNull(healthConnectValues)?.takeIf { it > 0.0 } ?: localEstimate?.takeIf { it > 0.0 }

internal data class LocalNutritionWorkoutCalories(
    val id: String,
    val date: LocalDate,
    val calories: Double,
)

internal fun deduplicatedLocalWorkoutCalories(
    workouts: Iterable<LocalNutritionWorkoutCalories>,
): Map<LocalDate, Double> {
    val seenWorkoutIds = mutableSetOf<String>()
    return workouts
        .filter { workout ->
            workout.id.isNotBlank() &&
                workout.calories > 0.0 &&
                seenWorkoutIds.add(workout.id)
        }
        .groupBy(LocalNutritionWorkoutCalories::date)
        .mapValues { (_, workoutsForDate) -> workoutsForDate.sumOf { it.calories } }
}

private fun localPersistedWorkoutCaloriesByDate(context: Context, zone: ZoneId): Map<LocalDate, Double> {
    val profilePreferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
    val planStore = WorkoutPlanStateStore(profilePreferences)
    val rawPlans = buildList {
        planStore.currentPlanJson()?.let(::add)
        planStore.archivedPlans().forEach { add(it.rawJson) }
    }
    val persisted = mutableListOf<LocalNutritionWorkoutCalories>()

    fun addWorkout(id: String, completedAt: String?, status: WorkoutStatus, activeCalories: Double?) {
        if (status != WorkoutStatus.Completed || activeCalories == null || activeCalories <= 0.0) return
        val date = completedAt
            ?.let { runCatching { Instant.parse(it).atZone(zone).toLocalDate() }.getOrNull() }
            ?: return
        persisted += LocalNutritionWorkoutCalories(id = id, date = date, calories = activeCalories)
    }

    rawPlans.forEach { rawPlan ->
        runCatching { WorkoutPlanGenerator.parse(rawPlan, "", "").workouts }
            .getOrNull()
            .orEmpty()
            .forEach { workout -> addWorkout(workout.id, workout.completedAt, workout.status, workout.activeCaloriesBurned) }
    }
    CustomWorkoutStore.load(context).forEach { workout ->
        addWorkout(workout.id, workout.completedAt, workout.status, workout.activeCaloriesBurned)
    }
    // The active and archived plan stores can contain the same completed
    // workout while a plan transition is being synchronized. Count the
    // session once, just as iOS's repository de-duplicates by identity.
    return deduplicatedLocalWorkoutCalories(persisted)
}

internal object NutritionEnergyBalance {
    suspend fun read(context: Context, eatenFor: (LocalDate) -> Int, days: Int = 15): Map<LocalDate, NutritionEnergyPoint> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // iOS always constructs the complete window and keeps intake-only days;
        // HealthKit contributes burned calories when available, but is not a
        // prerequisite for showing the energy-balance card.
        val windowDates = nutritionEnergyWindow(today, days)
        val persistedActiveByDate = localPersistedWorkoutCaloriesByDate(context, zone)
        val completionActiveByDate = WorkoutCompletionMetricsStore.loadAll(context)
            .groupBy { Instant.ofEpochMilli(it.completedAtMillis).atZone(zone).toLocalDate() }
            .mapValues { (_, metrics) -> metrics.sumOf { it.activeCaloriesBurned }.takeIf { it > 0.0 } }
        // A completion metric is the most precise local value. Synced or
        // archived workout days fill only dates where no completion metric is
        // available, so the same session is never counted twice.
        val localActiveByDate = buildMap {
            putAll(persistedActiveByDate)
            completionActiveByDate.forEach { (date, calories) ->
                if (calories != null) put(date, calories)
            }
        }
        fun localWorkoutHistory(): Map<LocalDate, NutritionEnergyPoint> =
            localWorkoutNutritionEnergyHistory(today, days, eatenFor) { date -> localActiveByDate[date] }

        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return localWorkoutHistory()
        val client = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
            ?: return localWorkoutHistory()
        val granted = runCatching { client.permissionController.getGrantedPermissions() }
            .getOrElse { return localWorkoutHistory() }
        val canReadActive = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        val canReadTotal = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted
        if (!canReadActive && !canReadTotal) return localWorkoutHistory()
        // Use calendar-day boundaries like HealthKit/iOS instead of subtracting
        // a fixed number of seconds (which drifts across DST transitions).
        val start = today.minusDays((days - 1).coerceAtLeast(0).toLong()).atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val active = if (canReadActive) {
            runCatching { client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records }.getOrDefault(emptyList())
        } else emptyList()
        val total = if (canReadTotal) {
            runCatching { client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records }.getOrDefault(emptyList())
        } else emptyList()
        // iOS builds a complete 15-day window and keeps days with food intake
        // even when Health Connect has no burned-calorie record for that day.
        // Android previously only returned dates present in Health Connect,
        // which hid the balance card for users who had logged food only.
        val recordDates = active.map { it.startTime.atZone(zone).toLocalDate() } +
            total.map { it.startTime.atZone(zone).toLocalDate() }
        val dates = (windowDates + recordDates).toSet()
        return dates.associateWith { date ->
            val activeForDate = active
                .filter { it.startTime.atZone(zone).toLocalDate() == date }
                .map { it.energy.inKilocalories }
            val totalForDate = total
                .filter { it.startTime.atZone(zone).toLocalDate() == date }
                .map { it.energy.inKilocalories }
            val healthConnectTotal = energyCaloriesOrNull(totalForDate)?.takeIf { it > 0.0 }
            NutritionEnergyPoint(
                // A completed Android workout already has the same local
                // active-calorie estimate used by the workout completion UI.
                // Use it when Health Connect has no active record for the day;
                // the chart must not disappear solely because Health Connect
                // is unavailable or empty.
                // A zero-valued provider record is not useful activity data;
                // keep the local workout estimate available in that case too.
                activeCalories = preferredActiveCalories(activeForDate, localActiveByDate[date]),
                totalCalories = healthConnectTotal,
                eatenCalories = eatenFor(date),
            )
        }
    }
}
