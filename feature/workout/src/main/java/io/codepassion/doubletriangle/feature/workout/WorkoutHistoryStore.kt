package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

internal data class ActiveExerciseRecords(
    val pr: String?,
    val lastBest: String?,
)

private val activeCardioExerciseKeys = setOf(
    "walking", "outdoorRunning", "treadmillWalking", "treadmillRunning", "stationaryBike",
    "elliptical", "rowing", "jumpRope", "stairClimber", "skiErgWork", "boxingBagWork",
)
private val activeHiitExerciseKeys = setOf(
    "mountainClimber", "burpee", "jumpingJacks", "highKnees", "squatJump", "butterflySitUp",
    "sprawl", "climber",
)
private val activeMobilityExerciseKeys = setOf(
    "catCow", "hipFlexorStretch", "hamstringStretch", "thoracicRotation", "shoulderMobilityFlow",
    "worldGreatestStretch", "pigeonPose", "cobraStretch", "childsPose", "inchworm", "armCircles",
    "legSwings", "wristCircles", "ankleCircles", "deepSquatHold", "scapularCircles", "quadStretch",
    "doorwayChestStretch", "scorpions", "threadTheNeedle", "neckCircles",
)

/** Mirrors iOS TrainingAnalytics.exerciseRecords for the active-workout header. */
internal fun activeExerciseRecords(
    exercise: ExerciseSummary,
    history: List<ExerciseHistoryEntry>,
    useImperial: Boolean,
): ActiveExerciseRecords? {
    if (history.isEmpty()) return null
    val key = exercise.imageKey.orEmpty().trim()
    val category = when {
        key in activeCardioExerciseKeys || exercise.trackingMode == ExerciseTrackingMode.DurationAndDistance -> "cardio"
        key in activeHiitExerciseKeys -> "hiit"
        key in activeMobilityExerciseKeys -> "mobility"
        else -> "strength"
    }

    fun preferredLoadOrReps(entries: List<ExerciseHistoryEntry>): String? {
        val maxWeight = entries.map { it.maxWeightKg }.maxOrNull()?.takeIf { it > 0.0 }
        if (maxWeight != null) return formatActiveRecordWeight(maxWeight, useImperial)
        // iOS prefers the result's completedReps (the session total); set-level
        // data is only a compatibility fallback for older Android records.
        val maxReps = entries.mapNotNull { entry ->
            entry.totalReps.takeIf { it > 0 } ?: entry.setDetails.sumOf(SetPerformance::reps).takeIf { it > 0 }
        }.maxOrNull()
        return maxReps?.let { "$it reps" }
    }

    fun value(entries: List<ExerciseHistoryEntry>): String? = when (category) {
        "strength" -> preferredLoadOrReps(entries)
        "cardio" -> entries.mapNotNull { entry ->
            if (entry.durationSeconds > 0 && entry.distanceKm > 0.0) {
                val secondsPerUnit = entry.durationSeconds.toDouble() / if (useImperial) entry.distanceKm * 0.621371192 else entry.distanceKm
                formatActivePace(secondsPerUnit, useImperial)
            } else null
        }.minByOrNull { it.first }?.second
        "hiit" -> entries.map { it.durationSeconds }.filter { it > 0 }.maxOrNull()?.let(::formatActiveSeconds)
        else -> entries.map { it.durationSeconds }.filter { it > 0 }.maxOrNull()?.let { formatActiveMinutes(it) }
    }

    val pr = value(history)
    val lastBest = value(history.firstOrNull()?.let(::listOf).orEmpty())
    return ActiveExerciseRecords(pr = pr, lastBest = lastBest).takeIf { it.pr != null || it.lastBest != null }
}

private fun formatActiveRecordWeight(kilograms: Double, useImperial: Boolean): String =
    if (useImperial) String.format(Locale.getDefault(), "%.1f lb", kilograms * 2.20462262)
    else String.format(Locale.getDefault(), "%.1f kg", kilograms)

private fun formatActiveSeconds(seconds: Int): String = when {
    seconds < 60 -> "$seconds sec"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "%d:%02d".format(Locale.getDefault(), seconds / 60, seconds % 60)
}

private fun formatActiveMinutes(seconds: Int): String {
    val minutes = (seconds / 60).coerceAtLeast(1)
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes == 0) "$hours h" else "$hours h $remainingMinutes min"
}

private fun formatActivePace(secondsPerUnit: Double, useImperial: Boolean): Pair<Double, String> {
    val rounded = secondsPerUnit.coerceAtLeast(0.0).roundToInt()
    val unit = if (useImperial) "min/mi" else "min/km"
    return rounded.toDouble() to "%d:%02d %s".format(Locale.getDefault(), rounded / 60, rounded % 60, unit)
}

internal data class ExerciseSessionStats(
    val sets: Int = 0,
    val totalReps: Int = 0,
    val maxWeightKg: Double = 0.0,
    val volumeKg: Double = 0.0,
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
    val averageWeightKg: Double = 0.0,
)

internal data class CompletedSetRecord(
    val exerciseIndex: Int,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val setStyle: ExerciseSetStyle = ExerciseSetStyle.Straight,
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
)

internal data class SetPerformance(
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val setStyle: ExerciseSetStyle,
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
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
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
    val workoutId: String? = null,
    val plannedExerciseId: String? = null,
    val remoteResultId: String? = null,
)

data class ExerciseAnalyticsSummary(
    val exercise: ExerciseSummary,
    val sessions: Int,
    val personalBestKg: Double,
    val totalVolumeKg: Double,
    val lastFeedback: String?,
)

data class ExerciseAnalyticsPoint(
    val timestampMillis: Long,
    val sets: Int,
    val reps: Int,
    val maxWeightKg: Double,
    val volumeKg: Double,
    val feedback: String?,
    val durationSeconds: Int = 0,
    val distanceKm: Double = 0.0,
)

/** Stable, UI-independent history snapshot exposed to the account synchronizer. */
data class RemoteExerciseHistoryEntry(
    val exerciseKey: String,
    val timestampMillis: Long,
    val sets: Int,
    val totalReps: Int,
    val maxWeightKg: Double,
    val setReps: List<Int>,
    val setWeightsKg: List<Double>,
    val feedback: String?,
    val note: String?,
    val durationSeconds: Int,
    val distanceKm: Double,
    val workoutId: String? = null,
    val plannedExerciseId: String? = null,
    val remoteResultId: String? = null,
)

internal fun ExerciseSummary.historyKey(): String = coachExerciseId?.trim()?.lowercase()?.takeIf(String::isNotBlank)
    ?.let { "coach:$it" }
    ?: imageKey?.takeIf(String::isNotBlank)?.lowercase()
    ?: name.lowercase()

/** iOS-equivalent progression metrics shown before the session list. */
internal enum class ExerciseHistoryChartMetric(val title: String) {
    Volume("Volumen"), Weight("Peso"), Reps("Repeticiones"), Distance("Distancia"), Duration("Duración"),
}

internal data class ExerciseHistoryChartPoint(val timestampMillis: Long, val value: Double)
internal data class ExerciseHistoryChartSeries(val metric: ExerciseHistoryChartMetric, val points: List<ExerciseHistoryChartPoint>)

internal fun ExerciseHistoryChartMetric.displayValue(value: Double, useImperial: Boolean = false): String = when (this) {
    ExerciseHistoryChartMetric.Volume, ExerciseHistoryChartMetric.Weight -> {
        val displayed = if (useImperial) value * 2.20462262 else value
        String.format(java.util.Locale.getDefault(), "%.1f %s", displayed, if (useImperial) "lb" else "kg")
    }
    ExerciseHistoryChartMetric.Reps -> "${value.toInt()} reps"
    ExerciseHistoryChartMetric.Distance -> String.format(java.util.Locale.getDefault(), "%.2f %s", if (useImperial) value * .621371192 else value, if (useImperial) "mi" else "km")
    ExerciseHistoryChartMetric.Duration -> {
        val seconds = value.toInt()
        if (seconds < 60) "$seconds s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}

internal fun exerciseHistoryChartSeries(
    exercise: ExerciseSummary,
    history: List<ExerciseHistoryEntry>,
): List<ExerciseHistoryChartSeries> {
    val metrics = when (exercise.trackingMode) {
        ExerciseTrackingMode.DurationAndDistance -> listOf(ExerciseHistoryChartMetric.Distance, ExerciseHistoryChartMetric.Duration)
        ExerciseTrackingMode.Duration -> listOf(ExerciseHistoryChartMetric.Duration, ExerciseHistoryChartMetric.Reps)
        ExerciseTrackingMode.Repetitions -> listOf(ExerciseHistoryChartMetric.Volume, ExerciseHistoryChartMetric.Weight, ExerciseHistoryChartMetric.Reps)
    }
    return metrics.mapNotNull { metric ->
        val points = history.asReversed().mapNotNull { entry ->
            val value = when (metric) {
                ExerciseHistoryChartMetric.Volume -> entry.volumeKg.takeIf { it > 0.0 }
                ExerciseHistoryChartMetric.Weight -> entry.maxWeightKg.takeIf { it > 0.0 }
                ExerciseHistoryChartMetric.Reps -> (entry.totalReps.takeIf { it > 0 }
                    ?: entry.setDetails.sumOf(SetPerformance::reps).takeIf { it > 0 })?.toDouble()
                ExerciseHistoryChartMetric.Distance -> entry.distanceKm.takeIf { it > 0.0 }
                ExerciseHistoryChartMetric.Duration -> entry.durationSeconds.takeIf { it > 0 }?.toDouble()
            } ?: return@mapNotNull null
            ExerciseHistoryChartPoint(entry.timestampMillis, value)
        }
        points.takeIf { it.size > 1 }?.let { ExerciseHistoryChartSeries(metric, it) }
    }
}

object WorkoutHistoryStore {
    private const val PREFERENCES = "wildforce_exercise_history"
    private const val HISTORY_KEYS = "exercise_history_keys"
    // Keep the same local horizon as the completed-session analytics store so
    // 12W and All can include the complete exercise history available on the
    // device instead of silently stopping after eight-ish weeks.
    private const val MAX_ENTRIES = 180

    internal fun history(context: Context, exercise: ExerciseSummary): List<ExerciseHistoryEntry> =
        historyForKey(context, exercise.historyKey()).sortedByDescending(ExerciseHistoryEntry::timestampMillis)

    /**
     * Returns every exercise key with local history, including exercises from
     * archived plans that are no longer present in the current plan.
     */
    internal fun historyKeys(context: Context): Set<String> {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val stored = preferences.getStringSet(HISTORY_KEYS, emptySet()).orEmpty()
        val discovered = preferences.all.mapNotNull { (key, value) ->
            key.takeIf { it != HISTORY_KEYS && value is String && runCatching { JSONArray(value) }.isSuccess }
        }.toSet()
        val allKeys = stored + discovered
        if (allKeys != stored) preferences.edit().putStringSet(HISTORY_KEYS, allKeys).apply()
        return allKeys
    }

    fun remoteEntries(context: Context): List<RemoteExerciseHistoryEntry> {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val keys = preferences.getStringSet(HISTORY_KEYS, emptySet()).orEmpty()
        return keys.flatMap { key -> historyForKey(context, key).map { entry ->
            RemoteExerciseHistoryEntry(
                exerciseKey = key,
                timestampMillis = entry.timestampMillis,
                sets = entry.sets,
                totalReps = entry.totalReps,
                maxWeightKg = entry.maxWeightKg,
                setReps = entry.setDetails.map(SetPerformance::reps),
                setWeightsKg = entry.setDetails.map(SetPerformance::weightKg),
                feedback = entry.feedback,
                note = entry.note,
                durationSeconds = entry.durationSeconds,
                distanceKm = entry.distanceKm,
                workoutId = entry.workoutId,
                plannedExerciseId = entry.plannedExerciseId,
                remoteResultId = entry.remoteResultId,
            )
        } }
    }

    /** Imports completed sets downloaded from another device without replacing local history. */
    fun importRemoteEntries(context: Context, entries: List<RemoteExerciseHistoryEntry>) {
        if (entries.isEmpty()) return
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val importedByKey = entries.groupBy { it.exerciseKey.trim().lowercase() }.filterKeys { it.isNotBlank() }
        val keys = preferences.getStringSet(HISTORY_KEYS, emptySet()).orEmpty().toMutableSet()

        importedByKey.forEach { (key, imported) ->
            val existing = historyForKey(context, key)
            val importedHistory = imported.map { entry ->
                val details = entry.setReps.mapIndexed { index, reps ->
                    SetPerformance(index + 1, reps, entry.setWeightsKg.getOrElse(index) { entry.maxWeightKg }, ExerciseSetStyle.Straight)
                }
                ExerciseHistoryEntry(
                    timestampMillis = entry.timestampMillis,
                    sets = entry.sets,
                    totalReps = entry.totalReps,
                    maxWeightKg = entry.maxWeightKg,
                    volumeKg = entry.setReps.mapIndexed { index, reps -> reps * entry.setWeightsKg.getOrElse(index) { entry.maxWeightKg } }.sum(),
                    setDetails = details,
                    feedback = entry.feedback,
                    note = entry.note,
                    durationSeconds = entry.durationSeconds,
                    distanceKm = entry.distanceKm,
                    workoutId = entry.workoutId,
                    plannedExerciseId = entry.plannedExerciseId,
                    remoteResultId = entry.remoteResultId,
                )
            }
            val merged = (existing + importedHistory)
                .distinctBy { entry -> entry.plannedExerciseId?.let { "$it|${entry.timestampMillis}" }
                    ?: entry.remoteResultId ?: "legacy|${entry.timestampMillis}" }
                .sortedByDescending { it.timestampMillis }
                .take(MAX_ENTRIES)
            preferences.edit().putString(key, historyJson(merged).toString()).apply()
            keys += key
        }
        preferences.edit().putStringSet(HISTORY_KEYS, keys).apply()
    }

    private fun historyForKey(context: Context, key: String): List<ExerciseHistoryEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(key, null) ?: return emptyList()
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
                                    set.optInt("durationSeconds"), set.optDouble("distanceKm", 0.0).takeIf(Double::isFinite) ?: 0.0,
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
                        durationSeconds = item.optInt("durationSeconds"),
                        distanceKm = item.optDouble("distanceKm", 0.0).takeIf(Double::isFinite) ?: 0.0,
                        workoutId = item.optString("workoutId").takeIf { it.isNotBlank() && it != "null" },
                        plannedExerciseId = item.optString("plannedExerciseId").takeIf { it.isNotBlank() && it != "null" },
                        remoteResultId = item.optString("remoteResultId").takeIf { it.isNotBlank() && it != "null" },
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun historyJson(entries: List<ExerciseHistoryEntry>) = JSONArray().apply {
        entries.forEach { entry ->
            val details = JSONArray().apply {
                entry.setDetails.forEach { set ->
                    put(JSONObject().put("setNumber", set.setNumber).put("reps", set.reps)
                        .put("weightKg", set.weightKg).put("setStyle", set.setStyle.name)
                        .put("durationSeconds", set.durationSeconds).put("distanceKm", set.distanceKm))
                }
            }
            put(JSONObject().put("timestamp", entry.timestampMillis).put("sets", entry.sets)
                .put("reps", entry.totalReps).put("maxWeightKg", entry.maxWeightKg).put("volumeKg", entry.volumeKg)
                .put("setDetails", details).put("feedback", entry.feedback ?: JSONObject.NULL)
                .put("note", entry.note ?: JSONObject.NULL).put("durationSeconds", entry.durationSeconds)
                .put("distanceKm", entry.distanceKm).put("workoutId", entry.workoutId ?: JSONObject.NULL)
                .put("plannedExerciseId", entry.plannedExerciseId ?: JSONObject.NULL).put("remoteResultId", entry.remoteResultId ?: JSONObject.NULL))
        }
    }

    internal fun record(
        context: Context,
        workout: WorkoutDaySummary,
        stats: Map<Int, ExerciseSessionStats>,
        completedSets: List<CompletedSetRecord> = emptyList(),
        feedbackByExercise: Map<Int, String> = emptyMap(),
        notesByExercise: Map<Int, String> = emptyMap(),
    ) {
        val timestamp = System.currentTimeMillis()
        val completedExercises = workout.pathBlocks().flatMap { it.exercises }
            .filter { item -> item.executionIndices.any { (stats[it]?.sets ?: 0) > 0 } }
        completedExercises.forEach { occurrence ->
            val exercise = occurrence.exercise
            val exerciseIndices = occurrence.executionIndices.toSet()
            val results = exerciseIndices.mapNotNull(stats::get)
            val result = ExerciseSessionStats(
                sets = results.sumOf { it.sets },
                totalReps = results.sumOf { it.totalReps },
                maxWeightKg = results.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                volumeKg = results.sumOf { it.volumeKg },
                durationSeconds = results.sumOf { it.durationSeconds },
                distanceKm = results.sumOf { it.distanceKm },
                averageWeightKg = results.map { it.averageWeightKg }.filter { it > 0 }.average().takeUnless(Double::isNaN) ?: 0.0,
            )
            val details = completedSets
                .filter { it.exerciseIndex in exerciseIndices }
                .sortedWith(compareBy(CompletedSetRecord::exerciseIndex, CompletedSetRecord::setNumber))
                .mapIndexed { index, set -> SetPerformance(index + 1, set.reps, set.weightKg, set.setStyle, set.durationSeconds, set.distanceKm) }
            val feedback = exerciseIndices.mapNotNull(feedbackByExercise::get).lastOrNull()?.trim()?.uppercase()?.takeIf(String::isNotBlank)
            val note = exerciseIndices.mapNotNull(notesByExercise::get).lastOrNull()?.trim()?.take(500)?.takeIf(String::isNotBlank)
            val entries = listOf(
                ExerciseHistoryEntry(timestamp, result.sets, result.totalReps, result.maxWeightKg, result.volumeKg, details, feedback, note, result.durationSeconds, result.distanceKm, workout.id, exercise.plannedExerciseId),
            ) + history(context, exercise)
            val json = JSONArray().apply {
                entries.take(MAX_ENTRIES).forEach { entry ->
                    val detailsJson = JSONArray().apply {
                        entry.setDetails.forEach { set ->
                            put(
                                JSONObject().put("setNumber", set.setNumber).put("reps", set.reps)
                                    .put("weightKg", set.weightKg).put("setStyle", set.setStyle.name)
                                    .put("durationSeconds", set.durationSeconds).put("distanceKm", set.distanceKm),
                            )
                        }
                    }
                    put(
                        JSONObject().put("timestamp", entry.timestampMillis).put("sets", entry.sets)
                            .put("reps", entry.totalReps).put("maxWeightKg", entry.maxWeightKg).put("volumeKg", entry.volumeKg)
                            .put("setDetails", detailsJson)
                            .put("feedback", entry.feedback ?: JSONObject.NULL)
                            .put("note", entry.note ?: JSONObject.NULL)
                            .put("durationSeconds", entry.durationSeconds).put("distanceKm", entry.distanceKm)
                            .put("workoutId", entry.workoutId ?: JSONObject.NULL).put("plannedExerciseId", entry.plannedExerciseId ?: JSONObject.NULL)
                            .put("remoteResultId", entry.remoteResultId ?: JSONObject.NULL),
                    )
                }
            }
            val preferences = context.getSharedPreferences(PREFERENCES, 0)
            val keys = preferences.getStringSet(HISTORY_KEYS, emptySet()).orEmpty() + exercise.historyKey()
            preferences.edit().putString(exercise.historyKey(), json.toString()).putStringSet(HISTORY_KEYS, keys).apply()
        }
    }

    /** Compact completed-session context used by every Android workout AI request. */
    fun aiPlanningContext(context: Context, maxEntries: Int = 48): String {
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val storedKeys = preferences.getStringSet(HISTORY_KEYS, emptySet()).orEmpty()
        val keys = storedKeys.ifEmpty {
            preferences.all.mapNotNull { (key, value) ->
                key.takeIf { it != HISTORY_KEYS && value is String && runCatching { JSONArray(value) }.isSuccess }
            }.toSet().also { migrated ->
                if (migrated.isNotEmpty()) preferences.edit().putStringSet(HISTORY_KEYS, migrated).apply()
            }
        }
        val entries = keys
            .flatMap { key -> historyForKey(context, key).map { key to it } }
            .sortedByDescending { (_, entry) -> entry.timestampMillis }
            // Keep several recent observations for every exercise instead of
            // allowing one long session to hide feedback from the rest of the plan.
            .groupBy { (exercise, _) -> exercise }
            .values
            .flatMap { it.take(4) }
            .sortedByDescending { (_, entry) -> entry.timestampMillis }
            .take(maxEntries)
        if (entries.isEmpty()) return "Historial de entrenamientos recientes: todavía no hay sesiones completadas."
        return buildString {
            appendLine("Historial de entrenamientos recientes (usa estos resultados para progresar o ajustar):")
            entries.forEach { (exercise, entry) ->
                append("- $exercise: ${entry.sets} series, ${entry.totalReps} reps, máximo ${"%.1f".format(java.util.Locale.US, entry.maxWeightKg)} kg, volumen ${"%.0f".format(java.util.Locale.US, entry.volumeKg)} kg")
                if (entry.setDetails.isNotEmpty()) {
                    append(", series registradas: ${entry.setDetails.joinToString("; ") { "${it.reps} reps @${"%.1f".format(java.util.Locale.US, it.weightKg)}kg (${it.setStyle})" }}")
                }
                entry.feedback?.let { append(", feedback: $it") }
                entry.note?.takeIf(String::isNotBlank)?.let { append(", nota: $it") }
                appendLine()
            }
            appendLine("Reglas de progresión: prioriza las entradas más recientes y usa el feedback y las notas como restricciones de la siguiente semana. Con feedback MUY FÁCIL o FÁCIL puedes subir carga, repeticiones o volumen de forma moderada; con JUSTO mantén o progresa hasta un 10%; con DIFÍCIL o MUY DIFÍCIL reduce la exigencia, mantiene la carga o usa una alternativa segura. Nunca ignores restricciones, molestias ni una nota explícita del usuario.")
        }
    }
}

object WorkoutAnalyticsStore {
    fun summaries(context: Context, exercises: List<ExerciseSummary>): List<ExerciseAnalyticsSummary> {
        val knownExercises = exercises.associateBy { it.historyKey() }
        val allExercises = WorkoutHistoryStore.historyKeys(context).map { key ->
            knownExercises[key] ?: storedExerciseSummary(context, key)
        } + exercises

        return allExercises.distinctBy { it.historyKey() }.mapNotNull { exercise ->
            val entries = WorkoutHistoryStore.history(context, exercise)
            if (entries.isEmpty()) null else ExerciseAnalyticsSummary(
                exercise = exercise,
                sessions = entries.size,
                personalBestKg = entries.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                totalVolumeKg = entries.sumOf { it.volumeKg },
                lastFeedback = entries.firstOrNull()?.feedback,
            )
        }.sortedWith(compareByDescending<ExerciseAnalyticsSummary> { it.totalVolumeKg }.thenBy { it.exercise.name })
    }

    private fun storedExerciseSummary(context: Context, key: String): ExerciseSummary {
        val entries = WorkoutHistoryStore.history(context, ExerciseSummary(key, key, 1, "1", 0))
        val trackingMode = when {
            entries.any { it.distanceKm > 0.0 } -> ExerciseTrackingMode.DurationAndDistance
            entries.any { it.durationSeconds > 0 } -> ExerciseTrackingMode.Duration
            else -> ExerciseTrackingMode.Repetitions
        }
        return ExerciseSummary(
            name = exerciseDisplayName(key, key),
            imageKey = key,
            sets = 1,
            reps = "1",
            restSeconds = 0,
            trackingMode = trackingMode,
        )
    }

    fun history(context: Context, exercise: ExerciseSummary): List<ExerciseAnalyticsPoint> =
        WorkoutHistoryStore.history(context, exercise).map {
            ExerciseAnalyticsPoint(
                timestampMillis = it.timestampMillis,
                sets = it.sets,
                reps = it.totalReps,
                maxWeightKg = it.maxWeightKg,
                volumeKg = it.volumeKg,
                feedback = it.feedback,
                durationSeconds = it.durationSeconds,
                distanceKm = it.distanceKm,
            )
        }
}
