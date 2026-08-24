package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.executionExercises

internal fun WorkoutDaySummary.editableBlocks(): List<WorkoutBlockSummary> =
    blocks.ifEmpty { exercises.map { WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(it)) } }.workoutOrder()

internal fun List<WorkoutBlockSummary>.createSupersetAt(index: Int): List<WorkoutBlockSummary> {
    val first = getOrNull(index) ?: return this
    val second = getOrNull(index + 1) ?: return this
    if (first.type != WorkoutBlockType.Standard || second.type != WorkoutBlockType.Standard) return this
    if (first.exercises.size != 1 || second.exercises.size != 1) return this
    val exercises = listOf(first.exercises.single(), second.exercises.single())
    val rounds = exercises.maxOf { it.sets }.coerceAtLeast(1)
    val rest = exercises.maxOf { it.restSeconds }.coerceAtLeast(15)
    val superset = WorkoutBlockSummary(
        type = WorkoutBlockType.Superset,
        rounds = rounds,
        restAfterBlockSeconds = rest,
        exercises = exercises.map { it.copy(sets = 1, restSeconds = 0) },
    )
    return take(index) + superset + drop(index + 2)
}

internal fun List<WorkoutBlockSummary>.splitSupersetAt(index: Int): List<WorkoutBlockSummary> {
    val block = getOrNull(index) ?: return this
    if (block.type != WorkoutBlockType.Superset) return this
    val restored = block.exercises.map { exercise ->
        WorkoutBlockSummary(
            type = WorkoutBlockType.Standard,
            exercises = listOf(exercise.copy(sets = block.rounds.coerceAtLeast(1), restSeconds = block.restAfterBlockSeconds ?: 60)),
        )
    }
    return take(index) + restored + drop(index + 1)
}

internal fun List<WorkoutBlockSummary>.cycleSectionAt(index: Int): List<WorkoutBlockSummary> = mapIndexed { itemIndex, block ->
    if (itemIndex != index || block.type == WorkoutBlockType.Superset) block else block.copy(
        type = when (block.type) {
            WorkoutBlockType.Standard -> WorkoutBlockType.Warmup
            WorkoutBlockType.Warmup -> WorkoutBlockType.Cooldown
            WorkoutBlockType.Cooldown -> WorkoutBlockType.Standard
            WorkoutBlockType.Superset -> WorkoutBlockType.Superset
        },
    )
}.workoutOrder()

internal fun WorkoutDaySummary.withEditableBlocks(blocks: List<WorkoutBlockSummary>): WorkoutDaySummary =
    blocks.workoutOrder().let { ordered -> copy(blocks = ordered, exercises = ordered.executionExercises()) }

internal fun List<WorkoutBlockSummary>.workoutOrder(): List<WorkoutBlockSummary> = sortedBy { block ->
    when (block.type) {
        WorkoutBlockType.Warmup -> 0
        WorkoutBlockType.Standard, WorkoutBlockType.Superset -> 1
        WorkoutBlockType.Cooldown -> 2
    }
}

internal fun List<WorkoutBlockSummary>.updateBlockExercise(blockIndex: Int, exerciseIndex: Int, transform: (io.codepassion.doubletriangle.core.model.ExerciseSummary) -> io.codepassion.doubletriangle.core.model.ExerciseSummary): List<WorkoutBlockSummary> =
    mapIndexed { index, block ->
        if (index != blockIndex) block else block.copy(exercises = block.exercises.mapIndexed { itemIndex, exercise -> if (itemIndex == exerciseIndex) transform(exercise) else exercise })
    }

internal fun List<WorkoutBlockSummary>.removeBlockExercise(blockIndex: Int, exerciseIndex: Int): List<WorkoutBlockSummary> {
    val block = getOrNull(blockIndex) ?: return this
    val remaining = block.exercises.filterIndexed { index, _ -> index != exerciseIndex }
    return when {
        remaining.isEmpty() -> filterIndexed { index, _ -> index != blockIndex }
        block.type == WorkoutBlockType.Superset && remaining.size == 1 -> mapIndexed { index, current ->
            if (index != blockIndex) current else WorkoutBlockSummary(
                WorkoutBlockType.Standard,
                exercises = listOf(remaining.single().copy(sets = block.rounds, restSeconds = block.restAfterBlockSeconds ?: 60)),
            )
        }
        else -> mapIndexed { index, current -> if (index == blockIndex) current.copy(exercises = remaining) else current }
    }
}

internal fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().also { item -> item.add(to, item.removeAt(from)) }
}
