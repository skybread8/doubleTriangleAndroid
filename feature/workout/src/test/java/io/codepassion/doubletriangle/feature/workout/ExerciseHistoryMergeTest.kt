package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseHistoryMergeTest {
    @Test fun remoteEditReplacesTheSameResultWithoutLosingLocalHistory() {
        val local = ExerciseHistoryEntry(100, 1, 8, 20.0, 160.0, plannedExerciseId = "local")
        val previous = ExerciseHistoryEntry(200, 1, 8, 30.0, 240.0, plannedExerciseId = "planned", remoteResultId = "result")
        val edited = previous.copy(totalReps = 10, volumeKg = 300.0)
        assertEquals(listOf(edited, local), mergeExerciseHistory(listOf(local, previous), listOf(edited), 50))
    }

    @Test fun distinctResultsAtSameTimeAreNotCollapsed() {
        val first = ExerciseHistoryEntry(100, 1, 8, 20.0, 160.0, plannedExerciseId = "planned", remoteResultId = "one")
        val second = first.copy(remoteResultId = "two")
        assertEquals(2, mergeExerciseHistory(listOf(first), listOf(second), 50).size)
    }

    @Test fun remoteResultClaimsAnUnambiguousLocalResult() {
        val local = ExerciseHistoryEntry(100, 1, 8, 20.0, 160.0, plannedExerciseId = "planned")
        val remote = local.copy(remoteResultId = "result")
        assertEquals(listOf(remote), mergeExerciseHistory(listOf(local), listOf(remote), 50))
    }

    @Test fun pullDoesNotOverwriteAnEditAwaitingUpload() {
        val server = ExerciseHistoryEntry(100, 1, 8, 20.0, 160.0, plannedExerciseId = "planned", remoteResultId = "result")
        val pending = server.copy(totalReps = 10, pendingEditAt = 123, pendingEditKind = "reps")
        assertEquals(listOf(pending), mergeExerciseHistory(listOf(pending), listOf(server), 50))
    }

    @Test fun pendingLocalEditKeepsItsValuesAndLearnsTheServerIdentity() {
        val pending = ExerciseHistoryEntry(100, 1, 10, 20.0, 200.0, plannedExerciseId = "planned", pendingEditAt = 123, pendingEditKind = "reps")
        val server = pending.copy(totalReps = 8, remoteResultId = "result", pendingEditAt = 0, pendingEditKind = null)
        assertEquals(listOf(pending.copy(remoteResultId = "result")), mergeExerciseHistory(listOf(pending), listOf(server), 50))
    }

    @Test fun newerRemoteEditWinsOverAnOlderPendingLocalEdit() {
        val local = ExerciseHistoryEntry(100, 1, 10, 20.0, 200.0, plannedExerciseId = "planned", remoteResultId = "result", pendingEditAt = 200, pendingEditKind = "reps")
        val remote = local.copy(totalReps = 12, pendingEditAt = 0, pendingEditKind = null, updatedAtMillis = 201)
        assertEquals(listOf(remote), mergeExerciseHistory(listOf(local), listOf(remote), 50))
    }

    @Test fun olderRemoteEditDoesNotOverwriteANewerPendingLocalEdit() {
        val local = ExerciseHistoryEntry(100, 1, 10, 20.0, 200.0, plannedExerciseId = "planned", remoteResultId = "result", pendingEditAt = 200, pendingEditKind = "reps")
        val remote = local.copy(totalReps = 8, pendingEditAt = 0, pendingEditKind = null, updatedAtMillis = 199)
        assertEquals(listOf(local), mergeExerciseHistory(listOf(local), listOf(remote), 50))
    }

    @Test fun uploadClaimsTheSameLocalResultWithoutDiscardingANewerEdit() {
        val latest = ExerciseHistoryEntry(100, 1, 12, 20.0, 240.0, workoutId = "day", plannedExerciseId = "planned", pendingEditAt = 200, pendingEditKind = "reps")
        val uploaded = RemoteExerciseHistoryEntry("pushup", 100, 1, 10, 20.0, listOf(10), listOf(20.0), null, null, 0, 0.0, "day", "planned", pendingEditAt = 100)
        assertEquals(listOf(latest.copy(remoteResultId = "result")), claimUploadedHistory(listOf(latest), uploaded, "result", "planned"))
    }

    @Test fun ambiguousLocalResultsAreNotAssignedToAnArbitraryServerResult() {
        val local = ExerciseHistoryEntry(100, 1, 10, 20.0, 200.0, workoutId = "day", plannedExerciseId = "planned")
        val uploaded = RemoteExerciseHistoryEntry("pushup", 100, 1, 10, 20.0, listOf(10), listOf(20.0), null, null, 0, 0.0, "day", "planned")
        assertEquals(listOf(local, local), claimUploadedHistory(listOf(local, local), uploaded, "result", "planned"))
    }
}
