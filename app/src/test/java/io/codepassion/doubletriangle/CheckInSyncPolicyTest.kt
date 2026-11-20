package io.codepassion.doubletriangle

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInSyncPolicyTest {
    @Test fun onlyAcknowledgesTheSameWriteAndContent() {
        val at = Instant.parse("2026-11-16T12:00:00Z")
        assertTrue(checkInWriteAcknowledged("ABC", "abc", at, at, true))
        assertFalse(checkInWriteAcknowledged("a", "b", at, at, true))
        assertFalse(checkInWriteAcknowledged("a", "a", at, at.plusSeconds(1), true))
        assertFalse(checkInWriteAcknowledged("a", "a", at, at.minusSeconds(1), true))
        assertFalse(checkInWriteAcknowledged("a", "a", at, at, false))
    }

    @Test fun laterLocalEditSurvivesDelayedResponseFromAnotherSync() {
        // Device A sends revision 1, then makes revision 2 before the reply.
        val sent = "revision-1"
        var pending: String? = "revision-2"
        if (checkInVersionAcknowledged(sent, pending)) pending = null
        assertTrue(pending == "revision-2")
        if (checkInVersionAcknowledged("revision-2", pending)) pending = null
        assertTrue(pending == null)
    }

    @Test fun remoteSubmissionAndDeletionOverrideAnOfflineDraft() {
        assertTrue(shouldOverlayCheckInDraft("pending", false))
        assertTrue(shouldOverlayCheckInDraft("draft", false))
        assertFalse(shouldOverlayCheckInDraft("submitted", false))
        assertFalse(shouldOverlayCheckInDraft("draft", true))
    }

    @Test fun permitsAssetsCapturedBeforeSubmissionEvenWhenUploadedLater() {
        val submitted = Instant.parse("2026-11-16T12:00:00Z")
        assertTrue(maySyncCheckInAsset(false, submitted, submitted.minusSeconds(1)))
        assertTrue(maySyncCheckInAsset(false, submitted, submitted))
        assertFalse(maySyncCheckInAsset(false, submitted, submitted.plusSeconds(1)))
        assertFalse(maySyncCheckInAsset(true, null, submitted.minusSeconds(1)))
    }

    @Test fun comparesServerAndClientTimestampFormatsAsInstants() {
        assertTrue(checkInTimestamp("2026-11-16T12:00:00.000000Z") == checkInTimestamp("2026-11-16T12:00:00Z"))
        assertTrue(checkInTimestamp("2026-11-16T12:00:01Z") > checkInTimestamp("2026-11-16T12:00:00.999999Z"))
    }

    @Test fun remoteMetricEditAndDeletionReplaceAcknowledgedLocalCopy() {
        val at = Instant.parse("2026-11-16T12:00:00Z")
        val local = CheckInMetricRevision("local", "id", at, at, deleted = false)
        val remote = CheckInMetricRevision("remote", "id", at.plusSeconds(1), at.plusSeconds(1), deleted = false)
        assertTrue(resolveCheckInMetric(listOf(local), listOf(remote))?.key == "remote")
        assertTrue(resolveCheckInMetric(listOf(local), listOf(remote.copy(deleted = true))) == null)
        assertTrue(resolveCheckInMetric(listOf(local.copy(pending = true)), listOf(remote))?.key == "local")
        assertTrue(resolveCheckInMetric(listOf(local.copy(pending = true, deleted = true)), listOf(remote)) == null)
        val other = remote.copy(id = "new-id", recordedAt = at.plusSeconds(5))
        assertTrue(resolveCheckInMetric(listOf(local), listOf(other))?.key == "remote")
    }

    @Test fun remotePhotoRetakeAndUnlinkReplaceAcknowledgedLocalSession() {
        val at = Instant.parse("2026-11-16T12:00:00Z")
        val local = CheckInPhotoRevision("local", "photo-1", "check-in", at, at, deleted = false)
        val remote = local.copy(key = "remote", updatedAt = at.plusSeconds(1))
        assertTrue(resolveCheckInPhotos("check-in", local, listOf(remote))?.key == "remote")
        assertTrue(resolveCheckInPhotos("check-in", local, listOf(remote.copy(checkInId = null))) == null)
        assertTrue(resolveCheckInPhotos("check-in", local, listOf(remote.copy(deleted = true))) == null)
        assertTrue(resolveCheckInPhotos("check-in", local.copy(pending = true), listOf(remote))?.key == "local")
        val retake = remote.copy(id = "photo-2", createdAt = at.plusSeconds(5))
        assertTrue(resolveCheckInPhotos("check-in", local, listOf(remote, retake))?.id == "photo-2")
    }
}
