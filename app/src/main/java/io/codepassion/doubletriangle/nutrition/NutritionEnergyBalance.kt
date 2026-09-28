package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class NutritionEnergyPoint(val activeCalories: Double?, val totalCalories: Double?, val eatenCalories: Int)

internal object NutritionEnergyBalance {
    suspend fun read(context: Context, eatenFor: (LocalDate) -> Int, days: Int = 14): Map<LocalDate, NutritionEnergyPoint> {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return emptyMap()
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val canReadActive = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        val canReadTotal = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted
        if (!canReadActive && !canReadTotal) return emptyMap()
        val zone = ZoneId.systemDefault()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 86_400)
        val active = if (canReadActive) client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records else emptyList()
        val total = if (canReadTotal) client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))).records else emptyList()
        val dates = (active.map { it.startTime.atZone(zone).toLocalDate() } + total.map { it.startTime.atZone(zone).toLocalDate() }).toSet()
        return dates.associateWith { date ->
            NutritionEnergyPoint(
                activeCalories = active.filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.energy.inKilocalories },
                totalCalories = total.filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.energy.inKilocalories },
                eatenCalories = eatenFor(date),
            )
        }
    }
}
