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
}
