package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomWorkoutStoreTest {
    @Test fun durationIncludesWorkAndRestForEverySet() {
        val exercises = listOf(
            ExerciseSummary("Press", "benchPress", 3, "10", 90),
            ExerciseSummary("Plancha", "plank", 2, "45 s", 60),
        )
        assertEquals(11, CustomWorkoutStore.estimateMinutes(exercises))
    }

    @Test fun emptyWorkoutHasZeroDuration() {
        assertEquals(0, CustomWorkoutStore.estimateMinutes(emptyList()))
    }
}
