package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType

internal enum class WorkoutRestContext {
    BetweenSets,
    BetweenRounds,
    BeforeNextBlock,
}

internal data class CompletedExerciseMetrics(
    val repetitions: Int,
    val weightKg: Double,
    val durationSeconds: Int,
)

internal fun completedExerciseMetrics(
    exercise: ExerciseSummary,
    repetitions: Int,
    weightKg: Double,
    initialDurationSeconds: Int,
    remainingDurationSeconds: Int,
): CompletedExerciseMetrics {
    val isTimed = exercise.resolvedTargetDurationSeconds() != null
    return CompletedExerciseMetrics(
        repetitions = if (isTimed) 0 else repetitions.coerceAtLeast(0),
        weightKg = if (isTimed) 0.0 else weightKg.coerceAtLeast(0.0),
        durationSeconds = if (isTimed) (initialDurationSeconds - remainingDurationSeconds).coerceAtLeast(0) else 0,
    )
}

internal fun shouldStartRest(restSeconds: Int, skipsRestPeriods: Boolean): Boolean =
    !skipsRestPeriods && restSeconds > 0

/** Matches iOS: a superset collects feedback once, after its last exercise in its last round. */
internal fun shouldRequestExerciseFeedback(exercise: ExerciseSummary): Boolean = when (exercise.blockType) {
    WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown -> false
    WorkoutBlockType.Standard -> true
    WorkoutBlockType.Superset -> exercise.isLastInBlock && exercise.blockRound == exercise.blockRounds
}
