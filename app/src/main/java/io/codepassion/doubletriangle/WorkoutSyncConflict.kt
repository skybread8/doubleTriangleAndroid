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
