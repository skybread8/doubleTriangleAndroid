package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

private object ExerciseSearchCatalog {
    @Volatile private var cached: JSONObject? = null
    fun get(context: Context): JSONObject = cached ?: synchronized(this) {
        cached ?: runCatching {
            JSONObject(context.applicationContext.resources.openRawResource(R.raw.ios_exercise_metadata).bufferedReader().use { it.readText() })
        }.getOrDefault(JSONObject()).also { cached = it }
    }
}

private object CanonicalExerciseCatalog {
    @Volatile private var cached: Map<String, JSONObject>? = null
    fun get(context: Context): Map<String, JSONObject> = cached ?: synchronized(this) {
        cached ?: runCatching {
            val root = JSONObject(context.applicationContext.resources.openRawResource(R.raw.exercise_catalog).bufferedReader().use { it.readText() })
            val exercises = root.getJSONArray("exercises")
            buildMap {
                for (index in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(index)
                    put(exercise.getString("id"), exercise)
                }
            }
        }.getOrDefault(emptyMap()).also { cached = it }
    }
}
/** Search the iOS catalog's English and selected-language names and aliases. */
fun exerciseMatchesSearch(context: Context, imageKey: String?, fallback: String, query: String): Boolean {
    val needle = normalizeExerciseSearch(query)
    if (needle.isEmpty()) return true
    val key = canonicalExerciseMediaKey(imageKey ?: fallback) ?: imageKey ?: fallback
    val metadata = ExerciseSearchCatalog.get(context).optJSONObject(key)
    val language = context.resources.configuration.locales[0].toLanguageTag().let {
        if (it.startsWith("zh")) "zh-Hans" else it.substringBefore("-")
    }
    val names = mutableListOf(fallback, exerciseDisplayName(imageKey, fallback), key)
    CanonicalExerciseCatalog.get(context)[key]?.let { names += it.optString("name") }
    metadata?.let { item ->
        names += item.optString("name")
        listOf(item.optJSONArray("aliases"), item.optJSONObject("translations")?.optJSONArray(language)).forEach { array ->
            if (array != null) for (index in 0 until array.length()) names += array.optString(index)
        }
    }
    return exerciseNamesMatch(names, needle)
}

internal fun normalizeExerciseSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

internal fun exerciseNamesMatch(names: List<String>, query: String): Boolean {
    val needle = normalizeExerciseSearch(query)
    return names.any { normalizeExerciseSearch(it).contains(needle) }
}
