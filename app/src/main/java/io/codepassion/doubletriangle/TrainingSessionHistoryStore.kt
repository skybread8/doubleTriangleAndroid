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
)

internal object TrainingSessionHistoryStore {
    private const val KEY = "completed_session_history"
    private const val MAX_ENTRIES = 180

    fun record(preferences: SharedPreferences, durationSeconds: Int, sets: Int, volumeKg: Double, focus: String? = null) {
        val entries = load(preferences)
        val updated = listOf(TrainingSessionHistoryEntry(System.currentTimeMillis(), durationSeconds.coerceAtLeast(0), sets.coerceAtLeast(0), volumeKg.coerceAtLeast(0.0), focus?.trim()?.takeIf(String::isNotBlank))) + entries
        val json = JSONArray().apply {
            updated.take(MAX_ENTRIES).forEach { entry ->
                put(JSONObject().put("timestamp", entry.timestampMillis).put("duration", entry.durationSeconds).put("sets", entry.sets).put("volumeKg", entry.volumeKg).put("focus", entry.focus ?: JSONObject.NULL))
            }
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    fun importRemote(preferences: SharedPreferences, entries: List<TrainingSessionHistoryEntry>) {
        if (entries.isEmpty()) return
        val merged = (load(preferences) + entries)
            .distinctBy { it.timestampMillis }
            .sortedByDescending { it.timestampMillis }
            .take(MAX_ENTRIES)
        val json = JSONArray().apply {
            merged.forEach { entry ->
                put(JSONObject().put("timestamp", entry.timestampMillis).put("duration", entry.durationSeconds)
                    .put("sets", entry.sets).put("volumeKg", entry.volumeKg).put("focus", entry.focus ?: JSONObject.NULL))
            }
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    fun load(preferences: SharedPreferences): List<TrainingSessionHistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]").orEmpty())
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TrainingSessionHistoryEntry(item.optLong("timestamp"), item.optInt("duration"), item.optInt("sets"), item.optDouble("volumeKg"), item.optString("focus").takeIf(String::isNotBlank))
        }
    }.getOrDefault(emptyList())
}
