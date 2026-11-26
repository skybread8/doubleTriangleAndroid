package io.codepassion.doubletriangle

/** Matches iOS Codable: omit the optional watermark until a resource was pulled. */
internal fun syncBatchResource(resource: String, watermark: String?): Map<String, String> = buildMap {
    put("resource", resource)
    // Membership changes can remove a program from the server's scoped query
    // without producing a tombstone. Always refresh this small collection.
    if (watermark != null && resource != "gym-programs") put("updated_after", watermark)
}

internal fun replaceSyncProjection(resource: String, wasPulled: Boolean): Boolean =
    resource == "gym-programs" && wasPulled
