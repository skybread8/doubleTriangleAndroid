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

    @Test fun `accepts contiguous slots across an offset transition`() {
        val slots = listOf(
            BookingSlot(OffsetDateTime.parse("2026-10-25T02:30:00+02:00"), OffsetDateTime.parse("2026-10-25T03:00:00+02:00"), 3, true),
            BookingSlot(OffsetDateTime.parse("2026-10-25T02:00:00+01:00"), OffsetDateTime.parse("2026-10-25T02:30:00+01:00"), 2, true),
        )
        val result = contiguousStarts(slots, 60)
        assertEquals(1, result.size)
        assertEquals(2, result.single().availablePlaces)
        assertEquals(OffsetDateTime.parse("2026-10-25T02:30:00+01:00").toInstant(), result.single().endsAt.toInstant())
    }

    @Test fun `selection updates duration and places only while the start remains available`() {
        val start = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val slots = listOf(BookingSlot(start, start.plusMinutes(30), 3, true), BookingSlot(start.plusMinutes(30), start.plusMinutes(60), 1, true))
        val old = BookingStart(start, start.plusMinutes(30), 3)
        val updated = refreshedBookingSelection(old, slots, 60)!!
        assertEquals(start.plusMinutes(60), updated.endsAt)
        assertEquals(1, updated.availablePlaces)
        assertEquals(null, refreshedBookingSelection(old, slots, 90))
        assertTrue(contiguousStarts(slots, 0).isEmpty())
    }

    @Test fun `retry keeps the idempotency key for the same room and interval even if places change`() {
        val start = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val first = bookingAttemptFor(null, "room-a", BookingStart(start, start.plusHours(1), 3))
        val retry = bookingAttemptFor(first, "room-a", BookingStart(start, start.plusHours(1), 1))
        assertEquals(first.key, retry.key)
        assertTrue(first.key != bookingAttemptFor(first, "room-b", BookingStart(start, start.plusHours(1), 3)).key)
        assertTrue(first.key != bookingAttemptFor(first, "room-a", BookingStart(start, start.plusHours(2), 3)).key)
    }

    @Test fun `transport and uncertain server failures retain retry identity`() {
        assertTrue(!isDefiniteBookingRejection(java.io.IOException("timeout")))
        assertTrue(!isDefiniteBookingRejection(BookingHttpException(408, "timeout")))
        assertTrue(!isDefiniteBookingRejection(BookingHttpException(500, "server")))
        assertTrue(isDefiniteBookingRejection(BookingHttpException(409, "full")))
    }
}
