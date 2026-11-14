package io.codepassion.doubletriangle

import java.time.Instant

/** The server returns its own row when it rejects an older client write. */
internal fun serverHasNewerWorkoutRecord(clientUpdatedAt: String, serverUpdatedAt: String): Boolean =
    Instant.parse(serverUpdatedAt).isAfter(Instant.parse(clientUpdatedAt))
