package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import org.json.JSONArray
import org.json.JSONObject

/** The origin shown beside a body-metric row, matching iOS's BodyMetricSource. */
internal enum class BodyMetricSource(val storedValue: String, val displayName: String) {
    Manual("manual", "Manual"),
    HealthConnect("healthConnect", "Health Connect"),
    AppleHealth("appleHealth", "Apple Health"),
    ;

    companion object {
        fun fromStoredValue(value: String?): BodyMetricSource = entries.firstOrNull {
            it.storedValue.equals(value, ignoreCase = true)
        } ?: Manual
    }
}

/**
 * A local equivalent of the iOS body-metric log.  Values are canonical metric
 * units so switching the unit preference never alters persisted data.
 */
internal data class BodyMetricEntry(
    val date: String,
    val heightCm: Double,
    /** iOS records every metric independently, so a circumference-only day is valid. */
    val weightKg: Double? = null,
    val leanBodyMassKg: Double? = null,
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null,
    val bodyFatPercentage: Double? = null,
    val source: BodyMetricSource = BodyMetricSource.Manual,
)

/** A dated Health Connect snapshot used by the manual full-history import. */
data class ImportedBodyMetric(
    val date: String,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val leanBodyMassKg: Double? = null,
    val bodyFatPercentage: Double? = null,
)

object BodyMetricsStore {
    private const val PREFS = "wildforce_body_metrics"
    private const val KEY = "history"

    internal fun record(
        context: Context,
        profile: OnboardingProfile,
        composition: BodyCompositionMeasurements = ProfileDetailPreferencesStore.loadBodyComposition(context),
        importedHeightCm: Double? = null,
        importedWeightKg: Double? = null,
        leanBodyMassKg: Double? = null,
        bodyFatPercentage: Double? = null,
        source: BodyMetricSource = BodyMetricSource.Manual,
    ) {
        val storedEntries = load(context)
        val entries = storedEntries.toMutableList()
        val today = java.time.LocalDate.now().toString()
        entries.removeAll { it.date == today }
        entries += mergeBodyMetricEntry(
            existing = storedEntries.firstOrNull { it.date == today },
            date = today,
            profile = profile,
            composition = composition,
            importedHeightCm = importedHeightCm,
            importedWeightKg = importedWeightKg,
            leanBodyMassKg = leanBodyMassKg,
            bodyFatPercentage = bodyFatPercentage,
            source = source,
        )
        save(context, entries)
    }

    /** Persists measurements imported during onboarding without exposing the internal composition model. */
    fun recordImported(
        context: Context,
        profile: OnboardingProfile,
        heightCm: Double? = null,
        weightKg: Double? = null,
        leanBodyMassKg: Double? = null,
        bodyFatPercentage: Double? = null,
    ) {
        record(
            context = context,
            profile = profile,
            importedHeightCm = heightCm,
            importedWeightKg = weightKg,
            leanBodyMassKg = leanBodyMassKg,
            bodyFatPercentage = bodyFatPercentage,
            source = BodyMetricSource.HealthConnect,
        )
    }

    /** Merges every dated Health Connect snapshot without replacing local-only metrics. */
    fun recordImportedHistory(
        context: Context,
        profile: OnboardingProfile,
        imports: List<ImportedBodyMetric>,
    ) {
        if (imports.isEmpty()) return
        val storedEntries = load(context).toMutableList()
        imports.forEach { imported ->
            val date = imported.date.takeIf { runCatching { java.time.LocalDate.parse(it) }.isSuccess } ?: return@forEach
            val existing = storedEntries.firstOrNull { it.date == date }
            storedEntries.removeAll { it.date == date }
            storedEntries += BodyMetricEntry(
                date = date,
                heightCm = imported.heightCm ?: existing?.heightCm ?: profile.heightCm.toDouble(),
                weightKg = imported.weightKg ?: existing?.weightKg,
                leanBodyMassKg = imported.leanBodyMassKg ?: existing?.leanBodyMassKg,
                waistCm = existing?.waistCm,
                neckCm = existing?.neckCm,
                hipCm = existing?.hipCm,
                bodyFatPercentage = imported.bodyFatPercentage ?: existing?.bodyFatPercentage,
                source = BodyMetricSource.HealthConnect,
            )
        }
        save(context, storedEntries)
    }

    internal fun deleteMetric(context: Context, date: String, kind: BodyMetricKind) {
        val updated = load(context).mapNotNull { entry ->
            if (entry.date != date) return@mapNotNull entry
            val cleared = when (kind) {
                BodyMetricKind.Weight -> entry.copy(weightKg = null)
                BodyMetricKind.LeanBodyMass -> entry.copy(leanBodyMassKg = null)
                BodyMetricKind.Waist -> entry.copy(waistCm = null)
                BodyMetricKind.Neck -> entry.copy(neckCm = null)
                BodyMetricKind.Hip -> entry.copy(hipCm = null)
                BodyMetricKind.BodyFat -> entry.copy(bodyFatPercentage = null)
            }
            cleared.takeIf {
                it.weightKg != null || it.leanBodyMassKg != null || it.waistCm != null ||
                    it.neckCm != null || it.hipCm != null || it.bodyFatPercentage != null
            }
        }
        save(context, updated)
    }

    internal fun load(context: Context): List<BodyMetricEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        sortBodyMetricEntries(buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val height = item.optDouble("heightCm", Double.NaN)
                val weight = item.takeUnless { it.isNull("weightKg") }
                    ?.optDouble("weightKg", Double.NaN)?.takeIf { it.isFinite() && it in 20.0..400.0 }
                val date = item.optString("date").takeIf(String::isNotBlank)
                if (date != null && height.isFinite() && height in 80.0..260.0) {
                    add(BodyMetricEntry(
                        date = date,
                        heightCm = height,
                        weightKg = weight,
                        leanBodyMassKg = item.optionalMetric("leanBodyMassKg", 1.0..300.0),
                        waistCm = item.optionalMetric("waistCm", 20.0..300.0),
                        neckCm = item.optionalMetric("neckCm", 20.0..300.0),
                        hipCm = item.optionalMetric("hipCm", 20.0..300.0),
                        bodyFatPercentage = item.optionalMetric("bodyFatPercentage", 1.0..90.0),
                        source = BodyMetricSource.fromStoredValue(item.optString("source", "")),
                    ))
                }
            }
        })
    }.getOrDefault(emptyList())

    private fun save(context: Context, entries: List<BodyMetricEntry>) {
        val array = JSONArray().apply {
            persistedBodyMetricEntries(entries).forEach { entry ->
                put(JSONObject()
                    .put("date", entry.date)
                    .put("heightCm", entry.heightCm)
                    .put("weightKg", entry.weightKg ?: JSONObject.NULL)
                    .put("leanBodyMassKg", entry.leanBodyMassKg ?: JSONObject.NULL)
                    .put("waistCm", entry.waistCm ?: JSONObject.NULL)
                    .put("neckCm", entry.neckCm ?: JSONObject.NULL)
                    .put("hipCm", entry.hipCm ?: JSONObject.NULL)
                    .put("bodyFatPercentage", entry.bodyFatPercentage ?: JSONObject.NULL)
                    .put("source", entry.source.storedValue))
            }
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, array.toString()).apply()
        RemoteSyncChangeTracker.mark(context, "body-metric-entries")
    }
}

/**
 * iOS's BodyMetricsRepository sorts the history by recorded date before it
 * derives the latest value or renders the chart. Remote sync can arrive in a
 * different order than local writes, so never expose JSON order to callers.
 */
internal fun sortBodyMetricEntries(entries: List<BodyMetricEntry>): List<BodyMetricEntry> =
    entries.sortedBy { it.date }

/** iOS keeps the complete body-metric history; aggregation is a presentation concern. */
internal fun persistedBodyMetricEntries(entries: List<BodyMetricEntry>): List<BodyMetricEntry> =
    sortBodyMetricEntries(entries)

internal fun mergeBodyMetricEntry(
    existing: BodyMetricEntry?,
    date: String,
    profile: OnboardingProfile,
    composition: BodyCompositionMeasurements,
    importedHeightCm: Double? = null,
    importedWeightKg: Double? = null,
    leanBodyMassKg: Double? = null,
    bodyFatPercentage: Double? = null,
    source: BodyMetricSource = BodyMetricSource.Manual,
): BodyMetricEntry = BodyMetricEntry(
    date = date,
    heightCm = importedHeightCm ?: profile.heightCm.toDouble(),
    weightKg = importedWeightKg ?: profile.weightKg,
    leanBodyMassKg = leanBodyMassKg ?: existing?.leanBodyMassKg,
    waistCm = composition.waistCm ?: existing?.waistCm,
    neckCm = composition.neckCm ?: existing?.neckCm,
    hipCm = composition.hipCm ?: existing?.hipCm,
    bodyFatPercentage = bodyFatPercentage ?: existing?.bodyFatPercentage,
    source = source,
)

private fun JSONObject.optionalMetric(key: String, range: ClosedFloatingPointRange<Double>): Double? =
    takeUnless { isNull(key) }?.optDouble(key)?.takeIf { it.isFinite() && it in range }
