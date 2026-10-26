package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.executionExercises
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPathModelTest {
    @Test
    fun `mesocycle start date keeps the iOS month and day metadata`() {
        assertTrue(formatMesocycleStartDate("2026-10-19T00:00:00Z")?.contains("19") == true)
        assertEquals(null, formatMesocycleStartDate("not-a-date"))
        assertEquals(null, formatMesocycleStartDate(null))
    }

    @Test
    fun `scheduled workout date mirrors the iOS full date metadata`() {
        assertTrue(formatScheduledWorkoutDate("2026-10-19T00:00:00Z", "Español")?.contains("19") == true)
        assertEquals(null, formatScheduledWorkoutDate("not-a-date", "Español"))
    }

    @Test
    fun `weekly calendar marks the exact completed date, not every matching weekday`() {
        val base = PreviewWorkoutRepository.load()
        val completed = base.workouts.first().copy(
            status = WorkoutStatus.Completed,
            completedAt = Instant.parse("2026-10-19T12:00:00Z").toString(),
        )
        val state = base.copy(workouts = listOf(completed) + base.workouts.drop(1))

        assertTrue(isWorkoutCompletedOnDate(state, LocalDate.of(2026, 10, 19)))
        assertTrue(!isWorkoutCompletedOnDate(state, LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun `plan groups completed sessions after upcoming sessions like iOS`() {
        val base = PreviewWorkoutRepository.load()
        val completed = base.workouts.first().copy(status = WorkoutStatus.Completed)
        val planned = base.workouts.drop(1).first().copy(status = WorkoutStatus.Planned)

        val (upNext, completedDays) = groupWorkoutPlanDays(listOf(completed, planned))

        assertEquals(listOf(planned.id), upNext.map { it.id })
        assertEquals(listOf(completed.id), completedDays.map { it.id })
    }

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
    fun `workout path keeps duration and distance typed`() {
        val exercise = ExerciseSummary(
            name = "Correr",
            sets = 1,
            reps = "—",
            restSeconds = 30,
            targetDurationSeconds = 90,
            targetDistanceKm = 0.4,
            trackingMode = ExerciseTrackingMode.DurationAndDistance,
        )

        val pathExercise = WorkoutPathExercise(exercise, null, 1, listOf(0))
        assertEquals("90s · 0.4 km · 30s", pathExercisePrescription(pathExercise))
    }

    @Test
    fun `workout path includes typed rep range and first target load`() {
        val exercise = ExerciseSummary(
            name = "Press",
            sets = 4,
            reps = "8-10",
            restSeconds = 90,
            targetWeightKg = 50.0,
            trackingMode = ExerciseTrackingMode.Repetitions,
        )

        val pathExercise = WorkoutPathExercise(exercise, null, 1, listOf(0))
        assertEquals("4 × 8-10 · 50,0 kg · 90s", pathExercisePrescription(pathExercise))
        assertTrue(pathExercisePrescription(pathExercise, useImperial = true).endsWith("lb · 90s"))
    }

    @Test
    fun `exercise history summary keeps timed distance work out of repetitions`() {
        val entry = ExerciseHistoryEntry(
            timestampMillis = 0,
            sets = 2,
            totalReps = 0,
            maxWeightKg = 0.0,
            volumeKg = 0.0,
            durationSeconds = 90,
            distanceKm = 0.4,
        )

        assertEquals("2 series · 1 min 30s · 0,40 km", historyEntrySummary(entry, useImperial = false))
        assertTrue(historyEntrySummary(entry, useImperial = true).endsWith("mi"))
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
