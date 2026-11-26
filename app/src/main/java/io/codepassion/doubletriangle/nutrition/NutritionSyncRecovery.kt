package io.codepassion.doubletriangle.nutrition

import java.util.Locale
import java.util.UUID
import java.time.Instant

internal fun nutritionEntryId(userId: String, localId: Long): String =
    UUID.nameUUIDFromBytes("$userId:nutrition-entry:$localId".toByteArray()).toString()

/** iOS sends a deletion only when the record actually has a deletion timestamp. */
internal fun shouldSendNutritionTombstone(resource: String, deletionTimestamp: String?): Boolean =
    resource !in setOf("nutrition-log-entries", "nutrition-log-items") ||
        deletionTimestamp?.let { runCatching { Instant.parse(it) }.isSuccess } == true

/** Assign distinct local identities to server UUIDs, retaining established mappings. */
internal fun resolveRemoteNutritionIdentities(
    userId: String,
    localSources: Map<Long, String>,
    storedByLocal: Map<Long, String>,
    remoteIds: Collection<String>,
): Map<String, Long> {
    val result = linkedMapOf<String, Long>()
    val occupied = mutableSetOf<Long>()
    storedByLocal.entries.sortedBy { it.key }.forEach { (local, remote) ->
        val key = remote.lowercase(Locale.ROOT)
        if (key !in result && occupied.add(local)) result[key] = local
    }
    // Only locally created entries have the deterministic outbound UUID. A
    // server-imported entry must never claim another server row's identity.
    localSources.filterValues { it != "sync" }.forEach { (local, _) ->
        val key = nutritionEntryId(userId, local).lowercase(Locale.ROOT)
        if (key !in result && occupied.add(local)) result[key] = local
    }
    remoteIds.map { it.lowercase(Locale.ROOT) }.distinct().sorted().forEach { remote ->
        if (remote !in result) {
            var suffix = 0
            var candidate = stableRemoteLogId(remote)
            while (candidate in occupied || (candidate in localSources && localSources[candidate] != "sync")) {
                suffix++
                candidate = stableRemoteLogId("$remote#$suffix")
            }
            occupied += candidate
            result[remote] = candidate
        }
    }
    return result
}

internal fun nutritionPushDependencies(resources: Set<String>): Set<String> =
    if (resources.any { it == "nutrition-log-media" || it == "nutrition-log-entries" || it == "nutrition-log-items" })
        resources + "nutrition-log-media" + "nutrition-log-entries"
    else resources

/** Deleting an iOS log entry does not soft-delete its food item rows. */
internal fun itemIdsForDeletedNutritionEntries(parentByItem: Map<String, String>, deletedEntryIds: Set<String>): Set<String> {
    val deleted = deletedEntryIds.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    return parentByItem.filterValues { it.lowercase(Locale.ROOT) in deleted }.keys
}

/** Imported media keeps the server identity; a new local attachment gets its own row. */
internal fun shouldReuseRemoteNutritionMedia(
    hasRemoteId: Boolean,
    hasLocalPhoto: Boolean,
    isDownloadedPhoto: Boolean,
    hasRemoteUrl: Boolean,
    importedEntry: Boolean,
    isUploadedPhoto: Boolean = false,
): Boolean = hasRemoteId && (isDownloadedPhoto || isUploadedPhoto || (!hasLocalPhoto && (!hasRemoteUrl || importedEntry)))

/** Acknowledging metadata before the image queue drains would lose retries. */
internal fun canAcknowledgeNutritionMedia(hasFailedUpload: Boolean, hasUnresolvedLegacy: Boolean): Boolean =
    !hasFailedUpload && !hasUnresolvedLegacy

/** A persisted child ID must never be reassigned to another remote parent. */
internal fun nutritionItemBelongsToParent(remoteParent: String?, parent: String): Boolean =
    remoteParent.isNullOrBlank() || remoteParent.equals(parent, ignoreCase = true)
