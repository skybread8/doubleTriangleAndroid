package io.codepassion.doubletriangle

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal data class TrainingSessionHistoryEntry(
    val timestampMillis: Long,
    val durationSeconds: Int,
    val sets: Int,
    val volumeKg: Double,
    val focus: String? = null,
    val workoutId: String? = null,
)

internal object TrainingSessionHistoryStore {
    private const val KEY = "completed_session_history"

    fun record(preferences: SharedPreferences, durationSeconds: Int, sets: Int, volumeKg: Double, focus: String? = null, workoutId: String? = null) {
        val entries = load(preferences)
        val updated = listOf(TrainingSessionHistoryEntry(System.currentTimeMillis(), durationSeconds.coerceAtLeast(0), sets.coerceAtLeast(0), volumeKg.coerceAtLeast(0.0), focus?.trim()?.takeIf(String::isNotBlank), workoutId)) + entries
        val json = JSONArray().apply {
            updated.forEach { entry ->
                put(JSONObject().put("timestamp", entry.timestampMillis).put("duration", entry.durationSeconds).put("sets", entry.sets).put("volumeKg", entry.volumeKg).put("focus", entry.focus ?: JSONObject.NULL).put("workoutId", entry.workoutId ?: JSONObject.NULL))
            }
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    fun importRemote(preferences: SharedPreferences, entries: List<TrainingSessionHistoryEntry>, legacyResultTimestamps: Set<Long> = emptySet()) {
        if (entries.isEmpty()) return
        val merged = mergeTrainingSessions(load(preferences), entries, legacyResultTimestamps)
        val json = JSONArray().apply {
            merged.forEach { entry ->
                put(JSONObject().put("timestamp", entry.timestampMillis).put("duration", entry.durationSeconds)
                    .put("sets", entry.sets).put("volumeKg", entry.volumeKg).put("focus", entry.focus ?: JSONObject.NULL).put("workoutId", entry.workoutId ?: JSONObject.NULL))
            }
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    fun load(preferences: SharedPreferences): List<TrainingSessionHistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]").orEmpty())
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TrainingSessionHistoryEntry(item.optLong("timestamp"), item.optInt("duration"), item.optInt("sets"), item.optDouble("volumeKg"), item.optString("focus").takeIf { it.isNotBlank() && it != "null" }, item.optString("workoutId").takeIf { it.isNotBlank() && it != "null" })
        }
    }.getOrDefault(emptyList())
}

internal fun mergeTrainingSessions(
    local: List<TrainingSessionHistoryEntry>,
    remote: List<TrainingSessionHistoryEntry>,
    legacyResultTimestamps: Set<Long>,
): List<TrainingSessionHistoryEntry> {
    val remoteIds = remote.mapNotNull { it.workoutId }.toSet()
    val remaining = local.filterNot { entry ->
        (entry.workoutId != null && entry.workoutId in remoteIds) || (entry.workoutId == null && (
            entry.timestampMillis in legacyResultTimestamps || remote.any {
                kotlin.math.abs(it.timestampMillis - entry.timestampMillis) <= 5_000 &&
                    it.sets == entry.sets && kotlin.math.abs(it.volumeKg - entry.volumeKg) < 0.01
            }
        ))
    }
    return (remote + remaining).distinctBy { it.workoutId ?: "timestamp:${it.timestampMillis}" }
        .sortedByDescending { it.timestampMillis }
}
