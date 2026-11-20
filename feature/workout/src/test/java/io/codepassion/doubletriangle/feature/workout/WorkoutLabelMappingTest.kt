package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutLabelMappingTest {
    @Test
    fun resolvesGoalNamesAndUsesSharedSheetKeys() {
        assertEquals("generalFitness", canonicalWorkoutLabelKey("Fitness general"))
        assertEquals("improveEndurance", canonicalWorkoutLabelKey("Mejorar resistencia"))
        assertEquals("improveMobility", canonicalWorkoutLabelKey("Mejorar movilidad"))
        assertEquals("ui_sheet_general_fitness", workoutLabelResourceName("generalFitness"))
        assertEquals("ui_sheet_build_muscle", workoutLabelResourceName("buildMuscle"))
        assertEquals("ui_sheet_full_body", workoutLabelResourceName("fullBody"))
        assertEquals("ui_sheet_pull", workoutLabelResourceName("pull"))
        assertEquals("workout_daytype_hypertrophy", workoutLabelResourceName("hypertrophy"))
        assertEquals(R.string.workout_daytype_hypertrophy, workoutLabelResourceId("hypertrophy"))
        assertEquals(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_pull, workoutLabelResourceId("pull"))
        assertEquals("workout_daytype_technique", workoutLabelResourceName("technique"))
        assertEquals("workout_daytype_deload", workoutLabelResourceName("deload"))
        assertEquals("workout_daytype_conditioning", workoutLabelResourceName("conditioning"))
    }

    @Test
    fun mapsPlanStatusesToExistingIosCatalogKeys() {
        assertEquals("active", canonicalWorkoutLabelKey("active"))
        assertEquals("completed", canonicalWorkoutLabelKey("Completed"))
        assertEquals("archived", canonicalWorkoutLabelKey("archivado"))
        assertEquals("draft", canonicalWorkoutLabelKey("borrador"))
    }

    @Test
    fun resolvesStoredLanguageTagsAsWellAsDisplayNames() {
        assertEquals("en", workoutLanguageTag("en-US"))
        assertEquals("fr", workoutLanguageTag("fr"))
        assertEquals("zh-Hans", workoutLanguageTag("zh-CN"))
        assertEquals("es", workoutLanguageTag("Español"))
    }

    @Test
    fun setStylesAndPhasesUseLocalizedResourcesWithoutChangingModelValues() {
        val style = io.codepassion.doubletriangle.core.model.ExerciseSetStyle.AscendingPyramid
        val phase = io.codepassion.doubletriangle.core.model.MesocyclePhase.Accumulation
        assertEquals(R.string.workout_local_pyramid, workoutSetStyleResource(style))
        assertEquals(R.string.workout_local_accumulation, workoutPhaseResource(phase))
        assertEquals("AscendingPyramid", style.name)
        assertEquals("Accumulation", phase.name)
    }

    @Test
    fun feedbackPresentationMapsStoredValuesWithoutChangingThem() {
        assertEquals(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_very_easy, feedbackTitleResource("MUY FÁCIL"))
        assertEquals(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_just_right, feedbackTitleResource("JUSTO"))
        assertEquals(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_very_hard, feedbackTitleResource("MUY DIFÍCIL"))
        assertEquals(null, feedbackTitleResource("custom"))
    }

    @Test
    fun onlyCompletedPlansUseTheHistoryCheckmark() {
        assertEquals(true, isCompletedPlanStatus("completed"))
        assertEquals(true, isCompletedPlanStatus("COMPLETED"))
        assertEquals(false, isCompletedPlanStatus("archived"))
        assertEquals(false, isCompletedPlanStatus(null))
    }
}
