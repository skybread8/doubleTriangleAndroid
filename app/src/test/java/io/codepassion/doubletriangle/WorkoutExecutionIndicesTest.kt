package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutExecutionIndicesTest {
    @Test fun `superset source exercises map to every round and next block starts after all rounds`() {
        val start = 2
        assertEquals(listOf(2, 4, 6), workoutBlockExecutionIndices(start, 2, 3, "superset", 0))
        assertEquals(listOf(3, 5, 7), workoutBlockExecutionIndices(start, 2, 3, "superset", 1))
        val next = start + workoutBlockExecutionCount(2, 3, "superset")
        assertEquals(listOf(8), workoutBlockExecutionIndices(next, 1, 1, "standard", 0))
    }

    @Test fun `standard blocks do not repeat and invalid exercise indices are ignored`() {
        assertEquals(2, workoutBlockExecutionCount(2, 5, "standard"))
        assertEquals(listOf(4), workoutBlockExecutionIndices(3, 2, 5, "standard", 1))
        assertEquals(emptyList<Int>(), workoutBlockExecutionIndices(3, 2, 5, "standard", 2))
    }

    @Test fun `result identity is scoped to workout and never guesses repeated exercises`() {
        val refs = listOf(
            PlannedExerciseSyncReference("monday", "press", "monday-1"),
            PlannedExerciseSyncReference("friday", "press", "friday-1"),
            PlannedExerciseSyncReference("friday", "press", "friday-2"),
        )
        assertEquals("monday-1", resolvePlannedExerciseId("monday", "press", null, refs))
        assertEquals("friday-2", resolvePlannedExerciseId("friday", "press", "friday-2", refs))
        assertEquals(null, resolvePlannedExerciseId("friday", "press", null, refs))
        assertEquals(null, resolvePlannedExerciseId("monday", "press", "friday-2", refs))
        assertEquals(null, resolvePlannedExerciseId(null, "press", null, refs))
    }
}
