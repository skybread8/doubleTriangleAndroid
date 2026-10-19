package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class BookingAvailabilityTest {
    @Test
    fun `only offers starts covered by consecutive bookable slots`() {
        val start = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val slots = listOf(
            BookingSlot(start, start.plusMinutes(30), 3, true),
            BookingSlot(start.plusMinutes(30), start.plusMinutes(60), 1, true),
            BookingSlot(start.plusMinutes(60), start.plusMinutes(90), 1, false),
        )

        val starts = contiguousStarts(slots, duration = 60)

        assertEquals(1, starts.size)
        assertEquals(start, starts.single().startsAt)
        assertEquals(1, starts.single().availablePlaces)
    }

    @Test
    fun `does not offer a range with a gap`() {
        val start = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val slots = listOf(
            BookingSlot(start, start.plusMinutes(30), 2, true),
            BookingSlot(start.plusMinutes(45), start.plusMinutes(75), 2, true),
        )

        assertTrue(contiguousStarts(slots, duration = 60).isEmpty())
    }
}
