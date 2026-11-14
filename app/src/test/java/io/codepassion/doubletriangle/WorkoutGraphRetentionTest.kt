package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutGraphRetentionTest {
    @Test fun previousPlanGraphIsRetainedButCurrentPlanCanBeEdited() {
        val result = historicalWorkoutGraph(
            "new", mapOf("old-day" to "old", "new-day" to "new"),
            mapOf("old-block" to "old-day", "new-block" to "new-day"),
            mapOf("old-exercise" to "old-block", "new-exercise" to "new-block"),
        )
        assertEquals(setOf("old-day"), result.days)
        assertEquals(setOf("old-block"), result.blocks)
        assertEquals(setOf("old-exercise"), result.exercises)
    }

    @Test fun unknownPlanIsConservativelyRetained() {
        val result = historicalWorkoutGraph("new", mapOf("day" to null), mapOf("block" to "day"), mapOf("exercise" to "block"))
        assertEquals(setOf("day"), result.days)
        assertEquals(setOf("block"), result.blocks)
        assertEquals(setOf("exercise"), result.exercises)
    }
}
