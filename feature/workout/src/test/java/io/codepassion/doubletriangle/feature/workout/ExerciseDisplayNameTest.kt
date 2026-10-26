package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseDisplayNameTest {
    @Test
    fun resolvesTechnicalExerciseKeyToReadableName() {
        assertEquals("Sentadilla libre", exerciseDisplayName("airSquat", "airSquat"))
    }

    @Test
    fun resolvesLegacyNameWhenImageKeyIsMissing() {
        assertEquals("Sentadilla libre", exerciseDisplayName(null, "airSquat"))
    }

    @Test
    fun resolvesTransportVariantToReadableName() {
        assertEquals("Sentadilla libre", exerciseDisplayName("air_squat", "air_squat"))
    }

    @Test
    fun preservesExplicitCustomExerciseName() {
        assertEquals("Mi circuito", exerciseDisplayName("custom-1", "Mi circuito"))
    }
}
