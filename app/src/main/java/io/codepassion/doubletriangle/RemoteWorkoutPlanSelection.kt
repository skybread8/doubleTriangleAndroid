package io.codepassion.doubletriangle

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private fun planDate(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC) }.getOrNull()
        ?: Instant.MIN

/** Mirrors User.currentWorkoutPlan in iOS. */
internal fun <T> selectCurrentRemoteWorkoutPlan(
    plans: List<T>,
    status: (T) -> String,
    createdAt: (T) -> String,
    updatedAt: (T) -> String,
    id: (T) -> String,
): T? {
    val eligible = plans.filter { status(it).equals("active", true) || status(it).equals("completed", true) }
    val active = eligible.filter { status(it).equals("active", true) }
    return active.ifEmpty { eligible }.maxWithOrNull(compareBy<T> { planDate(createdAt(it)) }.thenBy { planDate(updatedAt(it)) }.thenBy { id(it).lowercase(Locale.ROOT) })
}
