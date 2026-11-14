package io.codepassion.doubletriangle.nutrition

import java.util.Locale
import java.util.UUID

internal fun nutritionEntryId(userId: String, localId: Long): String =
    UUID.nameUUIDFromBytes("$userId:nutrition-entry:$localId".toByteArray()).toString()

internal fun missingNutritionLogIds(localRemoteIds: Iterable<String>, serverIds: Set<String>): Set<String> {
    val known = serverIds.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    return localRemoteIds.map { it.lowercase(Locale.ROOT) }.filterTo(mutableSetOf()) { it !in known }
}

/** Includes server tombstones: an intentionally deleted log must not be recreated. */
internal fun hasUnsentNutritionLogs(localRemoteIds: Iterable<String>, serverIds: Set<String>): Boolean {
    return missingNutritionLogIds(localRemoteIds, serverIds).isNotEmpty()
}

internal fun nutritionPushDependencies(resources: Set<String>): Set<String> =
    if (resources.any { it == "nutrition-log-media" || it == "nutrition-log-entries" || it == "nutrition-log-items" })
        resources + "nutrition-log-media" + "nutrition-log-entries"
    else resources
