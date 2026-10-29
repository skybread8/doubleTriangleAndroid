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
    fun calendarCoversThreeWeeksAndExpandsToIncludeThePlan() {
        assertEquals(LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 11, 1), nutritionCalendarWindow(today, emptyList()))
        assertEquals(today.minusDays(20) to today.plusDays(30), nutritionCalendarWindow(today, listOf(today.minusDays(20), today.plusDays(30))))
    }

    @Test
    fun calendarProgressHandlesZeroTargets() {
        assertEquals(0f, nutritionCalendarProgress(500, 0), 0f)
        assertEquals(1.2f, nutritionCalendarProgress(2400, 2000), 0.001f)
    }

    @Test
    fun selectedTodayFollowsTheCalendarButPinnedDaysDoNot() {
        val tomorrow = today.plusDays(1)
        assertEquals(tomorrow, nutritionSelectionAfterDateChange(today, today, tomorrow))
        assertEquals(today.minusDays(1), nutritionSelectionAfterDateChange(today.minusDays(1), today, tomorrow))
    }

    @Test
    fun replacingPlanClearsASelectionThatNoLongerExists() {
        val pinned = today.minusDays(1)
        assertEquals(pinned, nutritionSelectionAfterPlanChange(pinned, today, listOf(pinned, today)))
        assertEquals(today, nutritionSelectionAfterPlanChange(pinned, today, listOf(today)))
        assertEquals(today, nutritionSelectionAfterPlanChange(today, today, emptyList()))
    }

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
