package io.codepassion.doubletriangle.feature.workout

internal fun String?.visibleWorkoutBlockNotes(): String? =
    this?.takeIf { it.isNotBlank() && !it.trim().equals("null", ignoreCase = true) }
