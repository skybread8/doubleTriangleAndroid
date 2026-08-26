package io.codepassion.doubletriangle

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneId

/** Writes a user-confirmed profile weight only after Health Connect granted write access. */
internal object HealthConnectWeightSync {
    suspend fun publishManualWeight(context: Context, weightKg: Double): Boolean {
        if (!weightKg.isFinite() || weightKg !in 35.0..250.0) return false
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(WeightRecord::class) !in granted) return false
        val now = Instant.now()
        client.insertRecords(
            listOf(
                WeightRecord(
                    time = now,
                    zoneOffset = ZoneId.systemDefault().rules.getOffset(now),
                    weight = Mass.kilograms(weightKg),
                    metadata = Metadata.manualEntry(),
                ),
            ),
        )
        return true
    }

    suspend fun publishCompletedWorkout(context: Context, title: String, durationSeconds: Int): Boolean {
        if (durationSeconds <= 0 || HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted) return false
        val end = Instant.now()
        val start = end.minusSeconds(durationSeconds.toLong().coerceAtLeast(1))
        val offset = ZoneId.systemDefault().rules.getOffset(end)
        client.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = offset,
                    endTime = end,
                    endZoneOffset = offset,
                    metadata = Metadata.manualEntry(),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                    title = title.take(100),
                    notes = "Sesión registrada desde Wildforce.",
                ),
            ),
        )
        return true
    }
}
