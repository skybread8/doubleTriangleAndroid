package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseHistoryChartSeriesTest {
    @Test
    fun strengthHistoryUsesSessionTotalRepsAndTopLoad() {
        val exercise = ExerciseSummary("Bench press", "benchPress", 3, "8", 90)
        val history = listOf(
            ExerciseHistoryEntry(2L, 3, 24, 60.0, 1_440.0),
            ExerciseHistoryEntry(1L, 3, 30, 70.0, 2_100.0),
        )

        val series = exerciseHistoryChartSeries(exercise, history)

        assertEquals(
            listOf(ExerciseHistoryChartMetric.Volume, ExerciseHistoryChartMetric.Weight, ExerciseHistoryChartMetric.Reps),
            series.map(ExerciseHistoryChartSeries::metric),
        )
        assertEquals(listOf(30.0, 24.0), series.last().points.map(ExerciseHistoryChartPoint::value))
        assertEquals(listOf(70.0, 60.0), series[1].points.map(ExerciseHistoryChartPoint::value))
    }

    @Test
    fun chartSeriesRequiresTwoCompletedSessions() {
        val exercise = ExerciseSummary("Bike", "stationaryBike", 1, "", 0, trackingMode = ExerciseTrackingMode.DurationAndDistance)
        assertEquals(emptyList<ExerciseHistoryChartSeries>(), exerciseHistoryChartSeries(exercise, listOf(ExerciseHistoryEntry(1L, 1, 0, 0.0, 0.0, durationSeconds = 600, distanceKm = 2.0))))
    }

    @Test
    fun cardioIncludesStoredAndDerivedPace() {
        val exercise = ExerciseSummary("Bike", "stationaryBike", 1, "", 0, trackingMode = ExerciseTrackingMode.DurationAndDistance)
        val history = listOf(
            ExerciseHistoryEntry(2L, 1, 0, 0.0, 0.0, durationSeconds = 600, distanceKm = 2.0, paceSecondsPerKm = 310),
            ExerciseHistoryEntry(1L, 1, 0, 0.0, 0.0, durationSeconds = 600, distanceKm = 2.0),
        )
        val series = exerciseHistoryChartSeries(exercise, history)
        assertEquals(listOf(ExerciseHistoryChartMetric.Distance, ExerciseHistoryChartMetric.Pace, ExerciseHistoryChartMetric.Duration), series.map { it.metric })
        assertEquals(listOf(300.0, 310.0), series[1].points.map { it.value })
        assertEquals("5:00 min/km", ExerciseHistoryChartMetric.Pace.displayValue(300.0))
        assertEquals("8:03 min/mi", ExerciseHistoryChartMetric.Pace.displayValue(300.0, true))
    }
}
