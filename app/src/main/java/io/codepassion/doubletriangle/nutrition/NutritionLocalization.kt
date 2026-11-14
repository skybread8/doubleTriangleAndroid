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
    NutritionDayType.Training -> R.string.training
    NutritionDayType.Rest -> R.string.rest
    NutritionDayType.Recovery -> R.string.recovery
})

@Composable
internal fun energyDemandLabel(demand: EnergyDemand): String = stringResource(when (demand) {
    EnergyDemand.High -> R.string.high
    EnergyDemand.Medium -> R.string.medium
    EnergyDemand.Low -> R.string.low
})
