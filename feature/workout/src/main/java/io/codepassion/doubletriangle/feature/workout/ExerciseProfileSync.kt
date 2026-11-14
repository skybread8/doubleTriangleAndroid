package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/** Builds the same flat exercise-profile records used by iOS, retaining server IDs. */
object ExerciseProfileSync {
    private const val prefsName = "wildforce_exercise_profiles"
    data class Records(val values: JSONArray, val unresolved: Int)

    fun hasLocalProfiles(context: Context): Boolean =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).all.keys.any { it.endsWith(".level") }

    fun records(context: Context, userId: String, remote: JSONArray, now: String, trainingLevel: String): Records {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val metadata = JSONObject(context.resources.openRawResource(R.raw.ios_exercise_metadata).bufferedReader().use { it.readText() })
        val catalog = metadata.keys().asSequence().associateBy { it.lowercase(Locale.ROOT) }
        val result = JSONArray()
        var unresolved = 0
        prefs.all.keys.filter { it.endsWith(".level") }.sorted().forEach { levelKey ->
            val key = levelKey.removeSuffix(".level")
            val exercise = prefs.getString("$key.exercise", null)?.takeIf { it.isNotBlank() }
                ?: if (key.startsWith("coach:")) key else catalog[key.lowercase(Locale.ROOT)]
                ?: run { unresolved++; return@forEach }
            val existing = (0 until remote.length()).asSequence().mapNotNull(remote::optJSONObject)
                .firstOrNull { it.optString("exercise").equals(exercise, ignoreCase = true) }
            val id = existing?.optString("id")?.takeIf { it.isNotBlank() }
                ?: UUID.nameUUIDFromBytes("$userId:exercise-profile:$exercise".toByteArray()).toString()
            val record = existing?.let { JSONObject(it.toString()) } ?: JSONObject().put("id", id).put("created_at", now)
            record.put("updated_at", now).put("deleted_at", JSONObject.NULL).put("exercise", exercise)
            record.put("level", when (prefs.getString(levelKey, "Principiante")?.lowercase(Locale.ROOT)) {
                "intermedio", "intermediate" -> "intermediate"
                "avanzado", "advanced" -> "advanced"
                "novato", "novice" -> "novice"
                else -> "beginner"
            })
            listOf("workingWeightKg" to "working_weight", "estimatedOneRepMaxKg" to "estimated_one_rep_max", "typicalDistanceKm" to "typical_distance_km").forEach { (local, server) ->
                record.put(server, prefs.getString("$key.$local", null)?.toDoubleOrNull() ?: JSONObject.NULL)
            }
            listOf("maxReps" to "max_reps", "preferredRepRangeMin" to "preferred_rep_range_min", "preferredRepRangeMax" to "preferred_rep_range_max", "typicalDurationMinutes" to "typical_duration_minutes", "typicalPaceSecondsPerKm" to "typical_pace_seconds_per_km").forEach { (local, server) ->
                record.put(server, prefs.getInt("$key.$local", 0).takeIf { it > 0 } ?: JSONObject.NULL)
            }
            record.put("notes", prefs.getString("$key.notes", "").orEmpty())
            result.put(record)
        }
        // Older Android versions recorded workout results but did not create
        // the corresponding iOS-style profile. Reconstruct only profiles that
        // do not already exist locally or on the server (including tombstones).
        val localKeys = prefs.all.keys.filter { it.endsWith(".level") }.map { it.removeSuffix(".level") }.toSet()
        val serverKeys = (0 until remote.length()).mapNotNull { remote.optJSONObject(it)?.optString("exercise")?.lowercase(Locale.ROOT) }.toSet()
        WorkoutHistoryStore.remoteEntries(context).groupBy { it.exerciseKey }.forEach { (key, history) ->
            if (key in localKeys || key.lowercase(Locale.ROOT) in serverKeys) return@forEach
            val exercise = if (key.startsWith("coach:")) key else catalog[key.lowercase(Locale.ROOT)] ?: return@forEach
            val latest = history.maxByOrNull { it.timestampMillis } ?: return@forEach
            val feedback = latest.feedback.orEmpty().uppercase(Locale.ROOT)
            val factor = when (feedback) {
                "MUY FÁCIL", "MUY FACIL", "VERY EASY", "VERYEASY" -> 1.15
                "FÁCIL", "FACIL", "EASY" -> 1.05
                "MUY DIFÍCIL", "MUY DIFICIL", "VERY HARD", "VERYHARD" -> 0.90
                else -> 1.0
            }
            val weight = latest.maxWeightKg.takeIf { it > 0 }?.let { kotlin.math.round(it * factor * 2) / 2 }
            val maximumReps = history.maxOfOrNull { it.totalReps }?.takeIf { it > 0 }
            val reps = if (weight == null) maximumReps?.let { (it * factor).toInt() } else maximumReps
            result.put(JSONObject()
                .put("id", UUID.nameUUIDFromBytes("$userId:exercise-profile:$exercise".toByteArray()).toString())
                .put("created_at", now).put("updated_at", now).put("exercise", exercise).put("level", trainingLevel)
                .put("working_weight", weight ?: JSONObject.NULL).put("max_reps", reps ?: JSONObject.NULL)
                .put("typical_duration_minutes", (latest.durationSeconds / 60).takeIf { it > 0 } ?: JSONObject.NULL)
                .put("typical_distance_km", latest.distanceKm.takeIf { it > 0 } ?: JSONObject.NULL))
        }
        return Records(result, unresolved)
    }
}
