package io.codepassion.doubletriangle.feature.workout

import android.content.Context
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
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

internal object WorkoutSessionStore {
    private const val PREFERENCES = "wildforce_active_workouts"

    fun load(context: Context, workoutId: String): WorkoutSessionSnapshot? = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(workoutId, null) ?: return null
        val json = JSONObject(raw)
        val completedJson = json.optJSONObject("completed") ?: JSONObject()
        val completed = buildMap {
            completedJson.keys().forEach { key -> put(key.toInt(), completedJson.getInt(key)) }
        }
        val updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        val elapsedWhileAway = ((System.currentTimeMillis() - updatedAt) / 1_000).coerceAtLeast(0).toInt()
        WorkoutSessionSnapshot(
            exerciseIndex = json.getInt("exerciseIndex"), completedByExercise = completed,
            reps = json.getInt("reps"), weightKg = json.getDouble("weightKg"),
            restRemaining = if (json.isNull("restRemaining")) null else json.getInt("restRemaining"),
            restInitialSeconds = json.optInt("restInitialSeconds", 1),
            restBetweenExercises = json.optBoolean("restBetweenExercises"),
            elapsedSeconds = json.getInt("elapsedSeconds") + elapsedWhileAway,
            totalCompletedSets = json.getInt("totalCompletedSets"), totalVolumeKg = json.getDouble("totalVolumeKg"),
            pendingFeedback = json.optBoolean("pendingFeedback"), showsSummary = json.optBoolean("showsSummary"), selectedFeedback = json.optString("selectedFeedback").takeIf(String::isNotBlank),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }.getOrNull()

    fun save(context: Context, workoutId: String, snapshot: WorkoutSessionSnapshot) {
        val completed = JSONObject().apply { snapshot.completedByExercise.forEach { (index, sets) -> put(index.toString(), sets) } }
        val json = JSONObject()
            .put("exerciseIndex", snapshot.exerciseIndex).put("completed", completed)
            .put("reps", snapshot.reps).put("weightKg", snapshot.weightKg)
            .put("restRemaining", snapshot.restRemaining ?: JSONObject.NULL)
            .put("restInitialSeconds", snapshot.restInitialSeconds).put("restBetweenExercises", snapshot.restBetweenExercises)
            .put("elapsedSeconds", snapshot.elapsedSeconds).put("totalCompletedSets", snapshot.totalCompletedSets)
            .put("totalVolumeKg", snapshot.totalVolumeKg).put("pendingFeedback", snapshot.pendingFeedback).put("showsSummary", snapshot.showsSummary)
            .put("selectedFeedback", snapshot.selectedFeedback ?: JSONObject.NULL).put("updatedAt", System.currentTimeMillis())
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(workoutId, json.toString()).apply()
    }

    fun clear(context: Context, workoutId: String) {
        context.getSharedPreferences(PREFERENCES, 0).edit().remove(workoutId).apply()
    }
}