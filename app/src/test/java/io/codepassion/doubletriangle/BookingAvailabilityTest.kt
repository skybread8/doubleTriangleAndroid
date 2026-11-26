package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class BookingAvailabilityTest {
    @Test fun `cancellation is offered only before the cutoff on an active reservation`() {
        val cutoff = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val reservation = BookingReservation("id", "confirmed", cutoff.plusHours(1), cutoff.plusHours(2), cutoff, "Room", null, "gym", "Gym", null, java.time.ZoneId.of("Europe/Madrid"))
        assertTrue(reservation.isCancellableAt(cutoff.minusSeconds(1)))
        assertTrue(!reservation.isCancellableAt(cutoff))
        assertTrue(!reservation.isCancellableAt(cutoff.plusSeconds(1)))
        assertTrue(!reservation.copy(status = "cancelled").isCancellableAt(cutoff.minusSeconds(1)))
        assertTrue(!reservation.copy(cancellableUntil = null).isCancellableAt(cutoff.minusSeconds(1)))
    }
    @Test fun `calendar event keeps the reservation instants and gym timezone`() {
        val start = OffsetDateTime.parse("2026-10-25T02:30:00+02:00")
        val end = OffsetDateTime.parse("2026-10-25T02:30:00+01:00")
        val reservation = BookingReservation("id", "confirmed", start, end, null, "Sala", null, "gym", "Gimnasio", null, java.time.ZoneId.of("Europe/Madrid"))
        val event = bookingCalendarEvent(reservation, " Calle Mayor 1 ")
        assertEquals("Sala · Gimnasio", event.title)
        assertEquals("Gimnasio, Calle Mayor 1", event.location)
        assertEquals(start.toInstant().toEpochMilli(), event.beginMillis)
        assertEquals(end.toInstant().toEpochMilli(), event.endMillis)
        assertEquals(3600000L, event.endMillis - event.beginMillis)
        assertEquals("Europe/Madrid", event.timeZone)
        assertEquals("Gimnasio", bookingCalendarEvent(reservation, null).location)
    }
    @Test fun `existing active reservations block overlapping intervals but not adjacent or cancelled ones`() {
        val start = OffsetDateTime.parse("2026-10-15T10:00:00+02:00")
        val reservation = BookingReservation("id", "confirmed", start, start.plusHours(1), null, "Room", null, "other-gym", "Gym", null, java.time.ZoneId.of("Europe/Madrid"))
        fun choice(from: OffsetDateTime, to: OffsetDateTime) = BookingStart(from, to, 5)
        assertTrue(overlapsActiveReservation(choice(start, start.plusHours(1)), listOf(reservation)))
        assertTrue(overlapsActiveReservation(choice(start.minusMinutes(30), start.plusMinutes(30)), listOf(reservation)))
        assertTrue(overlapsActiveReservation(choice(start.plusMinutes(30).withOffsetSameInstant(java.time.ZoneOffset.UTC), start.plusHours(2)), listOf(reservation)))
        assertTrue(!overlapsActiveReservation(choice(start.plusHours(1), start.plusHours(2)), listOf(reservation)))
        assertTrue(!overlapsActiveReservation(choice(start.minusHours(1), start), listOf(reservation)))
        assertTrue(!overlapsActiveReservation(choice(start, start.plusHours(1)), listOf(reservation.copy(status = "cancelled"))))
    }
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
