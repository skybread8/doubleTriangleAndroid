package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMuscleEffortTest {
    @Test
    fun `calculates primary and secondary muscles from effective sets`() {
        val workout = workout(
            WorkoutBlockSummary(
                type = WorkoutBlockType.Standard,
                exercises = listOf(ExerciseSummary("Press de banca", "benchPress", 4, "8", 90)),
            ),
        )

        assertEquals(
            listOf(
                MuscleStimulus(MuscleVisual.Chest, 4),
                MuscleStimulus(MuscleVisual.Shoulders, 2),
                MuscleStimulus(MuscleVisual.Triceps, 2),
            ),
            workoutMuscleStimulus(workout),
        )
    }

    @Test
    fun `uses superset rounds instead of the exercise set prescription`() {
        val workout = workout(
            WorkoutBlockSummary(
                type = WorkoutBlockType.Superset,
                rounds = 3,
                exercises = listOf(ExerciseSummary("Remo", "seatedCableRow", 8, "10", 0)),
            ),
        )

        assertEquals(
            listOf(MuscleStimulus(MuscleVisual.Back, 3), MuscleStimulus(MuscleVisual.Biceps, 2)),
            workoutMuscleStimulus(workout),
        )
    }

    @Test
    fun `maps exercise equipment to its iOS illustration`() {
        assertEquals(
            listOf(EquipmentVisual.CableMachine),
            ExerciseVisualCatalog.equipmentVisualsFor("latPulldown"),
        )
        assertEquals(
            listOf(EquipmentVisual.LegPressMachine),
            ExerciseVisualCatalog.equipmentVisualsFor("legPress"),
        )
    }

    private fun workout(vararg blocks: WorkoutBlockSummary) = WorkoutDaySummary(
        id = "effort",
        order = 1,
        title = "Test",
        focus = "Tren superior",
        dayType = "Fuerza",
        scheduledDay = DayOfWeek.MONDAY,
        estimatedMinutes = 45,
        status = WorkoutStatus.Planned,
        blocks = blocks.toList(),
    )
}
