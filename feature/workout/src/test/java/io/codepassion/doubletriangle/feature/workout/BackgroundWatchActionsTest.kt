package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import org.junit.Assert.*
import org.junit.Test

class BackgroundWatchActionsTest {
    private val exercise = ExerciseSummary(name = "Press", sets = 2, reps = "10", restSeconds = 60, targetWeightKg = 20.0)
    private fun snapshot(blocks: List<WorkoutBlockSummary> = listOf(WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(exercise)))) = WorkoutSessionSnapshot(
        exerciseIndex = 0, completedByExercise = emptyMap(), reps = 10, weightKg = 20.0,
        restRemaining = null, restInitialSeconds = 0, restBetweenExercises = false, elapsedSeconds = 0,
        totalCompletedSets = 0, totalVolumeKg = 0.0, pendingFeedback = false, showsSummary = false,
        selectedFeedback = "JUSTO", editableBlocks = blocks,
    )
    private fun command(op: String) = WildforceWatchLocalBridge.Command(op)

    @Test fun completeAndSkipRestThenFinishAndSubmitFeedback() {
        val first = BackgroundWatchActions.apply(snapshot(), command("complete_set"))!!
        assertEquals(1, first.totalCompletedSets)
        assertEquals(60, first.restRemaining)
        assertEquals(200.0, first.totalVolumeKg, 0.0)
        assertNull(BackgroundWatchActions.apply(first, command("complete_set")))
        val skipped = BackgroundWatchActions.apply(first, command("skip_rest"))!!
        val last = BackgroundWatchActions.apply(skipped, command("complete_set"))!!
        assertTrue(last.pendingFeedback)
        assertEquals(2, last.completedSetRecords.size)
        val rated = BackgroundWatchActions.apply(last, WildforceWatchLocalBridge.Command("set_feedback", feedback = "DIFÍCIL"))!!
        val done = BackgroundWatchActions.apply(rated, command("complete_feedback"))!!
        assertTrue(done.showsSummary)
        assertEquals("DIFÍCIL", done.feedbackByExercise[0])
    }

    @Test fun addingRestAndChangingMetricsPersistsAcrossActions() {
        val weighted = BackgroundWatchActions.apply(snapshot(), WildforceWatchLocalBridge.Command("set_weight", weightKg = 25.0))!!
        val rep = BackgroundWatchActions.apply(weighted, WildforceWatchLocalBridge.Command("set_reps", reps = 8))!!
        val first = BackgroundWatchActions.apply(rep, command("complete_set"))!!
        val extended = BackgroundWatchActions.apply(first, command("add_rest"))!!
        assertEquals(90, extended.restRemaining)
        assertEquals(8, extended.reps)
        assertEquals(25.0, extended.weightKg, 0.0)
        assertEquals(200.0, extended.totalVolumeKg, 0.0)
    }

    @Test fun supersetAdvancesAndRestsBetweenRounds() {
        val block = WorkoutBlockSummary(WorkoutBlockType.Superset, rounds = 2, restAfterBlockSeconds = 45,
            exercises = listOf(exercise, exercise.copy(name = "Remo")))
        val first = BackgroundWatchActions.apply(snapshot(listOf(block)), command("complete_set"))!!
        assertEquals(1, first.exerciseIndex)
        assertNull(first.restRemaining)
        val second = BackgroundWatchActions.apply(first, command("complete_set"))!!
        assertEquals(2, second.exerciseIndex)
        assertEquals(45, second.restRemaining)
        assertEquals(WorkoutRestContext.BetweenRounds, second.restContext)
    }

    @Test fun userCanDisableRest() {
        val first = BackgroundWatchActions.apply(snapshot().copy(skipsRestPeriods = true), command("complete_set"))!!
        assertNull(first.restRemaining)
    }
}
