package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionCalculatorTest {
    private val workout = WorkoutDaySummary(
        id = "test", order = 1, title = "Push", focus = "Empuje", dayType = "Fuerza",
        scheduledDay = DayOfWeek.MONDAY, estimatedMinutes = 45, status = WorkoutStatus.Planned,
        exercises = listOf(
            ExerciseSummary("Press", "benchPress", 3, "8", 90),
            ExerciseSummary("Fondos", "chestDip", 3, "8-10", 90),
        ),
    )

    @Test fun completedPrescriptionWithCorrectEffortScoresOneHundred() {
        val score = WorkoutCompletionCalculator.score(
            workout,
            mapOf(0 to ExerciseSessionStats(3, 24, 50.0, 1_200.0), 1 to ExerciseSessionStats(3, 27, 0.0, 0.0)),
            mapOf(0 to "CORRECTO", 1 to "CORRECTO"),
        )
        assertEquals(100.0, score, 0.01)
    }

    @Test fun incompleteWorkoutDropsBelowSolidTier() {
        val score = WorkoutCompletionCalculator.score(workout, mapOf(0 to ExerciseSessionStats(1, 8, 40.0, 320.0)), emptyMap())
        assertTrue(score < 65.0)
        assertTrue(score >= 0.0)
    }

    @Test fun experienceThresholdsMatchIos() {
        assertEquals(0, WorkoutCompletionCalculator.minimumXp(1))
        assertEquals(60, WorkoutCompletionCalculator.minimumXp(2))
        assertEquals(155, WorkoutCompletionCalculator.minimumXp(3))
        assertEquals(1, WorkoutCompletionCalculator.level(59))
        assertEquals(2, WorkoutCompletionCalculator.level(60))
        assertEquals(3, WorkoutCompletionCalculator.level(155))
    }

    @Test fun setStyleInstructionsFollowIosScope() {
        assertEquals("TOP SET", setStyleInstruction(ExerciseSetStyle.TopSetBackoff, 1, 4))
        assertEquals("BACKOFF", setStyleInstruction(ExerciseSetStyle.TopSetBackoff, 2, 4))
        assertEquals("SERIE BASE", setStyleInstruction(ExerciseSetStyle.DropSet, 2, 3))
        assertEquals("DROP SET · SIN DESCANSO", setStyleInstruction(ExerciseSetStyle.DropSet, 3, 3))
        assertEquals("PESO MÁXIMO", setStyleInstruction(ExerciseSetStyle.AscendingPyramid, 4, 4))
    }
}
