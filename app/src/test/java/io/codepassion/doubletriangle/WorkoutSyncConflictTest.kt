package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSyncConflictTest {
    @org.junit.Test fun gymSyncUploadsOnlyNewOrNewerLocalRows() {
        org.junit.Assert.assertTrue(shouldPushGymRow("2026-11-25T10:00:01Z", null))
        org.junit.Assert.assertTrue(shouldPushGymRow("2026-11-25T10:00:01Z", "2026-11-25T10:00:00Z"))
        org.junit.Assert.assertFalse(shouldPushGymRow("2026-11-25T10:00:00Z", "2026-11-25T10:00:00Z"))
        org.junit.Assert.assertFalse(shouldPushGymRow("2026-11-25T10:00:00Z", "2026-11-25T10:00:01Z"))
    }
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
    @Test fun onlyUnversionedAndUnchangedPendingGraphsCanBeReplaced() {
        val dirty = setOf("workout-days")
        val before = mapOf("workout-days" to 2L)
        assertTrue(canReplaceLegacyWorkoutGraph(dirty, false, before, before))
        assertFalse(canReplaceLegacyWorkoutGraph(dirty, true, before, before))
        assertFalse(canReplaceLegacyWorkoutGraph(dirty, false, before, mapOf("workout-days" to 3L)))
        assertFalse(canReplaceLegacyWorkoutGraph(emptySet(), false, before, before))
    }
    @Test fun migratesOnlyUnversionedRowsThatHaveARemoteVersion() {
        org.junit.Assert.assertEquals(LegacyWorkoutRecordAction.UploadVersioned, legacyWorkoutRecordAction(true, true, false))
        org.junit.Assert.assertEquals(LegacyWorkoutRecordAction.UseRemote, legacyWorkoutRecordAction(false, true, false))
        org.junit.Assert.assertEquals(LegacyWorkoutRecordAction.UseMatchingRemote, legacyWorkoutRecordAction(false, true, true))
        org.junit.Assert.assertEquals(LegacyWorkoutRecordAction.KeepPending, legacyWorkoutRecordAction(false, false, false))
    }
    @Test fun equalNumericValuesHaveTheSameSyncFingerprintComponent() {
        org.junit.Assert.assertEquals(canonicalSyncNumber(12), canonicalSyncNumber(12.0))
        org.junit.Assert.assertEquals("0", canonicalSyncNumber(-0.0))
        org.junit.Assert.assertEquals("1.25", canonicalSyncNumber(1.25))
    }
    @Test fun missingBodyPhotoSessionRequiresExplicitDeletionTime() {
        assertFalse(shouldSendBodyPhotoTombstone(null))
        assertFalse(shouldSendBodyPhotoTombstone("invalid"))
        assertTrue(shouldSendBodyPhotoTombstone("2026-11-25T10:00:00Z"))
    }
}
