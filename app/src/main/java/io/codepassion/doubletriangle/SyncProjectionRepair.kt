package io.codepassion.doubletriangle

/** A saved cursor is only usable when the corresponding projection survived. */
internal fun needsFullProjectionRepair(hasUserWatermark: Boolean, userRecordCount: Int): Boolean =
    hasUserWatermark && userRecordCount == 0
