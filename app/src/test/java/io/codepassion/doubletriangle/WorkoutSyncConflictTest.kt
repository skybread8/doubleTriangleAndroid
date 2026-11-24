package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSyncConflictTest {
    @Test fun userDeletionUsesIosTimestampOrderingAndKeepsUnknownLegacyEdits() {
        val local = java.time.Instant.parse("2026-11-14T10:00:00Z").toEpochMilli()
        assertTrue(remoteDeletionWins(false, null, "2026-11-14T10:00:00Z"))
        assertTrue(remoteDeletionWins(true, local, "2026-11-14T10:00:01Z"))
        assertFalse(remoteDeletionWins(true, local, "2026-11-14T10:00:00Z"))
        assertFalse(remoteDeletionWins(true, null, "2026-11-14T10:00:01Z"))
    }

    @Test fun pendingNutritionRecordsAreProtectedIndividually() {
        val old = "2026-11-14T10:00:00Z"
        val changed = "2026-11-14T10:01:00Z"
        val local = mapOf("unchanged" to old, "edited" to changed)
        val baseline = mapOf("unchanged" to old, "edited" to old, "removed" to old)
        val deleted = mapOf("removed" to changed)
        org.junit.Assert.assertEquals(setOf("edited", "removed"), protectedVersionedRecordIds(local, baseline, deleted))
        org.junit.Assert.assertNull(protectedVersionedRecordIds(local, baseline, emptyMap()))
    }

    @Test fun concurrentBodyMetricPullNeedsVersionsAndDatedDeletions() {
        val time = "2026-11-14T10:00:00Z"
        assertTrue(canMergeVersionedRecords(mapOf("one" to time), setOf("one", "two"), mapOf("two" to time)))
        assertFalse(canMergeVersionedRecords(mapOf("one" to time), setOf("one", "two"), emptyMap()))
        assertFalse(canMergeVersionedRecords(mapOf("one" to null), setOf("one"), emptyMap()))
        assertFalse(canMergeVersionedRecords(emptyMap(), setOf("two"), mapOf("two" to "invalid")))
    }

    @Test fun remotePullDoesNotReplaceANewerOrEqualCachedRecord() {
        val local = "2026-11-14T10:00:01Z"
        assertFalse(shouldApplyRemoteRecord(local, "2026-11-14T10:00:00Z"))
        assertFalse(shouldApplyRemoteRecord(local, local))
        assertTrue(shouldApplyRemoteRecord(local, "2026-11-14T10:00:02Z"))
        assertTrue(shouldApplyRemoteRecord(null, local))
        assertFalse(shouldApplyRemoteRecord(local, null))
        assertFalse(shouldApplyRemoteRecord(null, "invalid"))
    }

    @Test fun serverTimestampDecidesWhetherAWriteWasRejected() {
        assertTrue(serverHasNewerWorkoutRecord("2026-11-14T10:00:00Z", "2026-11-14T10:00:00.001Z"))
        assertFalse(serverHasNewerWorkoutRecord("2026-11-14T10:00:00Z", "2026-11-14T10:00:00Z"))
        assertFalse(serverHasNewerWorkoutRecord("2026-11-14T10:00:01Z", "2026-11-14T10:00:00Z"))
    }
}
