package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class CalendarExportTest {
    private fun workout(id: String, date: String?, started: String? = null, status: WorkoutStatus = WorkoutStatus.Planned) =
        WorkoutDaySummary(id, 1, "Pierna, fuerza", "Línea 1\nLínea 2", "strength", DayOfWeek.MONDAY, 45, status, scheduledFor = date, startedAt = started)

    @Test fun `exports every dated workout in local time with escaped text`() {
        val state = PreviewWorkoutRepository.load().copy(planId = "plan", workouts = listOf(workout("a", "2026-03-30"), workout("b", null)))
        val file = calendarFile(state, LocalTime.of(9, 0), ZoneId.of("Europe/Madrid"), Instant.parse("2026-01-01T00:00:00Z"), mapOf("b" to LocalDate.parse("2026-10-26")))
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(file).count())
        assertTrue(file.contains("DTSTART:20260330T070000Z"))
        assertTrue(file.contains("DTSTART:20261026T080000Z"))
        assertTrue(file.contains("SUMMARY:Pierna\\, fuerza"))
        assertTrue(file.contains("DESCRIPTION:Línea 1\\nLínea 2"))
    }

    @Test fun `suggestion uses completed starts and handles midnight`() {
        val items = listOf(workout("a", null, "2026-01-01T23:50:00Z", WorkoutStatus.Completed), workout("b", null, "2026-01-02T00:10:00Z", WorkoutStatus.Completed), workout("c", null, "2026-01-03T12:00:00Z"))
        assertEquals(LocalTime.MIDNIGHT, suggestedWorkoutTime(items, ZoneOffset.UTC))
        assertNull(suggestedWorkoutTime(emptyList(), ZoneOffset.UTC))
    }
    @Test fun `missing dates use preferred weekdays in plan week and preserve scheduled dates`() {
        val monday = workout("a", null).copy(scheduledDay = DayOfWeek.MONDAY)
        val wednesday = workout("b", null).copy(order = 2, scheduledDay = DayOfWeek.WEDNESDAY)
        val scheduled = workout("c", "2026-11-20").copy(order = 3, scheduledDay = DayOfWeek.FRIDAY)
        val plan = PreviewWorkoutRepository.load().copy(planStartsOn = "2026-11-10", workouts = listOf(monday, wednesday, scheduled))
        val dates = suggestedCalendarDates(plan, LocalDate.parse("2026-12-01"))
        assertEquals(LocalDate.parse("2026-11-09"), dates["a"])
        assertEquals(LocalDate.parse("2026-11-11"), dates["b"])
        assertEquals(LocalDate.parse("2026-11-20"), dates["c"])
        assertEquals(LocalDate.parse("2026-11-16"), suggestedCalendarDates(plan.copy(planStartsOn = null, workouts = listOf(monday)), LocalDate.parse("2026-11-18"))["a"])
    }
    @Test fun `export excludes finalized sessions`() {
        val plan = PreviewWorkoutRepository.load().copy(workouts = listOf(
            workout("planned", "2026-11-12"),
            workout("done", "2026-11-13", status = WorkoutStatus.Completed),
            workout("skipped", "2026-11-14", status = WorkoutStatus.Skipped),
            workout("active", "2026-11-15", status = WorkoutStatus.InProgress),
        ))
        assertEquals(listOf("planned", "active"), exportableWorkouts(plan).map { it.id })
        val file = calendarFile(plan, LocalTime.NOON, ZoneOffset.UTC)
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(file).count())
        assertFalse(file.contains("20261113T120000Z"))
        assertFalse(file.contains("20261114T120000Z"))
    }
}
