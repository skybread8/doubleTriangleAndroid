package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import org.json.JSONArray
import org.json.JSONObject

internal data class ExerciseSessionStats(
    val sets: Int = 0,
    val totalReps: Int = 0,
    val maxWeightKg: Double = 0.0,
    val volumeKg: Double = 0.0,
)

internal data class CompletedSetRecord(
    val exerciseIndex: Int,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAtMillis: Long = System.currentTimeMillis(),
)
internal data class ExerciseHistoryEntry(
    val timestampMillis: Long,
    val sets: Int,
    val totalReps: Int,
    val maxWeightKg: Double,
    val volumeKg: Double,
)

internal object WorkoutHistoryStore {
    private const val PREFERENCES = "wildforce_exercise_history"
    private const val MAX_ENTRIES = 24

    fun history(context: Context, exercise: ExerciseSummary): List<ExerciseHistoryEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(key(exercise), null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ExerciseHistoryEntry(
                        timestampMillis = item.getLong("timestamp"),
                        sets = item.getInt("sets"),
                        totalReps = item.getInt("reps"),
                        maxWeightKg = item.getDouble("maxWeightKg"),
                        volumeKg = item.getDouble("volumeKg"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun record(context: Context, workout: WorkoutDaySummary, stats: Map<Int, ExerciseSessionStats>) {
        workout.exercises.forEachIndexed { index, exercise ->
            val result = stats[index] ?: return@forEachIndexed
            if (result.sets == 0) return@forEachIndexed
            val entries = listOf(
                ExerciseHistoryEntry(System.currentTimeMillis(), result.sets, result.totalReps, result.maxWeightKg, result.volumeKg),
            ) + history(context, exercise)
            val json = JSONArray().apply {
                entries.take(MAX_ENTRIES).forEach { entry ->
                    put(
                        JSONObject().put("timestamp", entry.timestampMillis).put("sets", entry.sets)
                            .put("reps", entry.totalReps).put("maxWeightKg", entry.maxWeightKg).put("volumeKg", entry.volumeKg),
                    )
                }
            }
            context.getSharedPreferences(PREFERENCES, 0).edit().putString(key(exercise), json.toString()).apply()
        }
    }

    private fun key(exercise: ExerciseSummary): String = exercise.imageKey?.takeIf(String::isNotBlank)?.lowercase() ?: exercise.name.lowercase()
}
