package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Resolve presentation text without changing the values stored or sent to the API. */
@Composable
internal fun onboardingLabel(kind: String, storedValue: String, fallback: String, description: Boolean = false): String {
    val property = if (description) "description" else "englishName"
    val id = onboardingSheetResources["$kind:$storedValue:$property"]
    return if (id == null) fallback else stringResource(id)
}

@Composable
internal fun onboardingSplitDescription(split: TrainingSplitPreference): String {
    val id = when (split) {
        TrainingSplitPreference.Automatic -> io.codepassion.doubletriangle.core.designsystem.R.string.no_fixed_split_preference__choose_the_workout_structure_that_best_matches_the_user_s_goal__schedule__and_recovery_capacity_
        TrainingSplitPreference.FullBody -> io.codepassion.doubletriangle.core.designsystem.R.string.prefer_full_body_sessions_across_the_training_week_unless_safety__equipment__or_recovery_constraints_make_a_close_variant_more_appropriate_
        TrainingSplitPreference.UpperLower -> io.codepassion.doubletriangle.core.designsystem.R.string.prefer_an_upper_lower_split_when_the_weekly_schedule_supports_it__if_the_user_has_an_odd_number_of_workout_days__adapt_sensibly_and_explain_the_rotation_
        TrainingSplitPreference.PushPullLegs -> io.codepassion.doubletriangle.core.designsystem.R.string.prefer_a_push_pull_legs_split_when_the_weekly_schedule_supports_it__if_the_user_has_fewer_than_3_weekly_sessions__fall_back_to_the_closest_practical_structure_
        TrainingSplitPreference.BodyPartSplit -> io.codepassion.doubletriangle.core.designsystem.R.string.prefer_a_body_part_split_for_advanced_hypertrophy_focused_structure_when_the_user_s_frequency_and_recovery_capacity_support_it__avoid_using_it_if_the_schedule_is_too_limited_
        TrainingSplitPreference.Custom -> io.codepassion.doubletriangle.core.designsystem.R.string.choose_the_focus_for_each_selected_training_day__the_planner_will_create_one_workout_per_selected_day_and_match_each_day_s_focus_
    }
    return stringResource(id)
}
