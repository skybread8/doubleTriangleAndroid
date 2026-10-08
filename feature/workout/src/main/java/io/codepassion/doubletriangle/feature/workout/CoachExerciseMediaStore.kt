package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Pull-only media supplied by the user's active coaches. */
data class CoachExerciseMedia(val imageUrl: String?, val youtubeVideoId: String?) {
    val youtubeUrl: String? get() = youtubeVideoId?.takeIf(String::isNotBlank)?.let { "https://www.youtube.com/watch?v=$it" }
    val youtubeThumbnailUrl: String? get() = youtubeVideoId?.takeIf(String::isNotBlank)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}

object CoachExerciseMediaStore {
    private const val preferencesName = "wildforce_coach_exercise_media"
    private const val entriesKey = "entries"

    fun replace(context: Context, remoteEntries: JSONArray) {
        val latestByExercise = mutableMapOf<String, JSONObject>()
        for (index in 0 until remoteEntries.length()) {
            val entry = remoteEntries.optJSONObject(index) ?: continue
            if (!entry.isNull("deleted_at")) continue
            val exercise = entry.optString("exercise").trim().lowercase()
            if (exercise.isBlank()) continue
            val current = latestByExercise[exercise]
            if (current == null || entry.optString("updated_at") > current.optString("updated_at")) latestByExercise[exercise] = entry
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putString(entriesKey, JSONObject(latestByExercise).toString()).apply()
    }

    fun latest(context: Context, exerciseKey: String?): CoachExerciseMedia? = runCatching {
        val key = exerciseKey?.trim()?.lowercase().orEmpty()
        if (key.isBlank()) return null
        val entry = JSONObject(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString(entriesKey, "{}") ?: "{}")
            .optJSONObject(key) ?: return null
        CoachExerciseMedia(entry.optString("image_url").ifBlank { null }, entry.optString("youtube_video_id").ifBlank { null })
    }.getOrNull()
}
