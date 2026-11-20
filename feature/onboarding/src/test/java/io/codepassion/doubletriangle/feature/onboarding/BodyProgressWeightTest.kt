package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyProgressWeightTest {
    private fun weight(date: String, value: Double?) = BodyMetricEntry(date = date, heightCm = 170.0, weightKg = value)

    @Test fun picksClosestWeightWithinSevenDaysAndPrefersLaterDateOnTie() {
        val entries = listOf(weight("2026-01-08", 80.0), weight("2026-01-12", 79.0), weight("2026-01-10", null))
        assertEquals(79.0, nearestProgressWeight("2026-01-10", entries)!!, 0.001)
        assertEquals(80.0, nearestProgressWeight("2026-01-01", entries)!!, 0.001)
    }

    @Test fun ignoresDistantAndInvalidDates() {
        assertNull(nearestProgressWeight("2026-01-01", listOf(weight("2026-01-09", 80.0), weight("invalid", 70.0))))
        assertNull(nearestProgressWeight("invalid", listOf(weight("2026-01-01", 80.0))))
    }
    @Test fun cameraRotationAccountsForSensorAndDisplay() {
        assertEquals(90, progressPhotoJpegRotation(90, 0, false))
        assertEquals(0, progressPhotoJpegRotation(90, 90, false))
        assertEquals(0, progressPhotoJpegRotation(270, 90, true))
        assertEquals(270, progressPhotoJpegRotation(90, 180, false))
    }

    @Test fun malformedPhotoDateIsDisplayedWithoutCrashing() {
        assertEquals("invalid", progressPhotoDate("invalid", includeYear = true, locale = java.util.Locale.US))
        assertEquals("Oct 1", progressPhotoDate("2026-10-01", includeYear = false, locale = java.util.Locale.US))
    }
}
