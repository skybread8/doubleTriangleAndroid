package io.codepassion.doubletriangle.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import org.json.JSONObject

/** Text and exercise names exported from the shared iOS string catalog. */
@Composable
internal fun tutorialText(key: String): String {
    sharedTutorialTextResources[key]?.let { return androidx.compose.ui.res.stringResource(it) }
    val catalog = tutorialCatalog()
    val value = catalog.optJSONObject("texts")?.optString(key)?.takeIf { it.isNotBlank() } ?: key
    // Xcode uses positional object placeholders (e.g. %1$@), while the
    // tutorial's single-object substitutions below use the Swift %@ form.
    return value.replace(Regex("%\\d+\\$@"), "%@")
}

@Composable
fun tutorialExerciseName(imageKey: String?, fallback: String): String =
    exerciseNameFromCatalog(tutorialCatalog(), imageKey, fallback)

/** Resolve names in services and widgets using the caller's configured locale. */
fun localizedExerciseName(context: android.content.Context, imageKey: String?, fallback: String): String {
    val locale = context.resources.configuration.locales.toLanguageTags()
    val catalog = synchronized(backgroundCatalogs) {
        backgroundCatalogs.getOrPut(locale) {
            runCatching {
                JSONObject(context.resources.openRawResource(R.raw.exercise_tutorial_labels).bufferedReader().use { it.readText() })
            }.getOrDefault(JSONObject())
        }
    }
    return exerciseNameFromCatalog(catalog, imageKey, fallback)
}

private val backgroundCatalogs = mutableMapOf<String, JSONObject>()

private fun exerciseNameFromCatalog(catalog: JSONObject, imageKey: String?, fallback: String): String {
    val key = canonicalExerciseMediaKey(imageKey?.takeIf { it.isNotBlank() } ?: fallback)
    val names = catalog.optJSONObject("names")
    val normalizedKey = key?.filter(Char::isLetterOrDigit)?.lowercase(java.util.Locale.ROOT)
    val catalogKey = names?.keys()?.asSequence()?.firstOrNull {
        it.filter(Char::isLetterOrDigit).lowercase(java.util.Locale.ROOT) == normalizedKey
    }
    return catalogKey?.let { names?.optString(it)?.takeIf(String::isNotBlank) }
        ?: exerciseDisplayName(imageKey, fallback)
}

@Composable
internal fun tutorialMuscleName(muscle: MuscleVisual): String = tutorialText(when (muscle) {
    MuscleVisual.LowerBack -> "Lower Back"
    MuscleVisual.UpperBack -> "Upper Back"
    MuscleVisual.InnerThigh -> "Inner Thigh"
    MuscleVisual.OuterThigh -> "Outer Thigh"
    MuscleVisual.LowerAbs -> "Lower Abs"
    MuscleVisual.UpperAbs -> "Upper Abs"
    MuscleVisual.FullBody -> "Full Body"
    else -> muscle.name
})

@Composable
internal fun tutorialEquipmentName(label: String): String = tutorialText(when (label) {
    "Banco plano" -> "Flat Bench"
    "Barra EZ" -> "EZ Bar"
    "Máquina Smith" -> "Smith Machine"
    "Banco ajustable" -> "Adjustable Bench"
    "Mancuernas" -> "Dumbbells"
    "Máquina de poleas" -> "Cable Machine"
    "Barra y discos" -> "Olympic Barbell"
    "Barra de dominadas" -> "Pull-Up Bar"
    "Prensa de piernas" -> "Leg Press Machine"
    else -> "Bodyweight"
})

@Composable
private fun tutorialCatalog(): JSONObject {
    val context = LocalContext.current
    val locale = LocalLocale.current
    return remember(context, locale) {
        runCatching {
            JSONObject(context.resources.openRawResource(R.raw.exercise_tutorial_labels).bufferedReader().use { it.readText() })
        }.getOrDefault(JSONObject())
    }
}
