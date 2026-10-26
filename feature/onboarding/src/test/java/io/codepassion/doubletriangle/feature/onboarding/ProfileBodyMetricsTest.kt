package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ProfileBodyMetricsTest {
    @Test
    fun waistToHeightZonesUseIosBoundaries() {
        assertEquals("Delgado", waistToHeightZone(.399))
        assertEquals("Saludable", waistToHeightZone(.40))
        assertEquals("Elevado", waistToHeightZone(.50))
        assertEquals("Alto", waistToHeightZone(.60))
    }

    @Test
    fun navyBodyFatConvertsMetricInputsToInchesLikeIos() {
        val estimate = navyBodyFatEstimate(
            gender = Gender.Male,
            heightCm = 180.0,
            waistCm = 90.0,
            neckCm = 40.0,
            hipCm = null,
        )

        assertEquals(12.0, estimate ?: error("Expected a valid estimate"), 0.2)
    }

    @Test
    fun ageUsesTheBirthMonthWhenCalculatingProfileMetrics() {
        assertEquals(29, ageInYears(1995, 11, LocalDate.of(2025, 10, 20)))
        assertEquals(30, ageInYears(1995, 1, LocalDate.of(2025, 10, 20)))
    }

    @Test
    fun legacyDateEditingClampsToTheActualMonthLength() {
        assertEquals(28, daysInBirthMonth(2025, 2))
        assertEquals(29, daysInBirthMonth(2024, 2))
    }

    @Test
    fun bodyMetricChartUsesWeeklyAveragesForTwelveWeeks() {
        val entries = listOf(
            BodyMetricEntry("2025-10-20", 180.0, weightKg = 80.0),
            BodyMetricEntry("2025-10-21", 180.0, weightKg = 78.0),
            BodyMetricEntry("2025-10-27", 180.0, weightKg = 76.0),
        )

        val points = bodyMetricChartPoints(entries, BodyMetricKind.Weight, 84)

        assertEquals(listOf("10-20", "10-27"), points.map { it.dateLabel })
        assertEquals(listOf(79.0, 76.0), points.map { it.value })
    }

    @Test
    fun bodyMetricChartUsesMonthlyAveragesForAllTime() {
        val entries = listOf(
            BodyMetricEntry("2025-10-02", 180.0, weightKg = 80.0),
            BodyMetricEntry("2025-10-20", 180.0, weightKg = 78.0),
            BodyMetricEntry("2025-11-04", 180.0, weightKg = 76.0),
        )

        val points = bodyMetricChartPoints(entries, BodyMetricKind.Weight, Int.MAX_VALUE)

        assertEquals(listOf("10-01", "11-01"), points.map { it.dateLabel })
        assertEquals(listOf(79.0, 76.0), points.map { it.value })
    }

    @Test
    fun bodyMetricChartExposesIosStyleAxisMarksForEachTimeframe() {
        val points = bodyMetricChartPoints(
            (0..29).map { offset ->
                BodyMetricEntry(
                    date = LocalDate.of(2025, 10, 1).plusDays(offset.toLong()).toString(),
                    heightCm = 180.0,
                    weightKg = 80.0,
                )
            },
            BodyMetricKind.Weight,
            30,
        )

        val indices = bodyMetricAxisIndices(points, 30)

        assertEquals(6, indices.size)
        assertEquals("1 Oct", bodyMetricAxisLabel(points[indices.first()].date, 30, Locale.US))
        assertEquals("30 Oct", bodyMetricAxisLabel(points[indices.last()].date, 30, Locale.US))
    }

    @Test
    fun bodyMetricHistoryDateUsesTheLocalizedAbbreviatedStyle() {
        assertEquals("23 oct 2026", bodyMetricHistoryDateLabel("2026-10-23", Locale.forLanguageTag("es")))
        assertEquals("Oct 23, 2026", bodyMetricHistoryDateLabel("2026-10-23", Locale.US))
        assertEquals("not-a-date", bodyMetricHistoryDateLabel("not-a-date", Locale.US))
    }

    @Test
    fun bodyMetricHistoryUsesRecordedDateInsteadOfSyncOrder() {
        val entries = listOf(
            BodyMetricEntry("2025-11-04", 180.0, weightKg = 76.0),
            BodyMetricEntry("2025-10-02", 180.0, weightKg = 80.0),
            BodyMetricEntry("2025-10-20", 180.0, weightKg = 78.0),
        )

        assertEquals(
            listOf("2025-10-02", "2025-10-20", "2025-11-04"),
            sortBodyMetricEntries(entries).map { it.date },
        )
    }

    @Test
    fun bodyMetricPersistenceKeepsHistoryOlderThanSixtyEntries() {
        val entries = (0..60).map { offset ->
            BodyMetricEntry(
                date = LocalDate.of(2025, 1, 1).plusDays(offset.toLong()).toString(),
                heightCm = 180.0,
                weightKg = 80.0 - offset,
            )
        }.reversed()

        val persisted = persistedBodyMetricEntries(entries)

        assertEquals(61, persisted.size)
        assertEquals("2025-01-01", persisted.first().date)
        assertEquals("2025-03-02", persisted.last().date)
    }

    @Test
    fun bodyMetricSourceMatchesIosAndKeepsLegacyRowsManual() {
        assertEquals(BodyMetricSource.Manual, BodyMetricSource.fromStoredValue(null))
        assertEquals(BodyMetricSource.Manual, BodyMetricSource.fromStoredValue("unknown"))
        assertEquals(BodyMetricSource.HealthConnect, BodyMetricSource.fromStoredValue("healthConnect"))
        assertEquals(BodyMetricSource.AppleHealth, BodyMetricSource.fromStoredValue("appleHealth"))

        val imported = mergeBodyMetricEntry(
            existing = null,
            date = "2025-10-20",
            profile = OnboardingProfile(name = "Test", goal = FitnessGoal.LoseWeight, heightCm = 180, weightKg = 80.0),
            composition = BodyCompositionMeasurements(),
            importedWeightKg = 78.5,
            source = BodyMetricSource.HealthConnect,
        )

        assertEquals(BodyMetricSource.HealthConnect, imported.source)
        assertEquals("Health Connect", imported.source.displayName)
    }

    @Test
    fun savingAnotherMetricPreservesTheOtherSameDayMetrics() {
        val existing = BodyMetricEntry(
            date = "2025-10-20",
            heightCm = 180.0,
            weightKg = 80.0,
            leanBodyMassKg = 64.0,
            bodyFatPercentage = 20.0,
        )

        val merged = mergeBodyMetricEntry(
            existing = existing,
            date = "2025-10-20",
            profile = OnboardingProfile(name = "Test", goal = FitnessGoal.LoseWeight, heightCm = 180, weightKg = 80.0),
            composition = BodyCompositionMeasurements(),
            bodyFatPercentage = 19.0,
        )

        assertEquals(64.0, merged.leanBodyMassKg ?: error("Expected lean mass"), 0.0)
        assertEquals(19.0, merged.bodyFatPercentage ?: error("Expected body fat"), 0.0)
    }

    @Test
    fun importedHealthMetricsPreferTheImportedHeightAndWeight() {
        val merged = mergeBodyMetricEntry(
            existing = null,
            date = "2025-10-20",
            profile = OnboardingProfile(name = "Test", goal = FitnessGoal.LoseWeight, heightCm = 180, weightKg = 80.0),
            composition = BodyCompositionMeasurements(),
            importedHeightCm = 182.0,
            importedWeightKg = 78.5,
            leanBodyMassKg = 63.0,
            bodyFatPercentage = 19.0,
        )

        assertEquals(182.0, merged.heightCm, 0.0)
        assertEquals(78.5, merged.weightKg ?: error("Expected imported weight"), 0.0)
        assertEquals(63.0, merged.leanBodyMassKg ?: error("Expected imported lean mass"), 0.0)
    }

    @Test
    fun bodyFatLoggingAcceptsTypicalPercentagesAndRejectsInvalidValues() {
        assertEquals(
            15.0,
            canonicalBodyMetric(BodyMetricKind.BodyFat, 15.0, MetricSystem.Metric)
                ?: error("Expected a typical body-fat percentage to be accepted"),
            0.0,
        )
        assertEquals(null, canonicalBodyMetric(BodyMetricKind.BodyFat, 0.0, MetricSystem.Metric))
        assertEquals(null, canonicalBodyMetric(BodyMetricKind.BodyFat, 91.0, MetricSystem.Metric))
    }
}
