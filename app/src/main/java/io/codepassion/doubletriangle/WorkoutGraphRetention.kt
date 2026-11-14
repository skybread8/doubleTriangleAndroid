package io.codepassion.doubletriangle

internal data class WorkoutGraphRetention(val days: Set<String>, val blocks: Set<String>, val exercises: Set<String>)

internal fun historicalWorkoutGraph(
    currentPlanId: String,
    days: Map<String, String?>,
    blocks: Map<String, String?>,
    exercises: Map<String, String?>,
): WorkoutGraphRetention {
    val historicalDays = days.filterValues { it != currentPlanId }.keys
    val historicalBlocks = blocks.filterValues { it in historicalDays }.keys
    val historicalExercises = exercises.filterValues { it in historicalBlocks }.keys
    return WorkoutGraphRetention(historicalDays, historicalBlocks, historicalExercises)
}
