package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.RepRange
import io.codepassion.doubletriangle.core.model.SetStyleParameters
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
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
    /** Exercises deliberately omitted from this active session. Kept separately
     * from completed sets so summaries never report them as performed. */
    val skippedExerciseIndices: Set<Int> = emptySet(),
    /** The editable workout structure must survive process recreation too. */
    val editableBlocks: List<WorkoutBlockSummary>? = null,
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
        val skipped = json.optJSONArray("skippedExerciseIndices")?.let { values ->
            buildSet { for (index in 0 until values.length()) add(values.optInt(index)) }
        }.orEmpty()
        val editableBlocks = json.optJSONArray("editableBlocks")?.let(::decodeEditableBlocks)
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
            skippedExerciseIndices = skipped,
            editableBlocks = editableBlocks,
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
            .put("skippedExerciseIndices", org.json.JSONArray(snapshot.skippedExerciseIndices.sorted()))
        if (snapshot.editableBlocks != null) json.put("editableBlocks", encodeEditableBlocks(snapshot.editableBlocks))
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(workoutId, json.toString()).apply()
    }

    private fun encodeEditableBlocks(blocks: List<WorkoutBlockSummary>): org.json.JSONArray = org.json.JSONArray().apply {
        blocks.forEach { block ->
            put(JSONObject().apply {
                put("type", block.type.name)
                put("rounds", block.rounds)
                put("restAfterBlockSeconds", block.restAfterBlockSeconds ?: JSONObject.NULL)
                put("notes", block.notes ?: JSONObject.NULL)
                put("exercises", encodeExercises(block.exercises))
            })
        }
    }

    private fun encodeExercises(exercises: List<ExerciseSummary>): org.json.JSONArray = org.json.JSONArray().apply {
        exercises.forEach { exercise ->
            put(JSONObject().apply {
                put("name", exercise.name)
                put("imageKey", exercise.imageKey ?: JSONObject.NULL)
                put("coachExerciseId", exercise.coachExerciseId ?: JSONObject.NULL)
                put("skippedAt", exercise.skippedAt ?: JSONObject.NULL)
                put("plannedExerciseId", exercise.plannedExerciseId ?: JSONObject.NULL)
                put("sets", exercise.sets)
                put("reps", exercise.reps)
                put("restSeconds", exercise.restSeconds)
                put("setStyle", exercise.setStyle.name)
                put("setStyleParameters", encodeSetStyleParameters(exercise.setStyleParameters))
                put("targetWeightKg", exercise.targetWeightKg ?: JSONObject.NULL)
                put("targetRepsPerSet", exercise.targetRepsPerSet?.let(::encodeInts) ?: JSONObject.NULL)
                put("targetWeightsKg", exercise.targetWeightsKg?.let(::encodeDoubles) ?: JSONObject.NULL)
                put("targetDurationSeconds", exercise.targetDurationSeconds ?: JSONObject.NULL)
                put("targetDurationMinutes", exercise.targetDurationMinutes ?: JSONObject.NULL)
                put("targetDistanceKm", exercise.targetDistanceKm ?: JSONObject.NULL)
                put("targetPaceSecondsPerKm", exercise.targetPaceSecondsPerKm ?: JSONObject.NULL)
                put("isPerSideLoad", exercise.isPerSideLoad)
                put("trackingMode", exercise.trackingMode.name)
                put("repRange", exercise.repRange?.let(::encodeRepRange) ?: JSONObject.NULL)
            })
        }
    }

    private fun encodeSetStyleParameters(parameters: SetStyleParameters) = JSONObject().apply {
        put("dropCount", parameters.dropCount)
        put("dropWeightPercent", parameters.dropWeightPercent)
        put("backoffSetCount", parameters.backoffSetCount)
        put("backoffWeightPercent", parameters.backoffWeightPercent)
        put("intraSetRestSeconds", parameters.intraSetRestSeconds)
        put("tempo", parameters.tempo)
        put("targetRir", parameters.targetRir)
        put("appliesToFinalSetOnly", parameters.appliesToFinalSetOnly)
    }

    private fun encodeRepRange(range: RepRange) = JSONObject().put("minimum", range.minimum).put("maximum", range.maximum)
    private fun encodeInts(values: List<Int>) = org.json.JSONArray().apply { values.forEach(::put) }
    private fun encodeDoubles(values: List<Double>) = org.json.JSONArray().apply { values.forEach(::put) }

    private fun decodeEditableBlocks(array: org.json.JSONArray): List<WorkoutBlockSummary> = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val exercises = item.optJSONArray("exercises")?.let(::decodeExercises).orEmpty()
            if (exercises.isEmpty()) continue
            add(
                WorkoutBlockSummary(
                    type = runCatching { WorkoutBlockType.valueOf(item.optString("type")) }.getOrDefault(WorkoutBlockType.Standard),
                    rounds = item.optInt("rounds", 1).coerceAtLeast(1),
                    restAfterBlockSeconds = item.optInt("restAfterBlockSeconds", -1).takeIf { it >= 0 },
                    notes = item.optString("notes").takeIf(String::isNotBlank),
                    exercises = exercises,
                ),
            )
        }
    }

    private fun decodeExercises(array: org.json.JSONArray): List<ExerciseSummary> = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name")
            val storedImageKey = item.optString("imageKey").trim()
                .takeIf(String::isNotBlank)
                // Older sync payloads serialized a JSON null as `coach:null`.
                // Recover the catalog key from the persisted exercise name.
                ?.takeUnless { it.equals("coach:null", ignoreCase = true) }
            val targetReps = item.optJSONArray("targetRepsPerSet")?.let(::decodeInts)
            val targetWeights = item.optJSONArray("targetWeightsKg")?.let(::decodeDoubles)
            val repRange = item.optJSONObject("repRange")?.let { RepRange(it.optInt("minimum", 1), it.optInt("maximum", 1)) }
            val parameters = item.optJSONObject("setStyleParameters")?.let { values ->
                SetStyleParameters(
                    dropCount = values.optInt("dropCount", 2),
                    dropWeightPercent = values.optInt("dropWeightPercent", 20),
                    backoffSetCount = values.optInt("backoffSetCount", 3),
                    backoffWeightPercent = values.optInt("backoffWeightPercent", 15),
                    intraSetRestSeconds = values.optInt("intraSetRestSeconds", 15),
                    tempo = values.optString("tempo", "3-1-1-0"),
                    targetRir = values.optInt("targetRir", 2),
                    appliesToFinalSetOnly = values.optBoolean("appliesToFinalSetOnly"),
                )
            } ?: SetStyleParameters()
            add(
                ExerciseSummary(
                    name = name,
                    imageKey = canonicalExerciseMediaKey(storedImageKey ?: name),
                    sets = item.optInt("sets", 1).coerceAtLeast(1),
                    reps = item.optString("reps", "10"),
                    restSeconds = item.optInt("restSeconds", 0).coerceAtLeast(0),
                    setStyle = runCatching { ExerciseSetStyle.valueOf(item.optString("setStyle")) }.getOrDefault(ExerciseSetStyle.Straight),
                    setStyleParameters = parameters,
                    targetWeightKg = item.optDouble("targetWeightKg").takeUnless { it.isNaN() },
                    targetRepsPerSet = targetReps,
                    targetWeightsKg = targetWeights,
                    targetDurationSeconds = item.optInt("targetDurationSeconds", -1).takeIf { it >= 0 },
                    targetDurationMinutes = item.optInt("targetDurationMinutes", -1).takeIf { it >= 0 },
                    targetDistanceKm = item.optDouble("targetDistanceKm").takeUnless { it.isNaN() },
                    targetPaceSecondsPerKm = item.optInt("targetPaceSecondsPerKm", -1).takeIf { it >= 0 },
                    isPerSideLoad = item.optBoolean("isPerSideLoad"),
                    trackingMode = runCatching { ExerciseTrackingMode.valueOf(item.optString("trackingMode")) }.getOrDefault(ExerciseTrackingMode.Repetitions),
                    repRange = repRange,
                    coachExerciseId = item.optString("coachExerciseId").trim()
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                    skippedAt = item.optString("skippedAt").trim()
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                    plannedExerciseId = item.optString("plannedExerciseId").trim()
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                ),
            )
        }
    }

    private fun decodeInts(array: org.json.JSONArray): List<Int> = buildList { for (index in 0 until array.length()) add(array.optInt(index)) }
    private fun decodeDoubles(array: org.json.JSONArray): List<Double> = buildList { for (index in 0 until array.length()) add(array.optDouble(index)) }

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
