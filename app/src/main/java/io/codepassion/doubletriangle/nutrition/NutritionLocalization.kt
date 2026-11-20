package io.codepassion.doubletriangle.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.codepassion.doubletriangle.core.designsystem.R

@Composable
internal fun mealTypeLabel(type: MealType): String = stringResource(when (type) {
    MealType.Breakfast -> R.string.breakfast
    MealType.Lunch -> R.string.lunch
    MealType.Dinner -> R.string.dinner
    MealType.Snack -> R.string.snack
    MealType.PreWorkout -> R.string.pre_workout
    MealType.PostWorkout -> R.string.post_workout
})

@Composable
internal fun nutritionDayTypeLabel(type: NutritionDayType): String = stringResource(when (type) {
    NutritionDayType.Training -> R.string.ui_sheet_training
    NutritionDayType.Rest -> R.string.rest
    NutritionDayType.Recovery -> R.string.ui_sheet_recovery
})

@Composable
internal fun energyDemandLabel(demand: EnergyDemand): String = stringResource(when (demand) {
    EnergyDemand.High -> R.string.high
    EnergyDemand.Medium -> R.string.medium
    EnergyDemand.Low -> R.string.low
})

/** Map known persisted values to the same dietary-style keys used by iOS. */
internal fun dietaryStyleResource(value: String): Int? = when (value.trim().lowercase(java.util.Locale.ROOT)) {
    "omnívora", "omnivora", "standard" -> R.string.ui_sheet_standard
    "vegetariana", "vegetarian" -> R.string.ui_sheet_vegetarian
    "vegana", "vegan" -> R.string.ui_sheet_vegan
    "pescetariana", "pescatarian", "pescetarian" -> R.string.ui_sheet_pescatarian
    else -> null
}

@Composable
internal fun dietaryStyleLabel(value: String): String = dietaryStyleResource(value)?.let { stringResource(it) } ?: value

@Composable
internal fun nutritionGoalLabel(goal: io.codepassion.doubletriangle.feature.onboarding.FitnessGoal): String = stringResource(when (goal) {
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.LoseWeight -> R.string.lose_weight
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.BuildMuscle -> R.string.build_muscle
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.GainStrength -> R.string.gain_strength
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.ImproveEndurance -> R.string.improve_endurance
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.ImproveMobility -> R.string.improve_mobility
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.BodyRecomposition -> R.string.body_recomposition
    io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.GeneralFitness -> R.string.general_fitness
})

@Composable
internal fun nutritionPhaseLabel(phase: io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase): String = stringResource(when (phase) {
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Automatic -> R.string.ui_sheet_automatic
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Bulk -> R.string.ios_onboarding_3140100df131cf30
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Cut -> R.string.ios_onboarding_1f45f02561f41db9
    io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase.Maintain -> R.string.ios_onboarding_0f011be4a5ebbfff
})
