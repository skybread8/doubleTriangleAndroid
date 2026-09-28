package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExerciseReplacementRankingTest {
    private val catalog = listOf(
        ExerciseChoice("Sentadilla libre", "airSquat"),
        ExerciseChoice("Sentadilla goblet", "gobletSquat"),
        ExerciseChoice("Sentadilla con barra", "barbellBackSquat"),
        ExerciseChoice("Peso muerto", "deadlift"),
        ExerciseChoice("Press de banca", "benchPress"),
    )

    @Test
    fun `puts the closest movement first and removes the original exercise`() {
        val replacing = ExerciseSummary("Sentadilla con barra", "barbellBackSquat", 3, "8", 90)

        val replacements = rankExerciseReplacements(catalog, replacing)

        assertEquals("gobletSquat", replacements.first().imageKey)
        assertFalse(replacements.any { it.imageKey == "barbellBackSquat" })
    }

    @Test
    fun `uses the movement family before alphabetical order`() {
        val replacing = ExerciseSummary("Dominadas", "pullUp", 3, "8", 90)
        val choices = catalog + listOf(
            ExerciseChoice("Jalón al pecho", "latPulldown"),
            ExerciseChoice("Remo sentado", "seatedCableRow"),
        )

        assertEquals("latPulldown", rankExerciseReplacements(choices, replacing).first().imageKey)
    }
}
