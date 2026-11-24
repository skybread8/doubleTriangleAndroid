package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSyncPolicyTest {
    private val local = OnboardingProfile(name = "Local", goal = FitnessGoal.BuildMuscle)

    @Test fun changingNameOnlyMarksTheUserRecord() {
        assertEquals(setOf("users"), changedProfileResources(local, local.copy(name = "Edited")))
    }

    @Test fun changingHealthSettingDoesNotMarkOtherProfileModels() {
        assertEquals(setOf("user-app-settings"), changedProfileResources(local, local.copy(isHealthConnectEnabled = true)))
    }

    @Test fun changingDurationSchedulesTrainingPreferencesAndKeepsTheEditedValue() {
        val previous = local.copy(preferredWorkoutDurationMinutes = 166)
        val edited = previous.copy(preferredWorkoutDurationMinutes = 50)
        val pending = changedProfileResources(previous, edited)

        assertEquals(setOf("training-preferences"), pending)
        assertEquals(50, mergeProfileResources(edited, previous, pending).preferredWorkoutDurationMinutes)
    }

    @Test fun independentRemoteModelsAreAppliedWhileLocalUserIsPending() {
        val remote = local.copy(name = "Remote", isHealthConnectEnabled = true, preferredWorkoutDurationMinutes = 70)
        val merged = mergeProfileResources(local, remote, setOf("users"))
        assertEquals("Local", merged.name)
        assertEquals(70, merged.preferredWorkoutDurationMinutes)
        assertEquals(true, merged.isHealthConnectEnabled)
    }

    @Test fun pendingTrainingPreferencesDoNotHideRemoteUserChanges() {
        val remote = local.copy(name = "Remote", preferredWorkoutDurationMinutes = 70)
        val merged = mergeProfileResources(local, remote, setOf("training-preferences"))
        assertEquals("Remote", merged.name)
        assertEquals(local.preferredWorkoutDurationMinutes, merged.preferredWorkoutDurationMinutes)
    }
}
