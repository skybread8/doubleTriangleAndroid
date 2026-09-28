package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import org.json.JSONObject

/** Persisted on device so completed cards do not depend on a wearable connection. */
internal data class WorkoutCompletionMetrics(
    val completedAtMillis: Long,
    val activeDurationSeconds: Int,
    val activeCaloriesBurned: Double,
    val totalVolumeKg: Double,
)

internal object WorkoutCompletionMetricsStore {
    private const val preferencesName = "wildforce_workout_completion_metrics"

    fun load(context: Context, workoutId: String): WorkoutCompletionMetrics? = runCatching {
        val raw = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(workoutId, null) ?: return null
        val json = JSONObject(raw)
        WorkoutCompletionMetrics(
            completedAtMillis = json.getLong("completedAtMillis"),
            activeDurationSeconds = json.getInt("activeDurationSeconds"),
            activeCaloriesBurned = json.getDouble("activeCaloriesBurned"),
            totalVolumeKg = json.getDouble("totalVolumeKg"),
        )
    }.getOrNull()

    fun save(context: Context, workoutId: String, metrics: WorkoutCompletionMetrics) {
        val json = JSONObject()
            .put("completedAtMillis", metrics.completedAtMillis)
            .put("activeDurationSeconds", metrics.activeDurationSeconds.coerceAtLeast(0))
            .put("activeCaloriesBurned", metrics.activeCaloriesBurned.coerceAtLeast(0.0))
            .put("totalVolumeKg", metrics.totalVolumeKg.coerceAtLeast(0.0))
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().putString(workoutId, json.toString()).apply()
    }
}

/**
 * Android's no-wearable fallback mirrors iOS's `WorkoutMetricCalculator`.
 * It uses the Mifflin–St Jeor BMR supplied by the profile and MET intensity
 * per exercise; it is deliberately an estimate, never Health Connect data.
 */
internal object WorkoutMetricCalculator {
    fun estimateCalories(
        workout: WorkoutDaySummary,
        stats: Map<Int, ExerciseSessionStats>,
        totalDurationSeconds: Int,
        userBmr: Double,
    ): Double {
        val exercises = workout.exercises
        if (exercises.isEmpty() || totalDurationSeconds <= 0 || userBmr <= 0) return 0.0

        val hourlyBaseCalories = userBmr / 24.0
        val documentedSeconds = exercises.indices.sumOf { index -> stats[index]?.durationSeconds ?: 0 }
        val documentedCalories = exercises.indices.sumOf { index ->
            val seconds = stats[index]?.durationSeconds ?: 0
            exerciseMet(workout.exercises[index]) * hourlyBaseCalories * seconds / 3_600.0
        }
        val remainingSeconds = (totalDurationSeconds - documentedSeconds).coerceAtLeast(0)
        val withoutDocumentedDuration = exercises.indices.filter { (stats[it]?.durationSeconds ?: 0) == 0 }
        if (remainingSeconds == 0 || withoutDocumentedDuration.isEmpty()) return documentedCalories

        val secondsPerExercise = remainingSeconds.toDouble() / withoutDocumentedDuration.size
        return documentedCalories + withoutDocumentedDuration.sumOf { index ->
            exerciseMet(exercises[index]) * hourlyBaseCalories * secondsPerExercise / 3_600.0
        }
    }

    private fun exerciseMet(exercise: io.codepassion.doubletriangle.core.model.ExerciseSummary): Double {
        val key = "${exercise.imageKey.orEmpty()} ${exercise.name}".lowercase()
        return when {
            key.contains("burpee") -> 10.0
            key.contains("run") || key.contains("sprint") -> 9.0
            key.contains("mountain") || key.contains("jump") -> 8.0
            key.contains("bike") || key.contains("cycle") || key.contains("row") -> 7.0
            exercise.trackingMode == ExerciseTrackingMode.DurationAndDistance -> 8.0
            else -> 5.0 // Same default MET used by the iOS exercise metadata.
        }
    }
}
