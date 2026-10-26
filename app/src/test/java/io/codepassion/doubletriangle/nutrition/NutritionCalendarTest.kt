package io.codepassion.doubletriangle.nutrition

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalendarTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun calorieRingIsShownForEveryPlannedPastOrCurrentDay() {
        assertTrue(shouldShowCalendarCalorieRing(today.minusDays(1), today, 1_500))
        assertTrue(shouldShowCalendarCalorieRing(today, today, 1_500))
        assertTrue(shouldShowCalendarCalorieRing(today.minusDays(1), today, 0))
        assertTrue(shouldShowCalendarCalorieRing(today, today, 0))
        assertFalse(shouldShowCalendarCalorieRing(today.plusDays(1), today, 1_500))
    }

    @Test
    fun futureNutritionDaysCannotBeLogged() {
        assertTrue(canLogNutritionDate(today.minusDays(1), today))
        assertTrue(canLogNutritionDate(today, today))
        assertFalse(canLogNutritionDate(today.plusDays(1), today))
    }

    @Test
    fun planGenerationHeroUsesTodayInsteadOfSelectedDay() {
        val planDates = listOf(today.minusDays(7), today.minusDays(1))
        assertTrue(nutritionPlanIsCompleteOrOnLastDay(planDates, today))
        assertTrue(nutritionPlanIsCompleteOrOnLastDay(planDates, today.minusDays(1)))
        assertFalse(nutritionPlanIsCompleteOrOnLastDay(planDates, today.minusDays(2)))
        assertTrue(nutritionPlanIsCompleteOrOnLastDay(emptyList(), today))
    }

    @Test
    fun workoutFuelingSectionIgnoresGeneralNotesWithoutWorkoutGuidance() {
        assertFalse(hasNutritionWorkoutFuelingContent("", null, null))
        assertTrue(hasNutritionWorkoutFuelingContent("Leg Day", null, null))
        assertTrue(hasNutritionWorkoutFuelingContent("", "Eat carbs before training", null))
        assertTrue(hasNutritionWorkoutFuelingContent("", null, "Prioritize protein after training"))
    }

    @Test
    fun nutritionProgressUsesLocalizedIntegerFormattingAndRoundedPercent() {
        assertEquals("2,323", formatNutritionInteger(2323, Locale.US))
        assertEquals("2.323", formatNutritionInteger(2323, Locale.GERMANY))
        assertEquals("24", formatNutritionInteger((0.2375f * 100).roundToInt(), Locale.US))
    }
}
