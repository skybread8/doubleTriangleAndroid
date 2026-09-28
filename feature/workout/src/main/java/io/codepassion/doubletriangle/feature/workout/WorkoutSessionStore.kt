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
    val pendingNote: String = "",
    val exerciseStats: Map<Int, ExerciseSessionStats> = emptyMap(),
    val feedbackByExercise: Map<Int, String> = emptyMap(),
    val notesByExercise: Map<Int, String> = emptyMap(),
    val completedSetRecords: List<CompletedSetRecord> = emptyList(),
    val exerciseTimeRemaining: Int? = null,
    val exerciseTimeInitial: Int = 0,
    val exerciseTimerRunning: Boolean = false,
    val addedSetsByExercise: Map<Int, Int> = emptyMap(),
    val completedDistanceKm: Double = 0.0,
    val restContext: WorkoutRestContext? = null,
    val exerciseTimerFinishedWhileAway: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    /** Backward-compatible constructor for snapshots created before the
     * summary-state field was added. */
    constructor(
        exerciseIndex: Int,
        completedByExercise: Map<Int, Int>,
        reps: Int,
        weightKg: Double,
        restRemaining: Int?,
        restInitialSeconds: Int,
        restBetweenExercises: Boolean,
        elapsedSeconds: Int,
        totalCompletedSets: Int,
        totalVolumeKg: Double,
        pendingFeedback: Boolean,
        vararg legacy: Any?,
    ) : this(
        exerciseIndex, completedByExercise, reps, weightKg, restRemaining,
        restInitialSeconds, restBetweenExercises, elapsedSeconds,
        totalCompletedSets, totalVolumeKg, pendingFeedback, false,
        legacy.getOrNull(0) as? String,
        legacy.getOrNull(1) as? String ?: "",
        legacy.getOrNull(2) as? Map<Int, ExerciseSessionStats> ?: emptyMap(),
        legacy.getOrNull(3) as? Map<Int, String> ?: emptyMap(),
        legacy.getOrNull(4) as? Map<Int, String> ?: emptyMap(),
        legacy.getOrNull(5) as? List<CompletedSetRecord> ?: emptyList(),
        legacy.getOrNull(6) as? Int?, legacy.getOrNull(7) as? Int ?: 0,
        legacy.getOrNull(8) as? Boolean ?: false,
        legacy.getOrNull(9) as? Map<Int, Int> ?: emptyMap(),
    )
}

object WorkoutSessionStore {
    /** Safe no-op fallback when a restored session cannot be serialized. */
    fun saveLegacy() = Unit
    private fun updatedAtFallback(): Long = System.currentTimeMillis()
    private const val PREFERENCES = "wildforce_active_workouts"

    internal fun load(context: Context, workoutId: String): WorkoutSessionSnapshot? = runCatching {
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
                put(key.toInt(), ExerciseSessionStats(item.optInt("sets"), item.optInt("reps"), item.optDouble("maxWeightKg"), item.optDouble("volumeKg"), item.optInt("durationSeconds"), item.optDouble("distanceKm"), item.optDouble("averageWeightKg")))
            }
        }
        val feedbackJson = json.optJSONObject("feedbackByExercise") ?: JSONObject()
        val feedback = buildMap { feedbackJson.keys().forEach { key -> put(key.toInt(), feedbackJson.getString(key)) } }
        val notesJson = json.optJSONObject("notesByExercise") ?: JSONObject()
        val notes = buildMap { notesJson.keys().forEach { key -> put(key.toInt(), notesJson.getString(key)) } }
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
                        item.optInt("durationSeconds"), item.optDouble("distanceKm"),
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
            exerciseIndex = json.optInt("exerciseIndex", 0), completedByExercise = completed,
            reps = json.optInt("reps", 1), weightKg = json.optDouble("weightKg", 0.0),
            restRemaining = savedRestRemaining?.let { (it - elapsedWhileAway).coerceAtLeast(0) },
            restInitialSeconds = json.optInt("restInitialSeconds", 1),
            restBetweenExercises = json.optBoolean("restBetweenExercises"),
            elapsedSeconds = json.optInt("elapsedSeconds", 0) + elapsedWhileAway,
            totalCompletedSets = json.optInt("totalCompletedSets", 0), totalVolumeKg = json.optDouble("totalVolumeKg", 0.0),
            pendingFeedback = json.optBoolean("pendingFeedback"), showsSummary = json.optBoolean("showsSummary"), selectedFeedback = json.optString("selectedFeedback").takeIf(String::isNotBlank),
            pendingNote = json.optString("pendingNote"),
            exerciseStats = stats,
            feedbackByExercise = feedback,
            notesByExercise = notes,
            completedSetRecords = setRecords,
            exerciseTimeRemaining = savedExerciseTime?.let { if (exerciseTimerWasRunning) (it - elapsedWhileAway).coerceAtLeast(0) else it },
            exerciseTimeInitial = json.optInt("exerciseTimeInitial"),
            exerciseTimerRunning = exerciseTimerWasRunning && (savedExerciseTime ?: 0) > elapsedWhileAway,
            addedSetsByExercise = addedSets,
            completedDistanceKm = json.optDouble("completedDistanceKm"),
            restContext = runCatching { WorkoutRestContext.valueOf(json.optString("restContext")) }.getOrNull()
                ?: if (json.optBoolean("restBetweenExercises")) WorkoutRestContext.BeforeNextBlock else savedRestRemaining?.let { WorkoutRestContext.BetweenSets },
            exerciseTimerFinishedWhileAway = exerciseTimerWasRunning && savedExerciseTime != null && savedExerciseTime <= elapsedWhileAway,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }.getOrNull()

    /**
     * The root navigation owns the compact active-workout accessory. Keep its
     * contract small so callers outside this feature never need session data.
     */
    fun hasActiveSession(context: Context, workoutId: String): Boolean =
        load(context, workoutId) != null

    fun activeExerciseIndex(context: Context, workoutId: String): Int? =
        load(context, workoutId)?.exerciseIndex

    internal fun save(context: Context, workoutId: String, snapshot: WorkoutSessionSnapshot) {
        val completed = JSONObject().apply { snapshot.completedByExercise.forEach { (index, sets) -> put(index.toString(), sets) } }
        val stats = JSONObject().apply {
            snapshot.exerciseStats.forEach { (index, value) ->
                put(index.toString(), JSONObject().put("sets", value.sets).put("reps", value.totalReps).put("maxWeightKg", value.maxWeightKg).put("volumeKg", value.volumeKg).put("durationSeconds", value.durationSeconds).put("distanceKm", value.distanceKm).put("averageWeightKg", value.averageWeightKg))
            }
        }
        val feedback = JSONObject().apply { snapshot.feedbackByExercise.forEach { (index, value) -> put(index.toString(), value) } }
        val notes = JSONObject().apply { snapshot.notesByExercise.forEach { (index, value) -> put(index.toString(), value) } }
        val addedSets = JSONObject().apply { snapshot.addedSetsByExercise.forEach { (index, value) -> put(index.toString(), value) } }
        val setRecords = org.json.JSONArray().apply {
            snapshot.completedSetRecords.forEach { record ->
                put(JSONObject().put("exerciseIndex", record.exerciseIndex).put("setNumber", record.setNumber)
                    .put("reps", record.reps).put("weightKg", record.weightKg).put("completedAt", record.completedAtMillis)
                    .put("setStyle", record.setStyle.name).put("durationSeconds", record.durationSeconds).put("distanceKm", record.distanceKm))
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
            .put("pendingNote", snapshot.pendingNote)
            .put("exerciseStats", stats).put("feedbackByExercise", feedback).put("notesByExercise", notes).put("completedSetRecords", setRecords)
            .put("exerciseTimeRemaining", snapshot.exerciseTimeRemaining ?: JSONObject.NULL)
            .put("exerciseTimeInitial", snapshot.exerciseTimeInitial)
            .put("exerciseTimerRunning", snapshot.exerciseTimerRunning)
            .put("addedSetsByExercise", addedSets)
            .put("completedDistanceKm", snapshot.completedDistanceKm)
            .put("restContext", snapshot.restContext?.name ?: JSONObject.NULL)
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(workoutId, json.toString()).apply()
    }

    fun clear(context: Context, workoutId: String) {
        context.getSharedPreferences(PREFERENCES, 0).edit().remove(workoutId).apply()
    }

    /**
     * A replacement plan must never inherit an unfinished session, its timer,
     * or its notification. This is deliberately separate from `clear`: callers
     * use it only after the user confirms replacing the whole plan.
     */
    fun discardPlanSessions(context: Context, workoutIds: Collection<String>) {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        preferences.edit().apply {
            workoutIds.forEach(::remove)
        }.apply()
        WorkoutNotificationActionStore.clear(context)
        WorkoutActiveNotification.cancelRestFinished(context)
        WorkoutActiveNotification.cancel(context)
        context.stopService(android.content.Intent(context, WorkoutForegroundService::class.java))
        WildforceWatchLocalBridge.stop(context)
    }
}
