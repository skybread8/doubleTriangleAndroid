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
    CheckIn("checkIn", "Check-in"),
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
    val chestCm: Double? = null,
    val thighCm: Double? = null,
    val armCm: Double? = null,
    val id: String? = null,
    val recordedAt: String? = null,
    val createdAt: String? = null,
    val checkInId: String? = null,
    val updatedAt: String? = null,
)

/** A dated Health Connect snapshot used by the manual full-history import. */
data class ImportedBodyMetric(
    val date: String,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val leanBodyMassKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val recordedAt: String? = null,
)

object BodyMetricsStore {
    private const val PREFS = "wildforce_body_metrics"
    private const val KEY = "history"
    const val PENDING_DELETIONS_KEY = "pending_metric_deletions_v1"

    @Synchronized
    internal fun record(
        context: Context,
        profile: OnboardingProfile,
        composition: BodyCompositionMeasurements = ProfileDetailPreferencesStore.loadBodyComposition(context),
        importedHeightCm: Double? = null,
        importedWeightKg: Double? = null,
        leanBodyMassKg: Double? = null,
        bodyFatPercentage: Double? = null,
        source: BodyMetricSource = BodyMetricSource.Manual,
        recordWeight: Boolean = true,
        chestCm: Double? = null,
        thighCm: Double? = null,
        armCm: Double? = null,
    ) {
        val now = java.time.Instant.now()
        val date = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val height = importedHeightCm ?: profile.heightCm.toDouble()
        val entries = load(context).toMutableList()
        fun add(entry: BodyMetricEntry) {
            entries += entry.copy(id = java.util.UUID.randomUUID().toString(), recordedAt = now.toString(), updatedAt = now.toString())
        }
        (importedWeightKg ?: profile.weightKg.takeIf { recordWeight })?.let { add(BodyMetricEntry(date, height, weightKg = it, source = source)) }
        leanBodyMassKg?.let { add(BodyMetricEntry(date, height, leanBodyMassKg = it, source = source)) }
        bodyFatPercentage?.let { add(BodyMetricEntry(date, height, bodyFatPercentage = it, source = source)) }
        composition.waistCm?.let { add(BodyMetricEntry(date, height, waistCm = it, source = source)) }
        composition.neckCm?.let { add(BodyMetricEntry(date, height, neckCm = it, source = source)) }
        composition.hipCm?.let { add(BodyMetricEntry(date, height, hipCm = it, source = source)) }
        chestCm?.let { add(BodyMetricEntry(date, height, chestCm = it, source = source)) }
        thighCm?.let { add(BodyMetricEntry(date, height, thighCm = it, source = source)) }
        armCm?.let { add(BodyMetricEntry(date, height, armCm = it, source = source)) }
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
            composition = BodyCompositionMeasurements(),
            recordWeight = weightKg != null,
        )
    }

    /** Merges every dated Health Connect snapshot without replacing local-only metrics. */
    @Synchronized
    fun recordImportedHistory(
        context: Context,
        profile: OnboardingProfile,
        imports: List<ImportedBodyMetric>,
    ) {
        if (imports.isEmpty()) return
        val existingEntries = load(context)
        val entries = existingEntries.toMutableList()
        imports.forEach { imported ->
            val date = imported.date.takeIf { runCatching { java.time.LocalDate.parse(it) }.isSuccess } ?: return@forEach
            val instant = imported.recordedAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
                ?: java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            val height = imported.heightCm ?: profile.heightCm.toDouble()
            fun add(value: Double?, kind: BodyMetricKind, make: (Double) -> BodyMetricEntry) {
                if (value == null || !value.isFinite()) return
                val alreadyStored = entries.any { existing -> matchesImportedMetric(existing, kind, value, instant) }
                if (!alreadyStored) entries += make(value).copy(id = java.util.UUID.randomUUID().toString(), recordedAt = instant.toString(), updatedAt = java.time.Instant.now().toString())
            }
            add(imported.weightKg, BodyMetricKind.Weight) { BodyMetricEntry(date, height, weightKg = it, source = BodyMetricSource.HealthConnect) }
            add(imported.leanBodyMassKg, BodyMetricKind.LeanBodyMass) { BodyMetricEntry(date, height, leanBodyMassKg = it, source = BodyMetricSource.HealthConnect) }
            add(imported.bodyFatPercentage, BodyMetricKind.BodyFat) { BodyMetricEntry(date, height, bodyFatPercentage = it, source = BodyMetricSource.HealthConnect) }
        }
        if (entries != existingEntries) save(context, entries)
    }

    @Synchronized
    internal fun deleteMetric(context: Context, target: BodyMetricEntry, kind: BodyMetricKind) {
        val existing = load(context)
        if (target !in existing || target.metricValue(kind) == null) return
        val userId = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("id", null)
        val type = when (kind) {
            BodyMetricKind.Weight -> "weight"
            BodyMetricKind.LeanBodyMass -> "leanBodyMass"
            BodyMetricKind.BodyFat -> "bodyFatPercentage"
            BodyMetricKind.Waist -> "waistCircumference"
            BodyMetricKind.Neck -> "neckCircumference"
            BodyMetricKind.Hip -> "hipCircumference"
            BodyMetricKind.Chest -> "chestCircumference"
            BodyMetricKind.Thigh -> "thighCircumference"
            BodyMetricKind.Arm -> "armCircumference"
        }
        val id = target.id ?: userId?.let {
            java.util.UUID.nameUUIDFromBytes("$it:$type:${target.date}".toByteArray()).toString()
        }
        if (id != null) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val pending = prefs.getString(PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
            pending.put(id.lowercase(), java.time.Instant.now().toString())
            prefs.edit().putString(PENDING_DELETIONS_KEY, pending.toString()).apply()
        }
        save(context, removeBodyMetric(existing, target, kind))
    }

    internal fun load(context: Context): List<BodyMetricEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        sortBodyMetricEntries(buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val height = item.optDouble("heightCm", Double.NaN)
                val weight = item.takeUnless { it.isNull("weightKg") }
                    ?.optDouble("weightKg", Double.NaN)?.takeIf { it.isFinite() && it > 0.0 }
                val date = item.optString("date").takeIf(String::isNotBlank)
                if (date != null && height.isFinite() && height in 80.0..260.0) {
                    add(BodyMetricEntry(
                        date = date,
                        heightCm = height,
                        weightKg = weight,
                        leanBodyMassKg = item.optionalMetric("leanBodyMassKg", Double.MIN_VALUE..Double.MAX_VALUE),
                        waistCm = item.optionalMetric("waistCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        neckCm = item.optionalMetric("neckCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        hipCm = item.optionalMetric("hipCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        bodyFatPercentage = item.optionalMetric("bodyFatPercentage", Double.MIN_VALUE..Double.MAX_VALUE),
                        source = BodyMetricSource.fromStoredValue(item.optString("source", "")),
                        chestCm = item.optionalMetric("chestCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        thighCm = item.optionalMetric("thighCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        armCm = item.optionalMetric("armCm", Double.MIN_VALUE..Double.MAX_VALUE),
                        id = item.optString("id").takeIf { it.isNotBlank() && it != "null" },
                        recordedAt = item.optString("recordedAt").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = item.optString("createdAt").takeIf { it.isNotBlank() && it != "null" },
                        checkInId = item.optString("checkInId").takeIf { it.isNotBlank() && it != "null" },
                        updatedAt = item.optString("updatedAt").takeIf { it.isNotBlank() && it != "null" },
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
                    .put("source", entry.source.storedValue)
                    .put("chestCm", entry.chestCm ?: JSONObject.NULL)
                    .put("thighCm", entry.thighCm ?: JSONObject.NULL)
                    .put("armCm", entry.armCm ?: JSONObject.NULL)
                    .put("id", entry.id ?: JSONObject.NULL)
                    .put("recordedAt", entry.recordedAt ?: JSONObject.NULL)
                    .put("createdAt", entry.createdAt ?: JSONObject.NULL)
                    .put("checkInId", entry.checkInId ?: JSONObject.NULL)
                    .put("updatedAt", entry.updatedAt ?: JSONObject.NULL))
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
    entries.sortedWith(compareBy<BodyMetricEntry> { it.instantOrNull() ?: java.time.Instant.EPOCH }.thenBy { it.date })

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
    recordWeight: Boolean = true,
    chestCm: Double? = null,
    thighCm: Double? = null,
    armCm: Double? = null,
): BodyMetricEntry = BodyMetricEntry(
    date = date,
    heightCm = importedHeightCm ?: profile.heightCm.toDouble(),
    weightKg = importedWeightKg ?: if (recordWeight) profile.weightKg else existing?.weightKg,
    leanBodyMassKg = leanBodyMassKg ?: existing?.leanBodyMassKg,
    waistCm = composition.waistCm ?: existing?.waistCm,
    neckCm = composition.neckCm ?: existing?.neckCm,
    hipCm = composition.hipCm ?: existing?.hipCm,
    bodyFatPercentage = bodyFatPercentage ?: existing?.bodyFatPercentage,
    source = source,
    chestCm = chestCm ?: existing?.chestCm,
    thighCm = thighCm ?: existing?.thighCm,
    armCm = armCm ?: existing?.armCm,
)

private fun JSONObject.optionalMetric(key: String, range: ClosedFloatingPointRange<Double>): Double? =
    takeUnless { isNull(key) }?.optDouble(key)?.takeIf { it.isFinite() && it in range }

/** The newest weight, independent of later entries for other metrics. */
internal fun latestRecordedWeight(entries: List<BodyMetricEntry>): Double? =
    entries.asSequence()
        .filter { it.weightKg?.let { weight -> weight.isFinite() && weight > 0.0 } == true }
        .let { sortBodyMetricEntries(it.toList()).lastOrNull() }
        ?.weightKg

internal fun BodyMetricEntry.instantOrNull(): java.time.Instant? =
    recordedAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        ?: runCatching { java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull()

internal fun BodyMetricEntry.metricValue(kind: BodyMetricKind): Double? = when (kind) {
    BodyMetricKind.Weight -> weightKg
    BodyMetricKind.BodyFat -> bodyFatPercentage
    BodyMetricKind.LeanBodyMass -> leanBodyMassKg
    BodyMetricKind.Waist -> waistCm
    BodyMetricKind.Neck -> neckCm
    BodyMetricKind.Hip -> hipCm
    BodyMetricKind.Chest -> chestCm
    BodyMetricKind.Thigh -> thighCm
    BodyMetricKind.Arm -> armCm
}

internal fun removeBodyMetric(entries: List<BodyMetricEntry>, target: BodyMetricEntry, kind: BodyMetricKind): List<BodyMetricEntry> =
    entries.mapNotNull { entry ->
            if (entry != target) return@mapNotNull entry
            val cleared = when (kind) {
                BodyMetricKind.Weight -> entry.copy(weightKg = null)
                BodyMetricKind.LeanBodyMass -> entry.copy(leanBodyMassKg = null)
                BodyMetricKind.Waist -> entry.copy(waistCm = null)
                BodyMetricKind.Neck -> entry.copy(neckCm = null)
                BodyMetricKind.Hip -> entry.copy(hipCm = null)
                BodyMetricKind.BodyFat -> entry.copy(bodyFatPercentage = null)
                BodyMetricKind.Chest -> entry.copy(chestCm = null)
                BodyMetricKind.Thigh -> entry.copy(thighCm = null)
                BodyMetricKind.Arm -> entry.copy(armCm = null)
            }
            cleared.takeIf {
                it.weightKg != null || it.leanBodyMassKg != null || it.waistCm != null ||
                    it.neckCm != null || it.hipCm != null || it.bodyFatPercentage != null ||
                    it.chestCm != null || it.thighCm != null || it.armCm != null
            }
        }

internal fun matchesImportedMetric(entry: BodyMetricEntry, kind: BodyMetricKind, value: Double, instant: java.time.Instant): Boolean {
    val previous = entry.metricValue(kind) ?: return false
    val recorded = entry.instantOrNull() ?: return false
    return kotlin.math.abs(previous - value) < .001 &&
        kotlin.math.abs(java.time.Duration.between(recorded, instant).toMillis()) < 60_000
}
