package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSheetResourcesTest {
    @Test fun selectableOnboardingValuesHaveLocalizedNamesWithoutChangingStoredValues() {
        val groups = mapOf(
            "FitnessGoal" to FitnessGoal.entries.map { it.storedValue },
            "LifestyleLevel" to LifestyleLevel.entries.map { it.storedValue },
            "TrainingLevel" to TrainingLevel.entries.map { it.storedValue },
            "TrainingSplitPreference" to TrainingSplitPreference.entries.map { it.storedValue },
            "BodyCompositionPhase" to BodyCompositionPhase.entries.map { it.storedValue },
            "GymType" to GymType.entries.map { it.storedValue },
            "Equipment" to Equipment.entries.map { it.storedValue },
            "MovementRestriction" to MovementRestriction.entries.map { it.storedValue },
            "Gender" to Gender.entries.map { it.storedValue },
            "WorkoutFocus" to WorkoutFocus.entries.map { it.storedValue },
        )
        groups.forEach { (kind, values) -> values.forEach { value ->
            assertTrue("Missing localized name for $kind:$value", onboardingSheetResources.containsKey("$kind:$value:englishName"))
        } }
    }

    @Test fun descriptiveChoicesHaveTheSameLocalizedDescriptionsAsIos() {
        val groups = mapOf(
            "FitnessGoal" to FitnessGoal.entries.map { it.storedValue },
            "LifestyleLevel" to LifestyleLevel.entries.map { it.storedValue },
            "TrainingLevel" to TrainingLevel.entries.map { it.storedValue },
            "BodyCompositionPhase" to BodyCompositionPhase.entries.map { it.storedValue },
        )
        groups.forEach { (kind, values) -> values.forEach { value ->
            assertTrue("Missing localized description for $kind:$value", onboardingSheetResources.containsKey("$kind:$value:description"))
        } }
    }
}
