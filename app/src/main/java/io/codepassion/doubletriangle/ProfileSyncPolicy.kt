package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.effectiveTrainingLocations

/** Profile fields are separate synchronized models on iOS. */
internal fun changedProfileResources(old: OnboardingProfile, new: OnboardingProfile): Set<String> = buildSet {
    if (listOf(old.name, old.heightCm, old.weightKg, old.birthYear, old.birthMonth, old.birthDay, old.gender, old.metricSystem, old.appLanguage) !=
        listOf(new.name, new.heightCm, new.weightKg, new.birthYear, new.birthMonth, new.birthDay, new.gender, new.metricSystem, new.appLanguage)) add("users")
    if (listOf(old.goal, old.lifestyle, old.gymType, old.trainingLevel, old.trainingSplitPreference, old.preferredWorkoutDurationMinutes,
            old.workoutPlannerNotes, old.workoutDays, old.customWorkoutFocuses, old.movementRestrictions, old.bodyCompositionPhase,
            old.skipsWarmups, old.skipsCooldowns, old.skipsRestPeriods) !=
        listOf(new.goal, new.lifestyle, new.gymType, new.trainingLevel, new.trainingSplitPreference, new.preferredWorkoutDurationMinutes,
            new.workoutPlannerNotes, new.workoutDays, new.customWorkoutFocuses, new.movementRestrictions, new.bodyCompositionPhase,
            new.skipsWarmups, new.skipsCooldowns, new.skipsRestPeriods)) add("training-preferences")
    if (old.effectiveTrainingLocations() != new.effectiveTrainingLocations()) add("training-locations")
    if (old.isHealthConnectEnabled != new.isHealthConnectEnabled) add("user-app-settings")
}

/** Preserve pending local models while applying independent remote models. */
internal fun mergeProfileResources(local: OnboardingProfile, remote: OnboardingProfile, pending: Set<String>): OnboardingProfile {
    var result = remote
    if ("users" in pending) result = result.copy(name = local.name, heightCm = local.heightCm, weightKg = local.weightKg,
        birthYear = local.birthYear, birthMonth = local.birthMonth, birthDay = local.birthDay, gender = local.gender,
        metricSystem = local.metricSystem, appLanguage = local.appLanguage)
    if ("training-preferences" in pending) result = result.copy(goal = local.goal, lifestyle = local.lifestyle,
        gymType = local.gymType, trainingLevel = local.trainingLevel, trainingSplitPreference = local.trainingSplitPreference,
        preferredWorkoutDurationMinutes = local.preferredWorkoutDurationMinutes, workoutPlannerNotes = local.workoutPlannerNotes,
        workoutDays = local.workoutDays, customWorkoutFocuses = local.customWorkoutFocuses, movementRestrictions = local.movementRestrictions,
        bodyCompositionPhase = local.bodyCompositionPhase, skipsWarmups = local.skipsWarmups, skipsCooldowns = local.skipsCooldowns,
        skipsRestPeriods = local.skipsRestPeriods)
    if ("training-locations" in pending) result = result.copy(trainingLocations = local.trainingLocations, availableEquipment = local.availableEquipment)
    if ("user-app-settings" in pending) result = result.copy(isHealthConnectEnabled = local.isHealthConnectEnabled)
    return result
}
