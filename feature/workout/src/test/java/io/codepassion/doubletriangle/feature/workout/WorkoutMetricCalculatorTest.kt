package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMetricCalculatorTest {
    @Test
    fun `estimates calories with the iOS MET and BMR formula when no wearable exists`() {
        val workout = WorkoutDaySummary(
            id = "test",
            order = 1,
            title = "Fuerza",
            focus = "Empuje",
            dayType = "Fuerza",
            scheduledDay = DayOfWeek.MONDAY,
            estimatedMinutes = 60,
            status = WorkoutStatus.Planned,
            exercises = listOf(ExerciseSummary("Press de banca", "benchPress", 3, "8", 90)),
        )

        // 5 MET × (2,400 kcal BMR / 24 h) × 1 h = 500 kcal.
        val calories = WorkoutMetricCalculator.estimateCalories(
            workout = workout,
            stats = emptyMap(),
            totalDurationSeconds = 3_600,
            userBmr = 2_400.0,
        )

        assertEquals(500.0, calories, 0.001)
    }
}
