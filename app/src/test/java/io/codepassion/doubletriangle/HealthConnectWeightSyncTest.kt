package io.codepassion.doubletriangle

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HealthConnectWeightSyncTest {
    @Test
    fun mapsIosWorkoutFocusToHealthConnectActivityType() {
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            exerciseTypeForFocus("cardio"),
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            exerciseTypeForFocus("mobility"),
        )
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            exerciseTypeForFocus("upperBody"),
        )
    }

    @Test
    fun usesPersistedCompletionTimeForHealthConnectWindow() {
        val completedAt = 1_720_000_000_000L
        val (start, end) = healthConnectWorkoutWindow(completedAt, 1_845)

        assertEquals(completedAt, end.toEpochMilli())
        assertEquals(1_719_998_155_000L, start.toEpochMilli())
    }

    @Test
    fun fallsBackToProvidedClockWhenCompletionTimeIsMissing() {
        val now = Instant.ofEpochMilli(1_730_000_000_000L)
        val (start, end) = healthConnectWorkoutWindow(null, 60, now)

        assertEquals(now.toEpochMilli(), end.toEpochMilli())
        assertEquals(1_729_999_940_000L, start.toEpochMilli())
    }
}
