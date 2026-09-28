package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A local equivalent of the iOS body-metric log.  Values are canonical metric
 * units so switching the unit preference never alters persisted data.
 */
internal data class BodyMetricEntry(
    val date: String,
    val heightCm: Double,
    val weightKg: Double,
    val leanBodyMassKg: Double? = null,
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null,
    val bodyFatPercentage: Double? = null,
)

internal object BodyMetricsStore {
    private const val PREFS = "wildforce_body_metrics"
    private const val KEY = "history"

    fun record(
        context: Context,
        profile: OnboardingProfile,
        composition: BodyCompositionMeasurements = ProfileDetailPreferencesStore.loadBodyComposition(context),
        leanBodyMassKg: Double? = null,
        bodyFatPercentage: Double? = null,
    ) {
        val entries = load(context).toMutableList()
        val today = java.time.LocalDate.now().toString()
        entries.removeAll { it.date == today }
        entries += BodyMetricEntry(
            date = today,
            heightCm = profile.heightCm.toDouble(),
            weightKg = profile.weightKg,
            leanBodyMassKg = leanBodyMassKg,
            waistCm = composition.waistCm,
            neckCm = composition.neckCm,
            hipCm = composition.hipCm,
            bodyFatPercentage = bodyFatPercentage,
        )
        val array = JSONArray().apply {
            entries.takeLast(60).forEach { entry ->
                put(JSONObject()
                    .put("date", entry.date)
                    .put("heightCm", entry.heightCm)
                    .put("weightKg", entry.weightKg)
                    .put("leanBodyMassKg", entry.leanBodyMassKg ?: JSONObject.NULL)
                    .put("waistCm", entry.waistCm ?: JSONObject.NULL)
                    .put("neckCm", entry.neckCm ?: JSONObject.NULL)
                    .put("hipCm", entry.hipCm ?: JSONObject.NULL)
                    .put("bodyFatPercentage", entry.bodyFatPercentage ?: JSONObject.NULL))
            }
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
                    add(BodyMetricEntry(
                        date = date,
                        heightCm = height,
                        weightKg = weight,
                        leanBodyMassKg = item.optionalMetric("leanBodyMassKg", 1.0..300.0),
                        waistCm = item.optionalMetric("waistCm", 20.0..300.0),
                        neckCm = item.optionalMetric("neckCm", 20.0..300.0),
                        hipCm = item.optionalMetric("hipCm", 20.0..300.0),
                        bodyFatPercentage = item.optionalMetric("bodyFatPercentage", 1.0..90.0),
                    ))
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun JSONObject.optionalMetric(key: String, range: ClosedFloatingPointRange<Double>): Double? =
    takeUnless { isNull(key) }?.optDouble(key)?.takeIf { it.isFinite() && it in range }
