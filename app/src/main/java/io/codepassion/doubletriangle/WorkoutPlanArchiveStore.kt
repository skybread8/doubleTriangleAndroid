package io.codepassion.doubletriangle

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal data class ArchivedWorkoutPlan(
    val archivedAtMillis: Long,
    val rawJson: String,
)

internal object WorkoutPlanArchiveStore {
    private const val Key = "workout_plan_archive"
    private const val MaximumPlans = 24

    fun archive(preferences: SharedPreferences, rawJson: String) {
        if (rawJson.isBlank() || runCatching { JSONObject(rawJson) }.isFailure) return
        val existing = load(preferences).filterNot { it.rawJson == rawJson }
        val updated = (listOf(ArchivedWorkoutPlan(System.currentTimeMillis(), rawJson)) + existing).take(MaximumPlans)
        val array = JSONArray().apply {
            updated.forEach { entry ->
                put(JSONObject().put("archivedAtMillis", entry.archivedAtMillis).put("rawJson", entry.rawJson))
            }
        }
        preferences.edit().putString(Key, array.toString()).apply()
    }

    fun load(preferences: SharedPreferences): List<ArchivedWorkoutPlan> = runCatching {
        val array = JSONArray(preferences.getString(Key, "[]").orEmpty())
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rawJson = item.optString("rawJson")
                if (rawJson.isNotBlank() && runCatching { JSONObject(rawJson) }.isSuccess) {
                    add(ArchivedWorkoutPlan(item.optLong("archivedAtMillis"), rawJson))
                }
            }
        }
    }.getOrDefault(emptyList())
}
