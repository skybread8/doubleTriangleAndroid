package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationProfile
import java.time.Instant
import java.util.UUID

internal data class RemoteLocationVersion(val id: String, val updatedAt: String, val value: TrainingLocationProfile?, val order: Int)
internal data class LocationMergeResult(val locations: List<TrainingLocationProfile>, val resolvedDeletions: Set<String>)
internal data class StampedLocationChanges(val locations: List<TrainingLocationProfile>, val deletedIds: Set<String>)

/** Assign stable identities and the modification date of each changed location. */
internal fun stampLocationChanges(old: List<TrainingLocationProfile>, edited: List<TrainingLocationProfile>, now: String): StampedLocationChanges {
    val used = mutableSetOf<Int>()
    val result = edited.mapIndexed { index, location ->
        val byId = location.id?.let { id -> old.indices.firstOrNull { it !in used && old[it].id.equals(id, true) } }
        val byValue = old.indices.filter { it !in used && old[it].copy(id = null, updatedAt = null) == location.copy(id = null, updatedAt = null) }.singleOrNull()
        val previousIndex = byId ?: byValue ?: index.takeIf { it in old.indices && it !in used }
        val previous = previousIndex?.let { used += it; old[it] }
        val id = location.id ?: previous?.id ?: UUID.randomUUID().toString()
        val same = previous != null && previous.copy(id = null, updatedAt = null) == location.copy(id = null, updatedAt = null) && previousIndex == index
        location.copy(id = id, updatedAt = if (same) previous?.updatedAt else now)
    }
    return StampedLocationChanges(result, old.indices.filterNot { it in used }.mapNotNull { old[it].id?.lowercase() }.toSet())
}

/** SwiftData applies a row or tombstone only when its timestamp is newer. */
internal fun mergeLocationVersions(local: List<TrainingLocationProfile>, remote: List<RemoteLocationVersion>, pendingDeletions: Map<String, String>): LocationMergeResult {
    val rows = local.mapIndexedNotNull { index, item -> item.id?.lowercase()?.let { it to (index to item) } }.toMap().toMutableMap()
    val resolved = mutableSetOf<String>()
    remote.forEach { incoming ->
        val id = incoming.id.lowercase()
        val remoteTime = runCatching { Instant.parse(incoming.updatedAt) }.getOrNull() ?: return@forEach
        val deletionTime = pendingDeletions[id]?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (deletionTime != null) {
            if (!remoteTime.isAfter(deletionTime)) return@forEach
            resolved += id
        }
        val existing = rows[id]
        val localTime = existing?.second?.updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (existing != null && (localTime == null || !remoteTime.isAfter(localTime))) return@forEach
        if (incoming.value == null) rows.remove(id) else rows[id] = incoming.order to incoming.value
    }
    return LocationMergeResult(rows.values.sortedWith(compareBy<Pair<Int, TrainingLocationProfile>> { it.first }.thenBy { it.second.id }).map { it.second }, resolved)
}
