package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsPoint
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsSummary
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId

class AnalyticsScreenTest {
    @Test
    fun longestStreakCountsConsecutiveUniqueTrainingDays() {
        fun session(daysAgo: Long) = TrainingSessionHistoryEntry(
            timestampMillis = LocalDate.now().minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = 1_800,
            sets = 12,
            volumeKg = 1_000.0,
        )

        assertEquals(3, longestStreak(listOf(session(5), session(4), session(4), session(3), session(0))))
    }

    @Test
    fun analyticsKeepsPersistedLongestStreakBeyondLocalHistory() {
        assertEquals(12, longestStreakForAnalytics(emptyList(), 12))
        assertEquals(3, longestStreakForAnalytics(emptyList(), 3))
    }

    @Test
    fun previousAnalyticsVolumeUsesTheEquivalentPriorPeriod() {
        fun session(date: String, volume: Double) = TrainingSessionHistoryEntry(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = 1_800,
            sets = 12,
            volumeKg = volume,
        )

        assertEquals(
            100.0,
            previousAnalyticsVolume(
                listOf(session("2026-10-01", 75.0), session("2026-10-10", 100.0)),
                AnalyticsTimeframe.SevenDays,
                LocalDate.of(2026, 10, 20),
            )!!,
            0.0,
        )
    }

    @Test
    fun muscleGroupsMatchEnglishAndSpanishExerciseNames() {
        assertEquals("Piernas", muscleGroup("barbellBackSquat", "Sentadilla"))
        assertEquals("Espalda", muscleGroup("bentOverRow", "Remo con barra"))
        assertEquals("Pecho", muscleGroup("benchPress", "Press de banca"))
        assertEquals("Core", muscleGroup("plank", "Plancha"))
    }

    @Test
    fun timeFormattingMatchesAnalyticsLabels() {
        assertEquals("45 min", formatMinutes(45))
        assertEquals("1 h", formatMinutes(60))
        assertEquals("1 h 15 min", formatMinutes(75))
        assertEquals("45 sec", formatDurationSeconds(45))
        assertEquals("1 min", formatDurationSeconds(60))
        assertEquals("1:05", formatDurationSeconds(65))
    }

    @Test
    fun exerciseSessionSummaryUsesTypedDurationAndDistanceMetrics() {
        val timed = ExerciseAnalyticsPoint(
            timestampMillis = 1L,
            sets = 1,
            reps = 0,
            maxWeightKg = 0.0,
            volumeKg = 0.0,
            feedback = null,
            durationSeconds = 65,
            distanceKm = 2.5,
        )
        val exercise = ExerciseSummary("Bicicleta", "bicycleCrunch", 1, "", 0, targetDistanceKm = 2.5)

        assertEquals("1:05 · 2,50 km", analyticsSessionSummary(timed, exercise, useImperial = false))
        assertTrue(!analyticsSessionSummary(timed, exercise, useImperial = false).contains("repeticiones"))
    }

    @Test
    fun exerciseAnalyticsUsesTheIosCatalogCategoryInsteadOfOnlyTrackingMode() {
        assertEquals("Peso corporal", analyticsExerciseCategory(ExerciseSummary("Bicycle Crunch", "bicycleCrunch", 3, "12", 30)))
        assertEquals("Cardio", analyticsExerciseCategory(ExerciseSummary("Walking", "walking", 1, "", 0, targetDurationMinutes = 20, targetDistanceKm = 2.0)))
        assertEquals("Movilidad", analyticsExerciseCategory(ExerciseSummary("Cat-Cow", "catCow", 1, "", 0, targetDurationMinutes = 5)))
        assertEquals("HIIT", analyticsExerciseCategory(ExerciseSummary("Burpee", "burpee", 1, "", 0, targetDurationSeconds = 30)))
        assertEquals("Fuerza", analyticsExerciseCategory(ExerciseSummary("Bench Press", "benchPress", 3, "8", 90)))
    }

    @Test
    fun timeframeLabelsAndLookbacksMatchIosAnalytics() {
        assertEquals(listOf("7D", "2W", "30D", "12W", "All"), AnalyticsTimeframe.entries.map { it.label })
        assertEquals(listOf(7, 14, 30, 84, null), AnalyticsTimeframe.entries.map { it.days })
        assertEquals(listOf(true, true, true, true, false), AnalyticsTimeframe.entries.map { it.comparesPlan })
    }

    @Test
    fun consistencyPercentageRoundsLikeIos() {
        assertEquals(67, analyticsCompletionPercentage(2, 3))
        assertEquals(33, analyticsCompletionPercentage(1, 3))
        assertEquals(0, analyticsCompletionPercentage(2, 0))
    }

    @Test
    fun focusDistributionUsesOnlySessionsAndMatchesIosOrdering() {
        val distribution = analyticsFocusDistribution(
            listOf(
                TrainingSessionHistoryEntry(3, 1, 1, 1.0, "lowerBody"),
                TrainingSessionHistoryEntry(2, 1, 1, 1.0, "fullBody"),
                TrainingSessionHistoryEntry(1, 1, 1, 1.0, "lowerBody"),
                TrainingSessionHistoryEntry(0, 1, 1, 1.0, null),
            ),
        )

        assertEquals(listOf("lowerBody" to 2, "fullBody" to 1), distribution)
    }

    @Test
    fun twelveWeekAnalyticsStartsOnTheCurrentWeekStartLikeIos() {
        assertEquals(
            LocalDate.of(2026, 8, 3),
            analyticsStartDate(AnalyticsTimeframe.TwelveWeeks, LocalDate.of(2026, 10, 20)),
        )
    }

    @Test
    fun dashboardExercisesKeepTheSelectedTimeframeButDetailKeepsFullHistoryLikeIos() {
        fun point(date: String, volume: Double) = ExerciseAnalyticsPoint(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sets = 1,
            reps = 10,
            maxWeightKg = 10.0,
            volumeKg = volume,
            feedback = null,
        )

        val history = listOf(
            point("2026-10-20", 20.0),
            point("2026-10-01", 100.0),
        )

        assertEquals(
            listOf(20.0),
            filteredAnalyticsHistory(history, AnalyticsTimeframe.SevenDays, LocalDate.of(2026, 10, 20)).map { it.volumeKg },
        )
        assertEquals(
            listOf(20.0, 100.0),
            filteredAnalyticsHistory(history, AnalyticsTimeframe.AllTime, LocalDate.of(2026, 10, 20)).map { it.volumeKg },
        )

        val summary = ExerciseAnalyticsSummary(
            exercise = ExerciseSummary("Press banca", "benchPress", 1, "libre", 0),
            sessions = 1,
            personalBestKg = 20.0,
            totalVolumeKg = 20.0,
            lastFeedback = null,
        )
        assertEquals(
            listOf(20.0, 100.0),
            exerciseHistoryForDetail(mapOf(summary to history), summary.exercise).map { it.volumeKg },
        )
    }

    @Test
    fun muscleDistributionUsesIosMajorGroupsAndUniqueCompletionDays() {
        fun point(date: String) = ExerciseAnalyticsPoint(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sets = 1,
            reps = 8,
            maxWeightKg = 20.0,
            volumeKg = 100.0,
            feedback = null,
        )

        val summary = ExerciseAnalyticsSummary(
            exercise = ExerciseSummary("Press banca", "benchPress", 1, "libre", 0),
            sessions = 3,
            personalBestKg = 20.0,
            totalVolumeKg = 300.0,
            lastFeedback = null,
        )
        val distribution = muscleDistribution(
            mapOf(summary to listOf(point("2026-10-01"), point("2026-10-01"), point("2026-10-03"))),
        )

        assertEquals(2.0, distribution.first { it.name == "Pecho" }.score, 0.0)
        assertEquals(0.6, distribution.first { it.name == "Brazos" }.score, 0.0)
        assertEquals(0.6, distribution.first { it.name == "Hombros" }.score, 0.0)
    }

    @Test
    fun muscleFatigueUsesIosStressFeedbackAndSecondaryWeight() {
        val point = ExerciseAnalyticsPoint(
            timestampMillis = System.currentTimeMillis(),
            sets = 1,
            reps = 8,
            maxWeightKg = 20.0,
            volumeKg = 100.0,
            feedback = "MUY DIFÍCIL",
        )
        val summary = ExerciseAnalyticsSummary(
            exercise = ExerciseSummary("Press banca", "benchPress", 1, "libre", 0),
            sessions = 1,
            personalBestKg = 20.0,
            totalVolumeKg = 100.0,
            lastFeedback = point.feedback,
        )

        val fatigue = muscleFatigue(mapOf(summary to listOf(point)))
        val chest = fatigue.first { it.name == "Pecho" }.score
        val secondary = fatigue.first { it.name == "Brazos" }.score

        assertEquals(8.30, chest, 0.05)
        assertEquals(2.57, secondary, 0.05)
        assertTrue(chest > secondary)
    }

    @Test
    fun analyticsEmptyStateUsesAllTimeHistoryLikeIos() {
        val emptyPeriod = AnalyticsData(
            sessions = emptyList(),
            exercises = emptyList(),
            histories = emptyMap(),
        )
        val populatedPeriod = emptyPeriod.copy(
            sessions = listOf(TrainingSessionHistoryEntry(System.currentTimeMillis(), 1_800, 1, 10.0)),
        )

        assertEquals(true, analyticsDataIsEmpty(emptyPeriod))
        assertEquals(false, analyticsDataIsEmpty(populatedPeriod))
        assertEquals(
            false,
            analyticsDataIsEmpty(
                data = emptyPeriod,
                allTimeCompletedWorkoutCount = 1,
                allTimeUniqueExercisesCompleted = 1,
            ),
        )
    }

    @Test
    fun allTimeVolumeUsesWeeklyWorkoutBucketsLikeIos() {
        fun session(date: String, volume: Double) = TrainingSessionHistoryEntry(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = 1_800,
            sets = 12,
            volumeKg = volume,
        )

        val buckets = volumeBuckets(
            AnalyticsData(
                sessions = listOf(session("2026-10-13", 100.0), session("2026-10-14", 50.0), session("2026-10-20", 75.0)),
                exercises = emptyList(),
                histories = emptyMap(),
            ),
            AnalyticsTimeframe.AllTime,
            today = LocalDate.of(2026, 10, 22),
        )

        assertEquals(2, buckets.size)
        assertEquals(150.0, buckets[0].second, 0.0)
        assertEquals(75.0, buckets[1].second, 0.0)
    }

    @Test
    fun allTimeVolumeKeepsEmptyWeeksBetweenTheFirstAndCurrentWeekLikeIos() {
        fun session(date: String, volume: Double) = TrainingSessionHistoryEntry(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = 1_800,
            sets = 12,
            volumeKg = volume,
        )

        val buckets = volumeBuckets(
            AnalyticsData(
                sessions = listOf(session("2026-10-06", 100.0), session("2026-10-20", 75.0)),
                exercises = emptyList(),
                histories = emptyMap(),
            ),
            AnalyticsTimeframe.AllTime,
            today = LocalDate.of(2026, 10, 22),
        )

        assertEquals(3, buckets.size)
        assertEquals(listOf(100.0, 0.0, 75.0), buckets.map { it.second })
    }

    @Test
    fun thirtyDayVolumeUsesOneDailyBucketPerDayLikeIos() {
        val buckets = volumeBuckets(
            AnalyticsData(
                sessions = listOf(
                    TrainingSessionHistoryEntry(
                        timestampMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        durationSeconds = 1_800,
                        sets = 12,
                        volumeKg = 100.0,
                    ),
                ),
                exercises = emptyList(),
                histories = emptyMap(),
            ),
            AnalyticsTimeframe.ThirtyDays,
        )

        assertEquals(30, buckets.size)
        assertEquals(100.0, buckets.last().second, 0.0)
    }

    @Test
    fun volumeChartUsesIosAxisDensityWithoutDroppingDailyBars() {
        val thirtyDayLabels = (0 until 30).map { "${it + 1} OCT" }
        assertEquals(
            setOf(0, 7, 14, 21, 28),
            volumeChartLabelIndices(thirtyDayLabels, AnalyticsTimeframe.ThirtyDays),
        )

        val twelveWeekLabels = listOf(
            "03 AGO", "10 AGO", "17 AGO", "24 AGO", "31 AGO",
            "07 SEP", "14 SEP", "21 SEP", "28 SEP", "05 OCT", "12 OCT", "19 OCT",
        )
        assertEquals(
            setOf(0, 5, 9),
            volumeChartLabelIndices(twelveWeekLabels, AnalyticsTimeframe.TwelveWeeks),
        )
    }

    @Test
    fun averageVolumeIgnoresWorkoutsWithoutVolumeLikeIos() {
        fun session(volume: Double) = TrainingSessionHistoryEntry(
            timestampMillis = System.currentTimeMillis(),
            durationSeconds = 1_800,
            sets = 12,
            volumeKg = volume,
        )

        assertEquals(150.0, averageVolumePerWorkout(listOf(session(100.0), session(0.0), session(200.0)))!!, 0.0)
        assertEquals(null, averageVolumePerWorkout(listOf(session(0.0))))
    }

    @Test
    fun volumeTrendAverageIgnoresRestBucketsLikeIos() {
        assertEquals(150.0, averageActiveVolume(listOf(0.0, 100.0, 0.0, 200.0))!!, 0.0)
        assertEquals(null, averageActiveVolume(listOf(0.0, 0.0)))
    }

    @Test
    fun durationMetricsRoundEachWorkoutLikeIos() {
        fun session(seconds: Int) = TrainingSessionHistoryEntry(System.currentTimeMillis(), seconds, 0, 0.0)

        val sessions = listOf(session(2_730), session(2_790)) // 45.5 and 46.5 minutes
        assertEquals(93, totalActiveMinutes(sessions))
        assertEquals(47, averageActiveMinutes(sessions))
    }

    @Test
    fun timedAndDistanceSessionsExposeCategoryPerformanceMetrics() {
        val metrics = exercisePerformanceMetrics(
            listOf(
                ExerciseAnalyticsPoint(1, 1, 0, 0.0, 0.0, null, durationSeconds = 600, distanceKm = 2.5),
                ExerciseAnalyticsPoint(2, 1, 0, 0.0, 0.0, null, durationSeconds = 900, distanceKm = 3.75),
            ),
        )

        assertEquals(listOf("Tiempo total", "Mejor duración", "Distancia total", "Mayor distancia"), metrics.map { it.title })
        assertEquals("25 min", metrics[0].value)
        assertEquals("6,25 km", metrics[2].value)
    }

    @Test
    fun exerciseVolumeChartAggregatesSessionsOnTheSameDayLikeIos() {
        fun point(date: String, volume: Double) = ExerciseAnalyticsPoint(
            timestampMillis = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sets = 1,
            reps = 10,
            maxWeightKg = 10.0,
            volumeKg = volume,
            feedback = null,
        )

        val buckets = exerciseDailyVolumeBuckets(
            listOf(
                point(LocalDate.now().minusDays(2).toString(), 7.0),
                point(LocalDate.now().minusDays(1).toString(), 10.0),
                point(LocalDate.now().minusDays(1).toString(), 20.0),
            ),
        )
        assertEquals(2, buckets.size)
        assertEquals(listOf(7.0, 30.0), buckets.map { it.second })
    }

    @Test
    fun plannedWorkoutComparisonUsesTheSelectedPeriodAndTrainingDays() {
        assertEquals(
            2,
            plannedWorkoutsInRange(
                trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                days = 7,
                today = LocalDate.of(2026, 8, 30), // Sunday
            ),
        )
    }

    @Test
    fun personalRecordsSelectOneMetricAccordingToExerciseCategory() {
        val summary = ExerciseAnalyticsSummary(
            exercise = ExerciseSummary("Carrera", "run", 1, "libre", 0),
            sessions = 1,
            personalBestKg = 0.0,
            totalVolumeKg = 0.0,
            lastFeedback = null,
        )
        val records = personalRecords(
            mapOf(summary to listOf(ExerciseAnalyticsPoint(1, 1, 12, 20.0, 240.0, null, durationSeconds = 600, distanceKm = 2.5))),
        )

        assertEquals(
            setOf("Mejor peso"),
            records.map { it.title }.toSet(),
        )
    }

    @Test
    fun personalRecordsPreferCategoryMetricAndExcludeFutureHistory() {
        val exercise = ExerciseSummary("Flexiones", "pushUp", 1, "libre", 0)
        val summary = ExerciseAnalyticsSummary(exercise, 1, 20.0, 100.0, null)
        val today = LocalDate.of(2026, 10, 20)
        fun point(day: LocalDate, reps: Int) = ExerciseAnalyticsPoint(
            day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            1, reps, 20.0, 100.0, null,
        )
        val filtered = filteredAnalyticsHistory(listOf(point(today, 12), point(today.plusDays(1), 50)), AnalyticsTimeframe.AllTime, today)
        val records = personalRecords(mapOf(summary to filtered))
        assertEquals(1, records.size)
        assertEquals("Mejores repeticiones", records.single().title)
        assertEquals("12 reps", records.single().value)
    }

    @Test
    fun averageDurationExcludesSessionsWithoutDuration() {
        val sessions = listOf(
            TrainingSessionHistoryEntry(timestampMillis = 1, durationSeconds = 1800, sets = 0, volumeKg = 0.0),
            TrainingSessionHistoryEntry(timestampMillis = 2, durationSeconds = 0, sets = 0, volumeKg = 0.0),
        )
        assertEquals(30, averageActiveMinutes(sessions))
    }

    @Test
    fun exerciseDetailShowsIosRecordAndLastBestHighlights() {
        val exercise = ExerciseSummary("Press banca", "benchPress", 1, "libre", 0)
        val history = listOf(
            ExerciseAnalyticsPoint(1, 3, 24, 80.0, 1_920.0, null),
            ExerciseAnalyticsPoint(2, 3, 24, 70.0, 1_680.0, null),
        )

        assertEquals(
            listOf("80 kg", "70 kg"),
            exerciseRecordMetrics(history, exercise, useImperial = false).map { it.value },
        )
        assertEquals(
            listOf("Récord personal", "Último mejor"),
            exerciseRecordMetrics(history, exercise, useImperial = false).map { it.title },
        )
    }

}
