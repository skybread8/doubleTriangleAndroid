package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutBlockNotesTest {
    @Test fun hidesMissingAndSerializedNullNotes() {
        listOf(null, "", "  ", "null", " NULL ").forEach { assertNull(it.visibleWorkoutBlockNotes()) }
    }

    @Test fun preservesActualNotes() {
        assertEquals("Keep a controlled pace", "Keep a controlled pace".visibleWorkoutBlockNotes())
    }
}
