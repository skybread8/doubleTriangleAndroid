package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExerciseReplacementRankingTest {
    private val catalog = listOf(
        ExerciseChoice("Sentadilla libre", "airSquat", primaryMuscles = setOf("quads")),
        ExerciseChoice("Sentadilla goblet", "gobletSquat", primaryMuscles = setOf("quads")),
        ExerciseChoice("Sentadilla con barra", "barbellBackSquat", listOf("barbellFrontSquat", "legPress", "gobletSquat"), setOf("quads", "glutes")),
        ExerciseChoice("Peso muerto", "deadlift", primaryMuscles = setOf("hamstrings", "glutes", "back")),
        ExerciseChoice("Press de banca", "benchPress", listOf("inclineBenchPress", "chestPressMachine", "pushUp", "dumbbellBenchPress", "smithMachineBenchPress"), setOf("chest")),
    )

    @Test
    fun `catalogues bicycle crunch muscles like iOS`() {
        assertEquals(listOf("Abdominales"), ExerciseVisualCatalog.primaryMuscleLabels("bicycleCrunch"))
        assertEquals(listOf("Oblicuos"), ExerciseVisualCatalog.metadata("bicycleCrunch")?.secondary?.map { it.label })
    }

    @Test
    fun `catalogues canonical iOS exercise identifiers`() {
        assertEquals(listOf("Pecho"), ExerciseVisualCatalog.primaryMuscleLabels("barbellBenchPress"))
        assertEquals(listOf("Espalda"), ExerciseVisualCatalog.primaryMuscleLabels("barbellBentOverRow"))
        assertEquals(listOf("Hombros"), ExerciseVisualCatalog.primaryMuscleLabels("barbellOverheadPress"))
        assertEquals(listOf("Isquiotibiales", "Glúteos"), ExerciseVisualCatalog.primaryMuscleLabels("barbellRomanianDeadlift"))
        assertEquals(listOf("Pecho"), ExerciseVisualCatalog.primaryMuscleLabels("dumbbellChestFly"))
        assertEquals(listOf("Isquiotibiales"), ExerciseVisualCatalog.primaryMuscleLabels("lyingLegCurl"))
    }

    @Test
    fun `catalogues unilateral metadata like iOS`() {
        assertEquals(true, ExerciseVisualCatalog.isUnilateral("walkingLunge"))
        assertEquals(true, ExerciseVisualCatalog.isUnilateral("sidePlank"))
        assertEquals(false, ExerciseVisualCatalog.isUnilateral("bicycleCrunch"))
    }

    @Test
    fun `does not show weight controls for bicycle crunch`() {
        assertFalse(exerciseTracksWeight(ExerciseSummary("Bicycle Crunch", "bicycleCrunch", 3, "12", 60)))
    }

    @Test
    fun `uses iOS candidate order and removes the original exercise`() {
        val replacing = ExerciseSummary("Sentadilla con barra", "barbellBackSquat", 3, "8", 90)

        val replacements = rankExerciseReplacements(catalog, replacing)

        // iOS puts barbellFrontSquat and legPress first, but neither is in
        // this local test catalogue. Goblet squat is the next available one.
        assertEquals("gobletSquat", replacements.first().imageKey)
        assertFalse(replacements.any { it.imageKey == "barbellBackSquat" })
    }

    @Test
    fun `uses the movement family before alphabetical order`() {
        val replacing = ExerciseSummary("Dominadas", "pullUp", 3, "8", 90)
        val choices = catalog + listOf(
            ExerciseChoice("Dominadas", "pullUp", listOf("chinUp", "latPulldown", "seatedCableRow"), setOf("back")),
            ExerciseChoice("Jalón al pecho", "latPulldown", primaryMuscles = setOf("back")),
            ExerciseChoice("Remo sentado", "seatedCableRow", primaryMuscles = setOf("back")),
        )

        assertEquals("latPulldown", rankExerciseReplacements(choices, replacing).first().imageKey)
    }

    @Test
    fun `separates same primary muscle exercises from all other exercises`() {
        val replacing = ExerciseSummary("Press de banca", "benchPress", 3, "8", 90)

        val sections = exerciseReplacementSections(catalog, replacing)

        assertEquals(listOf("deadlift", "barbellBackSquat", "gobletSquat", "airSquat"), sections.all.map { it.imageKey })
        assertEquals(emptyList<String>(), sections.recommendedSubstitutes.map { it.imageKey })
        assertEquals(emptyList<String>(), sections.sameMuscleGroup.map { it.imageKey })
    }
}
