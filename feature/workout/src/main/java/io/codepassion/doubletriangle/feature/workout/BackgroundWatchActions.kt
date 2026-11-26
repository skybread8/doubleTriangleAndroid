package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.executionExercises

/** Applies watch actions to the same persisted session owned by the foreground service. */
internal object BackgroundWatchActions {
    fun apply(snapshot: WorkoutSessionSnapshot, command: WildforceWatchLocalBridge.Command): WorkoutSessionSnapshot? {
        val blocks = snapshot.editableBlocks ?: return null
        val exercises = blocks.flatMap { it.executionExercises() }
        val index = snapshot.exerciseIndex
        val exercise = exercises.getOrNull(index) ?: return null
        val rest = snapshot.restRemaining?.takeIf { it > 0 }
        val current = if (snapshot.restRemaining != null && rest == null) snapshot.copy(restRemaining = null, restContext = null, restBetweenExercises = false) else snapshot
        fun targetReps(value: ExerciseSummary, set: Int): Int = value.targetRepsPerSet?.getOrNull(set)
            ?: Regex("\\d+").find(value.reps)?.value?.toIntOrNull() ?: 10
        fun next(from: WorkoutSessionSnapshot, restSeconds: Int): WorkoutSessionSnapshot {
            val nextIndex = nextUnskippedExecutionIndex(index, exercises.size, from.skippedExerciseIndices)
                ?: return from.copy(showsSummary = true, restRemaining = null, restContext = null)
            val nextExercise = exercises[nextIndex]
            var offset = 0
            val block = blocks.firstOrNull { candidate ->
                val end = offset + candidate.executionExercises().size
                val found = index in offset until end
                if (!found) offset = end
                found
            }
            val sameBlock = block != null && nextIndex < offset + block.executionExercises().size
            val crossesRound = exercise.blockType == WorkoutBlockType.Superset && sameBlock && nextExercise.blockRound > exercise.blockRound
            val seconds = if (crossesRound) block?.restAfterBlockSeconds ?: restSeconds else restSeconds
            val duration = nextExercise.resolvedTargetDurationSeconds()
            val restActive = shouldStartRest(seconds, from.skipsRestPeriods)
            return from.copy(
                exerciseIndex = nextIndex, reps = targetReps(nextExercise, 0),
                weightKg = nextExercise.targetWeightsKg?.firstOrNull() ?: nextExercise.targetWeightKg ?: 0.0,
                exerciseTimeRemaining = duration, exerciseTimeInitial = duration ?: 0,
                exerciseTimerRunning = duration != null && !restActive, completedDistanceKm = 0.0,
                restRemaining = seconds.takeIf { restActive }, restInitialSeconds = if (restActive) seconds else 0,
                restBetweenExercises = restActive,
                restContext = if (!restActive) null else if (crossesRound) WorkoutRestContext.BetweenRounds else WorkoutRestContext.BeforeNextBlock,
            )
        }
        return when (command.operation) {
            "set_reps" -> command.reps?.let { current.copy(reps = it.coerceIn(0, 999)) }
            "set_weight" -> command.weightKg?.takeIf { it.isFinite() }?.let { current.copy(weightKg = it.coerceIn(0.0, 999.0)) }
            "skip_rest" -> if (rest != null) current.copy(restRemaining = null, restContext = null, restBetweenExercises = false, exerciseTimerRunning = exercise.resolvedTargetDurationSeconds() != null) else null
            "add_rest" -> if (rest != null) current.copy(restRemaining = rest + 30, restInitialSeconds = current.restInitialSeconds + 30) else null
            "set_feedback" -> command.feedback?.takeIf { it in setOf("MUY FÁCIL", "FÁCIL", "JUSTO", "DIFÍCIL", "MUY DIFÍCIL") && current.pendingFeedback }?.let { current.copy(selectedFeedback = it) }
            "complete_feedback" -> if (!current.pendingFeedback) null else {
                val feedback = current.selectedFeedback ?: "JUSTO"
                next(current.copy(pendingFeedback = false, selectedFeedback = "JUSTO", feedbackByExercise = current.feedbackByExercise + (index to feedback)), if (exercise.blockType == WorkoutBlockType.Superset) 0 else exercise.restSeconds)
            }
            "complete_set" -> {
                if (rest != null || current.pendingFeedback || current.showsSummary) return null
                val total = exercise.sets + (current.addedSetsByExercise[index] ?: 0)
                val warmup = exercise.blockType == WorkoutBlockType.Warmup || exercise.blockType == WorkoutBlockType.Cooldown
                val completed = if (warmup) total else (current.completedByExercise[index] ?: 0) + 1
                val metrics = completedExerciseMetrics(exercise, current.reps, current.weightKg, current.exerciseTimeInitial, current.exerciseTimeRemaining ?: 0)
                val prior = current.exerciseStats[index] ?: ExerciseSessionStats()
                val stats = prior.copy(sets = prior.sets + 1, totalReps = prior.totalReps + metrics.repetitions,
                    maxWeightKg = maxOf(prior.maxWeightKg, metrics.weightKg), volumeKg = prior.volumeKg + metrics.repetitions * metrics.weightKg,
                    durationSeconds = prior.durationSeconds + metrics.durationSeconds,
                    averageWeightKg = if (metrics.weightKg > 0) (prior.averageWeightKg * prior.sets + metrics.weightKg) / (prior.sets + 1) else prior.averageWeightKg)
                val recorded = current.copy(completedByExercise = current.completedByExercise + (index to completed),
                    totalCompletedSets = current.totalCompletedSets + 1, totalVolumeKg = current.totalVolumeKg + metrics.repetitions * metrics.weightKg,
                    exerciseStats = current.exerciseStats + (index to stats), exerciseTimerRunning = false,
                    completedSetRecords = current.completedSetRecords + CompletedSetRecord(index, if (warmup) 1 else completed, metrics.repetitions, metrics.weightKg, setStyle = exercise.setStyle, durationSeconds = metrics.durationSeconds))
                if (completed < total) {
                    val targetWeight = exercise.targetWeightsKg?.getOrNull(completed - 1) ?: exercise.targetWeightKg
                    val duration = exercise.resolvedTargetDurationSeconds()
                    val restActive = shouldStartRest(exercise.restSeconds, current.skipsRestPeriods)
                    recorded.copy(reps = if (metrics.repetitions != targetReps(exercise, completed - 1)) metrics.repetitions else targetReps(exercise, completed),
                        weightKg = if (targetWeight != null && metrics.weightKg != targetWeight) metrics.weightKg else exercise.targetWeightsKg?.getOrNull(completed) ?: exercise.targetWeightKg ?: current.weightKg,
                        exerciseTimeRemaining = duration, exerciseTimeInitial = duration ?: 0, exerciseTimerRunning = duration != null && !restActive,
                        restRemaining = exercise.restSeconds.takeIf { restActive }, restInitialSeconds = if (restActive) exercise.restSeconds else 0,
                        restBetweenExercises = false, restContext = if (restActive) WorkoutRestContext.BetweenSets else null)
                } else {
                    var cursor = 0
                    val block = blocks.firstOrNull { candidate ->
                        val end = cursor + candidate.executionExercises().size
                        val found = index in cursor until end
                        if (!found) cursor = end
                        found
                    }
                    val lastInBlock = block?.let { b -> (index + 1 until cursor + b.executionExercises().size).none { it !in current.skippedExerciseIndices } } ?: exercise.isLastInBlock
                    if (shouldRequestExerciseFeedback(exercise.copy(isLastInBlock = lastInBlock))) recorded.copy(pendingFeedback = true)
                    else next(recorded, if (exercise.isLastInBlock) exercise.restAfterBlockSeconds ?: 0 else 0)
                }
            }
            else -> null
        }
    }
}
