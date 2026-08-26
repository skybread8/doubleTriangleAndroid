package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class BodyMetricEntry(val date: String, val heightCm: Double, val weightKg: Double)

internal object BodyMetricsStore {
    private const val PREFS = "wildforce_body_metrics"
    private const val KEY = "history"

    fun record(context: Context, profile: OnboardingProfile) {
        val entries = load(context).toMutableList()
        val today = java.time.LocalDate.now().toString()
        entries.removeAll { it.date == today }
        entries += BodyMetricEntry(today, profile.heightCm.toDouble(), profile.weightKg)
        val array = JSONArray().apply {
            entries.takeLast(60).forEach { put(JSONObject().put("date", it.date).put("heightCm", it.heightCm).put("weightKg", it.weightKg)) }
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, array.toString()).apply()
    }

    fun load(context: Context): List<BodyMetricEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val height = item.optDouble("heightCm", Double.NaN)
                val weight = item.optDouble("weightKg", Double.NaN)
                val date = item.optString("date").takeIf(String::isNotBlank)
                if (date != null && height.isFinite() && height in 80.0..260.0 && weight.isFinite() && weight in 20.0..400.0) {
                    add(BodyMetricEntry(date, height, weight))
                }
            }
        }
    }.getOrDefault(emptyList())
}
