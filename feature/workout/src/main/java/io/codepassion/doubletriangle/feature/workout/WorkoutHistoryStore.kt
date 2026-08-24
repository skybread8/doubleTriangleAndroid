package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
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
    val setStyle: ExerciseSetStyle = ExerciseSetStyle.Straight,
)

internal data class SetPerformance(
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val setStyle: ExerciseSetStyle,
)

internal data class ExerciseHistoryEntry(
    val timestampMillis: Long,
    val sets: Int,
    val totalReps: Int,
    val maxWeightKg: Double,
    val volumeKg: Double,
    val setDetails: List<SetPerformance> = emptyList(),
    val feedback: String? = null,
    val note: String? = null,
)

data class ExerciseAnalyticsSummary(
    val exercise: ExerciseSummary,
    val sessions: Int,
    val personalBestKg: Double,
    val totalVolumeKg: Double,
    val lastFeedback: String?,
)

internal fun ExerciseSummary.historyKey(): String = imageKey?.takeIf(String::isNotBlank)?.lowercase() ?: name.lowercase()

internal object WorkoutHistoryStore {
    private const val PREFERENCES = "wildforce_exercise_history"
    private const val MAX_ENTRIES = 24

    fun history(context: Context, exercise: ExerciseSummary): List<ExerciseHistoryEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(exercise.historyKey(), null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val detailsJson = item.optJSONArray("setDetails")
                val details = buildList {
                    if (detailsJson != null) {
                        for (setIndex in 0 until detailsJson.length()) {
                            val set = detailsJson.getJSONObject(setIndex)
                            add(
                                SetPerformance(
                                    set.getInt("setNumber"), set.getInt("reps"), set.getDouble("weightKg"),
                                    runCatching { ExerciseSetStyle.valueOf(set.optString("setStyle")) }.getOrDefault(ExerciseSetStyle.Straight),
                                ),
                            )
                        }
                    }
                }
                add(
                    ExerciseHistoryEntry(
                        timestampMillis = item.getLong("timestamp"),
                        sets = item.getInt("sets"),
                        totalReps = item.getInt("reps"),
                        maxWeightKg = item.getDouble("maxWeightKg"),
                        volumeKg = item.getDouble("volumeKg"),
                        setDetails = details,
                        feedback = item.optString("feedback").takeIf(String::isNotBlank),
                        note = item.optString("note").takeIf(String::isNotBlank),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun record(
        context: Context,
        workout: WorkoutDaySummary,
        stats: Map<Int, ExerciseSessionStats>,
        completedSets: List<CompletedSetRecord> = emptyList(),
        feedbackByExercise: Map<Int, String> = emptyMap(),
        notesByExercise: Map<Int, String> = emptyMap(),
    ) {
        val timestamp = System.currentTimeMillis()
        val completedExercises = workout.exercises.withIndex()
            .filter { (index, _) -> (stats[index]?.sets ?: 0) > 0 }
            .groupBy { (_, exercise) -> exercise.historyKey() }
        completedExercises.values.forEach { occurrences ->
            val exercise = occurrences.first().value
            val exerciseIndices = occurrences.map { it.index }.toSet()
            val results = exerciseIndices.mapNotNull(stats::get)
            val result = ExerciseSessionStats(
                sets = results.sumOf { it.sets },
                totalReps = results.sumOf { it.totalReps },
                maxWeightKg = results.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                volumeKg = results.sumOf { it.volumeKg },
            )
            val details = completedSets
                .filter { it.exerciseIndex in exerciseIndices }
                .sortedWith(compareBy(CompletedSetRecord::exerciseIndex, CompletedSetRecord::setNumber))
                .mapIndexed { index, set -> SetPerformance(index + 1, set.reps, set.weightKg, set.setStyle) }
            val feedback = exerciseIndices.mapNotNull(feedbackByExercise::get).lastOrNull()
            val note = exerciseIndices.mapNotNull(notesByExercise::get).lastOrNull()
            val entries = listOf(
                ExerciseHistoryEntry(timestamp, result.sets, result.totalReps, result.maxWeightKg, result.volumeKg, details, feedback, note),
            ) + history(context, exercise)
            val json = JSONArray().apply {
                entries.take(MAX_ENTRIES).forEach { entry ->
                    val detailsJson = JSONArray().apply {
                        entry.setDetails.forEach { set ->
                            put(
                                JSONObject().put("setNumber", set.setNumber).put("reps", set.reps)
                                    .put("weightKg", set.weightKg).put("setStyle", set.setStyle.name),
                            )
                        }
                    }
                    put(
                        JSONObject().put("timestamp", entry.timestampMillis).put("sets", entry.sets)
                            .put("reps", entry.totalReps).put("maxWeightKg", entry.maxWeightKg).put("volumeKg", entry.volumeKg)
                            .put("setDetails", detailsJson)
                            .put("feedback", entry.feedback ?: JSONObject.NULL)
                            .put("note", entry.note ?: JSONObject.NULL),
                    )
                }
            }
            context.getSharedPreferences(PREFERENCES, 0).edit().putString(exercise.historyKey(), json.toString()).apply()
        }
    }
}

object WorkoutAnalyticsStore {
    fun summaries(context: Context, exercises: List<ExerciseSummary>): List<ExerciseAnalyticsSummary> =
        exercises.distinctBy { it.historyKey() }.mapNotNull { exercise ->
            val entries = WorkoutHistoryStore.history(context, exercise)
            if (entries.isEmpty()) null else ExerciseAnalyticsSummary(
                exercise = exercise,
                sessions = entries.size,
                personalBestKg = entries.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                totalVolumeKg = entries.sumOf { it.volumeKg },
                lastFeedback = entries.firstOrNull()?.feedback,
            )
        }.sortedByDescending { it.totalVolumeKg }
}
