package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import java.time.LocalDate

internal data class CompletionProgress(
    val xpBefore: Int,
    val xpAfter: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val streakAfter: Int,
    val streakIncreased: Boolean,
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

    fun score(workout: WorkoutDaySummary, stats: Map<Int, ExerciseSessionStats>, feedback: Map<Int, String>): Double {
        val scoredExercises = workout.exercises.withIndex().filter { it.value.blockType !in setOf(WorkoutBlockType.Warmup, WorkoutBlockType.Cooldown) }
        if (scoredExercises.isEmpty()) return 0.0
        val completion = scoredExercises.count { (stats[it.index]?.sets ?: 0) > 0 }.toDouble() / scoredExercises.size
        val adherence = scoredExercises.map { (index, exercise) ->
            val result = stats[index] ?: return@map 0.0
            val setScore = ratio(result.sets.toDouble(), exercise.sets.coerceAtLeast(1).toDouble())
            val targetReps = targetReps(exercise.reps) * exercise.sets.coerceAtLeast(1)
            val repScore = ratio(result.totalReps.toDouble(), targetReps.toDouble())
            (setScore + repScore) / 2.0
        }.average()
        val effortValues = feedback.values.map {
            when (it) {
                "MUY FÁCIL" -> 0.55
                "FÁCIL" -> 0.70
                "JUSTO" -> 0.85
                "DIFÍCIL" -> 0.94
                "MUY DIFÍCIL" -> 1.0
                "CORRECTO" -> 1.0
                else -> 0.85
            }
        }
        val weighted = 0.30 * completion + 0.35 * adherence + if (effortValues.isNotEmpty()) 0.20 * effortValues.average() else 0.0
        val availableWeight = 0.30 + 0.35 + if (effortValues.isNotEmpty()) 0.20 else 0.0
        return ((weighted / availableWeight).coerceIn(0.0, 1.0) * 1_000).toInt() / 10.0
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
            if (previousWeight > 0 && current.maxWeightKg > previousWeight) add(ExerciseRecordEvent(exercise.name, "MEJOR PESO", previousWeight, current.maxWeightKg, "kg"))
            if (previousVolume > 0 && current.volumeKg > previousVolume) add(ExerciseRecordEvent(exercise.name, "MEJOR VOLUMEN", previousVolume, current.volumeKg, "kg"))
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
            values.size >= 2 && value.contains('-') -> ((values[0] + values[1]) / 2.0).toInt()
            values.isNotEmpty() -> values[0]
            else -> 1
        }
    }
}

internal object CompletionProgressStore {
    private const val PREFERENCES = "wildforce_completion_progress"

    fun preview(context: Context, fallbackStreak: Int): CompletionProgress {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val xpBefore = preferences.getInt("xp", 0)
        val today = LocalDate.now().toEpochDay()
        val previousDay = preferences.getLong("lastWorkoutDay", Long.MIN_VALUE)
        val storedStreak = preferences.getInt("streak", fallbackStreak)
        val increased = previousDay != today
        val streak = when {
            !increased -> storedStreak
            previousDay == today - 1 -> storedStreak + 1
            else -> 1
        }
        val xpAfter = xpBefore + WorkoutCompletionCalculator.WORKOUT_XP
        return CompletionProgress(xpBefore, xpAfter, WorkoutCompletionCalculator.level(xpBefore), WorkoutCompletionCalculator.level(xpAfter), streak, increased)
    }

    fun commit(context: Context, progress: CompletionProgress) {
        context.getSharedPreferences(PREFERENCES, 0).edit()
            .putInt("xp", progress.xpAfter).putInt("streak", progress.streakAfter)
            .putLong("lastWorkoutDay", LocalDate.now().toEpochDay()).apply()
    }
}
