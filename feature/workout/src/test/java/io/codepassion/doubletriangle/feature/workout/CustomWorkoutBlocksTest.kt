package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomWorkoutBlocksTest {
    private val workout = WorkoutDaySummary(
        "custom", 1, "Push", "Empuje", "Personalizado", DayOfWeek.MONDAY, 30, WorkoutStatus.Planned,
        exercises = listOf(
            ExerciseSummary("Press", sets = 4, reps = "8", restSeconds = 90),
            ExerciseSummary("Remo", sets = 3, reps = "10", restSeconds = 75),
            ExerciseSummary("Plancha", sets = 2, reps = "30 s", restSeconds = 60),
        ),
    )

    @Test fun `creates and executes an adjacent superset`() {
        val blocks = workout.editableBlocks().createSupersetAt(0)
        val edited = workout.withEditableBlocks(blocks)

        assertEquals(listOf(WorkoutBlockType.Superset, WorkoutBlockType.Standard), blocks.map { it.type })
        assertEquals(4, blocks.first().rounds)
        assertEquals(90, blocks.first().restAfterBlockSeconds)
        assertEquals(listOf("Press", "Remo", "Press", "Remo", "Press", "Remo", "Press", "Remo", "Plancha"), edited.exercises.map { it.name })
    }

    @Test fun `splitting a superset restores its rounds as sets`() {
        val blocks = workout.editableBlocks().createSupersetAt(0).splitSupersetAt(0)

        assertEquals(List(3) { WorkoutBlockType.Standard }, blocks.map { it.type })
        assertEquals(listOf(4, 4, 2), blocks.map { it.exercises.single().sets })
        assertEquals(listOf(90, 90, 60), blocks.map { it.exercises.single().restSeconds })
    }

    @Test fun `individual blocks cycle through workout sections`() {
        val original = listOf(workout.editableBlocks().first())
        val warmup = original.cycleSectionAt(0)
        val cooldown = warmup.cycleSectionAt(0)
        val standard = cooldown.cycleSectionAt(0)

        assertEquals(WorkoutBlockType.Warmup, warmup.first().type)
        assertEquals(WorkoutBlockType.Cooldown, cooldown.first().type)
        assertEquals(WorkoutBlockType.Standard, standard.first().type)
    }
}
