package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsPoint
import io.codepassion.doubletriangle.feature.workout.ExerciseAnalyticsSummary
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.junit.Assert.assertEquals
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
    fun personalRecordsIncludeEveryRecordedMetricType() {
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
            setOf("Mejor peso", "Mejores repeticiones", "Mayor volumen", "Mayor duración", "Mayor distancia"),
            records.map { it.title }.toSet(),
        )
    }
}
