package io.codepassion.doubletriangle

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.percent
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

    /** Mirrors the HealthKit body-metric writes for metrics supported by Health Connect. */
    suspend fun publishBodyMetric(context: Context, kind: String, value: Double): Boolean {
        if (!value.isFinite() || HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        val permission = when (kind) {
            "BodyFat" -> HealthPermission.getWritePermission(BodyFatRecord::class)
            "LeanBodyMass" -> HealthPermission.getWritePermission(LeanBodyMassRecord::class)
            else -> return false
        }
        if (permission !in client.permissionController.getGrantedPermissions()) return false
        val now = Instant.now()
        val offset = ZoneId.systemDefault().rules.getOffset(now)
        val record = when (kind) {
            "BodyFat" -> BodyFatRecord(now, offset, value.percent, Metadata.manualEntry())
            "LeanBodyMass" -> LeanBodyMassRecord(now, offset, Mass.kilograms(value), Metadata.manualEntry())
            else -> return false
        }
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun publishCompletedWorkout(
        context: Context,
        workoutId: String,
        title: String,
        focus: String? = null,
        durationSeconds: Int,
        activeCalories: Double = 0.0,
        completedAtMillis: Long? = null,
    ): Boolean {
        if (durationSeconds <= 0 || HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted) return false
        val (start, end) = healthConnectWorkoutWindow(completedAtMillis, durationSeconds)
        val offset = ZoneId.systemDefault().rules.getOffset(end)
        val records = buildList {
            add(ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = offset,
                    endTime = end,
                    endZoneOffset = offset,
                    metadata = Metadata.manualEntry("wildforce-workout-$workoutId"),
                    exerciseType = exerciseTypeForFocus(focus),
                    title = title.take(100),
                    notes = "Sesión registrada desde Wildforce.",
                ))
            if (activeCalories.isFinite() && activeCalories > 0 && HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class) in granted) {
                add(ActiveCaloriesBurnedRecord(start, offset, end, offset, androidx.health.connect.client.units.Energy.kilocalories(activeCalories), Metadata.manualEntry("wildforce-workout-$workoutId-calories")))
            }
        }
        client.insertRecords(records)
        return true
    }
}

internal fun healthConnectWorkoutWindow(
    completedAtMillis: Long?,
    durationSeconds: Int,
    now: Instant = Instant.now(),
): Pair<Instant, Instant> {
    val end = completedAtMillis
        ?.takeIf { it > 0L }
        ?.let(Instant::ofEpochMilli)
        ?: now
    return end.minusSeconds(durationSeconds.toLong().coerceAtLeast(1)) to end
}

/** Maps the Android workout focus to the closest Health Connect activity type. */
internal fun exerciseTypeForFocus(focus: String?): Int = when (focus?.lowercase()) {
    // Health Connect's dependency version has no functional-strength constant;
    // strength training is its closest stable equivalent. HIIT is a different
    // activity type and does not match iOS's `.functionalStrengthTraining`.
    "cardio" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    "mobility", "recovery" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
    else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
}
