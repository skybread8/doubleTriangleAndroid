package io.codepassion.doubletriangle.nutrition

import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.Gender
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionEnergyFallbackTest {
    private val profile = OnboardingProfile(name = "Test", goal = FitnessGoal.entries.first(), birthYear = 1990, birthMonth = 6, birthDay = 15, gender = Gender.Male, heightCm = 175, weightKg = 70.0)

    @Test fun restingEstimateUsesAgeOnEachDate() {
        assertEquals(1628.75, estimatedRestingCalories(profile, LocalDate.of(2026, 6, 14))!!, 0.001)
        assertEquals(1623.75, estimatedRestingCalories(profile, LocalDate.of(2026, 6, 15))!!, 0.001)
        assertNull(estimatedRestingCalories(null, LocalDate.of(2026, 6, 15)))
    }

    @Test fun restingAndWorkoutAreCombinedOnlyWhenHealthTotalIsMissing() {
        assertEquals(1600.0, nutritionEnergyPoint(null, null, 1600.0, 0).totalCalories!!, 0.0)
        assertEquals(1900.0, nutritionEnergyPoint(300.0, null, 1600.0, 0).totalCalories!!, 0.0)
        assertEquals(2100.0, nutritionEnergyPoint(300.0, 2100.0, 1600.0, 0).totalCalories!!, 0.0)
        assertEquals(300.0, nutritionEnergyPoint(300.0, null, null, 0).activeCalories!!, 0.0)
    }
}
