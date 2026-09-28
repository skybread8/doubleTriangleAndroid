package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.TrainingSplitPreference

/** Inputs which change the structure or safety of a generated workout plan. */
internal fun OnboardingProfile.workoutPlanProfileSignature(): String = listOf(
    goal.storedValue,
    lifestyle.storedValue,
    trainingLevel.storedValue,
    trainingSplitPreference.storedValue,
    preferredWorkoutDurationMinutes.toString(),
    workoutDays.map { it.storedValue }.sorted().joinToString(","),
    if (trainingSplitPreference == TrainingSplitPreference.Custom) {
        customWorkoutFocuses.entries
            .sortedBy { it.key.storedValue }
            .joinToString(",") { "${it.key.storedValue}:${it.value.storedValue}" }
    } else "",
    availableEquipment.map { it.storedValue }.sorted().joinToString(","),
    movementRestrictions.map { it.storedValue }.sorted().joinToString(","),
    bodyCompositionPhase?.storedValue ?: "none",
).joinToString("|")
