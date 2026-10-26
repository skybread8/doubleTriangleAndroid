package io.codepassion.doubletriangle

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppQuickActionsTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun completedOrSkippedWorkoutDoesNotExposeStartShortcut() {
        assertFalse(AppQuickActions.isPlannedWorkoutForToday("COMPLETED", "", "TUESDAY", today))
        assertFalse(AppQuickActions.isPlannedWorkoutForToday("SKIPPED", "", "TUESDAY", today))
    }

    @Test
    fun plannedWorkoutUsesScheduledDateWhenPresent() {
        assertTrue(AppQuickActions.isPlannedWorkoutForToday("PLANNED", "2026-10-20T00:00:00Z", "TUESDAY", today))
        assertFalse(AppQuickActions.isPlannedWorkoutForToday("PLANNED", "2026-10-21T00:00:00Z", "TUESDAY", today))
    }

    @Test
    fun legacyPlannedWorkoutFallsBackToWeekday() {
        assertTrue(AppQuickActions.isPlannedWorkoutForToday("", "", "TUESDAY", today))
    }
}
