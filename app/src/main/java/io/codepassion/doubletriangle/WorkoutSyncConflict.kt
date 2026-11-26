package io.codepassion.doubletriangle

import java.time.Instant

/** The server returns its own row when it rejects an older client write. */
internal fun serverHasNewerWorkoutRecord(clientUpdatedAt: String, serverUpdatedAt: String): Boolean =
    Instant.parse(serverUpdatedAt).isAfter(Instant.parse(clientUpdatedAt))

/** iOS applies a remote row only when its timestamp is newer than the local row. */
internal fun shouldApplyRemoteRecord(localUpdatedAt: String?, remoteUpdatedAt: String?): Boolean {
    val remote = remoteUpdatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
    val local = localUpdatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return true
    return remote.isAfter(local)
}

/** A full gym projection contains unchanged server rows; only local edits are sent. */
internal fun shouldPushGymRow(localUpdatedAt: String?, remoteUpdatedAt: String?): Boolean =
    remoteUpdatedAt == null || shouldApplyRemoteRecord(remoteUpdatedAt, localUpdatedAt)


/** A missing baseline row must have a dated local tombstone before a concurrent pull is safe. */
internal fun canMergeVersionedRecords(
    localVersions: Map<String, String?>,
    baselineIds: Set<String>,
    pendingDeletions: Map<String, String?>,
): Boolean {
    if (localVersions.any { (id, version) -> id.isBlank() || version == null || runCatching { Instant.parse(version) }.isFailure }) return false
    return baselineIds.all { id ->
        val key = id.lowercase()
        localVersions.keys.any { it.equals(id, ignoreCase = true) } ||
            pendingDeletions[key]?.let { runCatching { Instant.parse(it) }.isSuccess } == true
    }
}

/** Preserve changed records and local tombstones while importing unrelated rows. */
internal fun protectedVersionedRecordIds(
    localVersions: Map<String, String?>,
    baselineVersions: Map<String, String?>,
    pendingDeletions: Map<String, String?>,
): Set<String>? {
    if (!canMergeVersionedRecords(localVersions, baselineVersions.keys, pendingDeletions)) return null
    return buildSet {
        addAll(pendingDeletions.keys.map { it.lowercase() })
        localVersions.forEach { (id, version) ->
            val previous = baselineVersions[id.lowercase()]
            if (previous.isNullOrBlank() || shouldApplyRemoteRecord(previous, version)) add(id.lowercase())
        }
    }
}

/** Legacy pending edits without a timestamp must not be discarded. */
internal fun remoteDeletionWins(localDirty: Boolean, localUpdatedAtMillis: Long?, remoteUpdatedAt: String?): Boolean {
    val remote = remoteUpdatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
    if (!localDirty) return true
    val local = localUpdatedAtMillis?.let(Instant::ofEpochMilli) ?: return false
    return remote.isAfter(local)
}

/** A legacy graph can be replaced only if no row has an independently dated edit. */
internal fun canReplaceLegacyWorkoutGraph(
    dirtyResources: Set<String>,
    hasVersionedRows: Boolean,
    startingGenerations: Map<String, Long>,
    currentGenerations: Map<String, Long>,
): Boolean = dirtyResources.isNotEmpty() && !hasVersionedRows &&
    dirtyResources.all { startingGenerations[it] != null && startingGenerations[it] == currentGenerations[it] }

internal enum class LegacyWorkoutRecordAction { UploadVersioned, UseRemote, UseMatchingRemote, KeepPending }

internal fun legacyWorkoutRecordAction(hasVersion: Boolean, hasRemote: Boolean, remoteMatches: Boolean): LegacyWorkoutRecordAction = when {
    hasVersion -> LegacyWorkoutRecordAction.UploadVersioned
    !hasRemote -> LegacyWorkoutRecordAction.KeepPending
    remoteMatches -> LegacyWorkoutRecordAction.UseMatchingRemote
    else -> LegacyWorkoutRecordAction.UseRemote
}

/** The server may encode a whole number without the decimal suffix Android uses. */
internal fun canonicalSyncNumber(value: Number): String =
    runCatching { java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString() }
        .getOrElse { value.toString() }

/** A missing local photo session is not a deletion unless the user deleted it. */
internal fun shouldSendBodyPhotoTombstone(deletedAt: String?): Boolean =
    deletedAt?.let { runCatching { Instant.parse(it) }.isSuccess } == true
