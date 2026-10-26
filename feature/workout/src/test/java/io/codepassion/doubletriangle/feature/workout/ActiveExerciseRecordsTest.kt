package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActiveExerciseRecordsTest {
    @Test
    fun `bodyweight records fall back to reps like iOS`() {
        val exercise = ExerciseSummary("Bicycle Crunch", "bicycleCrunch", 3, "12", 30)
        val records = activeExerciseRecords(
            exercise,
            listOf(
                ExerciseHistoryEntry(20L, 3, 36, 0.0, 0.0),
                ExerciseHistoryEntry(10L, 3, 30, 0.0, 0.0),
            ),
            useImperial = false,
        )

        assertNotNull(records)
        assertEquals("36 reps", records?.pr)
        assertEquals("36 reps", records?.lastBest)
    }

    @Test
    fun `cardio records use the fastest pace`() {
        val exercise = ExerciseSummary(
            "Carrera exterior",
            "outdoorRunning",
            1,
            "600 s",
            0,
            trackingMode = ExerciseTrackingMode.DurationAndDistance,
        )
        val records = activeExerciseRecords(
            exercise,
            listOf(ExerciseHistoryEntry(20L, 1, 0, 0.0, 0.0, durationSeconds = 600, distanceKm = 2.0)),
            useImperial = false,
        )

        assertEquals("5:00 min/km", records?.pr)
    }

    @Test
    fun `hiit and mobility records use their duration units`() {
        val hiit = activeExerciseRecords(
            ExerciseSummary("Burpee", "burpee", 1, "30 s", 0, trackingMode = ExerciseTrackingMode.Duration),
            listOf(ExerciseHistoryEntry(20L, 1, 0, 0.0, 0.0, durationSeconds = 45)),
            useImperial = false,
        )
        val mobility = activeExerciseRecords(
            ExerciseSummary("Estiramiento", "hipFlexorStretch", 1, "2 min", 0, trackingMode = ExerciseTrackingMode.Duration),
            listOf(ExerciseHistoryEntry(20L, 1, 0, 0.0, 0.0, durationSeconds = 120)),
            useImperial = false,
        )

        assertEquals("45 sec", hiit?.pr)
        assertEquals("2 min", mobility?.pr)
    }
}
