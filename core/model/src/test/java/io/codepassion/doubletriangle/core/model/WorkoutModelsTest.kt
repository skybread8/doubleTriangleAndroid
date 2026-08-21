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
}
