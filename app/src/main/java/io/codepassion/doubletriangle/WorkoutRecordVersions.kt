package io.codepassion.doubletriangle

import java.time.Instant

/** A missing timestamp represents a legacy local change whose age is unknown. */
internal data class WorkoutRecordVersion<T>(
    val id: String,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
    val value: T,
)

/** Merge by identity without inventing an age for legacy pending edits. */
internal fun <T> mergeWorkoutRecordVersions(
    local: Collection<WorkoutRecordVersion<T>>,
    remote: Collection<WorkoutRecordVersion<T>>,
): List<WorkoutRecordVersion<T>> {
    val merged = linkedMapOf<String, WorkoutRecordVersion<T>>()
    local.forEach { record ->
        require(record.id.isNotBlank())
        check(merged.put(record.id.lowercase(java.util.Locale.ROOT), record) == null) { "Duplicate local workout record" }
    }
    val remoteIds = mutableSetOf<String>()
    remote.forEach { record ->
        require(record.id.isNotBlank() && record.updatedAt != null)
        val id = record.id.lowercase(java.util.Locale.ROOT)
        check(remoteIds.add(id)) { "Duplicate remote workout record" }
        val existing = merged[id]
        if (existing == null || (existing.updatedAt != null && record.updatedAt > existing.updatedAt)) {
            merged[id] = record
        }
    }
    return merged.values.toList()
}
