package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.designsystem.SeasonalDecorations
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonalDecorationsTest {
    @Test fun christmasIncludesBothEndsOfTheSeasonAcrossYears() {
        assertFalse(SeasonalDecorations.isChristmas(LocalDate.of(2026, 12, 6)))
        assertTrue(SeasonalDecorations.isChristmas(LocalDate.of(2026, 12, 7)))
        assertTrue(SeasonalDecorations.isChristmas(LocalDate.of(2026, 12, 31)))
        assertTrue(SeasonalDecorations.isChristmas(LocalDate.of(2027, 1, 1)))
        assertTrue(SeasonalDecorations.isChristmas(LocalDate.of(2027, 1, 6)))
        assertFalse(SeasonalDecorations.isChristmas(LocalDate.of(2027, 1, 7)))
        assertFalse(SeasonalDecorations.isChristmas(LocalDate.of(2027, 10, 31)))
    }
}
