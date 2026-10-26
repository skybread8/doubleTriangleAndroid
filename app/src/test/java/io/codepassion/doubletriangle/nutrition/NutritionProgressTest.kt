package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

class NutritionProgressTest {
    @Test
    fun macroWarningAppearsOnlyAboveTarget() {
        assertFalse(nutritionTargetExceeded(100, 100))
        assertFalse(nutritionTargetExceeded(99, 100))
        assertTrue(nutritionTargetExceeded(101, 100))
    }

    @Test
    fun macroColorsMatchIosIconPalette() {
        assertEquals(Color(0xFFFF9500), NutritionCaloriesColor)
        assertEquals(Color(0xFF00C7BE), ProteinColor)
        assertEquals(Color(0xFF34C759), CarbsColor)
        assertEquals(Color(0xFFFFCC00), FatColor)
    }

    @Test
    fun nutritionDayTypeColorsMatchIosHeaderPalette() {
        assertEquals(NutritionOrange, nutritionDayTypeColor(NutritionDayType.Training))
        assertEquals(Color(0xFF007AFF), nutritionDayTypeColor(NutritionDayType.Rest))
        assertEquals(NutritionGreen, nutritionDayTypeColor(NutritionDayType.Recovery))
    }

    @Test
    fun nutritionHeaderMacroProgressIsClampedLikeIosVariableValue() {
        assertEquals(0.5f, nutritionHeaderMacroProgress(30, 60))
        assertEquals(1f, nutritionHeaderMacroProgress(90, 60))
        assertEquals(0f, nutritionHeaderMacroProgress(30, 0))
    }

    @Test
    fun burnedCaloriesUseLocalActiveFallbackWhenHealthConnectTotalIsMissing() {
        assertEquals(315.0, burnedCaloriesOrNull(NutritionEnergyPoint(315.0, null, 0)))
        assertEquals(2280.0, burnedCaloriesOrNull(NutritionEnergyPoint(315.0, 2280.0, 0)))
        assertEquals(null, burnedCaloriesOrNull(NutritionEnergyPoint(null, null, 500)))
    }

    @Test
    fun nutritionMealsSectionTitleUsesIosCapitalizationAndTypographyContract() {
        val today = LocalDate.of(2026, 10, 23)
        assertEquals("Comidas de hoy", nutritionMealsSectionTitle(today, today))
        assertEquals("Comidas del 22 oct", nutritionMealsSectionTitle(today.minusDays(1), today))
    }
}
