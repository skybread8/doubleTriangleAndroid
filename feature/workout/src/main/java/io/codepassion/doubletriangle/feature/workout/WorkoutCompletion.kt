package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import kotlin.math.roundToInt
import java.time.LocalDate

internal data class CompletionProgress(
    val xpBefore: Int,
    val xpAfterWorkout: Int,
    val xpAfterPlan: Int,
    val xpAfter: Int,
    val levelBefore: Int,
    val levelAfterWorkout: Int,
    val levelAfterPlan: Int,
    val levelAfter: Int,
    val streakAfter: Int,
    val streakIncreased: Boolean,
)

data class TrainingProgress(
    val xp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val level: Int,
    val levelProgress: Float,
)

internal data class ExerciseRecordEvent(
    val exerciseName: String,
    val label: String,
    val previousValue: Double,
    val newValue: Double,
    val unit: String,
)

internal object WorkoutCompletionCalculator {
    const val WORKOUT_XP = 25
    const val PLAN_XP = 50
    const val MESOCYCLE_XP = 100

    private data class ScoredExercise(val exercise: ExerciseSummary, val indices: List<Int>, val feedbackIndices: List<Int>)

    /** Execution expands a superset into rounds; iOS scores its original exercises. */
    private fun scoredExercises(workout: WorkoutDaySummary): List<ScoredExercise> {
        val result = mutableListOf<ScoredExercise>()
        var index = 0
        while (index < workout.exercises.size) {
            val exercise = workout.exercises[index]
            if (exercise.blockType == WorkoutBlockType.Superset && exercise.blockLabel != null) {
                val blockIndices = mutableListOf<Int>()
                while (index < workout.exercises.size) {
                    val item = workout.exercises[index]
                    if (item.blockType != WorkoutBlockType.Superset || item.blockLabel == null) break
                    blockIndices += index++
                    if (item.isLastInBlock && item.blockRound >= item.blockRounds) break
                }
                blockIndices.groupBy { workout.exercises[it].blockLabel }.values.forEach { indices ->
                    result += ScoredExercise(workout.exercises[indices.first()], indices, blockIndices)
                }
            } else {
                if (exercise.blockType !in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown)) {
                    result += ScoredExercise(exercise, listOf(index), listOf(index))
                }
                index++
            }
        }
        return result
    }

    private fun aggregateStats(indices: List<Int>, stats: Map<Int, ExerciseSessionStats>): ExerciseSessionStats? {
        val values = indices.mapNotNull(stats::get)
        if (values.isEmpty()) return null
        val weightedValues = values.filter { it.averageWeightKg > 0 && it.sets > 0 }
        val weightedSets = weightedValues.sumOf { it.sets }
        return ExerciseSessionStats(
            sets = values.sumOf { it.sets }, totalReps = values.sumOf { it.totalReps },
            maxWeightKg = values.maxOf { it.maxWeightKg }, volumeKg = values.sumOf { it.volumeKg },
            durationSeconds = values.sumOf { it.durationSeconds }, distanceKm = values.sumOf { it.distanceKm },
            averageWeightKg = if (weightedSets > 0) weightedValues.sumOf { it.averageWeightKg * it.sets } / weightedSets else 0.0,
        )
    }

    fun score(workout: WorkoutDaySummary, stats: Map<Int, ExerciseSessionStats>, feedback: Map<Int, String>, userAgeYears: Int = 30): Double {
        val scoredExercises = scoredExercises(workout)
        if (scoredExercises.isEmpty()) return 0.0
        val completion = scoredExercises.count { (aggregateStats(it.indices, stats)?.sets ?: 0) > 0 }.toDouble() / scoredExercises.size
        val adherence = scoredExercises.map { scored ->
            val exercise = scored.exercise
            val result = aggregateStats(scored.indices, stats) ?: return@map 0.0
            val setScore = ratio(result.sets.toDouble(), exercise.sets.coerceAtLeast(1).toDouble())
            val metricScores = mutableListOf(setScore)
            val targetDuration = exercise.resolvedTargetDurationSeconds()
            if (targetDuration != null) {
                metricScores += ratio(result.durationSeconds.toDouble(), targetDuration.toDouble())
            } else {
                val prescribedRepetitions = exercise.targetRepsPerSet?.takeIf { it.isNotEmpty() }?.sum()
                    ?: exercise.repRange?.let { ((it.minimum + it.maximum) / 2.0).roundToInt() * exercise.sets.coerceAtLeast(1) }
                    ?: exercise.reps.takeIf { it.isNotBlank() }?.let { targetReps(it) * exercise.sets.coerceAtLeast(1) }
                if (prescribedRepetitions != null) {
                    metricScores += ratio(result.totalReps.toDouble(), prescribedRepetitions.coerceAtLeast(1).toDouble())
                }
            }
            exercise.targetDistanceKm?.takeIf { it > 0 }?.let { metricScores += ratio(result.distanceKm, it) }
            val plannedAverageWeight = exercise.targetWeightsKg?.filter { it > 0 }?.takeIf { it.isNotEmpty() }?.average()
                ?: exercise.targetWeightKg?.takeIf { it > 0 }
            if (plannedAverageWeight != null && result.averageWeightKg > 0) {
                metricScores += ratio(result.averageWeightKg, plannedAverageWeight)
            }
            metricScores.average()
        }.average()
        val effortValues = scoredExercises.mapNotNull { scored ->
            if (aggregateStats(scored.indices, stats) == null) null
            else scored.indices.asReversed().firstNotNullOfOrNull(feedback::get)
                ?: scored.feedbackIndices.asReversed().firstNotNullOfOrNull(feedback::get)
        }.map {
            when (it) {
                "MUY FÁCIL" -> 0.55
                "FÁCIL" -> 0.70
                "JUSTO" -> 1.0
                "DIFÍCIL" -> 0.94
                "MUY DIFÍCIL" -> 0.76
                "CORRECTO" -> 1.0
                else -> 0.85
            }
        }
        val heartRateEffort = heartRateEffortScore(workout, userAgeYears)
        val weighted = 0.30 * completion + 0.35 * adherence +
            (if (effortValues.isNotEmpty()) 0.20 * effortValues.average() else 0.0) +
            (if (heartRateEffort != null) 0.15 * heartRateEffort else 0.0)
        val availableWeight = 0.30 + 0.35 +
            (if (effortValues.isNotEmpty()) 0.20 else 0.0) +
            (if (heartRateEffort != null) 0.15 else 0.0)
        return ((weighted / availableWeight).coerceIn(0.0, 1.0) * 1_000).toInt() / 10.0
    }

    /** Matches iOS WorkoutScore.heartRateEffortScore when a watch/Health source supplied an average. */
    private fun heartRateEffortScore(workout: WorkoutDaySummary, userAgeYears: Int): Double? {
        val average = workout.averageHeartRate?.takeIf { it > 0 } ?: return null
        val estimatedMax = maxOf(150.0, 208.0 - (0.7 * userAgeYears.coerceIn(0, 120)))
        val ratio = average / estimatedMax
        return when {
            ratio < 0.50 -> 0.65
            ratio < 0.60 -> interpolate(ratio, 0.50, 0.60, 0.65, 0.78)
            ratio < 0.72 -> interpolate(ratio, 0.60, 0.72, 0.78, 0.94)
            ratio < 0.85 -> interpolate(ratio, 0.72, 0.85, 0.94, 1.0)
            ratio < 0.93 -> interpolate(ratio, 0.85, 0.93, 1.0, 0.88)
            else -> 0.82
        }
    }

    private fun interpolate(value: Double, inputStart: Double, inputEnd: Double, outputStart: Double, outputEnd: Double): Double {
        val progress = ((value - inputStart) / (inputEnd - inputStart)).coerceIn(0.0, 1.0)
        return outputStart + (outputEnd - outputStart) * progress
    }

    fun records(context: Context, workout: WorkoutDaySummary, stats: Map<Int, ExerciseSessionStats>): List<ExerciseRecordEvent> = buildList {
        val completed = workout.exercises.withIndex()
            .filter { (index, exercise) -> exercise.blockType !in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown) && stats[index] != null }
            .groupBy { it.value.historyKey() }
        completed.values.forEach { occurrences ->
            val exercise = occurrences.first().value
            val results = occurrences.mapNotNull { stats[it.index] }
            val current = ExerciseSessionStats(
                sets = results.sumOf { it.sets }, totalReps = results.sumOf { it.totalReps },
                maxWeightKg = results.maxOfOrNull { it.maxWeightKg } ?: 0.0, volumeKg = results.sumOf { it.volumeKg },
            )
            val previous = WorkoutHistoryStore.history(context, exercise)
            val previousWeight = previous.maxOfOrNull { it.maxWeightKg } ?: 0.0
            val previousVolume = previous.maxOfOrNull { it.volumeKg } ?: 0.0
            if (current.maxWeightKg > 0 && current.maxWeightKg > previousWeight) {
                add(ExerciseRecordEvent(exerciseDisplayName(exercise.imageKey, exercise.name), if (previousWeight > 0) "MEJOR PESO" else "PRIMER REGISTRO DE PESO", previousWeight, current.maxWeightKg, "kg"))
            }
            if (current.volumeKg > 0 && current.volumeKg > previousVolume) {
                add(ExerciseRecordEvent(exerciseDisplayName(exercise.imageKey, exercise.name), if (previousVolume > 0) "MEJOR VOLUMEN" else "PRIMER REGISTRO DE VOLUMEN", previousVolume, current.volumeKg, "kg"))
            }
        }
    }

    fun level(totalXp: Int): Int {
        var level = 1
        while (totalXp >= minimumXp(level + 1)) level++
        return level
    }

    fun minimumXp(level: Int): Int {
        if (level <= 1) return 0
        var total = 0
        for (current in 2..level) total += 60 + (current - 2) * 35
        return total
    }

    private fun ratio(actual: Double, target: Double): Double = if (target <= 0) 0.0 else (actual / target).coerceIn(0.0, 1.0)
    private fun targetReps(value: String): Int {
        val values = Regex("\\d+").findAll(value).mapNotNull { it.value.toIntOrNull() }.toList()
        return when {
            values.size >= 2 && value.contains('-') -> ((values[0] + values[1]) / 2.0).roundToInt()
            values.isNotEmpty() -> values[0]
            else -> 1
        }
    }
}

object CompletionProgressStore {
    private const val PREFERENCES = "wildforce_completion_progress"

    internal fun preview(
        context: Context,
        fallbackStreak: Int,
        completesPlan: Boolean = false,
        completesMesocycle: Boolean = false,
        completedOn: LocalDate = LocalDate.now(),
    ): CompletionProgress {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val xpBefore = preferences.getInt("xp", 0)
        val today = completedOn.toEpochDay()
        val previousDay = preferences.getLong("lastWorkoutDay", Long.MIN_VALUE)
        val storedStreak = preferences.getInt("streak", fallbackStreak)
        return calculate(xpBefore, storedStreak, previousDay, today, completesPlan, completesMesocycle)
    }

    internal fun calculate(
        xpBefore: Int,
        storedStreak: Int,
        previousDay: Long,
        today: Long,
        completesPlan: Boolean,
        completesMesocycle: Boolean,
    ): CompletionProgress {
        val increased = previousDay != today
        val streak = when {
            !increased -> storedStreak
            previousDay == today - 1 -> storedStreak + 1
            else -> 1
        }
        val xpAfterWorkout = xpBefore + WorkoutCompletionCalculator.WORKOUT_XP
        val xpAfterPlan = xpAfterWorkout + if (completesPlan) WorkoutCompletionCalculator.PLAN_XP else 0
        val xpAfter = xpAfterPlan + if (completesMesocycle) WorkoutCompletionCalculator.MESOCYCLE_XP else 0
        return CompletionProgress(
            xpBefore = xpBefore,
            xpAfterWorkout = xpAfterWorkout,
            xpAfterPlan = xpAfterPlan,
            xpAfter = xpAfter,
            levelBefore = WorkoutCompletionCalculator.level(xpBefore),
            levelAfterWorkout = WorkoutCompletionCalculator.level(xpAfterWorkout),
            levelAfterPlan = WorkoutCompletionCalculator.level(xpAfterPlan),
            levelAfter = WorkoutCompletionCalculator.level(xpAfter),
            streakAfter = streak,
            streakIncreased = increased,
        )
    }

    internal fun commit(context: Context, progress: CompletionProgress, completedOn: LocalDate) {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        preferences.edit()
            .putInt("xp", progress.xpAfter)
            .putInt("streak", progress.streakAfter)
            .putInt("longestStreak", maxOf(preferences.getInt("longestStreak", 0), progress.streakAfter))
            .putLong("lastWorkoutDay", completedOn.toEpochDay()).apply()
    }

    fun progress(context: Context): TrainingProgress {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val xp = preferences.getInt("xp", 0)
        val level = WorkoutCompletionCalculator.level(xp)
        val levelStart = WorkoutCompletionCalculator.minimumXp(level)
        val levelEnd = WorkoutCompletionCalculator.minimumXp(level + 1)
        return TrainingProgress(
            xp = xp,
            currentStreak = preferences.getInt("streak", 0),
            longestStreak = preferences.getInt("longestStreak", 0),
            level = level,
            levelProgress = ((xp - levelStart).toFloat() / (levelEnd - levelStart).coerceAtLeast(1)).coerceIn(0f, 1f),
        )
    }
}
