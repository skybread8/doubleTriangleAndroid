package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.RepRange
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.executionExercises

/** A single repetition control replaces any previously prescribed per-set targets. */
internal fun ExerciseSummary.withUniformRepTarget(value: Int): ExerciseSummary {
    val repetitions = value.coerceAtLeast(1)
    return copy(reps = repetitions.toString(), repRange = RepRange(repetitions, repetitions), targetRepsPerSet = null)
}

internal fun ExerciseSummary.withRepetitionTracking(): ExerciseSummary {
    val repetitions = repRange?.maximum ?: targetRepsPerSet?.firstOrNull()
        ?: (if (trackingMode == ExerciseTrackingMode.Repetitions) reps.toIntOrNull() else null)
        ?: 10
    return withUniformRepTarget(repetitions).copy(
        trackingMode = ExerciseTrackingMode.Repetitions,
        targetDurationSeconds = null,
        targetDurationMinutes = null,
        targetDistanceKm = null,
        targetPaceSecondsPerKm = null,
    )
}

internal fun WorkoutDaySummary.editableBlocks(): List<WorkoutBlockSummary> =
    blocks.ifEmpty { exercises.map { WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(it)) } }.workoutOrder()

/** Merge by planned-exercise ID; older snapshots use a unique, structurally matching catalog identity. */
internal fun reconcileImportedSkips(local: List<WorkoutBlockSummary>, imported: List<WorkoutBlockSummary>): List<WorkoutBlockSummary> {
    fun key(exercise: ExerciseSummary): String? = exercise.coachExerciseId?.let { "coach:$it" }
        ?: exercise.imageKey?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    val sameStructure = local.size == imported.size && local.indices.none { index ->
            local[index].type != imported[index].type || local[index].rounds != imported[index].rounds ||
                local[index].exercises.size != imported[index].exercises.size ||
                local[index].exercises.indices.any { key(local[index].exercises[it]) != key(imported[index].exercises[it]) }
        }
    val importedExercises = imported.flatMap { it.exercises }
    val byId = importedExercises.filter { it.plannedExerciseId != null }.groupBy { it.plannedExerciseId!! }
    val localIdCounts = local.flatMap { it.exercises }.mapNotNull { it.plannedExerciseId }.groupingBy { it }.eachCount()
    val keyCounts = importedExercises.mapNotNull(::key).groupingBy { it }.eachCount()
    return local.mapIndexed { blockIndex, block ->
        block.copy(exercises = block.exercises.mapIndexed { index, exercise ->
            val identity = key(exercise)
            val id = exercise.plannedExerciseId
            val importedExercise = if (id != null && localIdCounts[id] == 1) byId[id]?.singleOrNull()
                else if (id == null && sameStructure && identity != null && keyCounts[identity] == 1) imported[blockIndex].exercises[index]
                else null
            if (importedExercise == null) exercise else exercise.copy(
                skippedAt = exercise.skippedAt ?: importedExercise.skippedAt,
                plannedExerciseId = exercise.plannedExerciseId ?: importedExercise.plannedExerciseId,
            )
        })
    }
}

internal fun WorkoutDaySummary.asIndependentWorkoutCopy(): WorkoutDaySummary {
    fun ExerciseSummary.independentCopy() = copy(skippedAt = null, plannedExerciseId = java.util.UUID.randomUUID().toString())
    if (blocks.isEmpty()) return copy(exercises = exercises.map { it.independentCopy() })
    val copied = blocks.map { block -> block.copy(exercises = block.exercises.map { it.independentCopy() }) }
    return copy(blocks = copied, exercises = copied.executionExercises())
}

internal fun WorkoutDaySummary.clearExerciseOmissions(): WorkoutDaySummary {
    if (blocks.isEmpty()) return copy(exercises = exercises.map { it.copy(skippedAt = null) })
    val cleared = blocks.map { block -> block.copy(exercises = block.exercises.map { it.copy(skippedAt = null) }) }
    return copy(blocks = cleared, exercises = cleared.executionExercises())
}

internal fun WorkoutDaySummary.resetCancelledWorkout(): WorkoutDaySummary = clearExerciseOmissions().copy(
    status = WorkoutStatus.Planned, startedAt = null, completedAt = null, didCountTowardStreak = false,
    activeDurationSeconds = null, activeCaloriesBurned = null, averageHeartRate = null,
    maximumHeartRate = null, totalVolumeKg = null,
)

internal fun List<WorkoutBlockSummary>.blockIndexForExecution(executionIndex: Int): Int? {
    if (executionIndex < 0) return null
    var offset = 0
    forEachIndexed { index, block ->
        val size = block.executionExercises().size
        if (executionIndex in offset until offset + size) return index
        offset += size
    }
    return null
}

internal fun List<WorkoutBlockSummary>.canCreateActiveSuperset(executionIndex: Int): Boolean {
    val blockIndex = blockIndexForExecution(executionIndex) ?: return false
    val block = this[blockIndex]
    if (block.type != WorkoutBlockType.Standard) return false
    val offset = take(blockIndex).sumOf { it.executionExercises().size }
    return executionIndex - offset in 0 until block.exercises.lastIndex
}

/** iOS groups the selected exercise and its next neighbour in the same main block. */
internal fun List<WorkoutBlockSummary>.createActiveSuperset(executionIndex: Int): List<WorkoutBlockSummary> {
    if (!canCreateActiveSuperset(executionIndex)) return this
    val blockIndex = blockIndexForExecution(executionIndex) ?: return this
    val block = this[blockIndex]
    val offset = take(blockIndex).sumOf { it.executionExercises().size }
    val position = executionIndex - offset
    val selected = block.exercises.subList(position, position + 2)
    val replacement = buildList {
        val before = block.exercises.take(position)
        if (before.isNotEmpty()) add(block.copy(exercises = before))
        add(WorkoutBlockSummary(
            WorkoutBlockType.Superset,
            rounds = selected.maxOf { it.sets.coerceAtLeast(1) },
            restAfterBlockSeconds = selected.maxOf { it.restSeconds },
            exercises = selected,
        ))
        val after = block.exercises.drop(position + 2)
        if (after.isNotEmpty()) add(block.copy(exercises = after))
    }
    return take(blockIndex) + replacement + drop(blockIndex + 1)
}

private data class ExecutionSlot(val sourceIndex: Int, val round: Int)

private fun List<WorkoutBlockSummary>.executionSlots(): List<ExecutionSlot> {
    var sourceOffset = 0
    return buildList {
        for (block in this@executionSlots) {
            val rounds = if (block.type == WorkoutBlockType.Superset) block.rounds.coerceAtLeast(1) else 1
            repeat(rounds) { round ->
                block.exercises.indices.forEach { add(ExecutionSlot(sourceOffset + it, round)) }
            }
            sourceOffset += block.exercises.size
        }
    }
}

/** Maps execution indices by source object and round, even when equal exercises occur twice. */
internal fun executionIndexRemap(old: List<WorkoutBlockSummary>, new: List<WorkoutBlockSummary>, sourceOverrides: Map<Int, Int> = emptyMap()): Map<Int, Int> {
    val oldSources = old.flatMap { it.exercises }
    val newSources = new.flatMap { it.exercises }
    val claimed = sourceOverrides.values.toMutableSet()
    val sourceMap = oldSources.mapIndexedNotNull { oldIndex, item ->
        val newIndex = sourceOverrides[oldIndex] ?: newSources.indices.firstOrNull { it !in claimed && newSources[it] === item }
        newIndex?.let { claimed += it; oldIndex to it }
    }.toMap()
    val destination = new.executionSlots().withIndex().associate { it.value to it.index }
    val lastSlotBySource = destination.keys.groupBy { it.sourceIndex }.mapValues { (_, slots) -> slots.maxBy { it.round } }
    return old.executionSlots().mapIndexedNotNull { index, slot ->
        val targetSource = sourceMap[slot.sourceIndex] ?: return@mapIndexedNotNull null
        val target = destination[ExecutionSlot(targetSource, slot.round)]
            ?: lastSlotBySource[targetSource]?.let(destination::get)
        target?.let { index to it }
    }.toMap()
}

/** iOS expands a split superset into standard exercises with enough sets for its targets and rounds. */
internal fun List<WorkoutBlockSummary>.splitActiveSuperset(blockIndex: Int): List<WorkoutBlockSummary> {
    val block = getOrNull(blockIndex) ?: return this
    if (block.type != WorkoutBlockType.Superset) return this
    val split = block.exercises.map { exercise ->
        val sets = maxOf(exercise.sets, exercise.targetRepsPerSet?.size ?: 0, exercise.targetWeightsKg?.size ?: 0, block.rounds, 1)
        WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(exercise.copy(sets = sets)))
    }
    return take(blockIndex) + split + drop(blockIndex + 1)
}

internal fun <T> Map<Int, T>.remapExecutionIndices(mapping: Map<Int, Int>): Map<Int, T> =
    mapNotNull { (index, value) -> mapping[index]?.let { it to value } }.toMap()

internal fun Map<Int, Int>.sumRemappedExecutionIndices(mapping: Map<Int, Int>): Map<Int, Int> =
    entries.mapNotNull { (index, value) -> mapping[index]?.let { it to value } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.sum() }

internal fun Map<Int, ExerciseSessionStats>.remapAndCombineStats(mapping: Map<Int, Int>): Map<Int, ExerciseSessionStats> =
    entries.mapNotNull { (index, value) -> mapping[index]?.let { it to value } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, values) ->
            val weighted = values.filter { it.averageWeightKg > 0 && it.sets > 0 }
            val weightSets = weighted.sumOf { it.sets }
            ExerciseSessionStats(
                sets = values.sumOf { it.sets }, totalReps = values.sumOf { it.totalReps },
                maxWeightKg = values.maxOf { it.maxWeightKg }, volumeKg = values.sumOf { it.volumeKg },
                durationSeconds = values.sumOf { it.durationSeconds }, distanceKm = values.sumOf { it.distanceKm },
                averageWeightKg = if (weightSets > 0) weighted.sumOf { it.averageWeightKg * it.sets } / weightSets else 0.0,
            )
        }

internal fun List<CompletedSetRecord>.remapSetRecords(mapping: Map<Int, Int>): List<CompletedSetRecord> {
    val mergedTargets = mapping.entries.groupingBy { it.value }.eachCount().filterValues { it > 1 }.keys
    val counters = mutableMapOf<Int, Int>()
    return mapNotNull { record ->
        val target = mapping[record.exerciseIndex] ?: return@mapNotNull null
        val setNumber = if (target in mergedTargets) {
            (counters[target] ?: 0).plus(1).also { counters[target] = it }
        } else record.setNumber
        record.copy(exerciseIndex = target, setNumber = setNumber)
    }
}

/** iOS inserts after a superset as a separate block, not into every round. */
internal fun List<WorkoutBlockSummary>.insertExerciseAfter(
    blockIndex: Int,
    exerciseIndex: Int,
    exercise: ExerciseSummary,
): List<WorkoutBlockSummary> {
    val block = getOrNull(blockIndex) ?: return this + WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(exercise))
    if (block.type == WorkoutBlockType.Superset) {
        return take(blockIndex + 1) + WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(exercise)) + drop(blockIndex + 1)
    }
    val insertAt = (exerciseIndex + 1).coerceIn(0, block.exercises.size)
    val updated = block.copy(exercises = block.exercises.toMutableList().apply { add(insertAt, exercise) })
    return toMutableList().also { it[blockIndex] = updated }
}

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
