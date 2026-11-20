package io.codepassion.doubletriangle

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Presentation rules shared by the home card and notification entry point. */
internal data class CheckInCardRecord(
    val id: String,
    val userId: String?,
    val dueOn: LocalDate,
    val status: String,
    val deleted: Boolean = false,
    val missed: Boolean = false,
    val submittedAt: Instant? = null,
    val reviewedAt: Instant? = null,
    val coachReply: String? = null,
    val answered: Int = 0,
    val total: Int = 0,
)

internal enum class CheckInCardKind { Available, InProgress, Late, AwaitingReview, Replied }

internal data class CheckInCard(val record: CheckInCardRecord, val kind: CheckInCardKind, val otherOpenCount: Int = 0)

internal fun resolveCheckInCard(
    records: List<CheckInCardRecord>,
    userId: String,
    readReplyIds: Set<String>,
    today: LocalDate = LocalDate.now(),
    now: Instant = Instant.now(),
): CheckInCard? {
    val due = records.filter { !it.deleted && it.userId.equals(userId, ignoreCase = true) && it.dueOn <= today }
        .sortedBy { it.dueOn }
    val open = due.filter { it.status != "submitted" }
    open.firstOrNull()?.let { record ->
        val kind = when {
            record.missed -> CheckInCardKind.Late
            record.status == "draft" || record.answered > 0 -> CheckInCardKind.InProgress
            else -> CheckInCardKind.Available
        }
        return CheckInCard(record, kind, open.size - 1)
    }
    due.filter { it.status == "submitted" && it.reviewedAt != null && !it.coachReply.isNullOrEmpty() && it.id !in readReplyIds }
        .maxByOrNull { it.reviewedAt!! }
        ?.let { return CheckInCard(it, CheckInCardKind.Replied) }
    due.filter { it.status == "submitted" && it.reviewedAt == null && it.submittedAt != null && Duration.between(it.submittedAt, now) < Duration.ofDays(2) }
        .maxByOrNull { it.submittedAt!! }
        ?.let { return CheckInCard(it, CheckInCardKind.AwaitingReview) }
    return null
}
