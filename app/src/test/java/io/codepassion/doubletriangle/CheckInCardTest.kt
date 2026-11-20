package io.codepassion.doubletriangle

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckInCardTest {
    private val today = LocalDate.parse("2026-11-16")
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private fun card(vararg records: CheckInCardRecord, read: Set<String> = emptySet()) =
        resolveCheckInCard(records.toList(), "user", read, today, now)
    private fun record(id: String, date: LocalDate = today, status: String = "pending") =
        CheckInCardRecord(id, "user", date, status)

    @Test fun hidesFutureDeletedAndOtherUsers() {
        assertNull(card(record("future", today.plusDays(1)), record("deleted").copy(deleted = true), record("other").copy(userId = "other")))
    }

    @Test fun oldestOpenTakesPriorityAndCountsOthers() {
        val result = card(record("new"), record("old", today.minusDays(1)).copy(missed = true), record("draft").copy(status = "draft"))
        assertEquals("old", result?.record?.id)
        assertEquals(CheckInCardKind.Late, result?.kind)
        assertEquals(2, result?.otherOpenCount)
    }

    @Test fun unreadReplyPrecedesRecentSubmission() {
        val reply = record("reply", status = "submitted").copy(reviewedAt = now, coachReply = "Well done")
        val waiting = record("waiting", status = "submitted").copy(submittedAt = now.minusSeconds(60))
        assertEquals(CheckInCardKind.Replied, card(waiting, reply)?.kind)
        assertEquals(CheckInCardKind.AwaitingReview, card(waiting, reply, read = setOf("reply"))?.kind)
        assertNull(card(waiting.copy(submittedAt = now.minusSeconds(172800))))
    }
}
