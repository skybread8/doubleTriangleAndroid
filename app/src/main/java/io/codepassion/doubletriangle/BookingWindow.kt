package io.codepassion.doubletriangle

import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.util.UUID

internal fun bookingWindowDays(today: LocalDate, windowDays: Int): List<LocalDate> =
    (0..windowDays.coerceAtLeast(0)).map { today.plusDays(it.toLong()) }

internal fun normalizeBookingOptionalString(value: String?): String? = value?.trim()
    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

internal fun bookingZone(value: String?, fallback: ZoneId = ZoneId.systemDefault()): ZoneId =
    normalizeBookingOptionalString(value)?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: fallback

internal class BookingHttpException(val status: Int, message: String) : IllegalStateException(message)

internal fun isDefiniteBookingRejection(error: Throwable): Boolean =
    error is BookingHttpException && error.status in 400..499 && error.status !in setOf(408, 429)

internal data class BookingAttempt(val roomId: String, val startsAt: Instant, val endsAt: Instant, val key: UUID)

internal fun bookingAttemptFor(previous: BookingAttempt?, roomId: String, start: BookingStart): BookingAttempt {
    val begins = start.startsAt.toInstant()
    val ends = start.endsAt.toInstant()
    return previous?.takeIf { it.roomId == roomId && it.startsAt == begins && it.endsAt == ends }
        ?: BookingAttempt(roomId, begins, ends, UUID.randomUUID())
}
