package io.codepassion.doubletriangle.feature.workout

/** Compact motion payload received from the Wear companion for the active set. */
data class WatchMotionRepData(
    val durationMillis: Int,
    val rangeOfMotionPermille: Int,
    val velocityCode: Int,
)

data class WatchMotionSnapshotData(
    val statusCode: Int = 0,
    val autoDetectedRepCount: Int = 0,
    val confidencePermille: Int? = null,
    val reps: List<WatchMotionRepData> = emptyList(),
) {
    val isVisible: Boolean get() = statusCode != 0
}
