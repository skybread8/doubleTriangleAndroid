package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

internal data class NutritionEnergyPoint(val activeCalories: Double?, val totalCalories: Double?, val eatenCalories: Int)

internal fun nutritionEnergyWindow(today: LocalDate, days: Int): List<LocalDate> {
    val count = days.coerceAtLeast(0)
    return (0 until count).map { today.minusDays((count - 1 - it).toLong()) }
}

internal object NutritionEnergyBalance {
    suspend fun read(context: Context, eatenFor: (LocalDate) -> Int, days: Int = 15): Map<LocalDate, NutritionEnergyPoint> {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return emptyMap()
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val canReadActive = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        val canReadTotal = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted
        if (!canReadActive && !canReadTotal) return emptyMap()
        val zone = ZoneId.systemDefault()
        // Use calendar-day boundaries like HealthKit/iOS instead of subtracting
        // a fixed number of seconds (which drifts across DST transitions).
        val today = LocalDate.now(zone)
        val start = today.minusDays((days - 1).coerceAtLeast(0).toLong()).atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val active = if (canReadActive) client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records else emptyList()
        val total = if (canReadTotal) client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records else emptyList()
        // iOS builds a complete 15-day window and keeps days with food intake
        // even when Health Connect has no burned-calorie record for that day.
        // Android previously only returned dates present in Health Connect,
        // which hid the balance card for users who had logged food only.
        val windowDates = nutritionEnergyWindow(today, days)
        val recordDates = active.map { it.startTime.atZone(zone).toLocalDate() } +
            total.map { it.startTime.atZone(zone).toLocalDate() }
        val dates = (windowDates + recordDates).toSet()
        return dates.associateWith { date ->
            NutritionEnergyPoint(
                activeCalories = active.filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.energy.inKilocalories },
                totalCalories = total.filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.energy.inKilocalories },
                eatenCalories = eatenFor(date),
            )
        }
    }
}
