package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachExerciseMergeTest {
    @Test fun liveExerciseIsInsertedAndUpdated() {
        assertEquals(CoachExerciseMergeAction.Replace, coachExerciseMergeAction(null, "2026-01-01", false))
        assertEquals(CoachExerciseMergeAction.Replace, coachExerciseMergeAction("2026-01-01", "2026-01-02", false))
    }

    @Test fun newerDeletionRemovesExerciseAndOlderRowsAreIgnored() {
        assertEquals(CoachExerciseMergeAction.Remove, coachExerciseMergeAction("2026-01-01", "2026-01-02", true))
        assertEquals(CoachExerciseMergeAction.Ignore, coachExerciseMergeAction("2026-01-02", "2026-01-01", true))
        assertEquals(CoachExerciseMergeAction.Ignore, coachExerciseMergeAction("2026-01-02", "2026-01-01", false))
        assertEquals(CoachExerciseMergeAction.Ignore, coachExerciseMergeAction("2026-01-02", "2026-01-02", true))
    }
}
