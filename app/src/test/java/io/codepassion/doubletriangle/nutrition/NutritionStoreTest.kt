package io.codepassion.doubletriangle.nutrition

import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NutritionStoreTest {
    private val monday = LocalDate.of(2026, 8, 24)

    @Test
    fun `plan mirrors workout days and shifts their energy targets`() {
        val profile = OnboardingProfile(
            name = "Alex",
            goal = FitnessGoal.BuildMuscle,
            workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
        )
        val base = NutritionTargets(calories = 2_200, protein = 160, carbs = 240, fat = 70)

        val plan = NutritionStore.weekPlan(profile, base, monday)

        assertEquals(7, plan.size)
        assertEquals(NutritionDayType.Training, plan[0].type)
        assertEquals(EnergyDemand.High, plan[0].energyDemand)
        assertEquals(2_380, plan[0].targets.calories)
        assertEquals(NutritionDayType.Rest, plan[1].type)
        assertEquals(2_200, plan[1].targets.calories)
        assertEquals(NutritionDayType.Recovery, plan[6].type)
        assertEquals(2_040, plan[6].targets.calories)
    }

    @Test
    fun `planned meal macros closely add up to day target`() {
        val target = NutritionTargets(2_350, 165, 260, 75)
        val day = NutritionStore.weekPlan(null, target, monday).first()
        val totals = day.meals.map { it.targets }

        assertTrue(kotlin.math.abs(totals.sumOf { it.calories } - day.targets.calories) <= 4)
        assertTrue(kotlin.math.abs(totals.sumOf { it.protein } - day.targets.protein) <= 4)
        assertEquals(4, day.meals.size)
    }

    @Test
    fun `plan omits meal suggestions when the nutrition profile disables them`() {
        val plan = NutritionStore.weekPlan(
            profile = null,
            baseTargets = NutritionTargets(),
            today = monday,
            preferences = NutritionPreferences(wantsSuggestions = false),
        )

        assertEquals(7, plan.size)
        assertTrue(plan.all { it.meals.isEmpty() })
    }
}
