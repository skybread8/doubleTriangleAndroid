package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Resolve presentation text without changing the values stored or sent to the API. */
@Composable
internal fun onboardingLabel(kind: String, storedValue: String, fallback: String, description: Boolean = false): String {
    val property = if (description) "description" else "englishName"
    val id = onboardingSheetResources["$kind:$storedValue:$property"]
    return if (id == null) fallback else stringResource(onboardingChoiceResource(id))
}

@Composable
internal fun onboardingSplitDescription(split: TrainingSplitPreference): String {
    val id = when (split) {
        TrainingSplitPreference.Automatic -> io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_no_fixed_split_preference__choose_the_workout_structure_that_best_matches_the_user_s_goal__schedule__and_recovery_capacity_
        TrainingSplitPreference.FullBody -> io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_prefer_full_body_sessions_across_the_training_week_unless_safety__equipment__or_recovery_constraints_make_a_close_variant_more_appropriate_
        TrainingSplitPreference.UpperLower -> io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_prefer_an_upper_lower_split_when_the_weekly_schedule_supports_it__if_the_user_has_an_odd_number_of_workout_days__adapt_sensibly_and_explain_the_rotation_
        TrainingSplitPreference.PushPullLegs -> io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_prefer_a_push_pull_legs_split_when_the_weekly_schedule_supports_it__if_the_user_has_fewer_than_3_weekly_sessions__fall_back_to_the_closest_practical_structure_
        TrainingSplitPreference.BodyPartSplit -> io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_prefer_a_body_part_split_for_advanced_hypertrophy_focused_structure_when_the_user_s_frequency_and_recovery_capacity_support_it__avoid_using_it_if_the_schedule_is_too_limited_
        TrainingSplitPreference.Custom -> io.codepassion.doubletriangle.core.designsystem.R.string.choose_the_focus_for_each_selected_training_day__the_planner_will_create_one_workout_per_selected_day_and_match_each_day_s_focus_
    }
    return stringResource(id)
}

/** Display fallback only; persisted keys and the generated Sheet mapping stay unchanged. */
internal fun onboardingChoiceResource(sheetId: Int): Int = when (sheetId) {
    io.codepassion.doubletriangle.core.designsystem.R.string.full_body -> R.string.onboarding_equipment_00
    io.codepassion.doubletriangle.core.designsystem.R.string.upper_lower -> R.string.onboarding_equipment_01
    io.codepassion.doubletriangle.core.designsystem.R.string.push_pull_legs -> R.string.onboarding_equipment_02
    io.codepassion.doubletriangle.core.designsystem.R.string.custom -> R.string.onboarding_equipment_03
    io.codepassion.doubletriangle.core.designsystem.R.string.bodyweight -> R.string.onboarding_equipment_04
    io.codepassion.doubletriangle.core.designsystem.R.string.resistance_bands -> R.string.onboarding_equipment_05
    io.codepassion.doubletriangle.core.designsystem.R.string.dumbbells -> R.string.onboarding_equipment_06
    io.codepassion.doubletriangle.core.designsystem.R.string.kettlebells -> R.string.onboarding_equipment_07
    io.codepassion.doubletriangle.core.designsystem.R.string.medicine_ball -> R.string.onboarding_equipment_08
    io.codepassion.doubletriangle.core.designsystem.R.string.battle_ropes -> R.string.onboarding_equipment_09
    io.codepassion.doubletriangle.core.designsystem.R.string.jump_rope -> R.string.onboarding_equipment_10
    io.codepassion.doubletriangle.core.designsystem.R.string.suspension_trainer -> R.string.onboarding_equipment_11
    io.codepassion.doubletriangle.core.designsystem.R.string.gymnastic_rings -> R.string.onboarding_equipment_12
    io.codepassion.doubletriangle.core.designsystem.R.string.cable_machine -> R.string.onboarding_equipment_13
    io.codepassion.doubletriangle.core.designsystem.R.string.smith_machine -> R.string.onboarding_equipment_14
    io.codepassion.doubletriangle.core.designsystem.R.string.leg_press_machine -> R.string.onboarding_equipment_15
    io.codepassion.doubletriangle.core.designsystem.R.string.chest_press_machine -> R.string.onboarding_equipment_16
    io.codepassion.doubletriangle.core.designsystem.R.string.leg_curl_machine -> R.string.onboarding_equipment_17
    io.codepassion.doubletriangle.core.designsystem.R.string.rear_delt_machine -> R.string.onboarding_equipment_18
    io.codepassion.doubletriangle.core.designsystem.R.string.seated_row_machine -> R.string.onboarding_equipment_19
    io.codepassion.doubletriangle.core.designsystem.R.string.glute_kickback_machine -> R.string.onboarding_equipment_20
    io.codepassion.doubletriangle.core.designsystem.R.string.pec_deck_machine -> R.string.onboarding_equipment_21
    io.codepassion.doubletriangle.core.designsystem.R.string.hip_abduction_machine -> R.string.onboarding_equipment_22
    io.codepassion.doubletriangle.core.designsystem.R.string.hip_adduction_machine -> R.string.onboarding_equipment_23
    io.codepassion.doubletriangle.core.designsystem.R.string.hack_squat_machine -> R.string.onboarding_equipment_24
    io.codepassion.doubletriangle.core.designsystem.R.string.hip_thrust_machine -> R.string.onboarding_equipment_25
    io.codepassion.doubletriangle.core.designsystem.R.string.rowing_machine -> R.string.onboarding_equipment_26
    io.codepassion.doubletriangle.core.designsystem.R.string.treadmill -> R.string.onboarding_equipment_27
    io.codepassion.doubletriangle.core.designsystem.R.string.stationary_bike -> R.string.onboarding_equipment_28
    io.codepassion.doubletriangle.core.designsystem.R.string.elliptical -> R.string.onboarding_equipment_29
    io.codepassion.doubletriangle.core.designsystem.R.string.stair_climber -> R.string.onboarding_equipment_30
    io.codepassion.doubletriangle.core.designsystem.R.string.ski_erg -> R.string.onboarding_equipment_31
    io.codepassion.doubletriangle.core.designsystem.R.string.flat_bench -> R.string.onboarding_equipment_32
    io.codepassion.doubletriangle.core.designsystem.R.string.adjustable_bench -> R.string.onboarding_equipment_33
    io.codepassion.doubletriangle.core.designsystem.R.string.squat_rack -> R.string.onboarding_equipment_34
    io.codepassion.doubletriangle.core.designsystem.R.string.olympic_barbell -> R.string.onboarding_equipment_35
    io.codepassion.doubletriangle.core.designsystem.R.string.ez_bar -> R.string.onboarding_equipment_36
    io.codepassion.doubletriangle.core.designsystem.R.string.trap_bar -> R.string.onboarding_equipment_37
    io.codepassion.doubletriangle.core.designsystem.R.string.deadlift_platform -> R.string.onboarding_equipment_38
    io.codepassion.doubletriangle.core.designsystem.R.string.dip_station -> R.string.onboarding_equipment_39
    io.codepassion.doubletriangle.core.designsystem.R.string.plyo_box -> R.string.onboarding_equipment_40
    io.codepassion.doubletriangle.core.designsystem.R.string.boxing_bag -> R.string.onboarding_equipment_41
    io.codepassion.doubletriangle.core.designsystem.R.string.landmine_attachment -> R.string.onboarding_equipment_42
    io.codepassion.doubletriangle.core.designsystem.R.string.lower_back_pain -> R.string.onboarding_equipment_43
    io.codepassion.doubletriangle.core.designsystem.R.string.shoulder_pain -> R.string.onboarding_equipment_44
    io.codepassion.doubletriangle.core.designsystem.R.string.knee_pain -> R.string.onboarding_equipment_45
    io.codepassion.doubletriangle.core.designsystem.R.string.hip_pain -> R.string.onboarding_equipment_46
    io.codepassion.doubletriangle.core.designsystem.R.string.ankle_pain -> R.string.onboarding_equipment_47
    io.codepassion.doubletriangle.core.designsystem.R.string.wrist_pain -> R.string.onboarding_equipment_48
    io.codepassion.doubletriangle.core.designsystem.R.string.elbow_pain -> R.string.onboarding_equipment_49
    io.codepassion.doubletriangle.core.designsystem.R.string.neck_pain -> R.string.onboarding_equipment_50
    io.codepassion.doubletriangle.core.designsystem.R.string.limited_shoulder_mobility -> R.string.onboarding_equipment_51
    io.codepassion.doubletriangle.core.designsystem.R.string.limited_hip_mobility -> R.string.onboarding_equipment_52
    io.codepassion.doubletriangle.core.designsystem.R.string.limited_ankle_mobility -> R.string.onboarding_equipment_53
    io.codepassion.doubletriangle.core.designsystem.R.string.limited_knee_flexion -> R.string.onboarding_equipment_54
    io.codepassion.doubletriangle.core.designsystem.R.string.overhead_movement_limitation -> R.string.onboarding_equipment_55
    io.codepassion.doubletriangle.core.designsystem.R.string.impact_sensitivity -> R.string.onboarding_equipment_56
    io.codepassion.doubletriangle.core.designsystem.R.string.legs -> R.string.onboarding_equipment_57
    io.codepassion.doubletriangle.core.designsystem.R.string.mobility -> R.string.onboarding_equipment_58
    io.codepassion.doubletriangle.core.designsystem.R.string.sedentary -> R.string.onboarding_detail_00
    io.codepassion.doubletriangle.core.designsystem.R.string.lightly_active -> R.string.onboarding_detail_01
    io.codepassion.doubletriangle.core.designsystem.R.string.moderately_active -> R.string.onboarding_detail_02
    io.codepassion.doubletriangle.core.designsystem.R.string.very_active -> R.string.onboarding_detail_03
    io.codepassion.doubletriangle.core.designsystem.R.string.spend_most_of_the_day_sitting__e_g___office_job__desk_work__ -> R.string.onboarding_detail_04
    io.codepassion.doubletriangle.core.designsystem.R.string.spend_a_good_portion_of_the_day_on_your_feet__e_g___teacher__retail__ -> R.string.onboarding_detail_05
    io.codepassion.doubletriangle.core.designsystem.R.string.spend_a_lot_of_the_day_moving__e_g___waiter__mail_carrier__ -> R.string.onboarding_detail_06
    io.codepassion.doubletriangle.core.designsystem.R.string.spend_most_of_the_day_doing_physical_labor__e_g___construction__athlete__ -> R.string.onboarding_detail_07
    io.codepassion.doubletriangle.core.designsystem.R.string.lose_weight -> R.string.onboarding_detail_08
    io.codepassion.doubletriangle.core.designsystem.R.string.build_muscle -> R.string.onboarding_detail_09
    io.codepassion.doubletriangle.core.designsystem.R.string.gain_strength -> R.string.onboarding_detail_10
    io.codepassion.doubletriangle.core.designsystem.R.string.improve_endurance -> R.string.onboarding_detail_11
    io.codepassion.doubletriangle.core.designsystem.R.string.improve_mobility -> R.string.onboarding_detail_12
    io.codepassion.doubletriangle.core.designsystem.R.string.body_recomposition -> R.string.onboarding_detail_13
    io.codepassion.doubletriangle.core.designsystem.R.string.general_fitness -> R.string.onboarding_detail_14
    io.codepassion.doubletriangle.core.designsystem.R.string.burn_fat_and_improve_metabolic_health_through_a_combination_of_cardio_and_strength_training_ -> R.string.onboarding_detail_15
    io.codepassion.doubletriangle.core.designsystem.R.string.increase_muscle_mass_and_definition_with_hypertrophy_focused_resistance_training_ -> R.string.onboarding_detail_16
    io.codepassion.doubletriangle.core.designsystem.R.string.focus_on_lifting_heavier_weights_and_improving_neuromuscular_efficiency_in_core_lifts_ -> R.string.onboarding_detail_17
    io.codepassion.doubletriangle.core.designsystem.R.string.enhance_cardiovascular_capacity_and_stamina_for_longer__more_sustained_physical_activity_ -> R.string.onboarding_detail_18
    io.codepassion.doubletriangle.core.designsystem.R.string.improve_range_of_motion__flexibility__and_joint_health_for_better_movement_and_less_pain_ -> R.string.onboarding_detail_19
    io.codepassion.doubletriangle.core.designsystem.R.string.simultaneously_lose_fat_and_gain_muscle_for_a_more_athletic_and_toned_physique_ -> R.string.onboarding_detail_20
    io.codepassion.doubletriangle.core.designsystem.R.string.maintain_a_healthy_lifestyle_and_improve_overall_well_being_with_balanced_training_ -> R.string.onboarding_detail_21
    io.codepassion.doubletriangle.core.designsystem.R.string.complete_beginner -> R.string.onboarding_detail_22
    io.codepassion.doubletriangle.core.designsystem.R.string.beginner -> R.string.onboarding_detail_23
    io.codepassion.doubletriangle.core.designsystem.R.string.novice -> R.string.onboarding_detail_24
    io.codepassion.doubletriangle.core.designsystem.R.string.intermediate -> R.string.onboarding_detail_25
    io.codepassion.doubletriangle.core.designsystem.R.string.advanced -> R.string.onboarding_detail_26
    io.codepassion.doubletriangle.core.designsystem.R.string.elite -> R.string.onboarding_detail_27
    io.codepassion.doubletriangle.core.designsystem.R.string.you_are_new_to_structured_training_and_still_learning_basic_movement_patterns_ -> R.string.onboarding_detail_28
    io.codepassion.doubletriangle.core.designsystem.R.string.you_have_some_early_experience_and_are_building_consistency__technique__and_confidence_ -> R.string.onboarding_detail_29
    io.codepassion.doubletriangle.core.designsystem.R.string.you_train_somewhat_regularly_and_can_handle_simple_progression_across_core_exercises_ -> R.string.onboarding_detail_30
    io.codepassion.doubletriangle.core.designsystem.R.string.you_have_solid_training_habits_and_can_recover_well_from_moderate_training_volume_ -> R.string.onboarding_detail_31
    io.codepassion.doubletriangle.core.designsystem.R.string.you_train_consistently__understand_progression_well__and_can_tolerate_demanding_programming_ -> R.string.onboarding_detail_32
    io.codepassion.doubletriangle.core.designsystem.R.string.you_have_a_very_high_training_base_and_can_manage_highly_specialized_or_high_performance_plans_ -> R.string.onboarding_detail_33
    io.codepassion.doubletriangle.core.designsystem.R.string.automatic -> R.string.onboarding_choice_automatic
    io.codepassion.doubletriangle.core.designsystem.R.string.body_part_split -> R.string.onboarding_choice_body_part_split
    io.codepassion.doubletriangle.core.designsystem.R.string.big_gym -> R.string.onboarding_choice_big_gym
    io.codepassion.doubletriangle.core.designsystem.R.string.small_gym -> R.string.onboarding_choice_small_gym
    io.codepassion.doubletriangle.core.designsystem.R.string.home_gym -> R.string.onboarding_choice_home_gym
    io.codepassion.doubletriangle.core.designsystem.R.string.some_accessories -> R.string.onboarding_choice_some_accessories
    io.codepassion.doubletriangle.core.designsystem.R.string.bodyweight_only -> R.string.onboarding_choice_bodyweight_only
    io.codepassion.doubletriangle.core.designsystem.R.string.pull_up_bar -> R.string.onboarding_choice_pull_up_bar
    io.codepassion.doubletriangle.core.designsystem.R.string.cardio -> R.string.onboarding_choice_cardio
    io.codepassion.doubletriangle.core.designsystem.R.string.male -> R.string.onboarding_choice_male
    io.codepassion.doubletriangle.core.designsystem.R.string.female -> R.string.onboarding_choice_female
    io.codepassion.doubletriangle.core.designsystem.R.string.upper_body -> R.string.onboarding_choice_upper_body
    io.codepassion.doubletriangle.core.designsystem.R.string.lower_body -> R.string.onboarding_choice_lower_body
    io.codepassion.doubletriangle.core.designsystem.R.string.push -> R.string.onboarding_choice_push
    io.codepassion.doubletriangle.core.designsystem.R.string.pull -> R.string.onboarding_choice_pull
    io.codepassion.doubletriangle.core.designsystem.R.string.core -> R.string.onboarding_choice_core
    io.codepassion.doubletriangle.core.designsystem.R.string.recovery -> R.string.onboarding_choice_recovery
    else -> sheetId
}
