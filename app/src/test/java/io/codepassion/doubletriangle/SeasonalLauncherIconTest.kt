package io.codepassion.doubletriangle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonalLauncherIconTest {
    @Test fun nextBoundaryHandlesStartEndAndYearChanges() {
        assertEquals(LocalDate.of(2026, 12, 7), SeasonalLauncherIcon.nextBoundary(LocalDate.of(2026, 12, 6)))
        assertEquals(LocalDate.of(2027, 1, 7), SeasonalLauncherIcon.nextBoundary(LocalDate.of(2026, 12, 7)))
        assertEquals(LocalDate.of(2027, 1, 7), SeasonalLauncherIcon.nextBoundary(LocalDate.of(2027, 1, 6)))
        assertEquals(LocalDate.of(2027, 12, 7), SeasonalLauncherIcon.nextBoundary(LocalDate.of(2027, 1, 7)))
    }
    @Test fun brokenOnlyWhenScheduledTodayAndNoWorkoutCompleted() {
        val monday = LocalDate.of(2026, 12, 7)
        assertTrue(SeasonalLauncherIcon.isWorkoutPending(setOf("MONDAY"), monday, false))
        assertFalse(SeasonalLauncherIcon.isWorkoutPending(setOf("MONDAY"), monday, true))
        assertFalse(SeasonalLauncherIcon.isWorkoutPending(setOf("TUESDAY"), monday, false))
        assertFalse(SeasonalLauncherIcon.isWorkoutPending(emptySet(), monday, false))
    }

    @Test fun eachWorkoutAndSeasonCombinationHasAnIcon() {
        assertEquals("io.codepassion.doubletriangle.LauncherDefault", SeasonalLauncherIcon.aliasFor(false, false))
        assertEquals("io.codepassion.doubletriangle.LauncherBroken", SeasonalLauncherIcon.aliasFor(false, true))
        assertEquals("io.codepassion.doubletriangle.LauncherChristmas", SeasonalLauncherIcon.aliasFor(true, false))
        assertEquals("io.codepassion.doubletriangle.LauncherChristmasBroken", SeasonalLauncherIcon.aliasFor(true, true))
    }
}
