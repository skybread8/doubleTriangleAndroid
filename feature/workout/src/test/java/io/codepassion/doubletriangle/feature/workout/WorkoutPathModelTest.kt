package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.executionExercises
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPathModelTest {
    @Test
    fun `rest timer next exercise keeps the typed series reps and weight prescription`() {
        val exercise = ExerciseSummary(
            name = "Press de banca",
            sets = 4,
            reps = "8-10",
            restSeconds = 90,
            targetWeightsKg = listOf(50.0, 55.0),
            trackingMode = ExerciseTrackingMode.Repetitions,
        )

        val prescription = restTimerExercisePrescription(exercise, useImperial = false)
        assertEquals(listOf("4 series", "8-10 reps"), prescription.take(2))
        assertTrue(prescription.last().endsWith(" kg"))
    }

    @Test
    fun `rest timer next exercise shows duration and distance instead of repetitions`() {
        val exercise = ExerciseSummary(
            name = "Correr",
            sets = 1,
            reps = "—",
            restSeconds = 60,
            targetDurationMinutes = 20,
            targetDistanceKm = 3.5,
            trackingMode = ExerciseTrackingMode.DurationAndDistance,
        )

        assertEquals(listOf("1 serie", "20 min", "3.5 km"), restTimerExercisePrescription(exercise, useImperial = false))
    }

    @Test
    fun `superset rounds share one visual row per exercise`() {
        val blocks = listOf(
            WorkoutBlockSummary(WorkoutBlockType.Warmup, exercises = listOf(exercise("Movilidad"))),
            WorkoutBlockSummary(WorkoutBlockType.Superset, rounds = 3, notes = "Alterna sin descanso", restAfterBlockSeconds = 75, exercises = listOf(exercise("Press"), exercise("Remo"))),
            WorkoutBlockSummary(WorkoutBlockType.Cooldown, exercises = listOf(exercise("Respiración"))),
        )
        val workout = WorkoutDaySummary("test", 1, "Test", "Full body", "Fuerza", DayOfWeek.MONDAY, 30, WorkoutStatus.Planned, blocks.executionExercises(), blocks)

        val path = workout.pathBlocks()

        assertEquals(listOf("Movilidad", "Press", "Remo", "Respiración"), path.flatMap { it.exercises }.map { it.exercise.name })
        assertEquals(listOf(1, 3, 5), path[1].exercises[0].executionIndices)
        assertEquals(listOf(2, 4, 6), path[1].exercises[1].executionIndices)
        assertEquals(listOf("Press", "Remo"), path.mainExercises().map { it.exercise.name })
        assertEquals("Alterna sin descanso", path[1].notes)
        assertEquals(75, path[1].restAfterBlockSeconds)
        assertEquals(false, path[1].exercises[0].isCompleted(workout, mapOf(1 to 1, 3 to 1)))
        assertEquals(true, path[1].exercises[0].isCompleted(workout, mapOf(1 to 1, 3 to 1, 5 to 1)))
        assertEquals(false, path[1].exercises[0].isCompleted(workout, mapOf(1 to 1, 3 to 1, 5 to 1), mapOf(5 to 1)))
    }

    private fun exercise(name: String) = ExerciseSummary(name, sets = 1, reps = "10", restSeconds = 0)
}
