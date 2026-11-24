package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingSessionHistoryMergeTest {
    @Test fun `remote workout replaces old per exercise sessions and retains unrelated history`() {
        val local = listOf(
            TrainingSessionHistoryEntry(1000, 10, 2, 100.0),
            TrainingSessionHistoryEntry(2000, 10, 3, 150.0),
            TrainingSessionHistoryEntry(3001, 1200, 5, 250.0),
            TrainingSessionHistoryEntry(8000, 600, 1, 20.0),
        )
        val remote = listOf(TrainingSessionHistoryEntry(3000, 1200, 5, 250.0, workoutId = "day-1"))
        val merged = mergeTrainingSessions(local, remote, setOf(1000, 2000))
        assertEquals(listOf(8000L, 3000L), merged.map { it.timestampMillis })
        assertEquals(merged, mergeTrainingSessions(merged, remote, setOf(1000, 2000)))
    }

    @Test fun `remote session replaces same workout even when timestamp changes`() {
        val local = TrainingSessionHistoryEntry(100, 60, 3, 90.0, workoutId = "day-1")
        val remote = local.copy(timestampMillis = 200, sets = 4)
        assertEquals(listOf(remote), mergeTrainingSessions(listOf(local), listOf(remote), emptySet()))
    }
}
