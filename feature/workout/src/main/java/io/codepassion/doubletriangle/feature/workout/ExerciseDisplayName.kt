package io.codepassion.doubletriangle.feature.workout

import java.util.Locale

/**
 * Resolves the stable exercise key used by synced plans to the human-readable
 * name used by the workout UI. Plans created by older/server flows can carry
 * `airSquat` as their name, while iOS always presents the exercise's display
 * name. Keep this fallback local to the exercise domain; translation catalogs
 * remain owned by the separate localization workstream.
 */
fun exerciseDisplayName(imageKey: String?, fallback: String): String {
    // Legacy sessions may have only the technical name and no imageKey.
    val key = imageKey?.trim().takeUnless { it.isNullOrBlank() } ?: fallback.trim()
    // Remote/imported plans have historically used both the Swift camelCase
    // key (`airSquat`) and transport-safe variants such as `air_squat` or
    // `air-squat`. Resolve all of them to the same iOS display name before
    // falling back to the raw value; otherwise a restored active session can
    // expose implementation keys in its hero and next-exercise label.
    val normalizedKey = key.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)
    val knownName = spanishExerciseNames.entries
        .firstOrNull { (candidate, _) ->
            candidate.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT) == normalizedKey
        }
        ?.value
    if (!knownName.isNullOrBlank()) return knownName
    if (fallback.isNotBlank() && !fallback.equals(key, ignoreCase = true)) return fallback
    return key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
        .ifBlank { fallback }
}

private val spanishExerciseNames = mapOf(
    "airSquat" to "Sentadilla libre",
    "gobletSquat" to "Sentadilla goblet",
    "barbellBackSquat" to "Sentadilla con barra",
    "benchPress" to "Press de banca",
    "barbellBenchPress" to "Press de banca con barra",
    "inclineBenchPress" to "Press inclinado",
    "dumbbellInclineBenchPress" to "Press inclinado con mancuernas",
    "pushUp" to "Flexiones",
    "pullUp" to "Dominadas",
    "dumbbellRow" to "Remo con mancuerna",
    "bentOverRow" to "Remo con barra",
    "barbellBentOverRow" to "Remo inclinado con barra",
    "deadlift" to "Peso muerto",
    "romanianDeadlift" to "Peso muerto rumano",
    "barbellRomanianDeadlift" to "Peso muerto rumano con barra",
    "walkingLunge" to "Zancadas",
    "overheadPress" to "Press militar",
    "barbellOverheadPress" to "Press militar con barra",
    "dumbbellChestFly" to "Aperturas con mancuernas",
    "lyingLegCurl" to "Curl femoral tumbado",
    "bicepsCurl" to "Curl de bíceps",
    "chestDip" to "Fondos",
    "plank" to "Plancha",
)
