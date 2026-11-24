package io.codepassion.doubletriangle

import java.time.Instant

/** The sync API returns the current server row even when it rejects an older write. */
internal fun checkInWriteAcknowledged(
    sentId: String,
    returnedId: String,
    sentAt: Instant,
    returnedAt: Instant,
    sameContent: Boolean,
): Boolean = sentId.equals(returnedId, ignoreCase = true) && sentAt == returnedAt && sameContent

/** A response to an older request cannot clear edits created while it was in flight. */
internal fun checkInVersionAcknowledged(sentVersion: String, currentVersion: String?): Boolean =
    currentVersion != null && currentVersion == sentVersion

/** Once submitted or removed on another device, the server copy is authoritative. */
internal fun shouldOverlayCheckInDraft(serverStatus: String, serverDeleted: Boolean): Boolean =
    !serverDeleted && serverStatus != "submitted"

internal fun checkInDraftStatus(submit: Boolean, previousStatus: String, queuedStatus: String?, answered: Boolean): String =
    when {
        submit -> "submitted"
        previousStatus == "draft" || queuedStatus == "draft" || answered -> "draft"
        else -> "pending"
    }

/** A linked asset recorded before submission may arrive after it, per the sync contract. */
internal fun maySyncCheckInAsset(serverDeleted: Boolean, submittedAt: Instant?, assetUpdatedAt: Instant): Boolean =
    !serverDeleted && (submittedAt == null || assetUpdatedAt <= submittedAt)

internal fun checkInTimestamp(value: String): Instant = runCatching { Instant.parse(value) }.getOrDefault(Instant.MIN)

internal data class CheckInMetricRevision(
    val key: String,
    val id: String,
    val updatedAt: Instant,
    val recordedAt: Instant,
    val deleted: Boolean,
    val pending: Boolean = false,
)

/** Keeps unsent edits, otherwise uses the newest live metric including remote tombstones. */
internal fun resolveCheckInMetric(local: List<CheckInMetricRevision>, remote: List<CheckInMetricRevision>): CheckInMetricRevision? {
    local.firstOrNull { it.pending }?.let { return it.takeUnless(CheckInMetricRevision::deleted) }
    val retained = local.filterNot { it.deleted }.filter { cached ->
        remote.none { it.id.equals(cached.id, ignoreCase = true) && it.updatedAt >= cached.updatedAt }
    }
    return (remote.filterNot { it.deleted } + retained).maxWithOrNull(compareBy<CheckInMetricRevision> { it.recordedAt }.thenBy { it.updatedAt })
}

internal data class CheckInPhotoRevision(
    val key: String,
    val id: String,
    val checkInId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean,
    val pending: Boolean = false,
)

internal fun resolveCheckInPhotos(checkInId: String, local: CheckInPhotoRevision?, remote: List<CheckInPhotoRevision>): CheckInPhotoRevision? {
    if (local?.pending == true) return local.takeUnless { it.deleted }
    val sameRemote = local?.let { cached -> remote.firstOrNull { it.id.equals(cached.id, ignoreCase = true) } }
    val keepLocal = local != null && !local.deleted && (sameRemote == null || sameRemote.updatedAt < local.updatedAt)
    val candidates = remote.filter { !it.deleted && it.checkInId.equals(checkInId, ignoreCase = true) } + listOfNotNull(local.takeIf { keepLocal })
    return candidates.maxWithOrNull(compareBy<CheckInPhotoRevision> { it.createdAt }.thenBy { it.updatedAt })
}
