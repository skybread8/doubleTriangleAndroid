package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary

internal data class WorkoutPathExercise(
    val exercise: ExerciseSummary,
    val label: String?,
    val rounds: Int,
    val executionIndices: List<Int>,
)

internal data class WorkoutPathBlock(
    val type: WorkoutBlockType,
    val rounds: Int,
    val exercises: List<WorkoutPathExercise>,
    val notes: String? = null,
    val restAfterBlockSeconds: Int? = null,
)

internal fun WorkoutDaySummary.pathBlocks(): List<WorkoutPathBlock> {
    var executionCursor = 0
    return editableBlocks().map { block ->
        val items = if (block.type == WorkoutBlockType.Superset) {
            block.exercises.mapIndexed { exerciseIndex, exercise ->
                WorkoutPathExercise(
                    exercise = exercise,
                    label = "A${exerciseIndex + 1}",
                    rounds = block.rounds.coerceAtLeast(1),
                    executionIndices = List(block.rounds.coerceAtLeast(1)) { round -> executionCursor + round * block.exercises.size + exerciseIndex },
                )
            }.also { executionCursor += block.exercises.size * block.rounds.coerceAtLeast(1) }
        } else {
            block.exercises.map { exercise ->
                WorkoutPathExercise(exercise, null, 1, listOf(executionCursor++))
            }
        }
        WorkoutPathBlock(block.type, block.rounds.coerceAtLeast(1), items, block.notes, block.restAfterBlockSeconds)
    }
}

internal fun List<WorkoutPathBlock>.mainExercises(): List<WorkoutPathExercise> =
    flatMap { block ->
        if (block.type in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)) emptyList() else block.exercises
    }

internal fun WorkoutPathBlock.skippedExecutionIndices(currentIndex: Int, wholeBlock: Boolean): Set<Int> =
    if (wholeBlock) exercises.flatMap { it.executionIndices }.toSet()
    else exercises.firstOrNull { currentIndex in it.executionIndices }?.executionIndices?.toSet() ?: setOf(currentIndex)

internal fun nextUnskippedExecutionIndex(currentIndex: Int, count: Int, skipped: Set<Int>): Int? =
    (currentIndex + 1 until count).firstOrNull { it !in skipped }

internal fun firstUnskippedExecutionIndex(start: Int, count: Int, skipped: Set<Int>): Int? =
    (start.coerceAtLeast(0) until count).firstOrNull { it !in skipped }

internal fun List<WorkoutPathExercise>.skippedExerciseCount(skipped: Set<Int>): Int =
    count { item -> item.executionIndices.any { it in skipped } }

internal fun WorkoutPathExercise.isCompleted(workout: WorkoutDaySummary, completedSets: Map<Int, Int>, addedSets: Map<Int, Int> = emptyMap()): Boolean =
    executionIndices.all { index -> (completedSets[index] ?: 0) >= workout.exercises[index].sets + (addedSets[index] ?: 0) }

/** Only exercises that have not started may move; both ends stay in the same block. */
internal fun WorkoutPathBlock.canMoveUpcoming(from: Int, to: Int, currentIndex: Int): Boolean {
    if (from == to || from !in exercises.indices || to !in exercises.indices) return false
    return listOf(from, to).all { index ->
        exercises[index].executionIndices.minOrNull()?.let { it > currentIndex } == true
    }
}
