package io.codepassion.doubletriangle.core.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutModelsTest {
    private val state = PreviewWorkoutRepository.load()

    @Test fun noSelectedDayReturnsWholePlan() = assertEquals(4, state.workoutsFor(null).size)

    @Test fun selectedDayFiltersPlan() {
        assertEquals(listOf("legs"), state.workoutsFor(DayOfWeek.WEDNESDAY).map { it.id })
    }

    @Test fun restDayReturnsEmptyList() {
        assertEquals(emptyList<WorkoutDaySummary>(), state.workoutsFor(DayOfWeek.TUESDAY))
    }

    @Test fun intensiveStylesApplyOnlyToFinalSet() {
        assertEquals(false, ExerciseSetStyle.DropSet.appliesToSet(2, 3))
        assertEquals(true, ExerciseSetStyle.DropSet.appliesToSet(3, 3))
        assertEquals(false, ExerciseSetStyle.RestPause.appliesToSet(1, 4))
        assertEquals(true, ExerciseSetStyle.Tempo.appliesToSet(1, 4))
    }

    @Test fun exercisesKeepIosStyleDefaults() {
        val exercise = ExerciseSummary("Press", sets = 3, reps = "8", restSeconds = 90)
        assertEquals(ExerciseSetStyle.Straight, exercise.setStyle)
        assertEquals(SetStyleParameters(), exercise.setStyleParameters)
    }

    @Test fun legacyWorkoutsBecomeAStandardDisplayBlock() {
        val workout = state.workouts.first { it.id == "legs" }
        assertEquals(WorkoutBlockType.Standard, workout.displayBlocks().single().type)
        assertEquals(workout.exercises, workout.displayBlocks().single().exercises)
    }

    @Test fun fakePlanExposesEveryIosBlockType() {
        assertEquals(WorkoutBlockType.entries, state.workouts.first().blocks.map { it.type })
    }

    @Test fun supersetsAlternateExercisesForEveryRound() {
        val first = ExerciseSummary("Press", sets = 3, reps = "8", restSeconds = 0)
        val second = ExerciseSummary("Remo", sets = 3, reps = "10", restSeconds = 0)
        val block = WorkoutBlockSummary(WorkoutBlockType.Superset, rounds = 3, exercises = listOf(first, second))

        assertEquals(listOf("Press", "Remo", "Press", "Remo", "Press", "Remo"), block.executionExercises().map { it.name })
        assertEquals(List(6) { 1 }, block.executionExercises().map { it.sets })
        assertEquals(listOf(1, 1, 2, 2, 3, 3), block.executionExercises().map { it.blockRound })
        assertEquals(listOf("A1", "A2", "A1", "A2", "A1", "A2"), block.executionExercises().map { it.blockLabel })
        assertEquals(listOf(false, true, false, true, false, true), block.executionExercises().map { it.isLastInBlock })
    }

    @Test fun fourWeekCycleUsesAccumulationThenDeload() {
        assertEquals(MesocyclePhase.Accumulation, resolveMesocyclePhase(1, 4)?.phase)
        assertEquals(MesocyclePhase.Accumulation, resolveMesocyclePhase(3, 4)?.phase)
        assertEquals(MesocyclePhase.Deload, resolveMesocyclePhase(4, 4)?.phase)
    }

    @Test fun sixWeekCycleIncludesIntensification() {
        val phase = resolveMesocyclePhase(4, 6)
        assertEquals(MesocyclePhase.Intensification, phase?.phase)
        assertEquals(1, phase?.weekInPhase)
    }

    @Test fun phasePositionWrapsIntoNextCycle() {
        val phase = resolveMesocyclePhase(7, 6)
        assertEquals(1, phase?.positionInCycle)
        assertEquals(MesocyclePhase.Accumulation, phase?.phase)
    }

    @Test fun invalidCycleHasNoDerivedPhase() {
        assertEquals(null, resolveMesocyclePhase(1, 1))
    }
}
