package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.displayBlocks

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
)

internal fun WorkoutDaySummary.pathBlocks(): List<WorkoutPathBlock> {
    var executionCursor = 0
    return displayBlocks().map { block ->
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
        WorkoutPathBlock(block.type, block.rounds.coerceAtLeast(1), items)
    }
}

internal fun List<WorkoutPathBlock>.mainExercises(): List<WorkoutPathExercise> =
    flatMap { block ->
        if (block.type in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)) emptyList() else block.exercises
    }

internal fun WorkoutPathExercise.isCompleted(workout: WorkoutDaySummary, completedSets: Map<Int, Int>): Boolean =
    executionIndices.all { index -> (completedSets[index] ?: 0) >= workout.exercises[index].sets }

internal fun nextIndexAfterPreparationSection(exercises: List<ExerciseSummary>, currentIndex: Int): Int? {
    val type = exercises.getOrNull(currentIndex)?.blockType ?: return null
    if (type !in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)) return currentIndex
    return ((currentIndex + 1)..exercises.size).firstOrNull { index -> exercises.getOrNull(index)?.blockType != type }
        ?.takeIf { it < exercises.size }
}
