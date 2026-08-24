package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.executionExercises
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPathModelTest {
    @Test
    fun `superset rounds share one visual row per exercise`() {
        val blocks = listOf(
            WorkoutBlockSummary(WorkoutBlockType.Warmup, exercises = listOf(exercise("Movilidad"))),
            WorkoutBlockSummary(WorkoutBlockType.Superset, rounds = 3, exercises = listOf(exercise("Press"), exercise("Remo"))),
            WorkoutBlockSummary(WorkoutBlockType.Cooldown, exercises = listOf(exercise("Respiración"))),
        )
        val workout = WorkoutDaySummary("test", 1, "Test", "Full body", "Fuerza", DayOfWeek.MONDAY, 30, WorkoutStatus.Planned, blocks.executionExercises(), blocks)

        val path = workout.pathBlocks()

        assertEquals(listOf("Movilidad", "Press", "Remo", "Respiración"), path.flatMap { it.exercises }.map { it.exercise.name })
        assertEquals(listOf(1, 3, 5), path[1].exercises[0].executionIndices)
        assertEquals(listOf(2, 4, 6), path[1].exercises[1].executionIndices)
        assertEquals(listOf("Press", "Remo"), path.mainExercises().map { it.exercise.name })
        assertEquals(false, path[1].exercises[0].isCompleted(workout, mapOf(1 to 1, 3 to 1)))
        assertEquals(true, path[1].exercises[0].isCompleted(workout, mapOf(1 to 1, 3 to 1, 5 to 1)))
        assertEquals(1, nextIndexAfterPreparationSection(workout.exercises, 0))
        assertEquals(null, nextIndexAfterPreparationSection(workout.exercises, 7))
    }

    private fun exercise(name: String) = ExerciseSummary(name, sets = 1, reps = "10", restSeconds = 0)
}
