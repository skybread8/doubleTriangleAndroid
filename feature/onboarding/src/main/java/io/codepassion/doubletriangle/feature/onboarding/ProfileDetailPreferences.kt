package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class NutritionProfilePreferences(
    val isConfigured: Boolean = false,
    val dietaryStyle: String = "Omnívora",
    val mealsPerDay: Int = 3,
    val eatingWindowStartHour: Int? = null,
    val eatingWindowEndHour: Int? = null,
    val wantsMealSuggestions: Boolean = true,
    val cookingEffort: String = "Medio",
    val budgetSensitivity: String = "Media",
    val preferredProteinSources: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val excludedFoods: List<String> = emptyList(),
    val allergiesAndIntolerances: List<String> = emptyList(),
    val notes: String = "",
)

internal data class BodyCompositionMeasurements(
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null,
)

internal object ProfileDetailPreferencesStore {
    private const val PREFERENCES = "wildforce_profile_details"
    private const val NUTRITION_KEY = "nutrition_profile"
    private const val BODY_KEY = "body_composition"

    fun loadNutrition(context: Context): NutritionProfilePreferences = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(NUTRITION_KEY, null)
            ?: return NutritionProfilePreferences()
        val value = JSONObject(raw)
        NutritionProfilePreferences(
            isConfigured = value.optBoolean("isConfigured"),
            dietaryStyle = value.optString("dietaryStyle", "Omnívora"),
            mealsPerDay = value.optInt("mealsPerDay", 3).coerceIn(1, 8),
            eatingWindowStartHour = value.optionalHour("eatingWindowStartHour"),
            eatingWindowEndHour = value.optionalHour("eatingWindowEndHour"),
            wantsMealSuggestions = value.optBoolean("wantsMealSuggestions", true),
            cookingEffort = value.optString("cookingEffort", "Medio"),
            budgetSensitivity = value.optString("budgetSensitivity", "Media"),
            preferredProteinSources = value.stringList("preferredProteinSources"),
            dislikes = value.stringList("dislikes"),
            excludedFoods = value.stringList("excludedFoods"),
            allergiesAndIntolerances = value.stringList("allergiesAndIntolerances"),
            notes = value.optString("notes"),
        )
    }.getOrDefault(NutritionProfilePreferences())

    fun saveNutrition(context: Context, profile: NutritionProfilePreferences) {
        val value = JSONObject()
            .put("isConfigured", profile.isConfigured)
            .put("dietaryStyle", profile.dietaryStyle)
            .put("mealsPerDay", profile.mealsPerDay)
            .put("eatingWindowStartHour", profile.eatingWindowStartHour ?: JSONObject.NULL)
            .put("eatingWindowEndHour", profile.eatingWindowEndHour ?: JSONObject.NULL)
            .put("wantsMealSuggestions", profile.wantsMealSuggestions)
            .put("cookingEffort", profile.cookingEffort)
            .put("budgetSensitivity", profile.budgetSensitivity)
            .put("preferredProteinSources", profile.preferredProteinSources.toJsonArray())
            .put("dislikes", profile.dislikes.toJsonArray())
            .put("excludedFoods", profile.excludedFoods.toJsonArray())
            .put("allergiesAndIntolerances", profile.allergiesAndIntolerances.toJsonArray())
            .put("notes", profile.notes)
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(NUTRITION_KEY, value.toString()).apply()
    }

    fun loadBodyComposition(context: Context): BodyCompositionMeasurements = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(BODY_KEY, null)
            ?: return BodyCompositionMeasurements()
        val value = JSONObject(raw)
        BodyCompositionMeasurements(
            waistCm = value.optionalMeasurement("waistCm"),
            neckCm = value.optionalMeasurement("neckCm"),
            hipCm = value.optionalMeasurement("hipCm"),
        )
    }.getOrDefault(BodyCompositionMeasurements())

    fun saveBodyComposition(context: Context, measurements: BodyCompositionMeasurements) {
        val value = JSONObject()
            .put("waistCm", measurements.waistCm ?: JSONObject.NULL)
            .put("neckCm", measurements.neckCm ?: JSONObject.NULL)
            .put("hipCm", measurements.hipCm ?: JSONObject.NULL)
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(BODY_KEY, value.toString()).apply()
    }
}

private fun JSONObject.optionalHour(key: String): Int? =
    takeUnless { isNull(key) }?.optInt(key)?.takeIf { it in 0..23 }

private fun JSONObject.optionalMeasurement(key: String): Double? =
    takeUnless { isNull(key) }?.optDouble(key)?.takeIf { it.isFinite() && it in 20.0..300.0 }

private fun JSONObject.stringList(key: String): List<String> {
    val values = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until values.length()) {
            values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { array -> forEach(array::put) }

internal fun parseProfileList(value: String): List<String> = value.split(',')
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy { it.lowercase() }
