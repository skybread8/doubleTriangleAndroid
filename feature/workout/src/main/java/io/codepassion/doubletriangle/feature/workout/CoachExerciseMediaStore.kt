package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Pull-only details supplied by the user's active coaches. */
data class CoachExerciseMedia(
    val id: String? = null,
    val name: String? = null,
    val imageUrl: String?,
    val youtubeVideoId: String?,
    val notes: String? = null,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val requiredEquipment: List<String> = emptyList(),
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val trackingMode: String? = null,
    val supportsExternalLoad: Boolean = false,
    val isUnilateral: Boolean = false,
    val isPerSideLoad: Boolean = false,
) {
    val youtubeUrl: String? get() = youtubeVideoId?.takeIf(String::isNotBlank)?.let { "https://www.youtube.com/watch?v=$it" }
    val youtubeThumbnailUrl: String? get() = youtubeVideoId?.takeIf(String::isNotBlank)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}

object CoachExerciseMediaStore {
    private const val preferencesName = "wildforce_coach_exercise_media"
    private const val entriesKey = "entries"
    private val customImageUrls = ConcurrentHashMap<String, String>()

    fun replace(context: Context, remoteEntries: JSONArray) {
        val latestByExercise = entries(context)
        // The legacy content feed only owns catalog keys. Keep custom
        // `coach:{uuid}` records, which arrive in a separate pull-only feed.
        latestByExercise.keys().asSequence().filterNot { it.startsWith("coach:") }.toList()
            .forEach(latestByExercise::remove)
        for (index in 0 until remoteEntries.length()) {
            val entry = remoteEntries.optJSONObject(index) ?: continue
            if (!entry.isNull("deleted_at")) continue
            val exercise = entry.optString("exercise").trim().lowercase()
            if (exercise.isBlank()) continue
            val current = latestByExercise.optJSONObject(exercise)
            if (current == null || entry.optString("updated_at") > current.optString("updated_at")) latestByExercise.put(exercise, entry)
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putString(entriesKey, latestByExercise.toString()).apply()
    }

    /**
     * Current sync payload for a coach's custom library. It intentionally uses
     * the UUID as the key (`coach:{uuid}` in workout JSON), while the older
     * coach-exercise-content resource remains supported for existing plans.
     */
    fun replaceCoachExercises(context: Context, remoteEntries: JSONArray) {
        val latest = entries(context)
        for (index in 0 until remoteEntries.length()) {
            val entry = remoteEntries.optJSONObject(index) ?: continue
            val id = entry.optString("id").trim().lowercase()
            if (id.isBlank()) continue
            val key = "coach:$id"
            if (entry.isNull("deleted_at")) {
                latest.remove(key)
                customImageUrls.remove(key)
                continue
            }
            val current = latest.optJSONObject(key)
            if (current == null || entry.optString("updated_at") >= current.optString("updated_at")) {
                latest.put(key, entry)
            }
            entry.imageUrl()?.let { customImageUrls[key] = it }
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putString(entriesKey, latest.toString()).apply()
    }

    fun latest(context: Context, exerciseKey: String?): CoachExerciseMedia? = runCatching {
        val key = exerciseKey?.trim()?.lowercase().orEmpty()
        if (key.isBlank()) return null
        val entry = entries(context).optJSONObject(key) ?: return null
        CoachExerciseMedia(
            id = entry.optString("id").ifBlank { key.removePrefix("coach:").ifBlank { null } },
            name = entry.optString("name").trim().ifBlank { null },
            imageUrl = entry.optString("image_url").ifBlank { null },
            youtubeVideoId = entry.optString("youtube_video_id").ifBlank { null },
            notes = entry.optString("notes").trim().ifBlank { null },
            instructions = entry.stringList("instructions"),
            tips = entry.stringList("tips"),
            requiredEquipment = entry.stringList("required_equipment"),
            primaryMuscles = entry.stringList("primary_muscles"),
            secondaryMuscles = entry.stringList("secondary_muscles"),
            trackingMode = entry.optString("tracking_mode").trim().ifBlank { null },
            supportsExternalLoad = entry.optBoolean("supports_external_load", false),
            isUnilateral = entry.optBoolean("is_unilateral", false),
            isPerSideLoad = entry.optBoolean("is_per_side_load", false),
        ).also { media ->
            if (key.startsWith("coach:")) {
                (media.imageUrl ?: media.youtubeThumbnailUrl)?.let { customImageUrls[key] = it }
            }
        }
    }.getOrNull()

    /** Picker projection for the custom exercises that the active coach shared. */
    internal fun customExerciseChoices(context: Context): List<ExerciseChoice> = runCatching {
        val saved = entries(context)
        saved.keys().asSequence()
            .filter { it.startsWith("coach:") }
            .mapNotNull { key ->
                val media = latest(context, key) ?: return@mapNotNull null
                val id = media.id ?: return@mapNotNull null
                ExerciseChoice(
                    name = media.name ?: "Ejercicio del entrenador",
                    imageKey = key,
                    primaryMuscles = media.primaryMuscles.map { it.trim().lowercase() }.filter(String::isNotBlank).toSet(),
                    coachExerciseId = id,
                    imageUrl = media.imageUrl ?: media.youtubeThumbnailUrl,
                    trackingMode = media.trackingMode.androidTrackingMode(),
                    durationUsesMinutes = media.trackingMode?.trim()?.lowercase() in setOf("durationminutes", "duration_minutes"),
                    isPerSideLoad = media.isPerSideLoad,
                )
            }
            .sortedBy { it.name }
            .toList()
    }.getOrDefault(emptyList())

    /** Fast path for all generic workout cards, which only know an image key. */
    internal fun cachedImageUrl(exerciseKey: String?): String? =
        exerciseKey?.trim()?.lowercase()?.takeIf { it.startsWith("coach:") }?.let(customImageUrls::get)

    private fun entries(context: Context): JSONObject = JSONObject(
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString(entriesKey, "{}") ?: "{}",
    )

    private fun JSONObject.stringList(name: String): List<String> = optJSONArray(name)?.let { values ->
        List(values.length()) { index -> values.optString(index).trim() }.filter(String::isNotBlank)
    }.orEmpty()

    private fun JSONObject.imageUrl(): String? =
        optString("image_url").trim().takeIf(String::isNotBlank)
            ?: optString("youtube_video_id").trim().takeIf(String::isNotBlank)
                ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

    private fun String?.androidTrackingMode(): ExerciseTrackingMode = when (this?.trim()?.lowercase()) {
        "durationseconds", "duration_seconds", "durationminutes", "duration_minutes" -> ExerciseTrackingMode.Duration
        "durationminutesanddistance", "duration_minutes_and_distance" -> ExerciseTrackingMode.DurationAndDistance
        else -> ExerciseTrackingMode.Repetitions
    }
}
