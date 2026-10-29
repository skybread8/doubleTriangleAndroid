package io.codepassion.doubletriangle

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingWindowTest {
    @Test fun `booking window starts today and remains bounded across month changes`() {
        val today = LocalDate.of(2026, 10, 29)
        assertEquals(listOf(today, today.plusDays(1), today.plusDays(2), today.plusDays(3)), bookingWindowDays(today, 3))
        assertEquals(listOf(today), bookingWindowDays(today, -1))
    }

    @Test fun `optional booking fields and invalid timezone have safe fallbacks`() {
        assertEquals(null, normalizeBookingOptionalString(null))
        assertEquals(null, normalizeBookingOptionalString("null"))
        assertEquals(null, normalizeBookingOptionalString("  "))
        assertEquals("https://example.com/image", normalizeBookingOptionalString(" https://example.com/image "))
        assertEquals(ZoneId.of("UTC"), bookingZone("bad-zone", ZoneId.of("UTC")))
        assertEquals(ZoneId.of("Europe/Madrid"), bookingZone("Europe/Madrid", ZoneId.of("UTC")))
    }

    @Test fun `room with zero interval has no allowed durations`() {
        val room = BookingRoom("id", "room", null, 10, 0, 30, 120, 7, 60)
        assertEquals(emptyList<Int>(), room.durations)
    }
}
