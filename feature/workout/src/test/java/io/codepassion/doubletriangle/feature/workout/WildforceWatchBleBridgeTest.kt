package io.codepassion.doubletriangle.feature.workout

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WildforceWatchBleBridgeTest {
    private fun state(isSummary: Boolean = false, pendingFeedback: Boolean = false, selectedFeedback: String = "JUSTO", durationMinutes: Int? = null, distanceKm: Double? = null) = WildforceWatchBridge.State(
        workoutId = "workout-1",
        workoutTitle = "Upper body",
        exerciseName = "Press banca",
        exerciseIndex = 2,
        exerciseCount = 6,
        setNumber = 3,
        targetSets = 4,
        reps = 10,
        weightKg = 42.5,
        restRemaining = 75,
        restInitialSeconds = 90,
        elapsedSeconds = 840,
        isSummary = isSummary,
        pendingFeedback = pendingFeedback,
        selectedFeedback = selectedFeedback,
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
    )

    @Test
    fun numericFrameRemainsZeppCompatibleAndCarriesSummaryBit() {
        val active = state().toWire().split('|')
        val summary = state(isSummary = true).toWire().split('|')

        assertEquals(listOf("2", "6", "3", "4", "10", "85", "75"), active.take(7))
        assertEquals("0", active[7])
        assertEquals("1", summary[7])
    }

    @Test
    fun metadataFrameIsBase64AndRoundTripsLabels() {
        val fields = state().toMetadataWire().split('|')
        assertEquals(2, fields.size)
        assertEquals("Upper body", String(Base64.getDecoder().decode(fields[0]), StandardCharsets.UTF_8))
        assertEquals("Press banca", String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8))
        assertTrue(fields.all { it.isNotBlank() })
    }

    @Test
    fun numericFrameCarriesWatchFeedbackState() {
        val fields = state(pendingFeedback = true, selectedFeedback = "DIFÍCIL").toWire().split('|')

        assertEquals("1", fields[8])
        assertEquals("3", fields[9])
    }

    @Test
    fun numericFrameCarriesOptionalTypedDurationAndDistance() {
        val fields = state(durationMinutes = 12, distanceKm = 2.4).toWire().split('|')

        assertEquals("12", fields[10])
        assertEquals("24", fields[11])
    }
}
