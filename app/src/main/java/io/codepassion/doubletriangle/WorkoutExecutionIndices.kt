package io.codepassion.doubletriangle

/** Indices of the expanded Android path belonging to one logical planned exercise. */
internal fun workoutBlockExecutionIndices(start: Int, exerciseCount: Int, rounds: Int, type: String, exerciseIndex: Int): List<Int> {
    if (exerciseIndex !in 0 until exerciseCount) return emptyList()
    val repetitions = if (type.equals("superset", ignoreCase = true)) rounds.coerceAtLeast(1) else 1
    return List(repetitions) { round -> start + round * exerciseCount + exerciseIndex }
}

internal fun workoutBlockExecutionCount(exerciseCount: Int, rounds: Int, type: String): Int =
    exerciseCount.coerceAtLeast(0) * if (type.equals("superset", ignoreCase = true)) rounds.coerceAtLeast(1) else 1

internal data class PlannedExerciseSyncReference(val workoutId: String, val exerciseKey: String, val plannedExerciseId: String)

/** Never attach a result to another day or guess between repeated exercises. */
internal fun resolvePlannedExerciseId(workoutId: String?, exerciseKey: String, plannedExerciseId: String?, references: List<PlannedExerciseSyncReference>): String? {
    if (workoutId.isNullOrBlank()) return null
    val candidates = references.filter { it.workoutId.equals(workoutId, ignoreCase = true) && it.exerciseKey.equals(exerciseKey, ignoreCase = true) }
    if (plannedExerciseId != null) return candidates.firstOrNull { it.plannedExerciseId.equals(plannedExerciseId, ignoreCase = true) }?.plannedExerciseId
    return candidates.map { it.plannedExerciseId }.distinct().singleOrNull()
}
