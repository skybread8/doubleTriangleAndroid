package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.SetStyleParameters
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionCalculatorTest {
    @Test
    fun `completion progress splits workout plan and mesocycle xp like ios`() {
        val progress = CompletionProgressStore.calculate(
            xpBefore = 10,
            storedStreak = 2,
            previousDay = 99,
            today = 100,
            completesPlan = true,
            completesMesocycle = true,
        )

        assertEquals(35, progress.xpAfterWorkout)
        assertEquals(85, progress.xpAfterPlan)
        assertEquals(185, progress.xpAfter)
        assertEquals(3, progress.streakAfter)
        assertTrue(progress.streakIncreased)
    }

    @Test
    fun `completion progress uses the captured device date`() {
        val progress = CompletionProgressStore.calculate(
            xpBefore = 0,
            storedStreak = 4,
            previousDay = LocalDate.of(2026, 9, 28).toEpochDay(),
            today = LocalDate.of(2026, 9, 28).toEpochDay(),
            completesPlan = false,
            completesMesocycle = false,
        )

        assertEquals(4, progress.streakAfter)
        assertEquals(false, progress.streakIncreased)
    }

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

    @Test fun effortScaleRecognizesAllFiveFeedbackLevels() {
        val stats = mapOf(0 to ExerciseSessionStats(3, 24, 50.0, 1_200.0), 1 to ExerciseSessionStats(3, 24, 0.0, 0.0))
        val veryEasy = WorkoutCompletionCalculator.score(workout, stats, mapOf(0 to "MUY FÁCIL", 1 to "MUY FÁCIL"))
        val right = WorkoutCompletionCalculator.score(workout, stats, mapOf(0 to "JUSTO", 1 to "JUSTO"))
        val veryHard = WorkoutCompletionCalculator.score(workout, stats, mapOf(0 to "MUY DIFÍCIL", 1 to "MUY DIFÍCIL"))
        assertTrue(veryEasy < right)
        assertTrue(right > veryHard)
    }

    @Test fun warmupAndCooldownDoNotLowerTheIosWorkoutScore() {
        val sectioned = workout.copy(
            exercises = listOf(
                ExerciseSummary("Movilidad", sets = 1, reps = "10", restSeconds = 0, blockType = WorkoutBlockType.Warmup),
                ExerciseSummary("Press", sets = 3, reps = "8", restSeconds = 90),
                ExerciseSummary("Estiramiento", sets = 1, reps = "30 s", restSeconds = 0, blockType = WorkoutBlockType.Cooldown),
            ),
        )
        val score = WorkoutCompletionCalculator.score(sectioned, mapOf(1 to ExerciseSessionStats(3, 24, 50.0, 1_200.0)), mapOf(1 to "CORRECTO"))
        assertEquals(100.0, score, 0.01)
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
        assertEquals("SERIE BASE", setStyleInstruction(ExerciseSetStyle.DropSet, 2, 3, appliesToFinalSetOnly = true))
        assertEquals("DROP SET · SIN DESCANSO", setStyleInstruction(ExerciseSetStyle.DropSet, 3, 3, appliesToFinalSetOnly = true))
        assertEquals("PESO MÁXIMO", setStyleInstruction(ExerciseSetStyle.AscendingPyramid, 4, 4))
    }

    @Test fun styleParameterLabelsExposeThePlannedPrescription() {
        val parameters = SetStyleParameters(dropCount = 3, dropWeightPercent = 25, targetRir = 0)
        assertEquals(listOf("3 drops", "-25% por drop", "RIR 0"), styleParameterLabels(ExerciseSetStyle.DropSet, parameters))
        assertEquals(listOf("Tempo 3-1-1-0", "RIR 2"), styleParameterLabels(ExerciseSetStyle.Tempo, SetStyleParameters()))
        assertEquals("Tras el esfuerzo principal, baja el peso y continúa 3 veces casi sin descanso.", styleParameterDetails(ExerciseSetStyle.DropSet, parameters).first().explanation)
        assertEquals("Solo en la última serie", setStyleScopeLabel(ExerciseSetStyle.DropSet, 3, appliesToFinalSetOnly = true))
        assertEquals("Top set primero", setStyleScopeLabel(ExerciseSetStyle.TopSetBackoff, 4, appliesToFinalSetOnly = false))
    }

    @Test fun durationParserSeparatesTimedWorkFromRepetitions() {
        assertEquals(45, targetDurationSeconds("45 s"))
        assertEquals(120, targetDurationSeconds("2 min"))
        assertEquals(30, targetDurationSeconds("30 segundos"))
        assertEquals(null, targetDurationSeconds("8-10"))
    }

    @Test fun typedDurationTakesPriorityOverLegacyPresentationText() {
        val timed = ExerciseSummary("Plancha", sets = 1, reps = "10", restSeconds = 0, targetDurationSeconds = 45)
        val minutes = ExerciseSummary("Cardio", sets = 1, reps = "libre", restSeconds = 0, targetDurationMinutes = 2)
        assertEquals(45, timed.resolvedTargetDurationSeconds())
        assertEquals(120, minutes.resolvedTargetDurationSeconds())
    }

    @Test fun timedCompletionNeverBecomesRepetitionsOrVolume() {
        val timed = ExerciseSummary("Plancha", sets = 1, reps = "45 s", restSeconds = 0, targetDurationSeconds = 45)
        val result = completedExerciseMetrics(timed, repetitions = 45, weightKg = 20.0, initialDurationSeconds = 45, remainingDurationSeconds = 0)
        assertEquals(0, result.repetitions)
        assertEquals(0.0, result.weightKg, 0.0)
        assertEquals(45, result.durationSeconds)
    }

    @Test fun timedExerciseScoresDurationInsteadOfFakeRepetitions() {
        val timedWorkout = workout.copy(
            exercises = listOf(ExerciseSummary("Plancha", sets = 1, reps = "45 s", restSeconds = 0, targetDurationSeconds = 45)),
        )
        val score = WorkoutCompletionCalculator.score(
            timedWorkout,
            mapOf(0 to ExerciseSessionStats(sets = 1, durationSeconds = 45)),
            mapOf(0 to "CORRECTO"),
        )
        assertEquals(100.0, score, 0.01)
    }

    @Test fun zeroSecondRestAdvancesImmediatelyLikeIos() {
        assertEquals(false, shouldStartRest(restSeconds = 0, skipsRestPeriods = false))
        assertEquals(true, shouldStartRest(restSeconds = 30, skipsRestPeriods = false))
        assertEquals(false, shouldStartRest(restSeconds = 30, skipsRestPeriods = true))
    }

    @Test fun supersetRequestsFeedbackOnlyAfterItsFinalExerciseAndRound() {
        val firstInFinalRound = ExerciseSummary(
            name = "Press",
            sets = 1,
            reps = "10",
            restSeconds = 0,
            blockType = WorkoutBlockType.Superset,
            blockRound = 3,
            blockRounds = 3,
            isLastInBlock = false,
        )
        val lastBeforeFinalRound = firstInFinalRound.copy(
            name = "Row",
            blockRound = 2,
            isLastInBlock = true,
        )
        val lastInFinalRound = lastBeforeFinalRound.copy(blockRound = 3)

        assertEquals(false, shouldRequestExerciseFeedback(firstInFinalRound))
        assertEquals(false, shouldRequestExerciseFeedback(lastBeforeFinalRound))
        assertEquals(true, shouldRequestExerciseFeedback(lastInFinalRound))
    }

    @Test fun workTimerMatchesIosClockFormatting() {
        assertEquals("0:45", formatWorkDuration(45))
        assertEquals("2:00", formatWorkDuration(120))
        assertEquals("1:01:01", formatWorkDuration(3_661))
    }
}
