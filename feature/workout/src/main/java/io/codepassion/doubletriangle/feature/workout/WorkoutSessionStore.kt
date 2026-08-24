package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import org.json.JSONObject

internal data class WorkoutSessionSnapshot(
    val exerciseIndex: Int,
    val completedByExercise: Map<Int, Int>,
    val reps: Int,
    val weightKg: Double,
    val restRemaining: Int?,
    val restInitialSeconds: Int,
    val restBetweenExercises: Boolean,
    val elapsedSeconds: Int,
    val totalCompletedSets: Int,
    val totalVolumeKg: Double,
    val pendingFeedback: Boolean,
    val showsSummary: Boolean,
    val selectedFeedback: String?,
    val exerciseStats: Map<Int, ExerciseSessionStats> = emptyMap(),
    val feedbackByExercise: Map<Int, String> = emptyMap(),
    val completedSetRecords: List<CompletedSetRecord> = emptyList(),
    val exerciseTimeRemaining: Int? = null,
    val exerciseTimeInitial: Int = 0,
    val exerciseTimerRunning: Boolean = false,
    val addedSetsByExercise: Map<Int, Int> = emptyMap(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

internal object WorkoutSessionStore {
    private fun updatedAtFallback(): Long = System.currentTimeMillis()
    private const val PREFERENCES = "wildforce_active_workouts"

    fun load(context: Context, workoutId: String): WorkoutSessionSnapshot? = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(workoutId, null) ?: return null
        val json = JSONObject(raw)
        val completedJson = json.optJSONObject("completed") ?: JSONObject()
        val completed = buildMap {
            completedJson.keys().forEach { key -> put(key.toInt(), completedJson.getInt(key)) }
        }
        val statsJson = json.optJSONObject("exerciseStats") ?: JSONObject()
        val stats = buildMap {
            statsJson.keys().forEach { key ->
                val item = statsJson.getJSONObject(key)
                put(key.toInt(), ExerciseSessionStats(item.optInt("sets"), item.optInt("reps"), item.optDouble("maxWeightKg"), item.optDouble("volumeKg")))
            }
        }
        val feedbackJson = json.optJSONObject("feedbackByExercise") ?: JSONObject()
        val feedback = buildMap { feedbackJson.keys().forEach { key -> put(key.toInt(), feedbackJson.getString(key)) } }
        val addedSetsJson = json.optJSONObject("addedSetsByExercise") ?: JSONObject()
        val addedSets = buildMap { addedSetsJson.keys().forEach { key -> put(key.toInt(), addedSetsJson.getInt(key)) } }
        val setRecordsJson = json.optJSONArray("completedSetRecords")
        val setRecords = buildList {
            if (setRecordsJson != null) for (index in 0 until setRecordsJson.length()) {
                val item = setRecordsJson.getJSONObject(index)
                add(
                    CompletedSetRecord(
                        item.getInt("exerciseIndex"), item.getInt("setNumber"), item.getInt("reps"),
                        item.getDouble("weightKg"), item.optLong("completedAt", updatedAtFallback()),
                        runCatching { ExerciseSetStyle.valueOf(item.optString("setStyle")) }.getOrDefault(ExerciseSetStyle.Straight),
                    ),
                )
            }
        }
        val updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        val elapsedWhileAway = ((System.currentTimeMillis() - updatedAt) / 1_000).coerceAtLeast(0).toInt()
        val savedRestRemaining = if (json.isNull("restRemaining")) null else json.getInt("restRemaining")
        val savedExerciseTime = if (json.isNull("exerciseTimeRemaining")) null else json.optInt("exerciseTimeRemaining")
        val exerciseTimerWasRunning = json.optBoolean("exerciseTimerRunning")
        WorkoutSessionSnapshot(
            exerciseIndex = json.getInt("exerciseIndex"), completedByExercise = completed,
            reps = json.getInt("reps"), weightKg = json.getDouble("weightKg"),
            restRemaining = savedRestRemaining?.let { (it - elapsedWhileAway).coerceAtLeast(0) },
            restInitialSeconds = json.optInt("restInitialSeconds", 1),
            restBetweenExercises = json.optBoolean("restBetweenExercises"),
            elapsedSeconds = json.getInt("elapsedSeconds") + elapsedWhileAway,
            totalCompletedSets = json.getInt("totalCompletedSets"), totalVolumeKg = json.getDouble("totalVolumeKg"),
            pendingFeedback = json.optBoolean("pendingFeedback"), showsSummary = json.optBoolean("showsSummary"), selectedFeedback = json.optString("selectedFeedback").takeIf(String::isNotBlank),
            exerciseStats = stats,
            feedbackByExercise = feedback,
            completedSetRecords = setRecords,
            exerciseTimeRemaining = savedExerciseTime?.let { if (exerciseTimerWasRunning) (it - elapsedWhileAway).coerceAtLeast(0) else it },
            exerciseTimeInitial = json.optInt("exerciseTimeInitial"),
            exerciseTimerRunning = exerciseTimerWasRunning && (savedExerciseTime ?: 0) > elapsedWhileAway,
            addedSetsByExercise = addedSets,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }.getOrNull()

    fun save(context: Context, workoutId: String, snapshot: WorkoutSessionSnapshot) {
        val completed = JSONObject().apply { snapshot.completedByExercise.forEach { (index, sets) -> put(index.toString(), sets) } }
        val stats = JSONObject().apply {
            snapshot.exerciseStats.forEach { (index, value) ->
                put(index.toString(), JSONObject().put("sets", value.sets).put("reps", value.totalReps).put("maxWeightKg", value.maxWeightKg).put("volumeKg", value.volumeKg))
            }
        }
        val feedback = JSONObject().apply { snapshot.feedbackByExercise.forEach { (index, value) -> put(index.toString(), value) } }
        val addedSets = JSONObject().apply { snapshot.addedSetsByExercise.forEach { (index, value) -> put(index.toString(), value) } }
        val setRecords = org.json.JSONArray().apply {
            snapshot.completedSetRecords.forEach { record ->
                put(JSONObject().put("exerciseIndex", record.exerciseIndex).put("setNumber", record.setNumber)
                    .put("reps", record.reps).put("weightKg", record.weightKg).put("completedAt", record.completedAtMillis)
                    .put("setStyle", record.setStyle.name))
            }
        }
        val json = JSONObject()
            .put("exerciseIndex", snapshot.exerciseIndex).put("completed", completed)
            .put("reps", snapshot.reps).put("weightKg", snapshot.weightKg)
            .put("restRemaining", snapshot.restRemaining ?: JSONObject.NULL)
            .put("restInitialSeconds", snapshot.restInitialSeconds).put("restBetweenExercises", snapshot.restBetweenExercises)
            .put("elapsedSeconds", snapshot.elapsedSeconds).put("totalCompletedSets", snapshot.totalCompletedSets)
            .put("totalVolumeKg", snapshot.totalVolumeKg).put("pendingFeedback", snapshot.pendingFeedback).put("showsSummary", snapshot.showsSummary)
            .put("selectedFeedback", snapshot.selectedFeedback ?: JSONObject.NULL).put("updatedAt", System.currentTimeMillis())
            .put("exerciseStats", stats).put("feedbackByExercise", feedback).put("completedSetRecords", setRecords)
            .put("exerciseTimeRemaining", snapshot.exerciseTimeRemaining ?: JSONObject.NULL)
            .put("exerciseTimeInitial", snapshot.exerciseTimeInitial)
            .put("exerciseTimerRunning", snapshot.exerciseTimerRunning)
            .put("addedSetsByExercise", addedSets)
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(workoutId, json.toString()).apply()
    }

    fun clear(context: Context, workoutId: String) {
        context.getSharedPreferences(PREFERENCES, 0).edit().remove(workoutId).apply()
    }
}
