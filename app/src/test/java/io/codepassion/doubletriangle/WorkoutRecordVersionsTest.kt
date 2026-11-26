package io.codepassion.doubletriangle

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutRecordVersionsTest {
    private val old = Instant.parse("2026-01-01T10:00:00Z")
    private val recent = Instant.parse("2026-01-01T11:00:00Z")

    @Test fun mergesDifferentRecordsAndKeepsNewerVersionIndependently() {
        val result = mergeWorkoutRecordVersions(
            listOf(WorkoutRecordVersion("A", old, null, "local a"), WorkoutRecordVersion("B", recent, null, "local b")),
            listOf(WorkoutRecordVersion("a", recent, null, "remote a"), WorkoutRecordVersion("b", old, null, "remote b")),
        )
        assertEquals(listOf("remote a", "local b"), result.map { it.value })
    }

    @Test fun newerTombstoneWinsAndOlderTombstoneDoesNot() {
        val result = mergeWorkoutRecordVersions(
            listOf(WorkoutRecordVersion("a", old, null, "live"), WorkoutRecordVersion("b", recent, null, "live")),
            listOf(WorkoutRecordVersion("a", recent, recent, "deleted"), WorkoutRecordVersion("b", old, old, "deleted")),
        )
        assertEquals(recent, result[0].deletedAt)
        assertNull(result[1].deletedAt)
    }

    @Test fun unknownLegacyEditIsNotOverwritten() {
        val result = mergeWorkoutRecordVersions(
            listOf(WorkoutRecordVersion("a", null, null, "pending")),
            listOf(WorkoutRecordVersion("a", recent, null, "remote")),
        )
        assertEquals("pending", result.single().value)
    }
}
